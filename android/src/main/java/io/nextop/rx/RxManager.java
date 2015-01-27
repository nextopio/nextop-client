package io.nextop.rx;

import com.google.common.collect.Sets;
import io.nextop.Id;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.BooleanSubscription;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Base for model or view model managers. Provides a stream of objects out,
 * based on a stream of updates applied to a persistent state.
 *
 * TODO provide controls for caching the persistent state
 */
// FIXME never expose ManagedState, just thread id+subscriptions or vm+subscriptions
public abstract class RxManager<M extends RxManaged> {


    private final Map<Id, ManagedState<M>> states = new HashMap<Id, ManagedState<M>>(32);


    public Observable<M> peek(Id id) {
        @Nullable ManagedState<M> state = states.get(id);
        if (null != state && state.complete) {
            return Observable.just(state.m);
        }
        return Observable.empty();
    }
    public Observable<M> get(Id id) {
        return getCompleteState(id).map(new Func1<ManagedState<M>, M>() {
            @Override
            public M call(ManagedState<M> state) {
                return state.m;
            }
        });
    }




    // returned objects here are not guaranteed to have startUpdates/stopUpdates called before ejecting
    // creating these objects should have no side effects
    protected abstract M create(Id id);
    protected void startUpdates(M m, RxState state) {
        // the default action is to publish the state in memory
        // in cases where the state has to be read from
        complete(m.id);
    }
    protected void stopUpdates(Id id) {
        // Do nothing
    }
    // FIXME close callback for state
    // FIXME close callback on RxManaged


//    protected void publish(Id id) {
//        @Nullable MM state = states.get(id);
//        if (null != state) {
//            publish(state);
//        }
//    }
    private void publish(ManagedState<M> state) {
        int publishCount = ++state.publishCount;
        for (Subscriber<? super ManagedState<M>> subscriber : state.subscribers) {
            subscriber.onNext(state);
            // a nested publish cut off this publish
            // don't publish an older notification
            if (publishCount != state.publishCount) {
                break;
            }
        }
    }


    protected void complete(Id id) {
        updateState(id, new Action1<ManagedState<M>>() {
            @Override
            public void call(ManagedState<M> mm) {
                mm.complete = true;
            }
        });
    }



    protected void updateComplete(final Id id, final Func2<M, RxState, M> updater) {
        updateCompleteState(id, new UpdateAdapter(updater));
    }
    // updates the view model
    // publishes an update
    protected void update(final Id id, final Func2<M, RxState, M> updater) {
        updateState(id, new UpdateAdapter(updater));
    }
    final class UpdateAdapter implements Action1<ManagedState<M>> {
        final Func2<M, RxState, M> updater;


        UpdateAdapter(Func2<M, RxState, M> updater) {
            this.updater = updater;
        }


        @Override
        public void call(ManagedState<M> state) {
            state.m = updater.call(state.m, state);
        }
    }



    private void updateCompleteState(final Id id, final Action1<ManagedState<M>> updater) {
        getCompleteState(id).take(1).subscribe(new UpdateStateAdapter(updater));
    }
    // updates the view model
    // publishes an update
    private void updateState(final Id id, final Action1<ManagedState<M>> updater) {
        getState(id).take(1).subscribe(new UpdateStateAdapter(updater));
    }
    final class UpdateStateAdapter implements Observer<ManagedState<M>> {
        final Action1<ManagedState<M>> updater;

        @Nullable ManagedState<M> state = null;


        UpdateStateAdapter(Action1<ManagedState<M>> updater) {
            this.updater = updater;
        }


        @Override
        public void onNext(ManagedState<M> state) {
            // apply at most once
            if (null != this.state) {
                throw new IllegalStateException();
            }
            this.state = state;
            updater.call(state);
            verifyState(state);
        }

        @Override
        public void onCompleted() {
            if (null != state) {
                publish(state);
            }
        }

        @Override
        public void onError(Throwable e) {
            // TODO log
        }
    }

    private Observable<ManagedState<M>> getCompleteState(final Id id) {
        return getState(id).filter(new Func1<ManagedState<M>, Boolean>() {
                @Override
                public Boolean call(ManagedState<M> state) {
                    return state.complete;
                }
        });
    }

    private Observable<ManagedState<M>> getState(final Id id) {
        return Observable.create(new Observable.OnSubscribe<ManagedState<M>>() {
            @Override
            public void call(final Subscriber<? super ManagedState<M>> subscriber) {
                final ManagedState<M> state = createState(id);

                subscriber.add(BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        state.subscribers.remove(subscriber);
                        --state.refCount;
                        if (0 == state.refCount) {
                            state.binder.reset();
                            stopUpdates(state.id);
                        }
                    }
                }));
                int publishCount = state.publishCount;

                state.subscribers.add((Subscriber<ManagedState<M>>) subscriber);
                if (1 == ++state.refCount) {
                    startUpdates(state.m, state);
                }

                // a nested publish cut off this publish
                // don't publish an older notification
                if (publishCount == state.publishCount) {
                    publish(state);
                }
            }
        });
    }
    private ManagedState<M> createState(Id id) {
        ManagedState<M> state = states.get(id);
        if (null != state) {
            return state;
        }
        M m = create(id);
        if (!id.equals(m.id)) {
            throw new IllegalStateException("#create must return a managed object with the same id as input.");
        }
        state = new ManagedState<M>(m);
        verifyState(state);
        states.put(id, state);
        return state;
    }


    private void verifyState(ManagedState<M> state) {
        if (null == state.m) {
            throw new IllegalStateException();
        }
        if (!state.id.equals(state.m.id)) {
            throw new IllegalStateException();
        }
    }



    private static final class ManagedState<M extends RxManaged> implements RxState {
        public final Id id;
        public M m;
        // mark this true when the version in memory has caught up with the upstream
        // when complete, the state will be exposed via get/peek
        public boolean complete = false;

        Set<Subscriber<? super ManagedState<M>>> subscribers = Sets.newIdentityHashSet();
        int refCount = 0;
        int publishCount = 0;


        final RxLifecycleBinder binder = new RxLifecycleBinder.Lifted();



        public ManagedState(M m) {
            this.m = m;
            id = m.id;
        }


        /////// RxState ///////

        @Override
        public <T> Observable<T> add(Observable<T> source) {
            return binder.bind(source);
        }

        @Override
        public void add(Subscription s) {
            binder.bind(s);
        }
    }



    public static interface RxState {
        <T> Observable<T> add(Observable<T> source);
        void add(Subscription s);
    }


}
