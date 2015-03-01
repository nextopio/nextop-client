package io.nextop.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.WireValue;
import io.nextop.sortedlist.SortedList;
import io.nextop.sortedlist.SplaySortedList;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Shared state for all {@link MessageControlChannel} objects
 * for SEND.MESSAGE.
 * FIXME all MessageControl should go in here
 * TODO RECEIVE.* and SEND.{COMPLETED, ERROR} are multicasted
 * TODO to all upstream/downstream and not persisted in the state.
 *
 * Each {@link MessageControl} object is controlled by at most one channel object.
 * Nodes take/release control via {@link #take}/{@link #release}.
 *
 * The state allows introspection via the {@link #get} variants.
 *
 * Thread safe. */
public final class MessageControlState {

    private final MessageContext context;

    private final Object mutex = new Object();

    private int headIndex = 0;
    private final Map<Id, Entry> entries;
    private final Set<Id> pending;
    /** these need to be attached to an entry on {@link #add} */
    private final Multimap<Id, Subscriber<? super Entry>> pendingSubscribers;

    private final Map<Id, Group> groups;

    private final SortedList<Group> groupsByPriority;


    private final BehaviorSubject<MessageControlState> publish;



    public MessageControlState(MessageContext context) {
        this.context = context;

        entries = new HashMap<Id, Entry>(32);
        groups = new HashMap<Id, Group>(8);
        groupsByPriority = new SplaySortedList<Group>(COMPARATOR_GROUP_AVAILABLE);
        pending = new HashSet<Id>(4);
        pendingSubscribers = HashMultimap.create(4, 4);

        publish = BehaviorSubject.create(this);
    }





    /////// QUEUE MANAGEMENT ///////


    /** non-blocking */
    @Nullable
    public Entry takeFirstAvailable(MessageControlChannel owner) {
        synchronized (mutex) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (null == first.owner) {
                        take(first.id, owner);
                        return first;
                    }
                }
            }
            return null;
        }
    }

    /** non-blocking.
     * this version is useful if testing to replace the head of a transfer
     * with a more important entry.
     * @param min the minimum priority to search for the first available.
     *            If none available with greater priority, returns null. */
    @Nullable
    public Entry takeFirstAvailable(Id min, MessageControlChannel owner) {
        if (null == min) {
            throw new IllegalArgumentException();
        }
        synchronized (mutex) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (min.equals(first.id)) {
                        return null;
                    } else if (null == first.owner) {
                        take(first.id, owner);
                        return first;
                    }
                }
            }
            return null;
        }
    }


    /** blocking */
    @Nullable
    public Entry takeFirstAvailable(MessageControlChannel owner, long timeout, TimeUnit timeUnit) throws InterruptedException {
        final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
        synchronized (mutex) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            Entry entry;
            while (null == (entry = takeFirstAvailable(owner)) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                mutex.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                timeoutNanos -= (System.nanoTime() - nanos);
            }
            return entry;
        }
    }

    /** blocking */
    @Nullable
    public Entry takeFirstAvailable(Id min, MessageControlChannel owner, long timeout, TimeUnit timeUnit) throws InterruptedException {
        final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
        synchronized (mutex) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            Entry entry;
            while (null == (entry = takeFirstAvailable(min, owner)) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                mutex.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                timeoutNanos -= (System.nanoTime() - nanos);
            }
            return entry;
        }
    }




    /** non-blocking */
    @Nullable
    public boolean hasFirstAvailable() {
        synchronized (mutex) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (null == first.owner) {
                        return true;
                    }
                }
            }
            return false;
        }
    }



    /** non-blocking. */
    public boolean hasFirstAvailable(Id min) {
        if (null == min) {
            throw new IllegalArgumentException();
        }
        synchronized (mutex) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (min.equals(first.id)) {
                        return false;
                    } else if (null == first.owner) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


    /** blocking */
    public boolean hasFirstAvailable(long timeout, TimeUnit timeUnit) throws InterruptedException {
        final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
        synchronized (mutex) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            boolean a;
            while (!(a = hasFirstAvailable()) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                mutex.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                timeoutNanos -= (System.nanoTime() - nanos);
            }
            return a;
        }
    }

    /** blocking */
    public boolean hasFirstAvailable(Id min, long timeout, TimeUnit timeUnit) throws InterruptedException {
        final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
        synchronized (mutex) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            boolean a;
            while (!(a = hasFirstAvailable(min)) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                mutex.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                timeoutNanos -= (System.nanoTime() - nanos);
            }
            return a;
        }
    }



    /** available if all:
     * - no owner
     * - first in group */
    public boolean isAvailable(Id id) {
        synchronized (mutex) {
            @Nullable Entry entry = entries.get(id);
            if (null == entry) {
                return false;
            }

            // owner
            if (null != entry.owner) {
                return false;
            } // else no owner

            Group group = entry.group;
            assert null != group;

            // first in group
            if (group.entries.isEmpty()) {
                return false;
            }

            Entry first = group.entries.get(0);
            return id.equals(first.id);
        }
    }


    public void take(Id id, MessageControlChannel owner) {
        synchronized (mutex) {
            @Nullable Entry entry = entries.get(id);

            if (null == entry) {
                throw new IllegalArgumentException();
            }
            if (null != entry.owner) {
                throw new IllegalArgumentException();
            }

            Group group = entry.group;
            assert null != group;

            groupsByPriority.remove(group);
            try {
                group.take(entry, owner);
            } finally {
                groupsByPriority.insert(group);
            }

            mutex.notifyAll();
        }
        publish();
    }
    public void release(Id id, MessageControlChannel owner) {
        synchronized (mutex) {
            @Nullable Entry entry = entries.get(id);

            if (null == entry) {
                throw new IllegalArgumentException();
            }
            if (owner != entry.owner) {
                throw new IllegalArgumentException();
            }

            Group group = entry.group;
            assert null != group;

            groupsByPriority.remove(group);
            try {
                group.release(entry, owner);
            } finally {
                groupsByPriority.insert(group);
            }

            mutex.notifyAll();
        }
        publish();
    }


    /** this should be called immediately before inserting a message control
     * into the channel. It helps provide a fast negative for queries
     * for bad IDs (could be for a number of reasons).
     * @see #getObservable(io.nextop.Id, long, java.util.concurrent.TimeUnit) */
    public void notifyPending(Id id) {
        synchronized (mutex) {
            pending.add(id);
        }
    }

    public boolean add(Message message) {
        // see notes at top - only SEND.MESSAGE message control

        Entry entry;
        Collection<Subscriber<? super Entry>> subscribers;
        synchronized (mutex) {
            // check already added
            if (entries.containsKey(message.id)) {
                return false;
            }

            entry = new Entry(headIndex++, message);
            entries.put(entry.id, entry);
            pending.remove(entry.id);
            subscribers = pendingSubscribers.removeAll(entry.id);

            @Nullable Group group = groups.get(entry.groupId);
            if (null == group) {
                Id groupId = entry.groupId;
                group = new Group(groupId);
                groups.put(groupId, group);
                // don't remove from groupsByPriority because not present
            } else {
                groupsByPriority.remove(group);
            }

            group.add(entry);
            groupsByPriority.insert(group);

            mutex.notifyAll();
        }
        // add the subscribers (which publishes to them)
        for (Subscriber subscriber : subscribers) {
            entry.publish.subscribe(subscriber);
        }
        publish();
        return true;
    }


    @Nullable
    public Message remove(Id id, End end) {
        Entry entry;
        synchronized (mutex) {
            entry = entries.remove(id);

            if (null == entry) {
                return null;
            }


            assert null == entry.end;

            Group group = entry.group;
            assert null != group;

            groupsByPriority.remove(group);
            group.remove(entry);
            if (!group.entries.isEmpty()) {
                groupsByPriority.insert(group);
            }

            entry.end = end;

            mutex.notifyAll();
        }
        entry.publish();
        entry.publishComplete();
        publish();
        return entry.message;
    }

    public boolean yield(Id id) {
        Entry entry;
        synchronized (mutex) {
            entry = entries.get(id);

            if (null == entry) {
                return false;
            }
            assert null == entry.end;

            Group group = entry.group;
            assert null != group;

            groupsByPriority.remove(group);
            group.yield(entry);
            groupsByPriority.insert(group);

            mutex.notifyAll();
        }
        entry.publish();
        publish();
        return true;
    }




    public boolean setInboxTransferProgress(Id id, TransferProgress transferProgress) {
        Entry entry;
        synchronized (mutex) {
            entry = entries.get(id);

            if (null == entry) {
                return false;
            }

            if (null != entry.end) {
                return false;
            }

            entry.inboxTransferProgress = transferProgress;
        }
        entry.publish();
        publish();
        return true;
    }

    public boolean setOutboxTransferProgress(Id id, TransferProgress transferProgress) {
        Entry entry;
        synchronized (mutex) {
            entry = entries.get(id);

            if (null == entry) {
                return false;
            }

            if (null != entry.end) {
                return false;
            }

            entry.outboxTransferProgress = transferProgress;

            // FIXME remove
//            System.out.printf("  outbox transfer progress %s\n", transferProgress);
        }
        entry.publish();
        publish();
        return true;
    }



    /////// INSPECTION ///////


    // triggers when groups or indexes change
    // does not trigger when entry-only properties change (e.g. progress, active, etc)
    public Observable<MessageControlState> getObservable() {
        return publish;
    }


    private void publish() {
        publish.onNext(this);
    }



    public Observable<Entry> getObservable(final Id id) {
        return getObservable(id, 0, TimeUnit.MILLISECONDS);
    }

    public Observable<Entry> getObservable(final Id id, final long timeout, final TimeUnit timeUnit) {
        // on subscribe, if no entry, add subscriber to pending observers for entry
        return Observable.create(new Observable.OnSubscribe<Entry>() {
            @Override
            public void call(final Subscriber<? super Entry> subscriber) {
                @Nullable Entry entry;
                synchronized (mutex) {
                    entry = entries.get(id);
                    if (null == entry) {
                        if (0 < timeout && /* see #notifyPending */ pending.contains(id)) {
                            pendingSubscribers.put(id, subscriber);

                            // TODO manually clean up the timeout when the subscriber is taken
                            // add the timeout
                            subscriber.add(context.getScheduler().createWorker().schedule(new Action0() {
                                @Override
                                public void call() {
                                    synchronized (mutex) {
                                        if (pendingSubscribers.containsEntry(id, subscriber)) {
                                            pendingSubscribers.remove(id, subscriber);
                                            subscriber.onCompleted();
                                            subscriber.unsubscribe();
                                        }
                                    }
                                }
                            }, timeout, timeUnit));
                        } else {
                            subscriber.onCompleted();
                            subscriber.unsubscribe();
                        }
                    }
                }
                if (null != entry) {
                    entry.publish.subscribe(subscriber);
                }
            }
        });
    }



    public int size() {
        synchronized (mutex) {
            int c = 0;
            for (Group g : groupsByPriority) {
                c += g.entries.size();
            }
            return c;
        }
    }

    public int indexOf(Id id) {
        synchronized (mutex) {
            @Nullable Entry entry = entries.get(id);
            if (null == entry) {
                return -1;
            }

            @Nullable Group group = groups.get(entry.message.groupId);
            if (null == group) {
                return -1;
            }

            int c = 0;
            for (Group g : groupsByPriority) {
                if (group == g) {
                    break;
                }
                c += g.entries.size();
            }

            return c + group.entries.indexOf(entry);
        }
    }

    public Entry get(int index) {
        synchronized (mutex) {
            if (index < 0) {
                throw new IndexOutOfBoundsException();
            }
            int c = index;
            for (Group g : groupsByPriority) {
                int n = g.entries.size();
                if (c < n) {
                    return g.entries.get(c);
                }
                c -= n;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public List<GroupSnapshot> getGroups() {
        synchronized (mutex) {
            final List<GroupSnapshot> groupSnapshots = new ArrayList<GroupSnapshot>(groupsByPriority.size());
            for (Group g : groupsByPriority) {
                groupSnapshots.add(new GroupSnapshot(g.groupId, ImmutableList.copyOf(g.entries)));
            }
            return Collections.unmodifiableList(groupSnapshots);
        }
    }

    public Entry get(Id groupId, int index) {
        synchronized (mutex) {
            @Nullable Group group = groups.get(groupId);
            if (null == group) {
                throw new IndexOutOfBoundsException();
            }

            int n = group.entries.size();
            if (index < 0 || n <= index) {
                throw new IndexOutOfBoundsException();
            }

            return group.entries.get(index);
        }
    }




    /////// MessageControlChannel SUPPORT ///////

    // TODO (stuff to think about)
    // TODO there will likely be a version where some intermediary node persists to disk
    // TODO and rewrites the WireValue to have a pointer to disk location
    // TODO in those cases, the message out will need to be translated in the reverse direction


    /** standard implementation to respond to internal control messages */
    public boolean onActiveMessageControl(MessageControl mc, MessageControlChannel upstream) {
        Message message = mc.message;
        Route route = message.route;
        if (route.isLocal()) {
            Id id = route.getLocalId();
            if (null != id) {
                if (MessageControl.Type.ERROR.equals(mc.type) && Message.outboxRoute(id).equals(route)) {
                    // cancel
                    if (remove(id, End.CANCELED)) {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, Message.inboxRoute(id)));
                    }
                } else if (MessageControl.Type.MESSAGE.equals(mc.type) && Message.echoRoute(id).equals(route)) {
                    @Nullable MessageControl rmc = createRedirect(id, message.inboxRoute());
                    if (null != rmc) {
                        upstream.onMessageControl(rmc);
                        return true;
                    } else {
                        return false;
                    }
                } // else fall through
            } // else no entry for message; fall through
        }
        return false;
    }

    @Nullable
    private MessageControl createRedirect(Id id, Route newRoute) {
        @Nullable Entry entry;
        synchronized (mutex) {
            entry = entries.get(id);
        }
        if (null == entry) {
            return null;
        }

        Message message = entry.message;

        return MessageControl.receive(MessageControl.Type.MESSAGE, message.toBuilder()
                .setHeader(Message.H_REDIRECT, WireValue.of(Collections.singletonList(message.route.toString())))
                .setRoute(newRoute)
                .build());
    }


    public static enum End {
        COMPLETED,
        ERROR
    }

    public static final class Entry {



        int index;
        /** alias from message */
        public final Id id;
        /** alias from message */
        public final Id groupId;
        /** alias from message */
        public final int groupPriority;

        public final Message message;


        /////// PROPERTIES ///////

        // read-only to clients
        // these are updated in a lock
        // the volatile is for reading

        @Nullable
        public volatile MessageControlChannel owner = null;

        public volatile TransferProgress outboxTransferProgress;
        public volatile TransferProgress inboxTransferProgress;

        @Nullable
        public volatile End end = null;



        // internal

        final BehaviorSubject<Entry> publish;

        /** set by {@link Group#add}/{@link Group#remove} */
        @Nullable Group group = null;



        Entry(int index, Message message) {
            this.index = index;
            groupId = message.groupId;
            id = message.id;
            groupPriority = message.groupPriority;
            this.message = message;
            publish = BehaviorSubject.create(this);

            outboxTransferProgress = TransferProgress.none(id);
            inboxTransferProgress = TransferProgress.none(id);
        }


        private void publish() {
            publish.onNext(this);
        }
        private void publishComplete() {
            publish.onCompleted();
        }
    }

    public static final class TransferProgress {
        public static TransferProgress none(Id id) {
            return create(id, 0, 0);
        }

        public static TransferProgress create(Id id, long completedBytes, long totalBytes) {
            if (totalBytes < 0) {
                throw new IllegalArgumentException(String.format("%d", totalBytes));
            }
            if (completedBytes < 0 || 0 < totalBytes && totalBytes < completedBytes) {
                throw new IllegalArgumentException(String.format("%d %d", completedBytes, totalBytes));
            }
            return new TransferProgress(id, completedBytes, totalBytes);
        }


        public final Id id;
        public final long completedBytes;
        public final long totalBytes;


        TransferProgress(Id id, long completedBytes, long totalBytes) {
            this.id = id;
            this.completedBytes = completedBytes;
            this.totalBytes = totalBytes;
        }


        public boolean isNone() {
            return 0 == completedBytes && 0 == totalBytes;
        }


        public float asFloat() {
            final int q = 1000;
            return 0 < totalBytes ? (q * completedBytes / totalBytes) / (float) q : 0.f;
        }

        @Override
        public String toString() {
            if (isNone()) {
                return "-";
            } else {
                return String.format("%s %d/%d (%.2f%%)", id, completedBytes, totalBytes, asFloat());
            }
        }

        @Override
        public int hashCode() {
            int c = id.hashCode();
            c = 31 * c + (int)(completedBytes ^ (completedBytes >>> 32));
            c = 31 * c + (int)(totalBytes ^ (totalBytes >>> 32));
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TransferProgress)) {
                return false;
            }

            TransferProgress p = (TransferProgress) obj;
            return completedBytes == p.completedBytes
                    && totalBytes == p.totalBytes
                    && id.equals(p.id);
        }
    }


    public static final class GroupSnapshot {
        public final Id groupId;
        public final List<Entry> entries;
        // FIXME priority

        GroupSnapshot(Id groupId, List<Entry> entries) {
            this.groupId = groupId;
            this.entries = entries;
        }
    }



    // internal

    private final class Group {
        final Id groupId;

        final PriorityQueue<Entry> entriesByPriority;
        final SortedList<Entry> entries;


        Group(Id groupId) {
            this.groupId = groupId;

            entriesByPriority = new PriorityQueue<Entry>(8, COMPARATOR_ENTRY_DESCENDING_PRIORITY);
            entries = new SplaySortedList<Entry>(COMPARATOR_ENTRY_AVAILABLE);
        }


        void add(Entry entry) {
            if (null != entry.group) {
                throw new IllegalArgumentException();
            }

            entry.group = this;
            entriesByPriority.add(entry);
            entries.insert(entry);
        }
        void remove(Entry entry) {
            if (this != entry.group) {
                throw new IllegalArgumentException();
            }

            entries.remove(entry);
            entriesByPriority.remove(entry);
            entry.group = null;
        }

        void yield(Entry entry) {
            if (this != entry.group) {
                throw new IllegalArgumentException();
            }

            entries.remove(entry);
            entriesByPriority.remove(entry);
            entry.index = headIndex++;
            entriesByPriority.add(entry);
            entries.insert(entry);
        }

        void take(Entry entry, MessageControlChannel owner) {
            if (this != entry.group) {
                throw new IllegalArgumentException();
            }
            // this should be strictly enforced by the caller
            assert null == entry.owner;

            entries.remove(entry);
            try {
                entry.owner = owner;
            } finally {
                entries.insert(entry);
            }
        }
        void release(Entry entry, MessageControlChannel owner) {
            if (this != entry.group) {
                throw new IllegalArgumentException();
            }
            // this should be strictly enforced by the caller
            assert owner == entry.owner;

            entries.remove(entry);
            try {
                entry.owner = null;
            } finally {
                entries.insert(entry);
            }
        }
    }


    private static final Comparator<Group> COMPARATOR_GROUP_AVAILABLE = new Comparator<Group>() {
        @Override
        public int compare(Group a, Group b) {
            // compare by emptiness
            // -- if both empty, get an absolute order using the group id
            // compare by max prio of group
            // compare by index

            if (a == b) {
                return 0;
            }

            boolean aEmpty = a.entries.isEmpty();
            boolean bEmpty = b.entries.isEmpty();

            if (aEmpty && bEmpty) {
                // stable
                return a.groupId.compareTo(b.groupId);
            } else if (aEmpty) {
                return 1;
            } else if (bEmpty) {
                return -1;
            }

            int aMaxGroupPriority = a.entriesByPriority.peek().groupPriority;
            int bMaxGroupPriority = b.entriesByPriority.peek().groupPriority;

            if (aMaxGroupPriority < bMaxGroupPriority) {
                return 1;
            } else if (bMaxGroupPriority < aMaxGroupPriority) {
                return -1;
            }

            int aIndex = a.entries.get(0).index;
            int bIndex = b.entries.get(0).index;

            if (aIndex < bIndex) {
                return -1;
            } else if (bIndex < aIndex) {
                return 1;
            } else {
                // same entry in two different groups
                throw new IllegalStateException();
            }
        }
    };

    private static final Comparator<Entry> COMPARATOR_ENTRY_AVAILABLE = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            // compare by owner (owned at front)
            // compare by index

            if (a == b) {
                return 0;
            }

            boolean aOwned = null != a.owner;
            boolean bOwned = null != b.owner;

            if (aOwned != bOwned) {
                if (aOwned) {
                    return -1;
                } else {
                    return 1;
                }
            }

            int aIndex = a.index;
            int bIndex = b.index;
            if (aIndex < bIndex) {
                return -1;
            } else if (bIndex < aIndex) {
                return 1;
            } else {
                // same index in two different entries
                throw new IllegalStateException();
            }
        }
    };

    private static final Comparator<Entry> COMPARATOR_ENTRY_DESCENDING_PRIORITY = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            if (a.groupPriority < b.groupPriority) {
                return 1;
            } else if (b.groupPriority < a.groupPriority) {
                return -1;
            } else {
                return 0;
            }
        }
    };




}
