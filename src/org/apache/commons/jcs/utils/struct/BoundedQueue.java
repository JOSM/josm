package org.apache.commons.jcs.utils.struct;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This is a bounded queue. It only allows maxSize items.
 * <p>
 * @author Aaron Smuts
 */
public class BoundedQueue<T>
{
    /** Queue size limit. */
    private final int maxSize;

    /** The list backing the queue */
    private final DoubleLinkedList<DoubleLinkedListNode<T>> list =
        new DoubleLinkedList<DoubleLinkedListNode<T>>();

    /**
     * Initialize the bounded queue.
     * <p>
     * @param maxSize
     */
    public BoundedQueue( int maxSize )
    {
        this.maxSize = maxSize;
    }

    /**
     * Adds an item to the end of the queue, which is the front of the list.
     * <p>
     * @param object
     */
    public void add( T object )
    {
        if ( list.size() >= maxSize )
        {
            list.removeLast();
        }
        list.addFirst( new DoubleLinkedListNode<T>( object ) );
    }

    /**
     * Takes the last of the underlying double linked list.
     * <p>
     * @return null if it is epmpty.
     */
    public T take()
    {
        DoubleLinkedListNode<T> node = list.removeLast();
        if ( node != null )
        {
            return node.getPayload();
        }
        return null;
    }

    /**
     * Return the number of items in the queue.
     * <p>
     * @return size
     */
    public int size()
    {
        return list.size();
    }

    /**
     * Return true if the size is <= 0;
     * <p>
     * @return true is size <= 0;
     */
    public boolean isEmpty()
    {
        return list.size() <= 0;
    }
}
