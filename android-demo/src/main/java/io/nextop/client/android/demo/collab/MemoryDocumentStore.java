package io.nextop.client.android.demo.collab;

import rx.Observable;

import java.util.HashMap;
import java.util.Map;

final class MemoryDocumentStore implements DocumentStore {
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
