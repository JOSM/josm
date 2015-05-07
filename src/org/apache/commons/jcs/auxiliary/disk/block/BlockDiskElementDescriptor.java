package org.apache.commons.jcs.auxiliary.disk.block;

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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * This represents an element on disk. This is used when we persist the keys. We only store the
 * block addresses in memory. We don't need the length here, since all the blocks are the same size
 * receyle bin.
 * <p>
 * @author Aaron Smuts
 */
public class BlockDiskElementDescriptor<K>
    implements Serializable, Externalizable
{
    /** Don't change */
    private static final long serialVersionUID = -1400659301208101411L;

    /** The key */
    private K key;

    /** The array of block numbers */
    private int[] blocks;

    /**
     * @param key The key to set.
     */
    public void setKey( K key )
    {
        this.key = key;
    }

    /**
     * @return Returns the key.
     */
    public K getKey()
    {
        return key;
    }

    /**
     * @param blocks The blocks to set.
     */
    public void setBlocks( int[] blocks )
    {
        this.blocks = blocks;
    }

    /**
     * This holds the block numbers. An item my be dispersed between multiple blocks.
     * <p>
     * @return Returns the blocks.
     */
    public int[] getBlocks()
    {
        return blocks;
    }

    /**
     * For debugging.
     * <p>
     * @return Info on the descriptor.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nBlockDiskElementDescriptor" );
        buf.append( "\n key [" + this.getKey() + "]" );
        buf.append( "\n blocks [" );
        if ( this.getBlocks() != null )
        {
            for ( int i = 0; i < blocks.length; i++ )
            {
                buf.append( this.getBlocks()[i] );
            }
        }
        buf.append( "]" );
        return buf.toString();
    }

    /**
     * Saves on reflection.
     * <p>
     * (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @Override
    @SuppressWarnings("unchecked") // Need cast to K
    public void readExternal( ObjectInput input )
        throws IOException, ClassNotFoundException
    {
        this.key = (K) input.readObject();
        this.blocks = (int[]) input.readObject();
    }

    /**
     * Saves on reflection.
     * <p>
     * (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal( ObjectOutput output )
        throws IOException
    {
        output.writeObject( this.key );
        output.writeObject( this.blocks );
    }
}
