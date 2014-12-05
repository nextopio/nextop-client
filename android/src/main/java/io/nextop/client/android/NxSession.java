package io.nextop.client.android;

import rx.Observable;

public class NxSession<A> implements NxArgs {

    public final A value;
    public final NxMessage m;



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

    /////// ANDROID ///////

    public NxSession<A> set(String key, Object value) {

    }

}
