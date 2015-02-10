package io.nextop.sortedlist;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/** A {@link java.util.List} that further provides a total ordering on its elements.
 * The elements are ordered using their natural ordering, 
 * or by a {@link java.util.Comparator} typically provided at sorted list creation time. 
 * The list's iterator will traverse the list in ascending element order. 
 * Several additional operations are provided to take advantage of the ordering. 
 * (This interface is the list analogue of {@link java.util.SortedSet}.) 
 * 
 * <p>All elements inserted into a sorted list must implement the {@link java.lang.Comparable} interface
 * (or be accepted by the specified comparator).
 * 
 * <p>Unlike a {@link java.util.SortedSet}, the ordering maintained by a sorted list 
 * (whether or not an explicit comparator is provided) does not have to be consistent with equals
 * (for the definition in {@link java.util.Comparator}).
 * 
 * <p>Unlike a {@link java.util.SortedSet}, a sorted list allows duplicate values.
 * The sorted list analogues of the {@link java.util.SortedSet} operations 
 * are defined in the case of duplicate elements. 
 * 
 * <p>Useful (optional) extensions 
 * are to implement variants of the operations that handle a {@link java.lang.Comparable} query,
 * which must monotonically increase over the ordered elements. These extensions are 
 * coherent because the case of duplicates is well defined, and can be
 * efficiently implemented because of the property below.
 * 
 * <p>The optional {@link java.lang.Comparable} query operations are as if:
 * <ol>
 * <li>the elements were inserted into a new sorted list,
 *   using a stable sort and the comparator 
 *   <code>(a, b) -&gt; signum(q.compareTo(b)) - signum(q.compareTo(a))</code></li>
 * <li>the analogous element operations were applied for some element <code>t</code>
 *   such that <code>q.compareTo(t) == 0</code></li>
 * </ol>
 * However, because new sorted list would have the elements in the same order as the original,
 * and <code>compare(t, x) == q.compareTo(x)</code>, 
 * the optional operations can traverse the original structure
 * using the query comparable instead of the comparator,
 * and achieve the same result as if building a new sorted list. */
public interface SortedList<E> extends List<E> {
    /** Unsupported operation.
     * (this cannot satisfy both the sorted list contract and the {@link java.util.List#add} contract)
     * @see #insert */
    @Override
    @Deprecated
    boolean add(E e);
    /** Unsupported operation.
     * The insertion position is determined by the total ordering and cannot be specified by the caller. 
     * @see #insert */
    @Override
    @Deprecated
    void add(int index, E e);
    @Override
    @Deprecated
    /** Unsupported operation.
     * (this cannot satisfy both the sorted list contract and the {@link java.util.List#addAll} contract)
     * @see #insertAll */
    boolean addAll(Collection<? extends E> c);
    /** Unsupported operation.
     * The insertion position is determined by the total ordering and cannot be specified by the caller. 
     * @see #insertAll */
    @Override
    @Deprecated
    boolean addAll(int index, Collection<? extends E> c);
    
    
    
    
    /** Inserts the specified element into the list at a position according to the total order (optional operation).
     * @return as specified by {@link java.util.Collection#add} */
    boolean insert(E value);
    /** Inserts all of the elements in the specified collection at positions according to the total order, 
     * in the order that they are returned by the specified collection's iterator (optional operation).
     * @return as specified by {@link java.util.Collection#addAll} */
    boolean insertAll(Collection<? extends E> values);


    /** (optional operation) 
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the least index <code>i</code> in this list where <code>q.compareTo(get(i)) == 0</code>, 
     * or <code>-1</code> if there is no such element. */
    int indexOf(Comparable<? super E> q);
    
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the greatest index <code>i</code> in this list where <code>q.compareTo(get(i)) == 0</code>, 
     * or <code>-1</code> if there is no such element. */
    int lastIndexOf(Comparable<? super E> q);
    
    
    /** @return the greatest element in this list strictly less than the given element, 
     * or <code>null</code> if there is no such element.
     * @see java.util.NavigableSet#lower */
    @Nullable E lower(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the greatest element <code>x</code> in this list where <code>q.compareTo(x) &lt; 0</code>, 
     * or <code>null</code> if there is no such element. */
    @Nullable E lower(Comparable<? super E> q);
    
    
    /** @return the greatest index in this set strictly less than the given element, 
     * or <code>-1</code> if there is no such element. */
    int lowerIndex(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     *  @return the greatest index <code>i</code> in this list where <code>q.compareTo(get(i)) &lt; 0</code>, 
     * or <code>-1</code> if there is no such element. */
    int lowerIndex(Comparable<? super E> q);
    
    
    /** @return the greatest element in this list less than the given element,
     * or the least element in this least equal to the given element,  
     * or <code>null</code> if there is no such element.
     * @see java.util.NavigableSet#floor */
    @Nullable E floor(E value);
    /** @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the greatest element <code>x</code> in this list where <code>q.compareTo(x) &lt; 0</code>, 
     * or the least element <code>x</code> in this list where <code>q.compareTo(x) == 0</code>,
     * or <code>-1</code> if there is no such element. */
    @Nullable E floor(Comparable<? super E> q);
    
    
    /** @return the greatest index in this list less than the given element,
     * or the least index in this least equal to the given element,  
     * or <code>-1</code> if there is no such element. */
    int floorIndex(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the greatest index <code>i</code> in this list where <code>q.compareTo(get(i)) &lt; 0</code>, 
     * or the least index <code>i</code> in this list where <code>q.compareTo(get(i)) == 0</code>,
     * or <code>-1</code> if there is no such element. */
    int floorIndex(Comparable<? super E> q);
    
    
    /** @return the least element in this list strictly greater than the given element, 
     * or <code>null</code> if there is no such element.
     * @see java.util.NavigableSet#higher */
    @Nullable E higher(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the least element <code>x</code> in this list where <code>0 &lt; q.compareTo(x)</code>, 
     * or <code>null</code> if there is no such element.
     * @see java.util.NavigableSet#higher */
    @Nullable E higher(Comparable<? super E> q);
    
    
    /** @return the least index in this list strictly greater than the given element, 
     * or <code>size()</code> if there is no such element. */
    int higherIndex(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the least index <code>i</code> in this list where <code>0 &lt; q.compareTo(get(i))</code>, 
     * or <code>size()</code> if there is no such element. */
    int higherIndex(Comparable<? super E> q);
    
    
    /** @return the least element in this list greater than the given element,
     * or the greatest element in this list equal to the given element,
     * or <code>null</code> if there is no such element. 
     * @see java.util.NavigableSet#ceiling */
    @Nullable E ceiling(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the least element <code>x</code> in this list where <code>0 &lt; q.compareTo(x)</code>,
     * or the greatest element <code>x</code> in this list where where <code>q.compareTo(x) == 0</code>,
     * or <code>null</code> if there is no such element. */
    @Nullable E ceiling(Comparable<? super E> q);
    
    
    /** @return the least index in this list greater than the given element,
     * or the greatest index in this list equal to the given element,
     * or <code>size()</code> if there is no such element. */
    int ceilingIndex(E value);
    /** (optional operation)
     * @param q <code>compareTo</code> must be monotonically increasing over the ordered elements in the list 
     * @return the least index <code>i</code> in this list where <code>0 &lt; q.compareTo(get(i))</code>,
     * or the greatest index <code>i</code> in this list where where <code>q.compareTo(get(i)) == 0</code>,
     * or <code>size()</code> if there is no such element. */
    int ceilingIndex(Comparable<? super E> q);
}
