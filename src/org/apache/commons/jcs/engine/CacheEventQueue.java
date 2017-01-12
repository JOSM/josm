package org.apache.commons.jcs.engine;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An event queue is used to propagate ordered cache events to one and only one target listener.
 * <p>
 * This is a modified version of the experimental version. It should lazy initialize the processor
 * thread, and kill the thread if the queue goes empty for a specified period, now set to 1 minute.
 * If something comes in after that a new processor thread should be created.
 */
public class CacheEventQueue<K, V>
    extends AbstractCacheEventQueue<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( CacheEventQueue.class );

    /** The type of queue -- there are pooled and single */
    private static final QueueType queueType = QueueType.SINGLE;

    /** the thread that works the queue. */
    private Thread processorThread;

    /** Queue implementation */
    private LinkedBlockingQueue<AbstractCacheEvent> queue = new LinkedBlockingQueue<AbstractCacheEvent>();

    /**
     * Constructs with the specified listener and the cache name.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     */
    public CacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName )
    {
        this( listener, listenerId, cacheName, 10, 500 );
    }

    /**
     * Constructor for the CacheEventQueue object
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     */
    public CacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                            int waitBeforeRetry )
    {
        initialize( listener, listenerId, cacheName, maxFailure, waitBeforeRetry );
    }

    /**
     * What type of queue is this.
     * <p>
     * @return queueType
     */
    @Override
    public QueueType getQueueType()
    {
        return queueType;
    }

    /**
     * Kill the processor thread and indicate that the queue is destroyed and no longer alive, but it
     * can still be working.
     */
    protected void stopProcessing()
    {
        setAlive(false);
        processorThread = null;
    }

    /**
     * Event Q is empty.
     * <p>
     * Calling destroy interrupts the processor thread.
     */
    @Override
    public void destroy()
    {
        if ( isAlive() )
        {
            setAlive(false);

            if ( log.isInfoEnabled() )
            {
                log.info( "Destroying queue, stats =  " + getStatistics() );
            }

            if ( processorThread != null )
            {
                processorThread.interrupt();
                processorThread = null;
            }

            if ( log.isInfoEnabled() )
            {
                log.info( "Cache event queue destroyed: " + this );
            }
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Destroy was called after queue was destroyed. Doing nothing. Stats =  " + getStatistics() );
            }
        }
    }

    /**
     * Adds an event to the queue.
     * <p>
     * @param event
     */
    @Override
    protected void put( AbstractCacheEvent event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Event entering Queue for " + getCacheName() + ": " + event );
        }

        queue.offer(event);

        if ( isWorking() )
        {
            if ( !isAlive() )
            {
                setAlive(true);
                processorThread = new QProcessor();
                processorThread.start();
                if ( log.isInfoEnabled() )
                {
                    log.info( "Cache event queue created: " + this );
                }
            }
        }
    }

    // /////////////////////////// Inner classes /////////////////////////////

    /**
     * This is the thread that works the queue.
     * <p>
     * @author asmuts
     * @created January 15, 2002
     */
    protected class QProcessor
        extends Thread
    {
        /**
         * Constructor for the QProcessor object
         * <p>
         * @param aQueue the event queue to take items from.
         */
        QProcessor()
        {
            super( "CacheEventQueue.QProcessor-" + getCacheName() );
            setDaemon( true );
        }

        /**
         * Main processing method for the QProcessor object.
         * <p>
         * Waits for a specified time (waitToDieMillis) for something to come in and if no new
         * events come in during that period the run method can exit and the thread is dereferenced.
         */
        @Override
        public void run()
        {

            while ( CacheEventQueue.this.isAlive() )
            {
                AbstractCacheEvent event = null;

                try
                {
                    event = queue.poll(getWaitToDieMillis(), TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e)
                {
                    // is ok
                }

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Event from queue = " + event );
                }

                if ( event == null )
                {
                    stopProcessing();
                }

                if ( event != null && isWorking() && CacheEventQueue.this.isAlive() )
                {
                    event.run();
                }
            }
            if ( log.isDebugEnabled() )
            {
                log.debug( "QProcessor exiting for " + getCacheName() );
            }
        }
    }

    /**
     * This method returns semi-structured data on this queue.
     * <p>
     * @see org.apache.commons.jcs.engine.behavior.ICacheEventQueue#getStatistics()
     * @return information on the status and history of the queue
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Cache Event Queue" );

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        elems.add(new StatElement<Boolean>( "Working", Boolean.valueOf(this.isWorking()) ) );
        elems.add(new StatElement<Boolean>( "Alive", Boolean.valueOf(this.isAlive()) ) );
        elems.add(new StatElement<Boolean>( "Empty", Boolean.valueOf(this.isEmpty()) ) );
        elems.add(new StatElement<Integer>( "Size", Integer.valueOf(this.size()) ) );

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * @return whether there are any items in the queue.
     */
    @Override
    public boolean isEmpty()
    {
        return queue.isEmpty();
    }

    /**
     * Returns the number of elements in the queue.
     * <p>
     * @return number of items in the queue.
     */
    @Override
    public int size()
    {
        return queue.size();
    }
}
