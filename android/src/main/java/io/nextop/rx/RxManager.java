package io.nextop.rx;

import com.google.common.cache.*;
import immutablecollections.ImMap;
import immutablecollections.ImSet;
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
import java.util.Iterator;

/** Base for model or view model managers. Provides a stream of objects cleanup,
 * based on a stream of updates applied to a persistent state. */
// TODO provide controls for caching the persistent state
public abstract class RxManager<M extends RxManaged> {

    private final Cache<Id, ManagedState> cachedStates;
    private ImMap<Id, ManagedState> subscribedStates;


    public RxManager() {
        cachedStates = CacheBuilder.<Id, ManagedState>newBuilder()
                .concurrencyLevel(1)
                .removalListener(new RemovalListener<Id, ManagedState>() {
                    @Override
                    public void onRemoval(RemovalNotification<Id, ManagedState> notification) {
                        ManagedState state = notification.getValue();
                        state.cached = false;
                        cleanup(state);
                    }
                }).weigher(new Weigher<Id, ManagedState>() {
                    @Override
                    public int weigh(Id key, ManagedState value) {
                        return /* FIXME */ 1;
                    }
                }).maximumWeight(/* FIXME */ 1024
                ).build();

        subscribedStates = ImMap.empty();
    }



    public Observable<M> peek(Id id) {
        @Nullable M m = peekValue(id);
        if (null != m) {
            return Observable.just(m);
        } else {
            return Observable.empty();
        }
    }

    public @Nullable M peekValue(Id id) {
        @Nullable ManagedState state = state(id, false);
        if (null != state && state.syncd) {
            return state.m;
        } else {
            return null;
        }
    }

    public Observable<M> get(Id id) {
        return getCompleteState(id).map(new Func1<ManagedState, M>() {
            @Override
            public M call(ManagedState state) {
                return state.m;
            }
        });
    }


    /** removes all subscriptions and clears the cache.
     * The manager is still usable after this operation. */
    public void clear() {
        unsubscribe();
        cachedStates.invalidateAll();
        assert 0 == cachedStates.size();
    }
    public void unsubscribe() {
        for (Iterator<ImMap.Entry<Id, ManagedState>> itr = subscribedStates.iterator(); itr.hasNext(); ) {
            itr.next().getValue().unsubscribe();
        }
        assert subscribedStates.isEmpty();
    }








    private void addSubscribedState(ManagedState state) {
        subscribedStates = subscribedStates.put(state.id, state);
        startUpdates(state.m, state);
    }
    private void removeSubscribedState(ManagedState state) {
        stopUpdates(state.id);
        subscribedStates = subscribedStates.remove(state.id);
        cleanup(state);
    }

    @Nullable
    private ManagedState state(Id id, boolean create) {
        // always test the cache first because it counts references - e.g. lru
        @Nullable ManagedState state = cachedStates.getIfPresent(id);
        if (null != state) {
            return state;
        }

        state = subscribedStates.get(id);

        if (null == state && create) {
            M m = create(id);
            if (!id.equals(m.id)) {
                throw new IllegalStateException("#create must return a managed object with the same id as input.");
            }
            state = new ManagedState(m);
            verifyState(state);
        }

        if (null != state) {
            // put it in the cache
            state.cached = true;
            cachedStates.put(id, state);
        }

        return state;
    }

    private void cleanup(ManagedState state) {
        if (!state.isCached() && !state.isSubscribed()) {
            state.close();
        }
    }




    // returned objects here are not guaranteed to have startUpdates/stopUpdates called before ejecting
    // creating these objects should have no side effects
    protected abstract M create(Id id);

    // important: this is not an update block
    // updates must be done inside of update(...) to publish correctly
    protected void startUpdates(M m, RxState state) {
        // the default action is to publish the state in memory
        // in cases where the state has to be read from
        state.sync();
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
    private void publish(ManagedState state) {
        int publishCount = ++state.publishCount;
        for (Subscriber<? super ManagedState> subscriber : state.subscribers) {
            subscriber.onNext(state);
            // a nested publish cut off this publish
            // don't publish an older notification
            if (publishCount != state.publishCount) {
                break;
            }
        }
    }


    // FIXME rename "syncd" to "sync'd"
//    protected void setSyncd(Id id) {
//        updateState(id, new Action1<ManagedState>() {
//            @Override
//            public void call(ManagedState mm) {
//                mm.syncd = true;
//            }
//        });
//    }



    protected void updateComplete(final Id id, final Func2<M, RxState, M> updater) {
        updateCompleteState(id, new UpdateAdapter(updater));
    }
    // updates the view model
    // publishes an update
    protected void update(final Id id, final Func2<M, RxState, M> updater) {
        updateState(id, new UpdateAdapter(updater));
    }
    final class UpdateAdapter implements Action1<ManagedState> {
        final Func2<M, RxState, M> updater;


        UpdateAdapter(Func2<M, RxState, M> updater) {
            this.updater = updater;
        }


        @Override
        public void call(ManagedState state) {
            @Nullable M newM = updater.call(state.m, state);
            // null returns mean "do not update reference"
            if (null != newM) {
                state.m = newM;
            }
        }
    }



    private void updateCompleteState(final Id id, final Action1<ManagedState> updater) {
        getCompleteState(id).take(1).subscribe(new UpdateStateAdapter(updater));
    }
    // updates the view model
    // publishes an update
    private void updateState(final Id id, final Action1<ManagedState> updater) {
        getState(id).take(1).subscribe(new UpdateStateAdapter(updater));
    }
    final class UpdateStateAdapter implements Observer<ManagedState> {
        final Action1<ManagedState> updater;

        @Nullable ManagedState state = null;


        UpdateStateAdapter(Action1<ManagedState> updater) {
            this.updater = updater;
        }


        @Override
        public void onNext(ManagedState state) {
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

    private Observable<ManagedState> getCompleteState(final Id id) {
        return getState(id).filter(new Func1<ManagedState, Boolean>() {
                @Override
                public Boolean call(ManagedState state) {
                    return state.syncd;
                }
        });
    }

    private Observable<ManagedState> getState(final Id id) {
        return Observable.create(new Observable.OnSubscribe<ManagedState>() {
            @Override
            public void call(final Subscriber<? super ManagedState> subscriber) {
                final ManagedState state = state(id, true);

                subscriber.add(BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        state.removeSubscriber(subscriber);
                    }
                }));

                int publishCount = state.publishCount;
                state.addSubscriber(subscriber);
                // check to avoid double-publishing
                if (publishCount == state.publishCount) {
                    subscriber.onNext(state);
                }
            }
        });
    }


    private void verifyState(ManagedState state) {
        if (null == state.m) {
            throw new IllegalStateException();
        }
        if (!state.id.equals(state.m.id)) {
            throw new IllegalStateException();
        }
    }



    private final class ManagedState implements RxState {
        public final Id id;
        public M m;
        // mark this true when the version in memory has caught up with the upstream
        // when syncd, the state will be exposed via get/peek
        public boolean syncd = false;

        ImSet<Subscriber<? super ManagedState>> subscribers = ImSet.empty();
        int refCount = 0;
        int publishCount = 0;

        boolean cached = false;


        final RxLifecycleBinder.Lifted binder = new RxLifecycleBinder.Lifted();



        public ManagedState(M m) {
            this.m = m;
            id = m.id;

            // always connected
            binder.connect(null);
        }


        boolean isCached() {
            return cached;
        }

        boolean isSubscribed() {
            return 0 < refCount;
        }


        void addSubscriber(Subscriber<? super ManagedState> subscriber) {
            subscribers = subscribers.adding(subscriber);
            if (1 == ++refCount) {
                addSubscribedState(this);
//                startUpdates(state.m, state);
            }
        }
        void removeSubscriber(Subscriber<? super ManagedState> subscriber) {
            subscribers = subscribers.removing(subscriber);
            --refCount;
            if (0 == refCount) {
                binder.reset();
                removeSubscribedState(this);
//                stopUpdates(id);
            }
        }


        void unsubscribe() {
            for (Subscriber<? super ManagedState> subscriber : subscribers) {
                subscriber.unsubscribe();
            }
        }

        void close() {
            m.close();
        }


        /////// RxState ///////

        @Override
        public <T> Observable<T> bind(Observable<T> source) {
            return binder.bind(source);
        }

        @Override
        public void bind(Subscription s) {
            binder.bind(s);
        }

        @Override
        public void sync() {
            if (!syncd) {
                syncd = true;
                publish(this);
            }
        }
    }



    public static interface RxState {
        <T> Observable<T> bind(Observable<T> source);
        void bind(Subscription s);

        /** call this when the state in memory has caught up with some version of the truth */
        void sync();
    }


}
