package io.nextop.client;

import com.google.common.collect.*;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SubjectNode extends AbstractMessageControlNode {
    private MessageControlNode downstream;

    private ListMultimap<Route, Subscriber> receivers = ArrayListMultimap.create();
    private List<Subscriber> defaultReceivers = new ArrayList<Subscriber>();



    public SubjectNode(MessageControlNode downstream) {
        this.downstream = downstream;
    }


    // FIXME need initDownstream


    // cases:
    // 1. GET X, hits cache before subscribe
    //


    public void send(Message message) {
        onMessageControl(new MessageControl(MessageControl.Type.SEND, message));
    }


    // can only have one active subscriber per nurl
    // if attempt to subscribe more, the 2nd+ will not receive anything (until each becomes first in the list)
    // this rule makes it easier to reason about ack behavior
    // FIXME use NURL+priority here
    public Observable<Message> receive(final Route route) {
        return Observable.create(new Observable.OnSubscribe<Message>() {
            @Override
            public void call(final Subscriber<? super Message> subscriber) {
                boolean s = receivers.put(route, subscriber);
                assert s;
                Subscription subscription = BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        boolean s = receivers.remove(route, subscriber);
                        assert s;

                        downstream.post(new Runnable() {
                            @Override
                            public void run() {
                                onMessageControl(new MessageControl(MessageControl.Type.UNSUBSCRIBE,
                                        Message.newBuilder().setRoute(route).build()));
                                // FIXME CHANGE_SUBSCRIPTION with new spec if another instead of unsubscribe (?)
                            }
                        });
                    }
                });
                subscriber.add(subscription);
                assert !subscription.isUnsubscribed();
                // FIXME only send if not already subscribed (?)
                downstream.post(new Runnable() {
                    @Override
                    public void run() {
                        onMessageControl(new MessageControl(MessageControl.Type.SUBSCRIBE,
                                Message.newBuilder().setRoute(route).build()));
                    }
                });
            }
        });
    }

    // FIXME going to have to figure out if it is OK to send multiple subscriptions
    // FIXME this has to do with timeouts too. figure out later



    // FIXME defaultReceive is a kind of hack (?)
    // when a listener is added, only new values are surfaced to it (old values are not surfaced)
    public Observable<Message> defaultReceive() {
        return Observable.create(new Observable.OnSubscribe<Message>() {
            @Override
            public void call(final Subscriber<? super Message> subscriber) {
                boolean s = defaultReceivers.add(subscriber);
                assert s;
                Subscription subscription = BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        boolean s = defaultReceivers.remove(subscriber);
                        assert s;
                    }
                });
                subscriber.add(subscription);
                assert !subscription.isUnsubscribed();
            }
        });
    }


    public void cancelSend(final Id id) {
        downstream.post(new Runnable() {
            @Override
            public void run() {
                onMessageControl(new MessageControl(MessageControl.Type.SEND_NACK,
                        Message.newBuilder().setRoute(Message.outboxRoute(id)).build()));
            }
        });
    }






    @Override
    protected void initDownstream() {
        downstream.init(new MessageControlChannel() {
            @Override
            public void onActive(boolean active, MessageControlMetrics metrics) {
                upstream.onActive(active, metrics);
            }

            @Override
            public void onTransfer(MessageControlState mcs) {
                upstream.onTransfer(mcs);
            }

            @Override
            public void onMessageControl(MessageControl mc) {
                switch (mc.type) {
                    case RECEIVE: {
                        @Nullable Subscriber firstSubscriber = Iterables.getFirst(
                                Iterables.concat(receivers.get(mc.message.route), defaultReceivers),
                                null);
                        if (null != firstSubscriber) {
                            try {
                                firstSubscriber.onNext(mc.message);
                                onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ACK,
                                        Message.newBuilder().setRoute(Message.outboxRoute(mc.message.id)).build()));
                            } catch (Throwable t) {
                                onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_NACK,
                                        Message.newBuilder().setRoute(Message.outboxRoute(mc.message.id)).build()));
                                // FIXME log
                            }
                        }
                        // else the downstream will resend on subscribe
                        break;
                    }
                    case RECEIVE_ERROR: {
                        @Nullable Subscriber firstSubscriber = Iterables.getFirst(
                                Iterables.concat(receivers.get(mc.message.route), defaultReceivers),
                                null);
                        if (null != firstSubscriber) {
                            try {
                                firstSubscriber.onError(new ReceiveException(mc.message));
                                onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ACK,
                                        Message.newBuilder().setRoute(Message.outboxRoute(mc.message.id)).build()));
                            } catch (Throwable t) {
                                onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_NACK,
                                        Message.newBuilder().setRoute(Message.outboxRoute(mc.message.id)).build()));
                                // FIXME log
                            }
                        }
                        // else the downstream will resend on subscribe
                        break;
                    }
                }
            }

            @Override
            public void post(Runnable r) {
                upstream.post(r);
            }

            @Override
            public void postDelayed(Runnable r, int delayMs) {
                upstream.postDelayed(r, delayMs);
            }
        });
    }

    @Override
    protected void startDownstream() {
        downstream.start();
    }

    @Override
    protected void stopDownstream() {
        downstream.stop();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        downstream.onActive(active, metrics);
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        downstream.onTransfer(mcs);
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        downstream.onMessageControl(mc);
    }




    public static final class ReceiveException extends Exception {
        public final Message message;

        private ReceiveException(Message message) {
            this.message = message;
        }
    }

}
