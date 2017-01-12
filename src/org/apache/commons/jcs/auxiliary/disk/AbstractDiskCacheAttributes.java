package org.apache.commons.jcs.auxiliary.disk;

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

import java.io.File;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.behavior.IDiskCacheAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This has common attributes that any conceivable disk cache would need.
 */
public abstract class AbstractDiskCacheAttributes extends AbstractAuxiliaryCacheAttributes implements IDiskCacheAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = 8306631920391711229L;

    /** The logger */
    private static final Log log = LogFactory.getLog(AbstractDiskCacheAttributes.class);

    /** path to disk */
    private File diskPath;

    /** if this is false, we will not execute remove all */
    private boolean allowRemoveAll = true;

    /** default to 5000 */
    private int maxPurgatorySize = MAX_PURGATORY_SIZE_DEFAULT;

    /** Default amount of time to allow for key persistence on shutdown */
    private static final int DEFAULT_shutdownSpoolTimeLimit = 60;

    /**
     * This default determines how long the shutdown will wait for the key spool and data defrag to
     * finish.
     */
    private int shutdownSpoolTimeLimit = DEFAULT_shutdownSpoolTimeLimit;

    /** Type of disk limit: SIZE or COUNT */
    private DiskLimitType diskLimitType = DiskLimitType.COUNT;

    /**
     * Sets the diskPath attribute of the DiskCacheAttributes object
     * <p>
     *
     * @param path
     *            The new diskPath value
     */
    @Override
    public void setDiskPath(String path)
    {
        setDiskPath(new File(path));
    }

    /**
     * Sets the diskPath attribute of the DiskCacheAttributes object
     * <p>
     *
     * @param diskPath
     *            The new diskPath value
     */
    public void setDiskPath(File diskPath)
    {
        this.diskPath = diskPath;
        boolean result = this.diskPath.isDirectory();

        if (!result)
        {
            result = this.diskPath.mkdirs();
        }
        if (!result)
        {
            log.error("Failed to create directory " + diskPath);
        }
    }

    /**
     * Gets the diskPath attribute of the attributes object
     * <p>
     *
     * @return The diskPath value
     */
    @Override
    public File getDiskPath()
    {
        return this.diskPath;
    }

    /**
     * Gets the maxKeySize attribute of the DiskCacheAttributes object
     * <p>
     *
     * @return The maxPurgatorySize value
     */
    @Override
    public int getMaxPurgatorySize()
    {
        return maxPurgatorySize;
    }

    /**
     * Sets the maxPurgatorySize attribute of the DiskCacheAttributes object
     * <p>
     *
     * @param maxPurgatorySize
     *            The new maxPurgatorySize value
     */
    @Override
    public void setMaxPurgatorySize(int maxPurgatorySize)
    {
        this.maxPurgatorySize = maxPurgatorySize;
    }

    /**
     * Get the amount of time in seconds we will wait for elements to move to disk during shutdown
     * for a particular region.
     * <p>
     *
     * @return the time in seconds.
     */
    @Override
    public int getShutdownSpoolTimeLimit()
    {
        return this.shutdownSpoolTimeLimit;
    }

    /**
     * Sets the amount of time in seconds we will wait for elements to move to disk during shutdown
     * for a particular region.
     * <p>
     * This is how long we give the event queue to empty.
     * <p>
     * The default is 60 seconds.
     * <p>
     *
     * @param shutdownSpoolTimeLimit
     *            the time in seconds
     */
    @Override
    public void setShutdownSpoolTimeLimit(int shutdownSpoolTimeLimit)
    {
        this.shutdownSpoolTimeLimit = shutdownSpoolTimeLimit;
    }

    /**
     * @param allowRemoveAll
     *            The allowRemoveAll to set.
     */
    @Override
    public void setAllowRemoveAll(boolean allowRemoveAll)
    {
        this.allowRemoveAll = allowRemoveAll;
    }

    /**
     * @return Returns the allowRemoveAll.
     */
    @Override
    public boolean isAllowRemoveAll()
    {
        return allowRemoveAll;
    }

    /**
     * Includes the common attributes for a debug message.
     * <p>
     *
     * @return String
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("AbstractDiskCacheAttributes ");
        str.append("\n diskPath = " + getDiskPath());
        str.append("\n maxPurgatorySize   = " + getMaxPurgatorySize());
        str.append("\n allowRemoveAll   = " + isAllowRemoveAll());
        str.append("\n ShutdownSpoolTimeLimit   = " + getShutdownSpoolTimeLimit());
        return str.toString();
    }

    @Override
    public void setDiskLimitType(DiskLimitType diskLimitType)
    {
        this.diskLimitType = diskLimitType;
    }

    @Override
    public void setDiskLimitTypeName(String diskLimitTypeName)
    {
        if (diskLimitTypeName != null)
        {
            diskLimitType = DiskLimitType.valueOf(diskLimitTypeName.trim());
        }
    }

    @Override
    public DiskLimitType getDiskLimitType()
    {
        return diskLimitType;
    }
}
