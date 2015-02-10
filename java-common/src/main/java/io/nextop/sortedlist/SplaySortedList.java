package io.nextop.sortedlist;

import com.google.common.collect.Ordering;

import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;


/** Random-access data structure that maintains
 * elements in sorted order, with some extra sorted lookup functionality. 
 * Inserting by index {@link #add(int, Object)} is not supported,
 * since the index is determined by the sort order.
 * add/get/remove operations are logarithmic.
 * 
 * Implemented as a splay tree with a sub-tree counter on each node,
 * using top-down splaying. The splaying algorithm uses a custom 
 * modification to maintain the sub-tree counters per node.
 * 
 * Because of splaying, "get"s give faster access to 
 * recently accessed values/indexes, 
 * or values/indexes near them.
 * 
 * Based on notes:
 * see CLR
 * see ftp://ftp.cs.cmu.edu/usr/ftp/usr/sleator/splaying/SplayTree.java
 */
// FIXME inserting in sequential order bad perf
// FIXME support duplicates
// FIXME implement SortedList API correctly
// FIXME (iterator an all ops should splay)
public final class SplaySortedList<E> extends AbstractList<E> implements SortedList<E> {
    
    private final Comparator<? super E> comparator;
    private @Nullable Node<E> root;
    
    /* for splaying */
    private final Node<E> header = new Node<E>(null);
    
    
    @SuppressWarnings("unchecked")
	public SplaySortedList() {
        this((Comparator<? super E>) Ordering.<Comparable<E>>natural());
    }
    
    public SplaySortedList(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }
    
    
    
    /** Does not splay. */
    E search(int index) {
        if (null == root || index < 0 || root.count <= index)
            throw new IndexOutOfBoundsException();
        Node<E> y = root;
        for (int c; 0 != (c = index - (null != y.left ? y.left.count : 0));) {
            if (c < 0) {
                y = y.left;
            } else {
                index -= 1 + (null != y.left ? y.left.count : 0);
                y = y.right;
            }
        }
        return y.value;
    }
    
    FindResult<E> search(final E value) {
        return search(comparable(value, comparator));
    }
    /** Finds the value that matches, or the values around, 
     * the given query. Does not splay.
     * @param q must have an implied ordering of the internal
     * comparator, but can be based on a different object
     * representation than E. e.g. E may be a complex object
     * (A, B, ...), and <code>op</code> can be a prefix (A, ...),
     * or some order implied on other fields of E.*/
    FindResult<E> search(Comparable<? super E> q) {
        if (null == root) {
            return new FindResult<E>(null, -1, 1);
        }
        int index = 0;
        Node<E> y = root;
        
        
        int c;
        while (0 != (c = q.compareTo(y.value))) {
            if (c < 0) {
                if (null == y.left)
                    break;
                y = y.left;
            } else {
                if (null == y.right)
                    break;
                index += 1 + (null != y.left ? y.left.count : 0);
                y = y.right;
            }
        }
        

        index += null != y.left ? y.left.count : 0;
        
        // c is q.compare(y.value)
        return new FindResult<E>(y.value, index, c); 
    }
    
    
    /////// SPLAYING ///////
    
    // FIXME have a version of splay that uses a comparator
    
    private void splay(E value) {
        Node<E> l, r, t, y;
        l = r = header;
        t = root;
        header.left = header.right = null;
        header.count = 0;
        for (int c; 0 != (c = comparator.compare(value, t.value)); ) {
            if (c < 0) {
                if (null == t.left)
                    break;
                if (comparator.compare(value, t.left.value) < 0) {
                    // rotate right + preserve counts
                    y = t.left;
                    t.left = y.right;
                    y.right = t;
                    
                    y.count += 1 + (null != t.right ? t.right.count : 0);
                    t.count -= 1 + (null != y.left ? y.left.count : 0);
                    
                    t = y;
                    if (null == t.left)
                        break;
                }
                
                // link right
                r.left = t;
                r = t;
                t = t.left;
            } else {
                if (null == t.right)
                    break;
                if (0 < comparator.compare(value, t.right.value)) {
                    // rotate left + preserve counts
                    y = t.right;
                    t.right = y.left;
                    y.left = t;
                    
                    y.count += 1 + (null != t.left ? t.left.count : 0);
                    t.count -= 1 + (null != y.right ? y.right.count : 0);
                    
                    t = y;
                    if (null == t.right)
                        break;
                }
                
                // link left
                l.right = t;
                l = t;
                t = t.right;
            }
        }
        
        // assemble + reset counts
        l.right = t.left;
        r.left = t.right;        
        t.left = header.right;
        t.right = header.left;
        
        resetLrCounts(t);
        t.count = 1 + (null != t.left ? t.left.count : 0) + (null != t.right ? t.right.count : 0);
        
        root = t;
    }
    private void splay(int index) {
        Node<E> l, r, t, y;
        l = r = header;
        t = root;
        header.left = header.right = null;
        header.count = 0;
        for (int c; 0 != (c = index - (null != t.left ? t.left.count : 0));) {
            if (c < 0) {
                if (null == t.left)
                    break;
                if (index - (null != t.left.left ? t.left.left.count : 0) < 0) {
                    // rotate right + preserve counts
                    y = t.left;
                    t.left = y.right;
                    y.right = t;
                    
                    y.count += 1 + (null != t.right ? t.right.count : 0);
                    t.count -= 1 + (null != y.left ? y.left.count : 0);
                    
                    t = y;
                    if (null == t.left)
                        break;
                }
                
                // link right
                r.left = t;
                r = t;
                t = t.left;
            } else {
                index -= 1 + (null != t.left ? t.left.count : 0);
                if (null == t.right)
                    break;
                if (0 < index - (null != t.right.left ? t.right.left.count : 0)) {
                    index -= 1 + (null != t.right.left ? t.right.left.count : 0);
                    
                    // rotate left + preserve counts
                    y = t.right;
                    t.right = y.left;
                    y.left = t;
                    
                    y.count += 1 + (null != t.left ? t.left.count : 0);
                    t.count -= 1 + (null != y.right ? y.right.count : 0);
                    
                    t = y;
                    if (null == t.right)
                        break;
                }
                
                // link left
                l.right = t;
                l = t;
                t = t.right;
            }
        }
        
        // assemble + reset counts
        l.right = t.left;
        r.left = t.right;        
        t.left = header.right;
        t.right = header.left;
        
        resetLrCounts(t);
        t.count = 1 + (null != t.left ? t.left.count : 0) + (null != t.right ? t.right.count : 0);
        
        root = t;
    }
    /** repairs counts on re-linked LR subtrees from the top-down splay. 
     * The nodes on the paths to the next-lowest 
     * and next-highest elements did not have the sub-tree counts
     * updated correctly. The nodes off that path have
     * correct sub-tree counts due to preserving the counts in the rotation steps.
     * This algorithm walks the paths to the next-lowest and next-highest
     * (from <code>n</code>) and resets the counts, assuming the nodes
     * off that path have correct counts.
     */
    private void resetLrCounts(Node<E> n) {
        Node<E> y;
        int c;
        
        // reset counts on left
        c = 0;
        for (y = n.left; null != y; ) {
            c += 1;
            if (null != y.right) {
                if (null != y.left)
                    c += y.left.count;
                y = y.right;
            } else {
                y = y.left;
            }
        }
        for (y = n.left; null != y; ) {
            y.count = c;
            c -= 1;
            if (null != y.right) {
                if (null != y.left)
                    c -= y.left.count;
                y = y.right;
            } else {
                y = y.left;
            }
        }
        
        // reset counts on right
        c = 0;
        for (y = n.right; null != y; ) {
            c += 1;
            if (null != y.left) {
                if (null != y.right)
                    c += y.right.count;
                y = y.left;
            } else {
                y = y.right;
            }
        }
        for (y = n.right; null != y; ) {
            y.count = c;
            c -= 1;
            if (null != y.left) {
                if (null != y.right)
                    c -= y.right.count;
                y = y.left;
            } else {
                y = y.right;
            }
        }
    }
    
    
    /////// SortedList IMPLEMENTATION ///////
    
    @Override
    public @Nullable E lower(E value) {
        return lower(comparable(value, comparator));
    }

    @Override
    public @Nullable E lower(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (0 < r.c) {
            return r.value;
        } else if (0 < r.index) {
            return get(r.index - 1);
        } else {
            return null;
        }
    }
    
    @Override
    public int lowerIndex(E value) {
        return lowerIndex(comparable(value, comparator));
    }

    @Override
    public int lowerIndex(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (0 < r.c) {
            return r.index;
        } else {
            return r.index - 1;
        }
    }
    
    @Override
    public @Nullable E floor(E value) {
        return floor(comparable(value, comparator));
    }
    
    @Override
    public @Nullable E floor(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (0 <= r.c) {
            return r.value;
        } else if (0 < r.index) {
            return get(r.index - 1);
        } else {
            return null;
        }
    }
    
    @Override
    public int floorIndex(E value) {
        return floorIndex(comparable(value, comparator));
    }
    
    @Override
    public int floorIndex(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (0 <= r.c) {
            return r.index;
        } else {
            return r.index - 1;
        }
    }
    
    @Override
    public @Nullable E higher(E value) {
        return higher(comparable(value, comparator));
    }

    @Override
    public @Nullable E higher(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (r.c < 0) {
            return r.value;
        } else if (r.index + 1 < size()) {
            return get(r.index + 1);
        } else {
            return null;
        }
    }
    
    @Override
    public int higherIndex(E value) {
        return higherIndex(comparable(value, comparator));
    }

    @Override
    public int higherIndex(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (r.c < 0) {
            return r.index;
        } else {
            return r.index + 1;
        }
    }
    
    @Override
    public @Nullable E ceiling(E value) {
        return ceiling(comparable(value, comparator));
    }

    @Override
    public @Nullable E ceiling(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (r.c <= 0) {
            return r.value;
        } else if (r.index + 1 < size()) {
            return get(r.index + 1);
        } else {
            return null;
        }
    }
    
    @Override
    public int ceilingIndex(E value) {
        return ceilingIndex(comparable(value, comparator));
    }

    @Override
    public int ceilingIndex(Comparable<? super E> q) {
        FindResult<E> r = search(q);
        if (r.c <= 0) {
            return r.index;
        } else {
            return r.index + 1;
        }
    }
    
    /////// SortedList INSERTION IMPLEMENTATION ///////
    
    @Override
    public boolean insertAll(Collection<? extends E> values) {
    	boolean m = false;
    	for (E value : values) {
    		m |= insert(value);
    	}
    	return m;
    }

    @Override
    public boolean insert(E value) {
        if (null == value) {
            throw new NullPointerException();
        }
        
        try {
            if (null == root) {
                root = new Node<E>(value);
                return true;
            }
            
            splay(value);
            int c = comparator.compare(value, root.value);
            if (0 == c)
                return false;
            
            Node<E> n = new Node<E>(value);
            n.count += root.count;
            if (c < 0) {
                n.right = root;
                if (null != root.left) {
                    root.count -= root.left.count;
                    n.left = root.left;
                    root.left = null;
                }
            } else {
                n.left = root;
                if (null != root.right) {
                    root.count -= root.right.count;
                    n.right = root.right;
                    root.right = null;
                }
            }
            root = n;
            
            return true;
        } finally {
            assert checkInvariants();
        }
    }
    
    
    /////// List IMPLEMENTATION ///////
    
    @Override
    public int size() {
        return null != root ? root.count : 0;
    }
    
    @Override
    public boolean contains(Object value) {
        if (null == value) {
            return false;
        }
        
        try {
        	@SuppressWarnings("unchecked")
			int i = search((E) value).c;
            return 0 == i;
//            if (null == root)
//                return false;
//            splay((T) value);
//            return value.equals(root);
        } finally {
            assert checkInvariants();
        }
        
    }
    
    @Override
    public int indexOf(Comparable<? super E> q) {
        // FIXME
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int lastIndexOf(Comparable<? super E> q) {
        // FIXME
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int indexOf(Object value) {
        if (null == value) {
            return -1;
        }
        
        try {
            @SuppressWarnings("unchecked")
			FindResult<E> r = search((E) value);
            return 0 == r.c ? r.index : -1;
//            if (null == root)
//                return -1;
//            splay((T) value);
//            return null != root.left ? root.left.count : 0;
        } finally {
            assert checkInvariants();
        }
    }
    
    @Override
    public E get(int index) {
        try {
            if (null == root || index < 0 || root.count <= index)
                throw new IndexOutOfBoundsException("" + index);
            splay(index);
            assert (null != root.left ? root.left.count : 0) == index :
                "Expected root index " + index + " but found " + (null != root.left ? root.left.count : 0); 
            return root.value;
        } finally {
            assert checkInvariants();
        }
    }
    
    @Override
    public void add(int location, E object) {
        throw new UnsupportedOperationException("Inserting by index is not supported in a sorted list.");
    }
    
    @Override
    public E remove(int index) {
        try {
            if (null == root || index < 0 || root.count <= index)
                throw new IndexOutOfBoundsException();
            
            splay(index);
            E value = root.value;
            if (null == root.left) {
                root = root.right;
            } else {
                Node<E> t = root.right;
                root = root.left;
                splay(index);
                root.right = t;
                if (null != t)
                    root.count += t.count;
            }
            
            return value;
        } finally {
            assert checkInvariants();
        }
    }
    
    @Override
    public boolean remove(Object value) {
        if (null == value) {
            throw new NullPointerException();
        }
        
        try {
            if (null == root)
                return false;
            
            splay((E) value);
            int c = comparator.compare((E) value, root.value);
            if (0 != c)
                return false;
            
            if (null == root.left) {
                root = root.right;
            } else {
                Node<E> t = root.right;
                root = root.left;
                splay((E) value);
                root.right = t;
                if (null != t)
                    root.count += t.count;
            }
            
            return true;
        } finally {
            assert checkInvariants();
        }
    }
    
    @Override
    public void clear() {
        try {
            root = null;
        } finally {
            assert checkInvariants();
        }
    }
    
    
    /////// INVARIANTS ///////

    public boolean checkInvariants() {
        if (null != root) {
            if (size() < 128) {
                 // uses recursion; only call this for small trees
                _checkCount(root);
                _checkBst(root);
            } else {
                // FIXME these checks can be done by traversing the tree and maintaining state
                // FIXME but need to implement the iterator (also a TODO above)
            }
        }
        return true;
    }
    private int _checkCount(Node<E> n) {
        int expectedCount = 1;
        if (null != n.left)
            expectedCount += _checkCount(n.left);
        if (null != n.right)
            expectedCount += _checkCount(n.right);
        assert expectedCount == n.count : String.format("%d <> %d", expectedCount, n.count);
        return expectedCount;
    }
    private void _checkBst(Node<E> n) {
        if (null != n.left) {
            assert comparator.compare(n.left.value, n.value) <= 0 : String.format("%s <> %s (%d)",
                    n.left.value, n.value, comparator.compare(n.left.value, n.value));
            _checkBst(n.left);
        }
        if (null != n.right) {
            assert 0 <= comparator.compare(n.right.value, n.value) : String.format("%s <> %s (%d)",
                    n.right.value, n.value, comparator.compare(n.right.value, n.value));
            _checkBst(n.right);
        }
    }
    
    
    /////// INTERNAL ///////
    
    private static final class Node<T> {
        final @Nullable T value;
        int count = 1;
        @Nullable Node<T> left = null;
        @Nullable Node<T> right = null;

        Node(@Nullable T value) {
            this.value = value;
        }
    }
    
    
    private static <T> Comparable<T> comparable(final T value, final Comparator<? super T> comparator) {
        return new Comparable<T>() {
            @Override
            public int compareTo(T another) {
                return comparator.compare(value, another);
            }
        };
    }

    // FIXME remove this - (see notes at top)
    // FIXME use splay instead, and compare with the root after splay to derive c
    static final class FindResult<T> {
        public final T value;
        /** index of value */
        public final int index;
        /** the result of q.compare(value) */
        public final int c;
        
        FindResult(T value, int index, int c) {
            this.value = value;
            this.index = index;
            this.c = c;
        }
    }
    
    
    /** Tests if for successive items <code>x</code> from the iterator,
     * <code>q.compareTo(x)</code> is monotonically increasing. */
    private static <T> boolean isMonotonicallyIncreasing(Iterator<T> sortedItr, Comparable<? super T> q) {
        if (!sortedItr.hasNext()) {
            return true;
        }
        int ps = -1;
        while (sortedItr.hasNext()) {
            int c = q.compareTo(sortedItr.next());
            int s = c < 0 ? -1 : 0 < c ? 1 : 0;
            if (s < ps) {
                return false;
            }
            ps = s;
        }
        return true;
    }
}
