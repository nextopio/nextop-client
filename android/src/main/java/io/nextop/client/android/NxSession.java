package io.nextop.client.android;

import rx.Observable;

// a message id bound to a client
// use this to maintain state for processing a message
public class NxSession<M> implements NxArgs {

    final NxUri id;
    final NxClient client;
    public final M v;

    // FIXME this should be bound to a client
    // FIXME but how does this work if the client is an activity/fragment and goes away under the session?
    // FIXME should be bound to a context?
    // FIXME this should be bound to a client. the client should save messages when disconnected.




    public <B> NxSession<B> map(B bValue) {
        // FIXME
    }

    public <B> Observable<NxSession<B>> map(Observable<? extends B> bObs) {
        // FIXME
    }



    // FIXME all the get methods
    public NxSession<A> clear() {

    }
    public NxSession<A> remove(String key) {

    }
    public NxSession<A> set(String key, NxByteString value) {

    }
    public NxSession<A> set(String key, String value) {

    }
    public NxSession<A> set(String key, int value) {

    }
    public NxSession<A> set(String key, long value) {

    }
    public NxSession<A> set(String key, long value) {

    }


    // CLIENT


    Sender<NxMessage> sender();

    void nack();
    void ack();

    void cancel();
    void complete();




    /////// ANDROID ///////

    public NxSession<A> set(String key, Object value) {

    }

}
