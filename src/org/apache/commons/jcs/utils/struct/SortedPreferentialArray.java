package org.apache.commons.jcs.utils.struct;

import java.util.concurrent.ConcurrentSkipListSet;

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
 * This maintains a sorted array with a preferential replacement policy when full.
 * <p>
 * Clients must manage thread safety on previous version. I synchronized the public methods to add
 * easy thread safety. I synchronized all public methods that make modifications.
 */
public class SortedPreferentialArray<T extends Comparable<? super T>>
{
    /** The logger */
    private static final Log log = LogFactory.getLog( SortedPreferentialArray.class );

    /** prefer large means that the smallest will be removed when full. */
    private boolean preferLarge = true;

    /** maximum number allowed */
    private int maxSize = 0;

    /** The current number */
    private int curSize = 0;

    /** The primary array */
    private final ConcurrentSkipListSet<T> array;

    /**
     * Construct the array with the maximum size.
     * <p>
     * @param maxSize int
     */
    public SortedPreferentialArray( int maxSize )
    {
        this.maxSize = maxSize;
        array = new ConcurrentSkipListSet<T>();
    }

    /**
     * If the array is full this will remove the smallest if preferLarge==true and if obj is bigger,
     * or the largest if preferLarge=false and obj is smaller than the largest.
     * <p>
     * @param obj Object
     */
    public synchronized void add(T obj)
    {
        if ( obj == null )
        {
            return;
        }

        if ( curSize < maxSize )
        {
            insert( obj );
        }
        else if ( preferLarge )
        {
            // insert if obj is larger than the smallest
            if ( obj.compareTo( array.first() ) > 0 )
            {
                insert( obj );
            }
            // obj is less than or equal to the smallest.
            else if ( log.isDebugEnabled() )
            {
                log.debug( "New object is smaller than or equal to the smallest" );
            }
        }
        else
        {
	        // Not preferLarge
	        // insert if obj is smaller than the largest
	        if ( obj.compareTo( array.last() ) >= 0)
	        {
	            if ( log.isDebugEnabled() )
	            {
	                log.debug( "New object is larger than or equal to the largest" );
	            }
	        }
	        else
	        {
		        // obj is less than the largest.
		        insert( obj );
	        }
        }
    }

    /**
     * Returns the largest without removing it from the array.
     * <p>
     * @return Comparable
     */
    protected T getLargest()
    {
        return array.last();
    }

    /**
     * Returns the smallest element without removing it from the array.
     * <p>
     * @return Comparable
     */
    protected T getSmallest()
    {
        return array.first();
    }

    
    /**
     * Insert looks for the nearest largest. It then determines which way to shuffle depending on
     * the preference.
     * <p>
     * @param obj Comparable
     */
    private void insert(T obj)
    {
    	if (array.add(obj))
    	{
            if ( curSize < maxSize )
            {
            	curSize++;
            }
            else if (preferLarge)
            {
            	array.pollFirst();
            }
            else
            {
            	array.pollLast();
            }
    	}
    }

    /**
     * Determines whether the preference is for large or small.
     * <p>
     * @param pref boolean
     */
    public synchronized void setPreferLarge( boolean pref )
    {
        preferLarge = pref;
    }

    /**
     * Returns and removes the nearer larger or equal object from the array.
     * <p>
     * @param obj Comparable
     * @return Comparable, null if arg is null or none was found.
     */
    public synchronized T takeNearestLargerOrEqual( T obj )
    {
    	if (obj == null)
    	{
    		return null;
    	}
    	
    	T t = array.ceiling(obj);
    	
    	if (t != null)
    	{
	    	array.remove(t);
	    	curSize--;
    	}
    	
    	return t;
    }

    /**
     * Returns the current size of the array.
     * <p>
     * @return int
     */
    public synchronized int size()
    {
        return this.curSize;
    }
    
    /**
     * Get the underlying array (for testing)
     * 
     * @return a copy of the array
     */
	protected Object[] getArray()
    {
    	return array.toArray();
    }
}
