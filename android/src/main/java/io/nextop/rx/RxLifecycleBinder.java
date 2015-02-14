package io.nextop.rx;

import android.view.View;
import com.google.common.base.Objects;
import immutablecollections.ImSet;
import rx.*;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.observers.Subscribers;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

import javax.annotation.Nullable;

public interface RxLifecycleBinder extends Subscription {

    void reset();

    /** this version compares the current id to the given id.
     * This is useful for recycler views where tearing down and
     * setting up the same id is wasteful/visually jarring.
     * if different, does a reset and returns true.
     * if the same, does nothing and returns false. */
    boolean reset(Object id);

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
        private final Scheduler scheduler;

        @Nullable
        private Object currentId = null;

        private ImSet<Bind<?>> binds = ImSet.empty();
        private final CompositeSubscription subscriptions = new CompositeSubscription();

        private boolean connected = false;
        @Nullable
        private View connectedView = null;

        private boolean closed = false;

        @Nullable
        private Subscription cascadeSubscription = null;

        @Nullable
        private RxDebugger debugger;


        public Lifted() {
            this(RxDebugger.get());
        }

        public Lifted(@Nullable RxDebugger debugger) {
            scheduler = AndroidSchedulers.mainThread();
            this.debugger = debugger;
        }


        public void setDebugger(@Nullable RxDebugger debugger) {
            if (null != this.debugger) {
                // FIXME
                // FIXME detach
            }
            this.debugger = debugger;
            if (null != debugger) {
                // FIXME
                // FIXME attach
            }
        }

        public void connect(@Nullable View view) {
            if (closed) {
                throw new IllegalStateException();
            }
            if (!connected) {
                connected = true;
                connectedView = view;
                for (Bind<?> bind : binds) {
                    bind.connect();
                }
            }
        }
        public void disconnect() {
            if (closed) {
                throw new IllegalStateException();
            }
            if (connected) {
                connected = false;
                connectedView = null;
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
            currentId = null;
            clear();
        }

        @Override
        public boolean reset(Object id) {
            if (closed) {
                throw new IllegalStateException();
            }
            if (!Objects.equal(currentId, id)) {
                currentId = id;
                clear();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public <T> Observable<T> bind(Observable<T> source) {
            if (closed) {
                return Observable.empty();
            }
            Bind<T> bind = new Bind<T>(source.subscribeOn(scheduler));
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

        private final class Bind<T> {
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


                // DEBUG STATISTICS

                private int onNextCount = 0;
                private int onCompletedCount = 0;
                private int onErrorCount = 0;
                @Nullable
                private Notification mostRecentNotification = null;
                private int failedNotificationCount = 0;
                @Nullable
                private Notification mostRecentFailedNotification = null;
                @Nullable
                private Throwable mostRecentFailedNotificationReason = null;


                Bridge(Subscriber<? super T> subscriber) {
                    this.subscriber = subscriber;
                }


                void subscribe(Subscription subscription) {
                    unsubscribe();
                    if (subscriber.isUnsubscribed()) {
                        subscription.unsubscribe();
                    } else {
                        this.subscription = subscription;

                        if (null != debugger && debugger.isEnabled()) {
                            debugger.update(new RxDebugger.Stats(RxDebugger.Stats.F_CONNECTED, subscriber, connectedView, false, true,
                                    onNextCount, onCompletedCount, onErrorCount, mostRecentNotification,
                                    failedNotificationCount, mostRecentFailedNotification, mostRecentFailedNotificationReason));
                        }
                    }
                }

                void unsubscribe() {
                    if (null != subscription) {
                        subscription.unsubscribe();
                        subscription = null;

                        if (null != debugger && debugger.isEnabled()) {
                            debugger.update(new RxDebugger.Stats(RxDebugger.Stats.F_DISCONNECTED, subscriber, connectedView, false, false,
                                    onNextCount, onCompletedCount, onErrorCount, mostRecentNotification,
                                    failedNotificationCount, mostRecentFailedNotification, mostRecentFailedNotificationReason));
                        }
                    }
                }

                void close() {
                    unsubscribe();
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                        subscriber.unsubscribe();

                        if (null != debugger && debugger.isEnabled()) {
                            onCompletedCount += 1;
                            debugger.update(new RxDebugger.Stats(RxDebugger.Stats.F_COMPLETED, subscriber, connectedView, true, false,
                                    onNextCount, onCompletedCount, onErrorCount, mostRecentNotification,
                                    failedNotificationCount, mostRecentFailedNotification, mostRecentFailedNotificationReason));
                        }
                    }
                }


                public Subscriber inSubscriber() {
                    // if in debug, the notification is routed through the debugger
                    // which may step/filter notifications

                    final Action2<Subscriber, Notification> debugDelivery = new Action2<Subscriber, Notification>() {
                        @Override
                        public void call(Subscriber subscriber, Notification notification) {
                            if (!subscriber.isUnsubscribed()) {
                                mostRecentNotification = notification;

                                int flags = 0;

                                try {
                                    notification.accept(subscriber);
                                    switch (notification.getKind()) {
                                        case OnNext:
                                            onNextCount += 1;
                                            flags |= RxDebugger.Stats.F_NEXT;
                                            break;
                                        case OnError:
                                            onErrorCount += 1;
                                            flags |= RxDebugger.Stats.F_ERROR;
                                            break;
                                        case OnCompleted:
                                            onCompletedCount += 1;
                                            flags |= RxDebugger.Stats.F_COMPLETED;
                                            break;
                                        default:
                                            throw new IllegalArgumentException();
                                    }
                                } catch (Throwable t) {
                                    failedNotificationCount += 1;
                                    mostRecentFailedNotification = notification;
                                    mostRecentFailedNotificationReason = t;
                                    flags |= RxDebugger.Stats.F_FAILED;
                                }

                                if (null != debugger && debugger.isEnabled()) {
                                    debugger.update(new RxDebugger.Stats(flags, subscriber, connectedView, false, !subscription.isUnsubscribed(),
                                            onNextCount, onCompletedCount, onErrorCount, mostRecentNotification,
                                            failedNotificationCount, mostRecentFailedNotification, mostRecentFailedNotificationReason));
                                }
                            }
                        }
                    };

                    return Subscribers.from(new Observer<T>() {
                        @Override
                        public void onNext(T t) {
                            if (null != debugger && debugger.isEnabled()) {
                                debugger.deliver(subscriber, Notification.createOnNext(t), debugDelivery);
                            } else if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(t);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            if (null != debugger && debugger.isEnabled()) {
                                debugger.deliver(subscriber, Notification.createOnCompleted(), debugDelivery);
                            } else if (!subscriber.isUnsubscribed()) {
                                subscriber.onCompleted();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (null != debugger && debugger.isEnabled()) {
                                debugger.deliver(subscriber, Notification.createOnError(e), debugDelivery);
                            } else if (!subscriber.isUnsubscribed()) {
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
