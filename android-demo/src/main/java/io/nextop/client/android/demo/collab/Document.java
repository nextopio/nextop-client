package io.nextop.client.android.demo.collab;

import java.util.UUID;

public class Document {
    public static final UUID HEAD = new UUID(0L, 0L);

    public static final class Node {
        public final UUID id;
        public final String value;
    }



    // TODO map of uuid to before UUID for removed nodes
    // TODO


    // TODO make these op objects; have a merge(ops) that merges ops into a deterministic non-conflicting sequence
    //
//    public void add(Node node, UUID after) {
//
//    }
//
//    public void remove(Node node) {
//
//    }

    public void apply(Op op) {

    }

    public void update(Op op) {

    }

    // op can be verified or not verified

    Observable<Op> getOps() {

    }

    Observable<Node[]> getNodes() {

    }



}
