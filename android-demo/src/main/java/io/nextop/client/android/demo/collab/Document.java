package io.nextop.client.android.demo.collab;

import android.support.annotation.Nullable;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Document implements Serializable {
    public static final Node HEAD = new Node(new UUID(0L, 0L), "");


    public static final class Node implements Serializable {
        public final UUID id;
        public final String value;

        Node(UUID id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    public static final class Op implements Serializable {
        public static enum Type {
            INSERT,
            REMOVE,
            NOOP
        };

        public final Type type;
        public final Node node;
        public final UUID before;

        Op(Type type, Node node, UUID before) {
            this.type = type;
            this.node = node;
            this.before = before;
        }
    }


    private static final class DNode implements Serializable {
        final Node node;
        @Nullable DNode before = null;
        @Nullable DNode after = null;

        DNode(Node node) {
            this.node = node;
        }

        void remove() {
            if (null != before) {
                before.after = after;
            }
            if (null != after) {
                after.before = before;
            }
            before = null;
            after = null;
        }
        void insertAfter(DNode dnode) {
            dnode.after = after;
            dnode.before = this;
            if (null != after) {
                after.before = dnode;
            }
            after = dnode;
        }
    }



    private final Map<UUID, DNode> nodeMap = new HashMap<UUID, DNode>(32);

    /** the previous UUID for each removed UUID, at the time it was removed. */
    private final Map<UUID, UUID> beforeMap = new HashMap<UUID, UUID>(32);



    private final DNode head = new DNode(HEAD);


    private transient final PublishSubject<Op> publishOps = PublishSubject.<Op>create();



    public Document() {
        nodeMap.put(head.node.id, head);
    }


    public void apply(Op op) {
        switch (op.type) {
            case INSERT:
                insert(op.node, op.before);
                break;
            case REMOVE:
                remove(op.node);
                break;
            default:
                // ignore
                break;
        }
    }
    private void insert(Node node, UUID before) {
        if (nodeMap.containsKey(node.id)) {
            // cannot apply cleanly; local version is ahead
        } else if (beforeMap.containsKey(node.id)) {
            // cannot apply cleanly; local version is ahead
        } else {
            // insert

            DNode dbefore = findBefore(before);

            DNode dnode = new DNode(node);
            nodeMap.put(node.id, dnode);
            dbefore.insertAfter(dnode);

            publishOps.onNext(new Op(Op.Type.INSERT, node, dbefore.node.id));
        }
    }
    private void remove(Node node) {
        if (nodeMap.containsKey(node.id)) {
            // remove

            DNode dnode = nodeMap.get(node.id);
            DNode dbefore = dnode.before;

            dnode.remove();

            beforeMap.put(node.id, dbefore.node.id);

            publishOps.onNext(new Op(Op.Type.REMOVE, node, dbefore.node.id));
        } else if (beforeMap.containsKey(node.id)) {
            // cannot apply cleanly; local version is ahead
        } else {
            // cannot apply cleanly; local version is ahead
        }
    }

    private @Nullable DNode findBefore(UUID before) {
        UUID id = before;
        DNode dnode;
        do {
            dnode = nodeMap.get(id);
        } while (null == dnode && null != (id = beforeMap.get(id)));
        return dnode;
    }



    private Op[] getInitialOps() {
        Node[] nodes = getNodes();
        // [0] is the head
        int n = nodes.length - 1;
        Op[] ops = new Op[n];
        for (int i = 1; i < n; ++i) {
            ops[i] = new Op(Op.Type.INSERT, nodes[i + 1], nodes[i].id);
        }
        return ops;
    }

    private Observable<Op> getPublishOps() {
        return publishOps;
    }

    public Observable<Op> getOps() {
        return Observable.concat(Observable.from(getInitialOps()), getPublishOps());
    }

    // includes head
    public Node[] getNodes() {
        int n = nodeMap.size();
        Node[] nodes = new Node[n];
        DNode dnode = head;
        for (int i = 0; i < n; ++i, dnode = dnode.after) {
            nodes[i] = dnode.node;
        }
        assert null == dnode;
        return nodes;
    }
}
