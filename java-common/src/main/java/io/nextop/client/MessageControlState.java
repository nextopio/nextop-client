package io.nextop.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.sortedlist.SortedList;
import io.nextop.sortedlist.SplaySortedList;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Shared state for all {@link MessageControlChannel} objects.
 * This object should be updated from the active channel on message control
 * (the control logic of the active channel should update this object then update itself).
 * Each {@link MessageControl} object is controlled by at most one channel object.
 * Nodes take/release control via {@link #take}/{@link #release}.
 *
 * The state allows introspection via the {@link #get} variants.
 *
 * Thread safe. */
public final class MessageControlState {

    private final MessageContext context;

    // FIXME remove no longer apply
    // optional: the channel may create entries with no message (stub(id))
    // and - setProgess(...) to update the download progress
    // - call setMessage(...) to set the completed message
    // these entries show up in a null group (null == indexOf(id))

    private final Object monitor = new Object();

    private int headIndex = 0;
    private final Map<Id, Entry> entries;
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
        pendingSubscribers = HashMultimap.create(4, 4);

        publish = BehaviorSubject.create(this);
    }





    /////// QUEUE MANAGEMENT ///////



    /** non-blocking */
    @Nullable
    public Entry getFirstAvailable() {
        synchronized (monitor) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (null != first.owner) {
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
    public Entry getFirstAvailable(Id min) {
        if (null == min) {
            throw new IllegalArgumentException();
        }
        synchronized (monitor) {
            for (Group group : groupsByPriority) {
                if (!group.entries.isEmpty()) {
                    Entry first = group.entries.get(0);
                    if (min.equals(first.id)) {
                        return null;
                    } else if (null != first.owner) {
                        return first;
                    }
                }
            }
            return null;
        }
    }


    /** blocking */
    @Nullable
    public Entry getFirstAvailable(long timeout, TimeUnit timeUnit) {
        synchronized (monitor) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            Entry entry;
            while (null == (entry = getFirstAvailable()) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                try {
                    final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
                    monitor.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                    timeoutNanos = 0;
                } catch (InterruptedException e) {
                    // continue
                    timeoutNanos -= (System.nanoTime() - nanos);
                }
            }
            return entry;
        }
    }

    /** blocking */
    @Nullable
    public Entry getFirstAvailable(Id min, long timeout, TimeUnit timeUnit) {
        synchronized (monitor) {
            long timeoutNanos = timeUnit.toNanos(timeout);
            Entry entry;
            while (null == (entry = getFirstAvailable(min)) && 0 < timeoutNanos) {
                long nanos = System.nanoTime();
                try {
                    final long nanosPerMillis = TimeUnit.MILLISECONDS.toNanos(1);
                    monitor.wait(timeoutNanos / nanosPerMillis, (int) (timeoutNanos % nanosPerMillis));
                    timeoutNanos = 0;
                } catch (InterruptedException e) {
                    // continue
                    timeoutNanos -= (System.nanoTime() - nanos);
                }
            }
            return entry;
        }
    }



    /** available if all:
     * - no owner
     * - first in group */
    public boolean isAvailable(Id id) {
        synchronized (monitor) {
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
        synchronized (monitor) {
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

            monitor.notifyAll();
        }
        publish();
    }
    public void release(Id id, MessageControlChannel owner) {
        synchronized (monitor) {
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

            monitor.notifyAll();
        }
        publish();
    }






    public boolean add(MessageControl mc) {
        Entry entry;
        Collection<Subscriber<? super Entry>> subscribers;
        synchronized (monitor) {
            if (entries.containsKey(mc.message.id)) {
                return false;
            }

            entry = new Entry(headIndex++, mc);
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

            monitor.notifyAll();
        }
        // add the subscribers (which publishes to them)
        for (Subscriber subscriber : subscribers) {
            entry.publish.subscribe(subscriber);
        }
        publish();
        return true;
    }


    public boolean remove(Id id, Entry.End end) {
        Entry entry;
        synchronized (monitor) {
            entry = entries.remove(id);

            if (null == entry) {
                return false;
            }
            assert null == entry.end;

            Group group = entry.group;
            assert null != group;

            groupsByPriority.remove(group);
            try {
                group.remove(entry);
            } finally {
                if (!group.entries.isEmpty()) {
                    groupsByPriority.insert(group);
                }
            }

            entry.end = end;

            monitor.notifyAll();
        }
        entry.publish();
        publish();
        return true;
    }




    public boolean setTransferProgress(Id id, MessageControl.Direction dir, TransferProgress transferProgress) {
        Entry entry;
        synchronized (monitor) {
            entry = entries.get(id);

            if (null == entry) {
                return false;
            }

            if (null != entry.end) {
                return false;
            }

            switch (dir) {
                case SEND:
                    entry.sendTransferProgress = transferProgress;
                    break;
                case RECEIVE:
                    entry.receiveTransferProgress = transferProgress;
                    break;
                default:
                    throw new IllegalStateException();
            }
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

    public Observable<Entry> getObservable(final Id id) {
        // on subscribe, if no entry, add subscriber to pending observers for entry
        return Observable.create(new Observable.OnSubscribe<Entry>() {
            @Override
            public void call(Subscriber<? super Entry> subscriber) {
                @Nullable Entry entry;
                synchronized (monitor) {
                    entry = entries.get(id);
                    if (null == entry) {
                        pendingSubscribers.put(id, subscriber);
                    }
                }
                if (null != entry) {
                    entry.publish.subscribe(subscriber);
                }
            }
        });
    }



    public int size() {
        synchronized (monitor) {
            int c = 0;
            for (Group g : groupsByPriority) {
                c += g.entries.size();
            }
            return c;
        }
    }

    public int indexOf(Id id) {
        synchronized (monitor) {
            @Nullable Entry entry = entries.get(id);
            if (null == entry) {
                return -1;
            }

            @Nullable Group group = groups.get(entry.mc.message.groupId);
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
        synchronized (monitor) {
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
        synchronized (monitor) {
            List<GroupSnapshot> groupSnapshots = new ArrayList<GroupSnapshot>(groupsByPriority.size());
            for (Group g : groupsByPriority) {
                groupSnapshots.add(new GroupSnapshot(g.groupId, g.entries.size()));
            }
            return groupSnapshots;
        }
    }

    public Entry get(Id groupId, int index) {
        synchronized (monitor) {
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


    private void publish() {
        publish.onNext(this);
    }



    /////// MessageControlChannel SUPPORT ///////

    // TODO (stuff to think about)
    // TODO there will likely be a version where some intermediary node persists to disk
    // TODO and rewrites the WireValue to have a pointer to disk location
    // TODO in those cases, the message out will need to be translated in the reverse direction


    /** standard implementation to respond to internal control messages */
    public boolean onMessageControl(MessageControl mc, MessageControlChannel upstream) {
        Message message = mc.message;
        Route route = message.route;
        if (route.isLocal()) {
            switch (mc.type) {
                case MESSAGE:
                    Id id = route.getLocalId();

                    if (Message.echoRoute(id).equals(route)) {
                        @Nullable MessageControl rmc = createRedirect(id, message.inboxRoute());
                        if (null != rmc) {
                            upstream.onMessageControl(rmc);
                            return true;
                        } else {
                            return false;
                        }
                    } // else fall through
                    break;
                default:
                    // fall through
                    break;
            }
        }
        return false;
    }

    @Nullable
    private MessageControl createRedirect(Id id, Route newRoute) {
        @Nullable Entry entry;
        synchronized (monitor) {
            entry = entries.get(id);
        }
        if (null == entry) {
            return null;
        }

        Message message = entry.mc.message;

        MessageControl.Direction rdir;
        MessageControl.Type rtype;
        Message rmessage;

        rtype = entry.mc.type;
        // flip the direction
        switch (entry.mc.dir) {
            case SEND:
                rdir = MessageControl.Direction.RECEIVE;
                break;
            case RECEIVE:
                rdir = MessageControl.Direction.SEND;
                break;
            default:
                throw new IllegalStateException();
        }
        rmessage = message.toBuilder()
                .setHeader(Message.H_LOCATION, message.route)
                .setRoute(newRoute)
                .build();

        return new MessageControl(rdir, rtype, rmessage);
    }




    public static final class Entry {
        static enum End {
            CANCELED,
            COMPLETED,
            ERROR
        }


        final int index;
        /** alias from message */
        public final Id id;
        /** alias from message */
        public final Id groupId;
        /** alias from message */
        public final int groupPriority;

        public final MessageControl mc;


        /////// PROPERTIES ///////

        // read-only to clients
        // these are updated in a lock
        // the volatile is for reading

        @Nullable
        public volatile MessageControlChannel owner = null;

        public volatile TransferProgress sendTransferProgress = TransferProgress.none();
        public volatile TransferProgress receiveTransferProgress = TransferProgress.none();
        @Nullable
        public volatile End end = null;



        // internal

        final BehaviorSubject<Entry> publish;

        /** set by {@link Group#add}/{@link Group#remove} */
        @Nullable Group group = null;



        Entry(int index, MessageControl mc) {
            this.index = index;
            id = mc.message.id;
            groupId = mc.message.groupId;
            groupPriority = mc.message.groupPriority;
            this.mc = mc;
            publish = BehaviorSubject.create(this);
        }


        private void publish() {
            publish.onNext(this);
        }
    }

    public static final class TransferProgress {
        public static TransferProgress none() {
            return create(0, 0);
        }

        public static TransferProgress create(int completedBytes, int totalBytes) {
            if (totalBytes < 0) {
                throw new IllegalArgumentException();
            }
            if (completedBytes < 0 || totalBytes < completedBytes) {
                throw new IllegalArgumentException();
            }
            return new TransferProgress(completedBytes, totalBytes);
        }


        public final int completedBytes;
        public final int totalBytes;


        TransferProgress(int completedBytes, int totalBytes) {
            this.completedBytes = completedBytes;
            this.totalBytes = totalBytes;
        }


        public float asFloat() {
            return 0 < totalBytes ? (completedBytes / (float) totalBytes) : 0.f;
        }
    }


    public static final class GroupSnapshot {
        public final Id groupId;
        public final int size;

        GroupSnapshot(Id groupId, int size) {
            this.groupId = groupId;
            this.size = size;
        }
    }



    // internal

    private static final class Group {
        final Id groupId;

        final PriorityQueue<Entry> entriesByPriority;
        final SortedList<Entry> entries;


        Group(Id groupId) {
            this.groupId = groupId;

            entriesByPriority = new PriorityQueue<Entry>(COMPARATOR_ENTRY_DESCENDING_PRIORITY);
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
                entries.add(entry);
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
                entries.add(entry);
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
