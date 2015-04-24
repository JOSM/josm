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

import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCacheAttributes;

/**
 * This holds attributes for Block Disk Cache configuration.
 * <p>
 * @author Aaron Smuts
 */
public class BlockDiskCacheAttributes
    extends AbstractDiskCacheAttributes
{
    /** Don't change */
    private static final long serialVersionUID = 6568840097657265989L;

    /** The size per block in bytes. */
    private int blockSizeBytes;

    /** Maximum number of keys to be kept in memory */
    private static final int DEFAULT_MAX_KEY_SIZE = 5000;

    /** -1 means no limit. */
    private int maxKeySize = DEFAULT_MAX_KEY_SIZE;

    /** How often should we persist the keys. */
    private static final long DEFAULT_KEY_PERSISTENCE_INTERVAL_SECONDS = 5 * 60;

    /** The keys will be persisted at this interval.  -1 mean never. */
    private long keyPersistenceIntervalSeconds = DEFAULT_KEY_PERSISTENCE_INTERVAL_SECONDS;

    /**
     * The size of the blocks. All blocks are the same size.
     * <p>
     * @param blockSizeBytes The blockSizeBytes to set.
     */
    public void setBlockSizeBytes( int blockSizeBytes )
    {
        this.blockSizeBytes = blockSizeBytes;
    }

    /**
     * @return Returns the blockSizeBytes.
     */
    public int getBlockSizeBytes()
    {
        return blockSizeBytes;
    }

    /**
     * @param maxKeySize The maxKeySize to set.
     */
    public void setMaxKeySize( int maxKeySize )
    {
        this.maxKeySize = maxKeySize;
    }

    /**
     * @return Returns the maxKeySize.
     */
    public int getMaxKeySize()
    {
        return maxKeySize;
    }

    /**
     * @param keyPersistenceIntervalSeconds The keyPersistenceIntervalSeconds to set.
     */
    public void setKeyPersistenceIntervalSeconds( long keyPersistenceIntervalSeconds )
    {
        this.keyPersistenceIntervalSeconds = keyPersistenceIntervalSeconds;
    }

    /**
     * @return Returns the keyPersistenceIntervalSeconds.
     */
    public long getKeyPersistenceIntervalSeconds()
    {
        return keyPersistenceIntervalSeconds;
    }

    /**
     * Write out the values for debugging purposes.
     * <p>
     * @return String
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append( "\nBlockDiskAttributes " );
        str.append( "\n DiskPath [" + this.getDiskPath() + "]" );
        str.append( "\n MaxKeySize [" + this.getMaxKeySize() + "]" );
        str.append( "\n MaxPurgatorySize [" + this.getMaxPurgatorySize() + "]" );
        str.append( "\n BlockSizeBytes [" + this.getBlockSizeBytes() + "]" );
        str.append( "\n KeyPersistenceIntervalSeconds [" + this.getKeyPersistenceIntervalSeconds() + "]" );
        str.append( "\n DiskLimitType [" + this.getDiskLimitType() + "]" );
        return str.toString();
    }
}
