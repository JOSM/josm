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

import java.util.concurrent.ThreadFactory;

/**
 * Allows us to set the daemon status on the threads.
 * <p>
 * @author aaronsm
 */
public class DaemonThreadFactory
    implements ThreadFactory
{
    private String prefix;
    private boolean threadIsDaemon = true;
    private int threadPriority = Thread.NORM_PRIORITY;

    /**
     * Constructor
     *
     * @param prefix thread name prefix
     */
    public DaemonThreadFactory(String prefix)
    {
        this(prefix, Thread.NORM_PRIORITY);
    }

    /**
     * Constructor
     *
     * @param prefix thread name prefix
     * @param threadPriority set thread priority
     */
    public DaemonThreadFactory(String prefix, int threadPriority)
    {
        this.prefix = prefix;
        this.threadPriority = threadPriority;
    }

    /**
     * Sets the thread to daemon.
     * <p>
     * @param runner
     * @return a daemon thread
     */
    @Override
    public Thread newThread( Runnable runner )
    {
        Thread t = new Thread( runner );
        String oldName = t.getName();
        t.setName( prefix + oldName );
        t.setDaemon(threadIsDaemon);
        t.setPriority(threadPriority);
        return t;
    }
}