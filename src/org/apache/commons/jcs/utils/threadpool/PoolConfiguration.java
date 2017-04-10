package org.apache.commons.jcs.utils.threadpool;

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
 * This object holds configuration data for a thread pool.
 * <p>
 * @author Aaron Smuts
 */
public final class PoolConfiguration
    implements Cloneable
{
    /**
     * DEFAULT SETTINGS
     */
    private static final boolean DEFAULT_USE_BOUNDARY = true;

    /** Default queue size limit */
    private static final int DEFAULT_BOUNDARY_SIZE = 2000;

    /** Default max size */
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 150;

    /** Default min */
    private static final int DEFAULT_MINIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /** Default keep alive */
    private static final int DEFAULT_KEEPALIVE_TIME = 1000 * 60 * 5;

    /** Default when blocked */
    private static final WhenBlockedPolicy DEFAULT_WHEN_BLOCKED_POLICY = WhenBlockedPolicy.RUN;

    /** Default startup size */
    private static final int DEFAULT_STARTUP_SIZE = DEFAULT_MINIMUM_POOL_SIZE;

    /** Should we bound the queue */
    private boolean useBoundary = DEFAULT_USE_BOUNDARY;

    /** If the queue is bounded, how big can it get */
    private int boundarySize = DEFAULT_BOUNDARY_SIZE;

    /** only has meaning if a boundary is used */
    private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;

    /**
     * the exact number that will be used in a boundless queue. If the queue has a boundary, more
     * will be created if the queue fills.
     */
    private int minimumPoolSize = DEFAULT_MINIMUM_POOL_SIZE;

    /** How long idle threads above the minimum should be kept alive. */
    private int keepAliveTime = DEFAULT_KEEPALIVE_TIME;

    public enum WhenBlockedPolicy {
        /** abort when queue is full and max threads is reached. */
        ABORT,

        /** block when queue is full and max threads is reached. */
        BLOCK,

        /** run in current thread when queue is full and max threads is reached. */
        RUN,

        /** wait when queue is full and max threads is reached. */
        WAIT,

        /** discard oldest when queue is full and max threads is reached. */
        DISCARDOLDEST
    }

    /** should be ABORT, BLOCK, RUN, WAIT, DISCARDOLDEST, */
    private WhenBlockedPolicy whenBlockedPolicy = DEFAULT_WHEN_BLOCKED_POLICY;

    /** The number of threads to create on startup */
    private int startUpSize = DEFAULT_MINIMUM_POOL_SIZE;

    /**
     * @param useBoundary The useBoundary to set.
     */
    public void setUseBoundary( boolean useBoundary )
    {
        this.useBoundary = useBoundary;
    }

    /**
     * @return Returns the useBoundary.
     */
    public boolean isUseBoundary()
    {
        return useBoundary;
    }

    /**
     * Default
     */
    public PoolConfiguration()
    {
        this( DEFAULT_USE_BOUNDARY, DEFAULT_BOUNDARY_SIZE, DEFAULT_MAXIMUM_POOL_SIZE,
              DEFAULT_MINIMUM_POOL_SIZE, DEFAULT_KEEPALIVE_TIME,
              DEFAULT_WHEN_BLOCKED_POLICY, DEFAULT_STARTUP_SIZE );
    }

    /**
     * Construct a completely configured instance.
     * <p>
     * @param useBoundary
     * @param boundarySize
     * @param maximumPoolSize
     * @param minimumPoolSize
     * @param keepAliveTime
     * @param whenBlockedPolicy
     * @param startUpSize
     */
    public PoolConfiguration( boolean useBoundary, int boundarySize, int maximumPoolSize, int minimumPoolSize,
                              int keepAliveTime, WhenBlockedPolicy whenBlockedPolicy, int startUpSize )
    {
        setUseBoundary( useBoundary );
        setBoundarySize( boundarySize );
        setMaximumPoolSize( maximumPoolSize );
        setMinimumPoolSize( minimumPoolSize );
        setKeepAliveTime( keepAliveTime );
        setWhenBlockedPolicy( whenBlockedPolicy );
        setStartUpSize( startUpSize );
    }

    /**
     * @param boundarySize The boundarySize to set.
     */
    public void setBoundarySize( int boundarySize )
    {
        this.boundarySize = boundarySize;
    }

    /**
     * @return Returns the boundarySize.
     */
    public int getBoundarySize()
    {
        return boundarySize;
    }

    /**
     * @param maximumPoolSize The maximumPoolSize to set.
     */
    public void setMaximumPoolSize( int maximumPoolSize )
    {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * @return Returns the maximumPoolSize.
     */
    public int getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    /**
     * @param minimumPoolSize The minimumPoolSize to set.
     */
    public void setMinimumPoolSize( int minimumPoolSize )
    {
        this.minimumPoolSize = minimumPoolSize;
    }

    /**
     * @return Returns the minimumPoolSize.
     */
    public int getMinimumPoolSize()
    {
        return minimumPoolSize;
    }

    /**
     * @param keepAliveTime The keepAliveTime to set.
     */
    public void setKeepAliveTime( int keepAliveTime )
    {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * @return Returns the keepAliveTime.
     */
    public int getKeepAliveTime()
    {
        return keepAliveTime;
    }

    /**
     * @param whenBlockedPolicy The whenBlockedPolicy to set.
     */
    public void setWhenBlockedPolicy( String whenBlockedPolicy )
    {
        if ( whenBlockedPolicy != null )
        {
            WhenBlockedPolicy policy = WhenBlockedPolicy.valueOf(whenBlockedPolicy.trim().toUpperCase());
            setWhenBlockedPolicy(policy);
        }
        else
        {
            // the value is null, default to RUN
            this.whenBlockedPolicy = WhenBlockedPolicy.RUN;
        }
    }

    /**
     * @param whenBlockedPolicy The whenBlockedPolicy to set.
     */
    public void setWhenBlockedPolicy( WhenBlockedPolicy whenBlockedPolicy )
    {
        if ( whenBlockedPolicy != null )
        {
            this.whenBlockedPolicy = whenBlockedPolicy;
        }
        else
        {
            // the value is null, default to RUN
            this.whenBlockedPolicy = WhenBlockedPolicy.RUN;
        }
    }

    /**
     * @return Returns the whenBlockedPolicy.
     */
    public WhenBlockedPolicy getWhenBlockedPolicy()
    {
        return whenBlockedPolicy;
    }

    /**
     * @param startUpSize The startUpSize to set.
     */
    public void setStartUpSize( int startUpSize )
    {
        this.startUpSize = startUpSize;
    }

    /**
     * @return Returns the startUpSize.
     */
    public int getStartUpSize()
    {
        return startUpSize;
    }

    /**
     * To string for debugging purposes.
     * @return String
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "useBoundary = [" + isUseBoundary() + "] " );
        buf.append( "boundarySize = [" + boundarySize + "] " );
        buf.append( "maximumPoolSize = [" + maximumPoolSize + "] " );
        buf.append( "minimumPoolSize = [" + minimumPoolSize + "] " );
        buf.append( "keepAliveTime = [" + keepAliveTime + "] " );
        buf.append( "whenBlockedPolicy = [" + getWhenBlockedPolicy() + "] " );
        buf.append( "startUpSize = [" + startUpSize + "]" );
        return buf.toString();
    }

    /**
     * Copies the instance variables to another instance.
     * <p>
     * @return PoolConfiguration
     */
    @Override
    public PoolConfiguration clone()
    {
        return new PoolConfiguration( isUseBoundary(), boundarySize, maximumPoolSize, minimumPoolSize, keepAliveTime,
                                      getWhenBlockedPolicy(), startUpSize );
    }
}
