package io.nextop.client;

import com.google.common.collect.*;
import io.nextop.Message;
import io.nextop.Nurl;
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

    private ListMultimap<Nurl, Subscriber> receivers = ArrayListMultimap.create();
    private List<Subscriber> defaultReceivers = new ArrayList<Subscriber>();



    public SubjectNode(MessageControlNode downstream) {
        this.downstream = downstream;
    }


    // FIXME need initDownstream


    // cases:
    // 1. GET X, hits cache before subscribe
    //


    public void send(Message message) {
        downstream.onMessageControl(new MessageControl(MessageControl.Type.SEND, message));
    }


    // can only have one active subscriber per nurl
    // if attempt to subscribe more, the 2nd+ will not receive anything (until each becomes first in the list)
    // this rule makes it easier to reason about ack behavior
    // FIXME use NURL+priority here
    public Observable<Message> receive(final Message spec) {
        return Observable.create(new Observable.OnSubscribe<Message>() {
            @Override
            public void call(final Subscriber<? super Message> subscriber) {
                boolean s = receivers.put(spec.nurl, subscriber);
                assert s;
                Subscription subscription = BooleanSubscription.create(new Action0() {
                    @Override
                    public void call() {
                        boolean s = receivers.remove(spec.nurl, subscriber);
                        assert s;
                        downstream.onMessageControl(new MessageControl(MessageControl.Type.UNSUBSCRIBE, spec));
                        // FIXME CHANGE_SUBSCRIPTION with new spec if another instead of unsubscribe (?)
                    }
                });
                subscriber.add(subscription);
                assert !subscription.isUnsubscribed();
                // FIXME only send if not already subscribed (?)
                downstream.onMessageControl(new MessageControl(MessageControl.Type.SUBSCRIBE, spec));
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


    public void cancelSend(Message spec) {
        downstream.onMessageControl(new MessageControl(MessageControl.Type.SEND_NACK, spec));
    }




    @Override
    public void onMessageControl(MessageControl mc) {
        switch (mc.type) {
            case RECEIVE: {
                @Nullable Subscriber firstSubscriber = Iterables.getFirst(
                        Iterables.concat(receivers.get(mc.message.nurl), defaultReceivers),
                        null);
                if (null != firstSubscriber) {
                    try {
                        firstSubscriber.onNext(mc.message);
                        downstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ACK, mc.message));
                    } catch (Throwable t) {
                        downstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_NACK, mc.message));
                        // FIXME log
                    }
                }
                // else the downstream will resend on subscribe
                break;
            }
            case RECEIVE_ERROR: {
                @Nullable Subscriber firstSubscriber = Iterables.getFirst(
                        Iterables.concat(receivers.get(mc.message.nurl), defaultReceivers),
                        null);
                if (null != firstSubscriber) {
                    try {
                        firstSubscriber.onError(new ReceiveException(mc.message));
                        downstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ACK, mc.message));
                    } catch (Throwable t) {
                        downstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_NACK, mc.message));
                        // FIXME log
                    }
                }
                // else the downstream will resend on subscribe
                break;
            }
        }
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        upstream.onActive(active, metrics);
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        upstream.onTransfer(mcs);
    }



    public static final class ReceiveException extends Exception {
        public final Message message;

        private ReceiveException(Message message) {
            this.message = message;
        }
    }

}
