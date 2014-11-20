package io.nextop.client.demo.collab;

import io.nextop.client.Connection;
import io.nextop.client.ConnectionContext;
import io.nextop.client.Message;
import io.nextop.client.Path;
import rx.Observable;

public class Client {
    public static void main(String[] args) {
        final DocumentStore store = new MemoryDocumentStore();

        ConnectionContext cc;

        final Connection c = cc.get("demo.nextop.io");



        // TODO in a fragment that wants to view document ID
        final String id = "";

        Observable.zip(
                store.get(id),
            c.receive(c.send(new Path(Path.Action.GET, "/edits/${id}"), new MessageBuilder().set("id", id).build())),
            {d, m -> new DOp(d, (Op) m))}).subscribe({
                {dop -> dop.d.apply(dop.op) }
                {}
                {t -> }
            });

        store.get(id).flatMap({d -> d.getNodes()}).subscribe({
                {nodes -> /* set text into view. maintain cursor position */}
                {}
                {t -> }
        });


        // TO APPLY AN OP
        final Op op = new Op();
        store.get(id).subscribe({
                {d -> d.apply(op);}
                {}
                {t -> }
                });
        c.send(new Path(Path.Action.POST, "/edits/${id}"), op);
    }
}
