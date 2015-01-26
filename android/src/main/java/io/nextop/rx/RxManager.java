package io.nextop.rx;

import com.google.common.collect.Sets;
import io.nextop.Id;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.SubscriptionList;
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
public abstract class RxManager<M extends RxManaged, /* FIXME remove */ MM extends RxManager.ManagedState<M>> {


    private final Map<Id, MM> states = new HashMap<Id, MM>(32);


    public Observable<M> peek(Id id) {
        @Nullable MM state = states.get(id);
        if (null != state && state.complete) {
            return Observable.just(state.m);
        }
        return Observable.empty();
    }
    public Observable<M> get(Id id) {
        return getCompleteState(id).map(new Func1<MM, M>() {
            @Override
            public M call(MM state) {
                return state.m;
            }
        });
    }




    // returned objects here are not guaranteed to have startUpdates/stopUpdates called before ejecting
    // creating these objects should have no side effects
    protected abstract MM create(Id id);
    protected void startUpdates(MM state) {
        // the default action is to publish the state in memory
        // in cases where the state has to be read from
        state.complete = true;
        publish(state);
    }
    protected void stopUpdates(MM state) {
        // Do nothing
    }


//    protected void publish(Id id) {
//        @Nullable MM state = states.get(id);
//        if (null != state) {
//            publish(state);
//        }
//    }
    protected void publish(MM state) {
        int publishCount = ++state.publishCount;
        for (Subscriber<? super MM> subscriber : state.subscribers) {
            subscriber.onNext(state);
            // a nested publish cut off this publish
            // don't publish an older notification
            if (publishCount != state.publishCount) {
                break;
            }
        }
    }


    protected void complete(Id id) {
        update(id, new Action1<MM>() {
            @Override
            public void call(MM mm) {
                mm.complete = true;
            }
        });
    }

    protected void updateComplete(final Id id, final Action1<MM> updater) {
        getCompleteState(id).take(1).subscribe(new UpdaterAdapter(updater));
    }
    // updates the view model
    // publishes an update
    protected void update(final Id id, final Action1<MM> updater) {
        getState(id).take(1).subscribe(new UpdaterAdapter(updater));
    }
    final class UpdaterAdapter implements Observer<MM> {
        final Action1<MM> updater;

        @Nullable MM state = null;


        UpdaterAdapter(Action1<MM> updater) {
            this.updater = updater;
        }


        @Override
        public void onNext(MM state) {
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

    private Observable<MM> getCompleteState(final Id id) {
        return getState(id).filter(new Func1<MM, Boolean>() {
                @Override
                public Boolean call(MM state) {
                    return state.complete;
                }
        });
    }

    private Observable<MM> getState(final Id id) {
        return Observable.create(new Observable.OnSubscribe<MM>() {
            @Override
            public void call(final Subscriber<? super MM> subscriber) {
                final MM state = createState(id);

                subscriber.add(BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        state.subscribers.remove(subscriber);
                        --state.refCount;
                        if (0 == state.refCount) {
                            stopUpdates(state);
                        }
                    }
                }));
                int publishCount = state.publishCount;

                state.subscribers.add((Subscriber<ManagedState<M>>) subscriber);
                if (1 == ++state.refCount) {
                    startUpdates(state);
                }

                // a nested publish cut off this publish
                // don't publish an older notification
                if (publishCount == state.publishCount) {
                    publish(state);
                }
            }
        });
    }
    private MM createState(Id id) {
        MM state = states.get(id);
        if (null != state) {
            return state;
        }
        state = create(id);
        if (!id.equals(state.id)) {
            throw new IllegalStateException("#create must return a managed object with the same id as input.");
        }
        verifyState(state);
        states.put(id, state);
        return state;
    }


    private void verifyState(MM state) {
        if (null == state.m) {
            throw new IllegalStateException();
        }
    }



    // FIXME bring this back internally
    public static class ManagedState<M extends RxManaged> {
        public final Id id;
        public final SubscriptionList subscriptions;
        public M m;
        // mark this true when the version in memory has caught up with the upstream
        // when complete, the state will be exposed via get/peek
        public boolean complete = false;

        Set<Subscriber<? super ManagedState<M>>> subscribers = Sets.newIdentityHashSet();
        int refCount = 0;
        int publishCount = 0;

        public ManagedState(M m) {
            this.m = m;
            id = m.id;
            subscriptions = new SubscriptionList();
        }
    }


}
