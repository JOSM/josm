// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A List implementation initially based on given array, but never modifying
 * the array directly. On the first modification, the implementation will
 * create its own copy of the array, and after that it behaves mostly as
 * an ArrayList.
 *
 * @author nenik
 */
public final class CopyList<E> extends AbstractList<E> implements RandomAccess, Cloneable {
    private E[] array;
    private int size;
    private boolean pristine;

    /**
     * Create a List over given array.
     * @param array The initial List content. The array is never modified
     * by the {@code CopyList}.
     */
    public CopyList(E[] array) {
        this(array, array.length);
    }

    public CopyList(E[] array, int size) {
        this.array = array;
        this.size = size;
        pristine = true;
    }

    // read-only access:
    @Override
    public E get(int index) {
        rangeCheck(index);
        return array[index];
    }

    @Override
    public int size() {
        return size;
    }

    // modification:
    @Override
    public E set(int index, E element) {
        rangeCheck(index);
        changeCheck();

        E old = array[index];
        array[index] = element;
        return old;
    }

    // full resizable semantics:
    @Override
    public void add(int index, E element) {
        // range check
        ensureCapacity(size+1);
        changeCheck();

        System.arraycopy(array, index, array, index+1, size-index);
        array[index] = element;
        size++;
    }

    @Override
    public E remove(int index) {
        rangeCheck(index);
        changeCheck();

        modCount++;
        E element = array[index];
        if (index < size-1) {
            System.arraycopy(array, index+1, array, index, size-index-1);
        } else {
            array[index] = null;
        }
        size--;
        return element;
    }

    // speed optimizations:
    @Override
    public boolean add(E element) {
        ensureCapacity(size+1);
        changeCheck();
        array[size++] = element;
        return true;
    }

    @Override
    public void clear() {
        modCount++;

        // clean up the array
        while (size > 0) {
            array[--size] = null;
        }
    }

    // helpers:
    /**
     * Returns another independent copy-on-write copy of this <tt>List</tt>
     * instance. Neither the elements nor the backing storage are copied.
     *
     * @return a clone of this <tt>CopyList</tt> instance
     */
    @Override
    public Object clone() {
        return new CopyList<E>(array, size);
    }

    private void rangeCheck(int index) {
        if (index >= size || index < 0) throw new IndexOutOfBoundsException("Index:" + index + " Size:" + size);
    }

    private void changeCheck() {
        if (pristine) {
            array = array.clone();
            pristine = false;
        }
    }

    private void ensureCapacity(int target) {
        modCount++;
        if (target > array.length) {
            int newCapacity = Math.max(target, (array.length * 3)/2 + 1);
            array = Arrays.copyOf(array, newCapacity);
            pristine = false;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         */
        int lastRet = -1;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        int expectedModCount = modCount;

        @Override
        public boolean hasNext() {
            return cursor != size;
        }

        @Override
        public E next() {
            checkForComodification();
            try {
                E next = array[cursor];
                lastRet = cursor++;
                return next;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if (lastRet == -1)
                throw new IllegalStateException();
            checkForComodification();

            try {
                CopyList.this.remove(lastRet);
                if (lastRet < cursor) {
                    cursor--;
                }
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

}
