package io.nextop.client.android.demo.collab;

import android.support.annotation.Nullable;
import io.nextop.client.Connection;
import io.nextop.client.ConnectionContext;
import io.nextop.client.Message;
import io.nextop.client.Path;
import io.nextop.client.android.*;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import java.util.Map;

final class Server {
    private final NxClient client;
    private final EphermeralDocumentStore store;

    private @Nullable Subscription editsGetSubscription = null;
    private @Nullable Subscription editsPostSubscription = null;


    Server(NxClient client) {
        this.client = client;
    }


    public void onResume() {
        NxUri edits = NxUri.decode("https://demo.nextop.io/collab/${id}/edits");

        if (null == editsGetSubscription) {
            editsGetSubscription = client.receiver(edits.method(NxUri.Method.GET)).flatMap(new Func1<NxMessage, Observable<NxSession<Document>>>() {
                @Override
                public Observable<NxSession<Document>> call(NxMessage m) {
                    String id = m.get("id");
                    return m.session().map(null != id ? store.get(id) : Observable.<Document>never());
                }
            }).flatMap(new Func1<NxSession<Document>, Observable<NxSession<Document.Op>>>() {
                @Override
                public Observable<NxSession<Document.Op>> call(NxSession<Document> sd) {
                    return sd.map(sd.value.getOps());
                }
            }).subscribe(new Observer<NxSession<Document.Op>>() {
                @Override
                public void onNext(NxSession<Document.Op> sop) {
                    client.sender(sop.m.id).send(sop.value.toBytes());
                }

                @Override
                public void onCompleted() {
                    // TODO
                }

                @Override
                public void onError(Throwable e) {
                    // TODO
                }
            });
        }

        if (null == editsPostSubscription) {
            editsPostSubscription = client.receiver(edits.method(NxUri.Method.POST)).flatMap(new Func1<NxMessage, Observable<NxSession<Document>>>() {
                @Override
                public Observable<NxSession<Document>> call(NxMessage m) {
                    String id = m.get("id");
                    return m.session().map(null != id ? store.get(id) : Observable.<Document>never());
                }
            }).subscribe(new Observer<NxSession<Document>>() {
                @Override
                public void onNext(NxSession<Document> sd) {
                    Document.Op op = Document.Op.fromBytes(sd.m.getBytes(NxArgs.BODY_KEY));
                    sd.value.apply(op);
                    // (note: can persist the op before ack)
                    // client.hold(sd.m.id)
                    // ...
                    // client.complete(sd.m.id)
                }

                @Override
                public void onCompleted() {
                    // TODO
                }

                @Override
                public void onError(Throwable e) {
                    // TODO
                }
            });
        }
    }

    public void onPause() {
        if (null != editsGetSubscription) {
            editsGetSubscription.unsubscribe();
            editsGetSubscription = null;
        }
        if (null != editsPostSubscription) {
            editsPostSubscription.unsubscribe();
            editsPostSubscription = null;
        }
    }

    // FIXME how to handle the case where the client with the route drops
    // FIXME nextop will elect a new route, and the route will have to use its local version of the document

    // FIXME this exercise shows what might be a good election strategy:
    // FIXME use connected time and load (or probably some combo of time to delivery and time to ack).
    // FIXME (phrase it as an optimization, where trying to min the time to ack)

    // FIXME server document store should be able to reach into the client document store in the case of a new docmument
    final class EphemeralDocumentStore {
        final Map<String, Observable<Document>> dObsMap;

        // NxUri.decode("https://demo.nextop.io/collab/${id}/ops");

        @Override
        public Observable<Document> get(String id) {
            Observable<Document> dObs = docs.get(id);
            if (null == dObs) {
                // FIXME on first subscribe,
                // FIXME build the document based on drain. when that completes, release the document

                final BehaviorSubject<Document> s = BehaviorSubject.<Document>create();


                client.receiver(NxUri.decode("POST https://demo.nextop.io/collab/${id}/ops")).drain().subscribe(

                        // FIXME when the observables goes from 1->0 subscribers, post the ops back and eject from the map
                        // FIXME not on error (work through normal and error paths; if post when back online after the route was changed, it will corrupt data)

                dObsMap.put(id, dObs);
                dObs = s;
            }
            return dObs;
        }

    }
}
