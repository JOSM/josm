package org.apache.commons.jcs.auxiliary.disk.indexed;

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

import java.io.Serializable;

/**
 * Disk objects are located by descriptor entries. These are saved on shutdown and loaded into
 * memory on startup.
 */
public class IndexedDiskElementDescriptor
    implements Serializable, Comparable<IndexedDiskElementDescriptor>
{
    /** Don't change */
    private static final long serialVersionUID = -3029163572847659450L;

    /** Position of the cache data entry on disk. */
    long pos;

    /** Number of bytes the serialized form of the cache data takes. */
    int len;

    /**
     * Constructs a usable disk element descriptor.
     * <p>
     * @param pos
     * @param len
     */
    public IndexedDiskElementDescriptor( long pos, int len )
    {
        this.pos = pos;
        this.len = len;
    }

    /**
     * @return debug string
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "[DED: " );
        buf.append( " pos = " + pos );
        buf.append( " len = " + len );
        buf.append( "]" );
        return buf.toString();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Long.valueOf(this.pos).hashCode() ^ Integer.valueOf(len).hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o)
    {
    	if (o == null)
    	{
    		return false;
    	}
    	else if (o instanceof IndexedDiskElementDescriptor)
        {
    		IndexedDiskElementDescriptor ided = (IndexedDiskElementDescriptor)o;
            return pos == ided.pos && len == ided.len;
        }

        return false;
    }

    /**
     * Compares based on length, then on pos descending.
     * <p>
     * @param o Object
     * @return int
     */
    @Override
    public int compareTo( IndexedDiskElementDescriptor o )
    {
        if ( o == null )
        {
            return 1;
        }

        if ( o.len == len )
        {
        	if ( o.pos == pos )
        	{
        		return 0;
        	}
        	else if ( o.pos < pos )
        	{
        		return -1;
        	}
        	else
        	{
        		return 1;
        	}
        }
        else if ( o.len > len )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
