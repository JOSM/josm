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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is an basic thread safe single linked list. It provides very limited functionality. It is
 * small and fast.
 * <p>
 * @author Aaron Smuts
 */
public class SingleLinkedList<T>
{
    /** The logger */
    private static final Log log = LogFactory.getLog( SingleLinkedList.class );

    /** for sync */
    private final Object lock = new Object();

    /** the head of the queue */
    private Node<T> head = new Node<T>();

    /** the end of the queue */
    private Node<T> tail = head;

    /** The size of the list */
    private int size = 0;

    /**
     * Takes the first item off the list.
     * <p>
     * @return null if the list is empty.
     */
    public T takeFirst()
    {
        synchronized ( lock )
        {
            // wait until there is something to read
            if ( head == tail )
            {
                return null;
            }

            Node<T> node = head.next;

            T value = node.payload;

            if ( log.isDebugEnabled() )
            {
                log.debug( "head.payload = " + head.payload );
                log.debug( "node.payload = " + node.payload );
            }

            // Node becomes the new head (head is always empty)

            node.payload = null;
            head = node;

            size--;
            return value;
        }
    }

    /**
     * Adds an item to the end of the list.
     * <p>
     * @param payload
     */
    public void addLast( T payload )
    {
        Node<T> newNode = new Node<T>();

        newNode.payload = payload;

        synchronized ( lock )
        {
            size++;
            tail.next = newNode;
            tail = newNode;
        }
    }

    /**
     * Removes everything.
     */
    public void clear()
    {
        synchronized ( lock )
        {
            head = tail;
            size = 0;
        }
    }

    /**
     * The list is composed of nodes.
     * <p>
     * @author Aaron Smuts
     */
    protected static class Node<T>
    {
        /** next in the list */
        Node<T> next = null;

        /** The data in this node */
        T payload;
    }

    /**
     * Returns the number of elements in the list.
     * <p>
     * @return number of items in the list.
     */
    public int size()
    {
        return size;
    }
}
