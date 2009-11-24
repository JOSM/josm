/*
 *  JOSMng - a Java Open Street Map editor, the next generation.
 *
 *  Copyright (C) 2008 Petr Nejedly <P.Nejedly@sh.cvut.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.openstreetmap.josm.tools;

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * A List implementation initially based on given array, but never modifying
 * the array directly. On the first modification, the implementation will
 * create its own copy of the array, and after that it behaves mostly as
 * an ArrayList.
 *
 * @author nenik
 */
public class CopyList<E> extends AbstractList<E> implements RandomAccess, Cloneable {
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

    private CopyList(E[] array, int size) {
        this.array = array;
        this.size = size;
        pristine = true;
    }

    // read-only access:
    public @Override E get(int index) {
        rangeCheck(index);
        return array[index];
    }

    public @Override int size() {
        return size;
    }

    // modification:
    public @Override E set(int index, E element) {
        rangeCheck(index);
        changeCheck();

        E old = array[index];
        array[index] = element;
        return old;
    }

    // full resizable semantics:
    public @Override void add(int index, E element) {
        // range check
        ensureCapacity(size+1);
        changeCheck();

        System.arraycopy(array, index, array, index+1, size-index);
        array[index] = element;
        size++;
    }

    public @Override E remove(int index) {
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
    public @Override boolean add(E element) {
        ensureCapacity(size+1);
        changeCheck();
        array[size++] = element;
        return true;
    }

    public @Override void clear() {
    modCount++;

        // clean up the array
        while (size > 0) array[--size] = null;
    }

    // helpers:
    /**
     * Returns another independent copy-on-write copy of this <tt>List</tt>
     * instance. Neither the elements nor the backing storage are copied.
     *
     * @return a clone of this <tt>CopyList</tt> instance
     */
    public @Override Object clone() {
        return new CopyList<E>(array, size);
    }

    private void rangeCheck(int index) {
    if (index >= size || index < 0) throw new IndexOutOfBoundsException();
    }

    private void changeCheck() {
        if (pristine) {
            array = array.clone();
            pristine = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int target) {
        modCount++;
        if (target > array.length) {
            E[] old = array;

            int newCapacity = Math.max(target, (array.length * 3)/2 + 1);
            array = (E[]) new Object[newCapacity];
            System.arraycopy(old, 0, array, 0, size);
            pristine = false;
    }
    }
}
