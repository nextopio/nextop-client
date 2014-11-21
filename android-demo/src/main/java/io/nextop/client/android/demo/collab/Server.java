package io.nextop.client.android.demo.collab;

import io.nextop.client.Connection;
import io.nextop.client.ConnectionContext;
import io.nextop.client.Message;
import io.nextop.client.Path;
import rx.Observable;
import rx.Observer;

public class Server {

    public static void main(String[] args) {
        final DocumentStore store = new MemoryDocumentStore();

        ConnectionContext cc;

        final Connection c = cc.get("demo.nextop.io");



        Observable<Message> getEdits = c.receive(new Path(Path.Action.GET, "/edits/${id}"));
        Observable<Message> postEdits = c.receive(new Path(Path.Action.POST, "/edits/${id}"));


        Observable.zip(
                getEdits.flatMap({m ->
                        String id = m.get("id");
        if (null != id) {
            return store.get(id);
        } else {
            return Observable.<Document>never();
        }}).flatMap({d ->
        return d.getOps();
        }),
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

    static final class DocumentStore {
        Observable<Document> get(String id);
    }
}
