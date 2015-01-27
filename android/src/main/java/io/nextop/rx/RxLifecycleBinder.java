package io.nextop.rx;

import immutablecollections.ImSet;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

import javax.annotation.Nullable;

public interface RxLifecycleBinder extends Subscription {

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
    void bind(Subscription sub);




    /** Binds to an internal lifecycle start/stop. */
    final class Lifted implements RxLifecycleBinder {

        private ImSet<Bind<?>> binds = ImSet.empty();
        private final CompositeSubscription subscriptions = new CompositeSubscription();

        private boolean connected = false;
        private boolean closed = false;

        @Nullable
        private Subscription cascadeSubscription = null;


        public Lifted() {
        }


        public void start() {
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
        public void stop() {
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

        public void close() {
            if (closed) {
                throw new IllegalStateException();
            }
            closed = true;
            removeCascadeUnsubscribe();
            clear();
            subscriptions.unsubscribe();
        }


        public void removeCascadeUnsubscribe() {
            if (null != cascadeSubscription) {
                cascadeSubscription.unsubscribe();
                cascadeSubscription = null;
            }
        }

        // replaces previous cascade parent
        public void cascadeUnsubscribe(@Nullable RxLifecycleBinder parent) {
            removeCascadeUnsubscribe();
            if (null != parent) {
                cascadeSubscription = parent.bind(MoreObservables.hanging()).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        unsubscribe();
                    }
                }).subscribe();
            }
        }


        private void clear() {
            ImSet<Bind<?>> _binds = binds;
            binds = ImSet.empty();
            for (Bind bind : _binds) {
                bind.close();
            }
            subscriptions.clear();
        }



        /////// Subscription ///////

        @Override
        public void unsubscribe() {
            close();
        }

        @Override
        public boolean isUnsubscribed() {
            return closed;
        }

        /////// RxLifecycleBinder ///////


        @Override
        public void reset() {
            if (closed) {
                throw new IllegalStateException();
            }
            clear();
        }

        @Override
        public <T> Observable<T> bind(Observable<T> source) {
            if (closed) {
                return Observable.empty();
            }
            Bind<T> bind = new Bind<T>(source);
            binds = binds.adding(bind);
            if (connected) {
                bind.connect();
            }
            return bind.adapter;
        }

        @Override
        public void bind(Subscription sub) {
            if (closed) {
                sub.unsubscribe();
            } else {
                subscriptions.add(sub);
            }
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
