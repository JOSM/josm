package org.apache.commons.jcs.engine.control.event;

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

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.jcs.engine.control.event.behavior.IElementEvent;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventHandler;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventQueue;
import org.apache.commons.jcs.utils.threadpool.PoolConfiguration;
import org.apache.commons.jcs.utils.threadpool.PoolConfiguration.WhenBlockedPolicy;
import org.apache.commons.jcs.utils.threadpool.ThreadPoolManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An event queue is used to propagate ordered cache events to one and only one target listener.
 */
public class ElementEventQueue
    implements IElementEventQueue
{
    private static final String THREAD_PREFIX = "JCS-ElementEventQueue-";

    /** The logger */
    private static final Log log = LogFactory.getLog( ElementEventQueue.class );

    /** shutdown or not */
    private boolean destroyed = false;

    /** The worker thread pool. */
    private ExecutorService queueProcessor;

    /**
     * Constructor for the ElementEventQueue object
     */
    public ElementEventQueue()
    {
        queueProcessor = ThreadPoolManager.getInstance().createPool(
        		new PoolConfiguration(false, 0, 1, 1, 0, WhenBlockedPolicy.RUN, 1), THREAD_PREFIX);

        if ( log.isDebugEnabled() )
        {
            log.debug( "Constructed: " + this );
        }
    }

    /**
     * Dispose queue
     */
    @Override
    public void dispose()
    {
        if ( !destroyed )
        {
            destroyed = true;

            // synchronize on queue so the thread will not wait forever,
            // and then interrupt the QueueProcessor
            queueProcessor.shutdownNow();
            queueProcessor = null;

            if ( log.isInfoEnabled() )
            {
                log.info( "Element event queue destroyed: " + this );
            }
        }
    }

    /**
     * Adds an ElementEvent to be handled
     * @param hand The IElementEventHandler
     * @param event The IElementEventHandler IElementEvent event
     * @throws IOException
     */
    @Override
    public <T> void addElementEvent( IElementEventHandler hand, IElementEvent<T> event )
        throws IOException
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "Adding Event Handler to QUEUE, !destroyed = " + !destroyed );
        }

        if (destroyed)
        {
            log.warn("Event submitted to disposed element event queue " + event);
        }
        else
        {
            ElementEventRunner runner = new ElementEventRunner( hand, event );

            if ( log.isDebugEnabled() )
            {
                log.debug( "runner = " + runner );
            }

            queueProcessor.execute(runner);
        }
    }

    // /////////////////////////// Inner classes /////////////////////////////

    /**
     * Retries before declaring failure.
     */
    protected abstract class AbstractElementEventRunner
        implements Runnable
    {
        /**
         * Main processing method for the AbstractElementEvent object
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void run()
        {
            try
            {
                doRun();
                // happy and done.
            }
            catch ( IOException e )
            {
                // Too bad. The handler has problems.
                log.warn( "Giving up element event handling " + ElementEventQueue.this, e );
            }
        }

        /**
         * This will do the work or trigger the work to be done.
         * <p>
         * @throws IOException
         */
        protected abstract void doRun()
            throws IOException;
    }

    /**
     * ElementEventRunner.
     */
    private class ElementEventRunner
        extends AbstractElementEventRunner
    {
        /** the handler */
        private final IElementEventHandler hand;

        /** event */
        private final IElementEvent<?> event;

        /**
         * Constructor for the PutEvent object.
         * <p>
         * @param hand
         * @param event
         * @throws IOException
         */
        @SuppressWarnings("synthetic-access")
        ElementEventRunner( IElementEventHandler hand, IElementEvent<?> event )
            throws IOException
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Constructing " + this );
            }
            this.hand = hand;
            this.event = event;
        }

        /**
         * Tells the handler to handle the event.
         * <p>
         * @throws IOException
         */
        @Override
        protected void doRun()
            throws IOException
        {
            hand.handleElementEvent( event );
        }
    }
}
