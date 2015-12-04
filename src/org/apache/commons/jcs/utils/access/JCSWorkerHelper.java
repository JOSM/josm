package org.apache.commons.jcs.utils.access;

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
 * Interface for doing a piece of work which is expected to be cached. This is
 * meant to be used in conjunction with JCSWorker.
 * <p>
 * Implement doWork() to return the work being done. isFinished() should return
 * false until setFinished(true) is called, after which time it should return
 * true.
 * <p>
 * @author tsavo
 */
public interface JCSWorkerHelper<V>
{
    /**
     * Tells us whether or not the work has been completed. This will be called
     * automatically by JCSWorker. You should not call it yourself.
     * <p>
     * @return True if the work has already been done, otherwise false.
     */
    boolean isFinished();

    /**
     * Sets whether or not the work has been done.
     * <p>
     * @param isFinished
     *            True if the work has already been done, otherwise false.
     */
    void setFinished( boolean isFinished );

    /**
     * The method to implement to do the work that should be cached. JCSWorker
     * will call this itself! You should not call this directly.
     * <p>
     * @return The result of doing the work to be cached.
     * @throws Exception
     *             If anything goes wrong while doing the work, an Exception
     *             should be thrown.
     */
    V doWork() throws Exception;
}
