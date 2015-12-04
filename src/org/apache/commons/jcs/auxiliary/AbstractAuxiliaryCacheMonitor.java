package org.apache.commons.jcs.auxiliary;

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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to monitor and repair any failed connection for the lateral cache service. By default the
 * monitor operates in a failure driven mode. That is, it goes into a wait state until there is an
 * error. Upon the notification of a connection error, the monitor changes to operate in a time
 * driven mode. That is, it attempts to recover the connections on a periodic basis. When all failed
 * connections are restored, it changes back to the failure driven mode.
 */
public abstract class AbstractAuxiliaryCacheMonitor extends Thread
{
    /** The logger */
    protected final Log log = LogFactory.getLog( this.getClass() );

    /** How long to wait between runs */
    protected static long idlePeriod = 20 * 1000;

    /**
     * Must make sure AbstractAuxiliaryCacheMonitor is started before any error can be detected!
     */
    protected AtomicBoolean allright = new AtomicBoolean(true);

    /**
     * shutdown flag
     */
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    /** Synchronization helper lock */
    private Lock lock = new ReentrantLock();

    /** Synchronization helper condition */
    private Condition trigger = lock.newCondition();

    /**
     * Constructor
     *
     * @param name the thread name
     */
    public AbstractAuxiliaryCacheMonitor(String name)
    {
        super(name);
    }

    /**
     * Configures the idle period between repairs.
     * <p>
     * @param idlePeriod The new idlePeriod value
     */
    public static void setIdlePeriod( long idlePeriod )
    {
        if ( idlePeriod > AbstractAuxiliaryCacheMonitor.idlePeriod )
        {
            AbstractAuxiliaryCacheMonitor.idlePeriod = idlePeriod;
        }
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the error recovery process.
     */
    public void notifyError()
    {
        if (allright.compareAndSet(true, false))
        {
            signalTrigger();
        }
    }

    /**
     * Notifies the cache monitor that the service shall shut down
     */
    public void notifyShutdown()
    {
        if (shutdown.compareAndSet(false, true))
        {
            signalTrigger();
        }
    }

    // Trigger continuation of loop
    private void signalTrigger()
    {
        try
        {
            lock.lock();
            trigger.signal();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Clean up all resources before shutdown
     */
    protected abstract void dispose();

    /**
     * do actual work
     */
    protected abstract void doWork();

    /**
     * Main processing method for the AbstractAuxiliaryCacheMonitor object
     */
    @Override
    public void run()
    {
        do
        {
            if ( log.isDebugEnabled() )
            {
                if ( allright.get() )
                {
                    log.debug( "ERROR DRIVEN MODE: allright = true, cache monitor will wait for an error." );
                }
                else
                {
                    log.debug( "ERROR DRIVEN MODE: allright = false cache monitor running." );
                }
            }

            if ( allright.get() )
            {
                // Failure driven mode.
                try
                {
                    lock.lock();
                    trigger.await();
                    // wake up only if there is an error.
                }
                catch ( InterruptedException ignore )
                {
                    //no op, this is expected
                }
                finally
                {
                    lock.unlock();
                }
            }

            // check for requested shutdown
            if ( shutdown.get() )
            {
                log.info( "Shutting down cache monitor" );
                dispose();
                return;
            }

            // The "allright" flag must be false here.
            // Simply presume we can fix all the errors until proven otherwise.
            allright.set(true);

            if ( log.isDebugEnabled() )
            {
                log.debug( "Cache monitor running." );
            }

            doWork();

            try
            {
                // don't want to sleep after waking from an error
                // run immediately and sleep here.
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Cache monitor sleeping for " + idlePeriod + " between runs." );
                }

                Thread.sleep( idlePeriod );
            }
            catch ( InterruptedException ex )
            {
                // ignore;
            }
        }
        while ( true );
    }
}
