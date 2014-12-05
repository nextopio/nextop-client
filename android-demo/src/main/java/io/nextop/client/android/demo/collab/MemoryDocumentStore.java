package io.nextop.client.android.demo.collab;

import rx.Observable;

import java.util.HashMap;
import java.util.Map;

// FIXME remove this. replace with ephemeralDocumetnStore,
// FIXME that sends a message to $id/nodes with the node data
// FIXME relies on root routing for the next server that is given the $id to receive the node message
// FIXME if the server discard the message because of timeout, then the document is lost
final class MemoryDocumentStore implements DocumentStore {
    // FIXME store observable here, and when the subscription count reaches 0, trigger some "save" logic to persist and eject the observable
    private Map<String, Document> documentMap = new HashMap<String, Document>(32);


    public MemoryDocumentStore() {
    }


    @Override
    public Observable<Document> get(String id) {
        Document d = documentMap.get(id);
        if (null == d) {
            d = new Document();
            documentMap.put(id, d);
        }
        return Observable.just(d);
    }
}
