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

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCacheAttributes;

/**
 * Configuration class for the Indexed Disk Cache
 */
public class IndexedDiskCacheAttributes
    extends AbstractDiskCacheAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = -2190863599358782950L;

    /** default value */
    private static final int DEFAULT_maxKeySize = 5000;

    /** -1 means no limit. */
    private int maxKeySize = DEFAULT_maxKeySize;

    /** default value */
    private static final int DEFAULT_maxRecycleBinSize = 5000;

    /**
     * Cannot be larger than the max size. If max is less than 0, this will be 5000
     */
    private int maxRecycleBinSize = DEFAULT_maxRecycleBinSize;

    /** default to -1, i.e., don't optimize until shutdown */
    private int optimizeAtRemoveCount = -1;

    /** Should we optimize on shutdown. */
    public static final boolean DEFAULT_OPTIMIZE_ON_SHUTDOWN = true;

    /** Should we optimize on shutdown. */
    private boolean optimizeOnShutdown = DEFAULT_OPTIMIZE_ON_SHUTDOWN;

    /** Should we clear the disk on startup. */
    public static final boolean DEFAULT_CLEAR_DISK_ON_STARTUP = false;

    /** Should we clear the disk on startup. If true the congtents of disk are cleared. */
    private boolean clearDiskOnStartup = DEFAULT_CLEAR_DISK_ON_STARTUP;

    private DiskLimitType diskLimitType = DiskLimitType.COUNT;

    /**
     * Constructor for the DiskCacheAttributes object
     */
    public IndexedDiskCacheAttributes()
    {
        super();
    }

    /**
     * Gets the maxKeySize attribute of the DiskCacheAttributes object
     * <p>
     * @return The maxKeySize value
     */
    public int getMaxKeySize()
    {
        return this.maxKeySize;
    }

    /**
     * Sets the maxKeySize attribute of the DiskCacheAttributes object
     * <p>
     * @param maxKeySize The new maxKeySize value
     */
    public void setMaxKeySize( int maxKeySize )
    {
        this.maxKeySize = maxKeySize;

        // make sure the sizes are in accord with our rule.
        setMaxRecycleBinSize( maxRecycleBinSize );
    }

    /**
     * Gets the optimizeAtRemoveCount attribute of the DiskCacheAttributes object
     * <p>
     * @return The optimizeAtRemoveCount value
     */
    public int getOptimizeAtRemoveCount()
    {
        return this.optimizeAtRemoveCount;
    }

    /**
     * Sets the optimizeAtRemoveCount attribute of the DiskCacheAttributes object This number
     * determines how often the disk cache should run real time optimizations.
     * <p>
     * @param cnt The new optimizeAtRemoveCount value
     */
    public void setOptimizeAtRemoveCount( int cnt )
    {
        this.optimizeAtRemoveCount = cnt;
    }

    /**
     * This cannot be larger than the maxKeySize. It wouldn't hurt anything, but it makes the config
     * necessary. The recycle bin entry willbe at least as large as a key.
     * <p>
     * If the maxKeySize is -1 this will be set tot he default, which is 5000.
     * <p>
     * @param maxRecycleBinSize The maxRecycleBinSize to set.
     */
    public void setMaxRecycleBinSize( int maxRecycleBinSize )
    {
        this.maxRecycleBinSize = maxRecycleBinSize;
    }

    /**
     * @return Returns the maxRecycleBinSize.
     */
    public int getMaxRecycleBinSize()
    {
        return maxRecycleBinSize;
    }

    /**
     * @param optimizeOnShutdown The optimizeOnShutdown to set.
     */
    public void setOptimizeOnShutdown( boolean optimizeOnShutdown )
    {
        this.optimizeOnShutdown = optimizeOnShutdown;
    }

    /**
     * @return Returns the optimizeOnShutdown.
     */
    public boolean isOptimizeOnShutdown()
    {
        return optimizeOnShutdown;
    }

    /**
     * @param clearDiskOnStartup the clearDiskOnStartup to set
     */
    public void setClearDiskOnStartup( boolean clearDiskOnStartup )
    {
        this.clearDiskOnStartup = clearDiskOnStartup;
    }

    /**
     * @return the clearDiskOnStartup
     */
    public boolean isClearDiskOnStartup()
    {
        return clearDiskOnStartup;
    }

    /**
     * Returns a copy of the attributes.
     * <p>
     * @return AuxiliaryCacheAttributes
     */
    @Override
    public AuxiliaryCacheAttributes copy()
    {
        try
        {
            return (AuxiliaryCacheAttributes) this.clone();
        }
        catch ( Exception e )
        {
            // swallow
        }
        return this;
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
        str.append( "IndexedDiskCacheAttributes " );
        str.append( "\n diskPath = " + super.getDiskPath() );
        str.append( "\n maxPurgatorySize   = " + super.getMaxPurgatorySize() );
        str.append( "\n maxKeySize  = " + maxKeySize );
        str.append( "\n maxRecycleBinSize  = " + maxRecycleBinSize );
        str.append( "\n optimizeAtRemoveCount  = " + optimizeAtRemoveCount );
        str.append( "\n shutdownSpoolTimeLimit  = " + super.getShutdownSpoolTimeLimit() );
        str.append( "\n optimizeOnShutdown  = " + optimizeOnShutdown );
        str.append( "\n clearDiskOnStartup  = " + clearDiskOnStartup );
        return str.toString();
    }
}
