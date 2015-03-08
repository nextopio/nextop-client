package io.nextop.client.node;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.client.*;
import io.nextop.log.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class Head implements MessageControlNode, Log.Out {

    public static Head create(MessageContext context, MessageControlState mcs, MessageControlNode downstream,
                                            Scheduler callbackScheduler) {
        return new Head(context, mcs, downstream, callbackScheduler);
    }




    final MessageContext context;
    final MessageControlState mcs;
    final MessageControlNode downstream;

    // callbacks are called on this scheduler
    // this is important for ordering, so that send, receive.subscribe from the callback thread
    // are correctly subscribed in all cases (always call into the head on the callback thread to get this safety)
    final Scheduler callbackScheduler;
    final Scheduler.Worker callbackWorker;


    final Object receiverMutex = new Object();
    final ListMultimap<Route, Subscriber> receivers = ArrayListMultimap.create();
    final List<Subscriber> defaultReceivers = new ArrayList<Subscriber>();



    Head(MessageContext context, MessageControlState mcs, MessageControlNode downstream, Scheduler callbackScheduler) {
        this.context = context;
        this.mcs = mcs;
        this.downstream = downstream;
        this.callbackScheduler = callbackScheduler;
        callbackWorker = callbackScheduler.createWorker();
    }


    /* threading notes:
     * if calling from a thread that is not the callback thread,
     * receive.subscribe ... send to ensure
     * responses get delivered to the receiver.
     * Calling send ... receive.subscribe needs to be done
     * on the same execution of the callback thread
     * to ensure responses get delivered to the receiver.
     */


    /** thread-safe */
    public void send(final Message message) {
        mcs.notifyPending(message.id);
        post(new Runnable() {
            @Override
            public void run() {
                onMessageControl(MessageControl.send(message));
            }
        });
    }

    /** thread-safe */
    public void complete(final Message message) {
        mcs.notifyPending(message.id);
        post(new Runnable() {
            @Override
            public void run() {
                onMessageControl(MessageControl.send(MessageControl.Type.COMPLETE, message));
            }
        });
    }

    /** thread-safe */
    public void error(final Message message) {
        mcs.notifyPending(message.id);
        post(new Runnable() {
            @Override
            public void run() {
                onMessageControl(MessageControl.send(MessageControl.Type.ERROR, message));
            }
        });
    }

    /** thread-safe */
    public void cancelSend(final Id id) {
        post(new Runnable() {
            @Override
            public void run() {
                onMessageControl(MessageControl.send(MessageControl.Type.ERROR, Message.outboxRoute(id)));
            }
        });
    }

    /** thread-safe */
    public Observable<Message> receive(final Route route) {
        return Observable.create(new Observable.OnSubscribe<Message>() {
            @Override
            public void call(final Subscriber<? super Message> subscriber) {
                synchronized (receiverMutex) {
                    boolean s = receivers.put(route, subscriber);
                    assert s;
                }
                Subscription subscription = Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        synchronized (receiverMutex) {
                            boolean s = receivers.remove(route, subscriber);
                            assert s;
                        }
                    }
                });
                subscriber.add(subscription);
                assert !subscription.isUnsubscribed();
            }
        });
    }



    // when a listener is added, only new values are surfaced to it (old values are not surfaced)
    /** thread-safe */
    public Observable<Message> defaultReceive() {
        return Observable.create(new Observable.OnSubscribe<Message>() {
            @Override
            public void call(final Subscriber<? super Message> subscriber) {
                synchronized (receiverMutex) {
                    boolean s = defaultReceivers.add(subscriber);
                    assert s;
                }
                Subscription subscription = Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        synchronized (receiverMutex) {
                            boolean s = defaultReceivers.remove(subscriber);
                            assert s;
                        }
                    }
                });
                subscriber.add(subscription);
                assert !subscription.isUnsubscribed();
            }
        });
    }



    /** thread-safe */
    public void init(final @Nullable Bundle savedState) {
        post(new Runnable() {
            @Override
            public void run() {
                init(null, savedState);
            }
        });
    }

    /** thread-safe */
    public void start() {
        post(new Runnable() {
            @Override
            public void run() {
                onActive(true);
            }
        });
    }

    /** thread-safe */
    public void stop() {
        post(new Runnable() {
            @Override
            public void run() {
                onActive(false);
            }
        });
    }




    /////// MessageControlNode IMPLEMENTATION ///////

    @Override
    public void init(@Nullable final MessageControlChannel upstream, @Nullable Bundle savedState) {
        if (null != upstream) {
            throw new IllegalArgumentException();
        }

        downstream.init(new MessageControlChannel() {
            @Override
            public MessageControlState getMessageControlState() {
                return Head.this.getMessageControlState();
            }

            @Override
            public void onActive(boolean active) {
                /* ignore */
            }

            @Override
            public void onMessageControl(final MessageControl mc) {
                switch (mc.type) {
                    case MESSAGE: {
                        callbackWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                @Nullable Subscriber firstSubscriber;
                                synchronized (receiverMutex) {
                                    firstSubscriber = Iterables.getFirst(
                                            Iterables.concat(receivers.get(mc.message.route), defaultReceivers),
                                            null);
                                }
                                if (null != firstSubscriber) {
                                    firstSubscriber.onNext(mc.message);
                                }
                            }
                        });
                        break;
                    }
                    case COMPLETE: {
                        callbackWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                @Nullable Subscriber firstSubscriber;
                                synchronized (receiverMutex) {
                                    firstSubscriber = Iterables.getFirst(receivers.get(mc.message.route), null);
                                }
                                if (null != firstSubscriber) {
                                    firstSubscriber.onCompleted();
                                }
                            }
                        });
                        break;
                    }
                    case ERROR: {
                        callbackWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                @Nullable Subscriber firstSubscriber;
                                synchronized (receiverMutex) {
                                    firstSubscriber = Iterables.getFirst(receivers.get(mc.message.route), null);
                                }
                                if (null != firstSubscriber) {
                                    firstSubscriber.onError(new ReceiveException(mc.message));
                                }
                            }
                        });
                        break;
                    }
                }
            }

            @Override
            public void post(Runnable r) {
                Head.this.post(r);
            }

            @Override
            public void postDelayed(Runnable r, int delayMs) {
                Head.this.postDelayed(r, delayMs);
            }

            @Override
            public Scheduler getScheduler() {
                return Head.this.getScheduler();
            }
        }, savedState);
    }

    @Override
    public void onSaveState(Bundle savedState) {
        downstream.onSaveState(savedState);
    }


    /////// MessageControlChannel IMPLEMENTATION ///////

    public void onActive(boolean active) {
        downstream.onActive(active);
    }

    public void onMessageControl(MessageControl mc) {
        downstream.onMessageControl(mc);
    }

    @Override
    public MessageControlState getMessageControlState() {
        return mcs;
    }


    /////// MessageContext IMPLEMENTATION ///////

    public void post(Runnable r) {
        context.post(r);
    }

    public void postDelayed(Runnable r, int delayMs) {
        context.postDelayed(r, delayMs);
    }

    public Scheduler getScheduler() {
        return context.getScheduler();
    }



    public static final class ReceiveException extends Exception {
        public final Message message;

        private ReceiveException(Message message) {
            this.message = message;
        }
    }

}
