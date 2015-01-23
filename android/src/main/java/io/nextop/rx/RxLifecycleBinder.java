package io.nextop.rx;

import immutablecollections.ImSet;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.subscriptions.BooleanSubscription;

import javax.annotation.Nullable;

public interface RxLifecycleBinder {

    void reset();

    /** Wraps the given observable in an observable bound to the lifecyle of a container.
     * The wrapper allocates on subscribe and cleans up on unsubscribe.
     * Unsubscription of all can be forced with {@link #reset}.
     *
     * This ensures:
     * - the subscription is connected to the source when started
     * - the subscription is dropped from the source when stopped
     *   (onComplete is not called, because the subscription will resume when restarted) */
    <T> Observable<T> bind(Observable<T> source);




    /** Binds to an internal lifecycle onStart/onStop. */
    final class Lifted implements RxLifecycleBinder {

        private ImSet<Bind<?>> binds = ImSet.empty();

        private boolean connected = false;
        private boolean closed = false;

        @Nullable
        private Subscription cascadeDestroySubscription = null;


        public Lifted() {
        }


        public void onStart() {
            if (closed) {
                throw new IllegalStateException();
            }
            if (!connected) {
                connected = true;
                for (Bind<?> bind : binds) {
                    bind.connect();
                }
            }
        }
        public void onStop() {
            if (closed) {
                throw new IllegalStateException();
            }
            if (connected) {
                connected = false;
                for (Bind<?> bind : binds) {
                    bind.disconnect();
                }
            }
        }
        public void onDestroy() {
            if (closed) {
                throw new IllegalStateException();
            }
            closed = true;
            removeCascadeDestroy();
            ImSet<Bind<?>> _binds = binds;
            binds = ImSet.empty();
            for (Bind bind : _binds) {
                bind.close();
            }
        }


        public void removeCascadeDestroy() {
            if (null != cascadeDestroySubscription) {
                cascadeDestroySubscription.unsubscribe();
                cascadeDestroySubscription = null;
            }
        }

        // replaces previous cascade parent
        public void cascadeDestroy(@Nullable RxLifecycleBinder parent) {
            removeCascadeDestroy();
            if (null != parent) {
                cascadeDestroySubscription = parent.bind(MoreRx.hanging()).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        onDestroy();
                    }
                }).subscribe();
            }
        }



        @Override
        public void reset() {
            if (closed) {
                throw new IllegalStateException();
            }
            ImSet<Bind<?>> _binds = binds;
            binds = ImSet.empty();
            for (Bind bind : _binds) {
                bind.close();
            }
        }

        @Override
        public <T> Observable<T> bind(Observable<T> source) {
            if (closed) {
                throw new IllegalStateException();
            }
            Bind<T> bind = new Bind<T>(source);
            binds = binds.adding(bind);
            if (connected) {
                bind.connect();
            }
            return bind.adapter;
        }


        private static final class Bind<T> {
            private final Observable<T> source;
            final Observable<T> adapter;

            private boolean closed = false;
            private boolean connected;

            private ImSet<Bridge> subscribers = ImSet.empty();


            Bind(Observable<T> source) {
                this.source = source;

                adapter = Observable.create(new Observable.OnSubscribe<T>() {
                    @Override
                    public void call(Subscriber<? super T> subscriber) {
                        add(subscriber);
                    }
                });
            }

            final class Bridge {
                final Subscriber<? super T> subscriber;
                private @Nullable Subscription subscription = null;


                Bridge(Subscriber<? super T> subscriber) {
                    this.subscriber = subscriber;
                }


                void subscribe(Subscription subscription) {
                    unsubscribe();
                    if (subscriber.isUnsubscribed()) {
                        subscription.unsubscribe();
                    } else {
                        this.subscription = subscription;
                    }
                }

                void unsubscribe() {
                    if (null != subscription) {
                        subscription.unsubscribe();
                        subscription = null;
                    }
                }

                void close() {
                    unsubscribe();
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                        subscriber.unsubscribe();
                    }
                }


                public Subscriber inSubscriber() {
                    return Subscribers.from(new Observer<T>() {
                        @Override
                        public void onNext(T t) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(t);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onCompleted();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(e);
                            }
                        }
                    });
                }

                public Subscription outSubscription() {
                    return BooleanSubscription.create(new Action0() {
                        @Override
                        public void call() {
                            unsubscribe();
                            subscribers = subscribers.removing(Bridge.this);
                        }
                    });
                }





            };



            private void add(final Subscriber<? super T> subscriber) {
                Bridge bridge = new Bridge(subscriber);
                subscribers = subscribers.adding(bridge);

                subscriber.add(bridge.outSubscription());
                connect(bridge);
            }

            void connect() {
                if (closed) {
                    throw new IllegalStateException();
                }
                if (!connected) {
                    connected = true;
                    for (Bridge bridge : subscribers) {
                        connect(bridge);
                    }
                }
            }
            void connect(Bridge bridge) {
                if (connected && !bridge.subscriber.isUnsubscribed()) {
                    bridge.subscribe(source.subscribe(bridge.inSubscriber()));
                }
            }


            void disconnect() {
                if (closed) {
                    throw new IllegalStateException();
                }
                if (connected) {
                    connected = false;
                    for (Bridge bridge : subscribers) {
                        bridge.unsubscribe();
                    }
                }
            }

            // implies disconnect
            void close() {
                if (closed) {
                    throw new IllegalStateException();
                }
                disconnect();
                if (!closed) {
                    closed = true;
                    for (Bridge bridge : subscribers) {
                        bridge.close();
                    }
                }
            }
        }
    }
}
