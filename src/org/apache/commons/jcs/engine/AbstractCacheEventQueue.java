package org.apache.commons.jcs.engine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;
import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract base class to the different implementations
 */
public abstract class AbstractCacheEventQueue<K, V>
    implements ICacheEventQueue<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( AbstractCacheEventQueue.class );

    /** default */
    protected static final int DEFAULT_WAIT_TO_DIE_MILLIS = 10000;

    /**
     * time to wait for an event before snuffing the background thread if the queue is empty. make
     * configurable later
     */
    private int waitToDieMillis = DEFAULT_WAIT_TO_DIE_MILLIS;

    /**
     * When the events are pulled off the queue, then tell the listener to handle the specific event
     * type. The work is done by the listener.
     */
    private ICacheListener<K, V> listener;

    /** Id of the listener registered with this queue */
    private long listenerId;

    /** The cache region name, if applicable. */
    private String cacheName;

    /** Maximum number of failures before we buy the farm. */
    private int maxFailure;

    /** in milliseconds */
    private int waitBeforeRetry;

    /**
     * This means that the queue is functional. If we reached the max number of failures, the queue
     * is marked as non functional and will never work again.
     */
    private final AtomicBoolean working = new AtomicBoolean(true);

    /**
     * Returns the time to wait for events before killing the background thread.
     * <p>
     * @return int
     */
    public int getWaitToDieMillis()
    {
        return waitToDieMillis;
    }

    /**
     * Sets the time to wait for events before killing the background thread.
     * <p>
     * @param wtdm the ms for the q to sit idle.
     */
    public void setWaitToDieMillis( int wtdm )
    {
        waitToDieMillis = wtdm;
    }

    /**
     * Creates a brief string identifying the listener and the region.
     * <p>
     * @return String debugging info.
     */
    @Override
    public String toString()
    {
        return "CacheEventQueue [listenerId=" + listenerId + ", cacheName=" + cacheName + "]";
    }

    /**
     * If they queue has an active thread it is considered alive.
     * <p>
     * @return The alive value
     * 
     * @deprecated The alive-logic is not used 
     */
    @Deprecated
	@Override
    public boolean isAlive()
    {
        return true;
    }

    /**
     * Sets whether the queue is actively processing -- if there are working threads.
     * <p>
     * @param aState
     * 
     * @deprecated The alive-logic is not used 
     */
    @Deprecated
	public void setAlive( boolean aState )
    {
        // do nothing
    }

    /**
     * @return The listenerId value
     */
    @Override
    public long getListenerId()
    {
        return listenerId;
    }

    /**
     * @return the cacheName
     */
    protected String getCacheName()
    {
        return cacheName;
    }

    /**
     * Initializes the queue.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     */
    protected void initialize( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                            int waitBeforeRetry)
    {
        if ( listener == null )
        {
            throw new IllegalArgumentException( "listener must not be null" );
        }

        this.listener = listener;
        this.listenerId = listenerId;
        this.cacheName = cacheName;
        this.maxFailure = maxFailure <= 0 ? 3 : maxFailure;
        this.waitBeforeRetry = waitBeforeRetry <= 0 ? 500 : waitBeforeRetry;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Constructed: " + this );
        }
    }

    /**
     * This adds a put event to the queue. When it is processed, the element will be put to the
     * listener.
     * <p>
     * @param ce The feature to be added to the PutEvent attribute
     * @throws IOException
     */
    @Override
    public void addPutEvent( ICacheElement<K, V> ce )
    {
        put( new PutEvent( ce ) );
    }

    /**
     * This adds a remove event to the queue. When processed the listener's remove method will be
     * called for the key.
     * <p>
     * @param key The feature to be added to the RemoveEvent attribute
     * @throws IOException
     */
    @Override
    public void addRemoveEvent( K key )
    {
        put( new RemoveEvent( key ) );
    }

    /**
     * This adds a remove all event to the queue. When it is processed, all elements will be removed
     * from the cache.
     */
    @Override
    public void addRemoveAllEvent()
    {
        put( new RemoveAllEvent() );
    }

    /**
     * This adds a dispose event to the queue. When it is processed, the cache is shut down
     */
    @Override
    public void addDisposeEvent()
    {
        put( new DisposeEvent() );
    }

    /**
     * Adds an event to the queue.
     * <p>
     * @param event
     */
    protected abstract void put( AbstractCacheEvent event );


    // /////////////////////////// Inner classes /////////////////////////////
    /**
     * Retries before declaring failure.
     * <p>
     * @author asmuts
     */
    protected abstract class AbstractCacheEvent implements Runnable
    {
        /** Number of failures encountered processing this event. */
        int failures = 0;

        /**
         * Main processing method for the AbstractCacheEvent object
         */
        @Override
        @SuppressWarnings("synthetic-access")
        public void run()
        {
            try
            {
                doRun();
            }
            catch ( IOException e )
            {
                if ( log.isWarnEnabled() )
                {
                    log.warn( e );
                }
                if ( ++failures >= maxFailure )
                {
                    if ( log.isWarnEnabled() )
                    {
                        log.warn( "Error while running event from Queue: " + this
                            + ". Dropping Event and marking Event Queue as non-functional." );
                    }
                    destroy();
                    return;
                }
                if ( log.isInfoEnabled() )
                {
                    log.info( "Error while running event from Queue: " + this + ". Retrying..." );
                }
                try
                {
                    Thread.sleep( waitBeforeRetry );
                    run();
                }
                catch ( InterruptedException ie )
                {
                    if ( log.isErrorEnabled() )
                    {
                        log.warn( "Interrupted while sleeping for retry on event " + this + "." );
                    }
                    destroy();
                }
            }
        }

        /**
         * @throws IOException
         */
        protected abstract void doRun()
            throws IOException;
    }

    /**
     * An element should be put in the cache.
     * <p>
     * @author asmuts
     */
    protected class PutEvent
        extends AbstractCacheEvent
    {
        /** The element to put to the listener */
        private final ICacheElement<K, V> ice;

        /**
         * Constructor for the PutEvent object.
         * <p>
         * @param ice
         */
        PutEvent( ICacheElement<K, V> ice )
        {
            this.ice = ice;
        }

        /**
         * Call put on the listener.
         * <p>
         * @throws IOException
         */
        @Override
        protected void doRun()
            throws IOException
        {
            listener.handlePut( ice );
        }

        /**
         * For debugging.
         * <p>
         * @return Info on the key and value.
         */
        @Override
        public String toString()
        {
            return new StringBuilder( "PutEvent for key: " ).append( ice.getKey() ).append( " value: " )
                .append( ice.getVal() ).toString();
        }

    }

    /**
     * An element should be removed from the cache.
     * <p>
     * @author asmuts
     */
    protected class RemoveEvent
        extends AbstractCacheEvent
    {
        /** The key to remove from the listener */
        private final K key;

        /**
         * Constructor for the RemoveEvent object
         * <p>
         * @param key
         */
        RemoveEvent( K key )
        {
            this.key = key;
        }

        /**
         * Call remove on the listener.
         * <p>
         * @throws IOException
         */
        @Override
        protected void doRun()
            throws IOException
        {
            listener.handleRemove( cacheName, key );
        }

        /**
         * For debugging.
         * <p>
         * @return Info on the key to remove.
         */
        @Override
        public String toString()
        {
            return new StringBuilder( "RemoveEvent for " ).append( key ).toString();
        }

    }

    /**
     * All elements should be removed from the cache when this event is processed.
     * <p>
     * @author asmuts
     */
    protected class RemoveAllEvent
        extends AbstractCacheEvent
    {
        /**
         * Call removeAll on the listener.
         * <p>
         * @throws IOException
         */
        @Override
        protected void doRun()
            throws IOException
        {
            listener.handleRemoveAll( cacheName );
        }

        /**
         * For debugging.
         * <p>
         * @return The name of the event.
         */
        @Override
        public String toString()
        {
            return "RemoveAllEvent";
        }
    }

    /**
     * The cache should be disposed when this event is processed.
     * <p>
     * @author asmuts
     */
    protected class DisposeEvent
        extends AbstractCacheEvent
    {
        /**
         * Called when gets to the end of the queue
         * <p>
         * @throws IOException
         */
        @Override
        protected void doRun()
            throws IOException
        {
            listener.handleDispose( cacheName );
        }

        /**
         * For debugging.
         * <p>
         * @return The name of the event.
         */
        @Override
        public String toString()
        {
            return "DisposeEvent";
        }
    }

    /**
     * @return whether the queue is functional.
     */
    @Override
    public boolean isWorking()
    {
        return working.get();
    }

    /**
     * This means that the queue is functional. If we reached the max number of failures, the queue
     * is marked as non functional and will never work again.
     * <p>
     * @param b
     */
    public void setWorking( boolean b )
    {
        working.set(b);
    }
}
