package io.nextop.client.android.demo.collab;

import io.nextop.client.Connection;
import io.nextop.client.ConnectionContext;
import io.nextop.client.Message;
import io.nextop.client.Path;
import io.nextop.client.android.NxClient;
import io.nextop.client.android.NxMessage;
import io.nextop.client.android.NxUri;
import rx.Observable;
import rx.Observer;

final class Server {
    private final NxClient client;
    private final DocumentStore store;


    Server(NxClient client, DocumentStore store) {
        this.client = client;
        this.store = store;
    }


    public void onResume() {
        NxUri edits = NxUri.decode("https://demo.nextop.io/collab/edits/${id}");
        Observable<NxMessage> getEdits = client.receiver(edits.method(NxUri.Method.GET));
        Observable<NxMessage> postEdits = client.receiver(edits.method(NxUri.Method.POST));


        Observable.zip(
                getEdits.flatMap({(NxMessage m) -> {
                    String id = m.get("id");
                    if(null != id) {
                        return store.get(id);
                    } else{
                        return Observable.<Document>never();
                    }
                }}).flatMap({(Document d) -> Observable.concat(Observable.from(d.getInitialOps()), d.getPublishOps()) }),
        getEdits,
                {op, m -> new ROp(op, m.id)}).subscribe({
                {rop -> c.send(rop.toId, rop)}
                {}
                {t ->}
        });

        Observable.zip(
                postEdits.flatMap({m ->
                        String id = m.get("id");
        if (null != id) {
            return store.get(id);
        } else {
            return Observable.<Document>never();
        }}),
        postEdits,
                {d, m -> new DOp(d, (Op) m)}).subscribe({
                {dop -> dop.d.apply(dop.op)}
                {}
                {t ->}
        });

    }

    public void onPause() {

    }
}
