package io.nextop.client.android.demo.collab;

import rx.Observable;

interface DocumentStore {
    Observable<Document> get(String id);
}
