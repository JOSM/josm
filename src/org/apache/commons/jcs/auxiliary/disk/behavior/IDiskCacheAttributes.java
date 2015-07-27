package org.apache.commons.jcs.auxiliary.disk.behavior;

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

import java.io.File;

/**
 * Common disk cache attributes.
 */
public interface IDiskCacheAttributes
    extends AuxiliaryCacheAttributes
{
    enum DiskLimitType {
        /** limit elements by count (default) */
        COUNT,
        /** limit elements by their size */
        SIZE
    }
    /**
     * This is the default purgatory size limit. Purgatory is the area where
     * items to be spooled are temporarily stored. It basically provides access
     * to items on the to-be-spooled queue.
     */
    int MAX_PURGATORY_SIZE_DEFAULT = 5000;

    /**
     * Sets the diskPath attribute of the IJISPCacheAttributes object
     * <p>
     * @param path
     *            The new diskPath value
     */
    void setDiskPath( String path );

    /**
     * Gets the diskPath attribute of the attributes object
     * <p>
     * @return The diskPath value
     */
    File getDiskPath();

    /**
     * Gets the maxKeySize attribute of the DiskCacheAttributes object
     * <p>
     * @return The maxPurgatorySize value
     */
    int getMaxPurgatorySize();

    /**
     * Sets the maxPurgatorySize attribute of the DiskCacheAttributes object
     * <p>
     * @param maxPurgatorySize
     *            The new maxPurgatorySize value
     */
    void setMaxPurgatorySize( int maxPurgatorySize );

    /**
     * Get the amount of time in seconds we will wait for elements to move to
     * disk during shutdown for a particular region.
     * <p>
     * @return the time in seconds.
     */
    int getShutdownSpoolTimeLimit();

    /**
     * Sets the amount of time in seconds we will wait for elements to move to
     * disk during shutdown for a particular region.
     * <p>
     * This is how long we give the event queue to empty.
     * <p>
     * The default is 60 seconds.
     * <p>
     * @param shutdownSpoolTimeLimit
     *            the time in seconds
     */
    void setShutdownSpoolTimeLimit( int shutdownSpoolTimeLimit );

    /**
     * If this is true then remove all is not prohibited.
     * <p>
     * @return boolean
     */
    boolean isAllowRemoveAll();

    /**
     * If this is false, then remove all requests will not be honored.
     * <p>
     * This provides a safety mechanism for the persistent store.
     * <p>
     * @param allowRemoveAll
     */
    void setAllowRemoveAll( boolean allowRemoveAll );

    /**
     * set the type of the limit of the cache size
     * @param diskLimitType COUNT - limit by count of the elements, SIZE, limit by sum of element's size
     */
    void setDiskLimitType(DiskLimitType diskLimitType);

    /**
     * Translates and stores String values  of DiskLimitType
     *
     * Allowed values: "COUNT" and "SIZE"
     * @param diskLimitTypeName
     */
    void setDiskLimitTypeName(String diskLimitTypeName);

    /**
     *
     * @return active DiskLimitType
     */
    DiskLimitType getDiskLimitType();
}
