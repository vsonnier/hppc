package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * A Heap-based, min-priority queue of <code>KType</code>s.
 * i.e. top() is the smallest element,
 * as defined by Sedgewick: Algorithms 4th Edition (2011)
 * It assure O(log2(N)) complexity for insertion, deletion of min element,
 * and constant time to examine the first element.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $DEBUG = false) !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeHeapPriorityQueue<KType> extends AbstractKTypeCollection<KType>
        implements KTypePriorityQueue<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 16;

    /**
     * Internal array for storing the priority queue
     * The array may be larger than the current size
     * ({@link #size()}).
     * <p>
     * Direct priority queue iteration: iterate buffer[i] for i in [1; size()] (included)
     * </p>
     */
    public KType[] buffer;

    /**
     * Number of elements in the queue.
     */
    protected int elementsCount;

    /**
     * Defines the Comparator ordering of the queue,
     * If null, natural ordering is used.
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    protected Comparator<? super KType> comparator;
    /*! #else
    protected KTypeComparator<? super KType> comparator;
    #end !*/

    /**
     * Buffer resizing strategy.
     */
    protected final ArraySizingStrategy resizer;

    /**
     * internal pool of ValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, ValueIterator> valueIteratorPool;

    /**
     * Create with a Comparator, an initial capacity, and a custom buffer resizing strategy.
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/final Comparator<? super KType> comp,
            /*! #else
            KTypeComparator<? super KType> comp,
            #end !*/final int initialCapacity, final ArraySizingStrategy resizer)
    {
        this.comparator = comp;

        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;

        //1-based index buffer, assure allocation
        final int internalSize = resizer.round(initialCapacity);
        this.buffer = Intrinsics.newKTypeArray(internalSize + 1);

        this.valueIteratorPool = new IteratorPool<KTypeCursor<KType>, ValueIterator>(
                new ObjectFactory<ValueIterator>() {

                    @Override
                    public ValueIterator create()
                    {
                        return new ValueIterator();
                    }

                    @Override
                    public void initialize(final ValueIterator obj)
                    {
                        obj.cursor.index = 0;
                        obj.size = KTypeHeapPriorityQueue.this.size();
                        obj.buffer = KTypeHeapPriorityQueue.this.buffer;
                    }

                    @Override
                    public void reset(final ValueIterator obj) {
                        // for GC sake
                        obj.buffer = null;

                    }
                });
    }

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/final Comparator<? super KType> comp
    /*! #else
    KTypeComparator<? super KType> comp
    #end !*/)
    {
        this(comp, KTypeHeapPriorityQueue.DEFAULT_CAPACITY);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Create with an initial capacity,
     * using the Comparable natural ordering
     */
    /*! #else !*/
    /**
     * Create with an initial capacity,
     * using the natural ordering of <code>KType</code>s
     */
    /*! #end !*/
    public KTypeHeapPriorityQueue(final int initialCapacity)
    {
        this(null, initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a given initial capacity, using a
     * Comparator for ordering.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/final Comparator<? super KType> comp,
            /*! #else
            KTypeComparator<? super KType> comp,
            #end !*/final int initialCapacity)
    {
        this(comp, initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e1)
    {
        //remove by position
        int deleted = 0;
        final KType[] buffer = this.buffer;

        int elementsCount = this.elementsCount;

        //1-based index
        int pos = 1;

        while (pos <= elementsCount)
        {
            //delete it
            if (Intrinsics.equalsKType(e1, buffer[pos]))
            {
                //put the last element at position pos, like in deleteIndex()
                buffer[pos] = buffer[elementsCount];

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //Diminish size
                elementsCount--;
                deleted++;
            } //end if to delete
            else
            {
                pos++;
            }
        } //end while

        this.elementsCount = elementsCount;

        //reestablish heap
        refreshPriorities();

        /*! #if($DEBUG) !*/
        assert isMinHeap();
        /*! #end !*/

        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        //remove by position
        int deleted = 0;
        final KType[] buffer = this.buffer;
        int elementsCount = this.elementsCount;

        //1-based index
        int pos = 1;

        try
        {
            while (pos <= elementsCount)
            {
                //delete it
                if (predicate.apply(buffer[pos]))
                {
                    //put the last element at position pos, like in deleteIndex()
                    buffer[pos] = buffer[elementsCount];

                    //for GC
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/

                    //Diminish size
                    elementsCount--;
                    deleted++;
                } //end if to delete
                else
                {
                    pos++;
                }
            } //end while
        }
        finally
        {
            this.elementsCount = elementsCount;
            //reestablish heap
            refreshPriorities();
        }

        /*! #if($DEBUG) !*/
        assert isMinHeap();
        /*! #end !*/

        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        //1-based indexing
        Internals.blankObjectArray(buffer, 1, elementsCount + 1);
        /*! #end !*/
        this.elementsCount = 0;
    }

    /**
     * An iterator implementation for {@link HeapPriorityQueue#iterator}.
     */
    public final class ValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        KType[] buffer;
        int size;

        public ValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            //index 0 is not used in Priority queue
            this.cursor.index = 0;
            this.size = KTypeHeapPriorityQueue.this.size();
            this.buffer = KTypeHeapPriorityQueue.this.buffer;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            //priority is 1-based index
            if (cursor.index == size)
                return done();

            cursor.value = buffer[++cursor.index];
            return cursor;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueIterator iterator()
    {
        //return new ValueIterator<KType>(buffer, size());
        return this.valueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final KType element)
    {
        //1-based index
        final int size = elementsCount;
        final KType[] buff = this.buffer;

        for (int i = 1; i <= size; i++)
        {
            if (Intrinsics.equalsKType(element, buff[i]))
            {
                return true;
            }
        } //end for

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return elementsCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        final KType[] buff = this.buffer;
        final int size = elementsCount;

        for (int i = 1; i <= size; i++)
        {
            procedure.apply(buff[i]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        final KType[] buff = this.buffer;
        final int size = elementsCount;

        for (int i = 1; i <= size; i++)
        {
            if (!predicate.apply(buff[i]))
                break;
        }

        return predicate;
    }

    /**
     * Insert a KType into the queue.
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public void insert(final KType element)
    {
        ensureBufferSpace(1);

        //add at the end
        elementsCount++;
        buffer[elementsCount] = element;

        //swim last element
        swim(elementsCount);
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public KType top()
    {
        KType elem = this.defaultValue;

        if (elementsCount > 0)
        {
            elem = buffer[1];
        }

        return elem;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public KType popTop()
    {
        KType elem = this.defaultValue;

        if (elementsCount > 0)
        {
            elem = buffer[1];

            if (elementsCount == 1)
            {
                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[1] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
                //diminuish size
                elementsCount = 0;
            }
            else
            {
                //at least 2 elements
                //put the last element in first position

                buffer[1] = buffer[elementsCount];

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //diminuish size
                elementsCount--;

                //percolate down the first element
                sink(1);
            }
        }

        return elem;
    }

    /**
     * Adds all elements from another container.
     * cost: O(N*log2(N)) for N elements
     */
    public int addAll(final KTypeContainer<? extends KType> container)
    {
        final int size = container.size();
        ensureBufferSpace(size);

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            elementsCount++;
            buffer[elementsCount] = cursor.value;
        }

        //restore heap
        refreshPriorities();
        /*! #if($DEBUG) !*/
        assert isMinHeap();
        /*! #end !*/

        return size;
    }

    /**
     * Adds all elements from another iterable.
     * cost: O(N*log2(N)) for N elements
     */
    public int addAll(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        final KType[] buff = this.buffer;
        int count = this.elementsCount;

        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            ensureBufferSpace(1);
            count++;
            buff[count] = cursor.value;
            size++;
        }

        this.elementsCount = count;

        //restore heap
        refreshPriorities();
        /*! #if($DEBUG) !*/
        assert isMinHeap();
        /*! #end !*/

        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 1;
        final int max = elementsCount;
        final KType[] buff = this.buffer;

        //1-based index
        for (int i = 1; i <= max; i++)
        {
            h = 31 * h + Internals.rehash(buff[i]);
        }
        return h;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPriorities()
    {
        if (this.comparator == null)
        {
            for (int k = elementsCount >> 1; k >= 1; k--)
            {
                sinkComparable(k);
            }
        }
        else
        {
            for (int k = elementsCount >> 1; k >= 1; k--)
            {
                sinkComparator(k);
            }
        }
    }

    /**
     * Clone this object. The returned clone will use the same resizing strategy and comparator.
     */
    @Override
    public KTypeHeapPriorityQueue<KType> clone()
    {
        //real constructor call
        final KTypeHeapPriorityQueue<KType> cloned = new KTypeHeapPriorityQueue<KType>(this.comparator, this.buffer.length + 1, this.resizer);

        cloned.addAll(this);

        cloned.defaultValue = this.defaultValue;

        return cloned;
    }

    /**
     * this instance and obj can only be equal if either: <pre>
     * (both don't have set comparators)
     * or
     * (both have equal comparators defined by {@link #comparator}.equals(obj.comparator))</pre>
     * then, both heaps are compared as follows: <pre>
     * {@inheritDoc}</pre>
     */
    @Override
/* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
/* #end */
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
                return true;

            //we can only compare both KTypeHeapPriorityQueue,
            //that has the same comparison function reference
            if (obj instanceof KTypeHeapPriorityQueue<?>)
            {
                final KTypeHeapPriorityQueue<KType> other = (KTypeHeapPriorityQueue<KType>) obj;

                if (other.size() != this.size()) {

                    return false;
                }

                final int size = this.elementsCount;
                final KType[] buffer = this.buffer;
                final KType[] otherbuffer = other.buffer;

                //both heaps must have the same comparison criteria
                if (this.comparator == null && other.comparator == null) {

                    for (int i = 1; i <= size; i++)
                    {
                        if (!Intrinsics.isCompEqualKTypeUnchecked(buffer[i], otherbuffer[i]))
                        {
                            return false;
                        }
                    }

                    return true;
                }
                else if (this.comparator != null && this.comparator.equals(other.comparator)) {

                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    final Comparator<? super KType> comp = this.comparator;
                    /*! #else
                    KTypeComparator<? super KType> comp = this.comparator;
                    #end !*/

                    for (int i = 1; i <= size; i++)
                    {
                        if (comp.compare(buffer[i], otherbuffer[i]) != 0)
                        {
                            return false;
                        }
                    }

                    return true;
                }
            } //end if KTypeHeapPriorityQueue<?>
        }

        return false;
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(final int expectedAdditions)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length - 1);
        if (elementsCount > bufferLen - expectedAdditions)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions + 1);
            assert newSize >= elementsCount + expectedAdditions : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= "
                    + (elementsCount + expectedAdditions);

            final KType[] newBuffer = Intrinsics.newKTypeArray(newSize);
            if (bufferLen > 0)
            {
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            }
            this.buffer = newBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        //copy from index 1
        System.arraycopy(buffer, 1, target, 0, elementsCount);
        return target;
    }

    /**
     * Sink function for Comparable elements
     * @param k
     */
    private void sinkComparable(int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        KType tmp;
        int child;

        while ((k << 1) <= N)
        {
            //get the child of k
            child = k << 1;

            if (child < N && Intrinsics.isCompSupKTypeUnchecked(buffer[child], buffer[child + 1]))
            {
                child++;
            }

            if (!Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[child]))
            {
                break;
            }

            //swap k and child
            tmp = buffer[k];
            buffer[k] = buffer[child];
            buffer[child] = tmp;

            k = child;
        } //end while
    }

    /**
     * Sink function for KTypeComparator elements
     * @param k
     */
    private void sinkComparator(int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        KType tmp;
        int child;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
        #end !*/

        while ((k << 1) <= N)
        {
            //get the child of k
            child = k << 1;

            if (child < N && comp.compare(buffer[child], buffer[child + 1]) > 0)
            {
                child++;
            }

            if (comp.compare(buffer[k], buffer[child]) <= 0)
            {
                break;
            }

            //swap k and child
            tmp = buffer[k];
            buffer[k] = buffer[child];
            buffer[child] = tmp;

            k = child;
        } //end while
    }

    /**
     * Swim function for Comparable elements
     * @param k
     */
    private void swimComparable(int k)
    {
        KType tmp;
        int parent;
        final KType[] buffer = this.buffer;

        while (k > 1 && Intrinsics.isCompSupKTypeUnchecked(buffer[k >> 1], buffer[k]))
        {
            //swap k and its parent
            parent = k >> 1;

            tmp = buffer[k];
            buffer[k] = buffer[parent];
            buffer[parent] = tmp;

            k = parent;
        }
    }

    /**
     * Swim function for Comparator elements
     * @param k
     */
    private void swimComparator(int k)
    {
        KType tmp;
        int parent;
        final KType[] buffer = this.buffer;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
        #end !*/

        while (k > 1 && comp.compare(buffer[k >> 1], buffer[k]) > 0)
        {
            //swap k and its parent
            parent = k >> 1;
            tmp = buffer[k];
            buffer[k] = buffer[parent];
            buffer[parent] = tmp;

            k = parent;
        }
    }

    private void swim(final int k)
    {
        if (this.comparator == null)
        {
            swimComparable(k);
        }
        else
        {
            swimComparator(k);
        }
    }

    private void sink(final int k)
    {
        if (this.comparator == null)
        {
            sinkComparable(k);
        }
        else
        {
            sinkComparator(k);
        }
    }

    /*! #if($DEBUG) !*/
    /**
     * methods to test invariant in assert, not present in final generated code
     */
// is pq[1..N] a min heap?
    private boolean isMinHeap()
    {
        if (this.comparator == null)
        {
            return isMinHeapComparable(1);
        }

        return isMinHeapComparator(1);
    }

// is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparable(final int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[left]))
            return false;
        if (right <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[right]))
            return false;
        //recursively test
        return isMinHeapComparable(left) && isMinHeapComparable(right);
    }

// is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparator(final int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
        #end !*/

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && comp.compare(buffer[k], buffer[left]) > 0)
            return false;
        if (right <= N && comp.compare(buffer[k], buffer[right]) > 0)
            return false;
        //recursively test
        return isMinHeapComparator(left) && isMinHeapComparator(right);
    }

    //end of ifdef debug
/*! #end !*/
}
