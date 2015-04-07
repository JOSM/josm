package org.apache.commons.jcs.engine.behavior;

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
 * ShutdownObservers can observe ShutdownObservable objects.
 * The CacheManager is the primary observable that this is intended for.
 * <p>
 * Most shutdown operations will occur outside this framework for now.  The initial
 * goal is to allow background threads that are not reachable through any reference
 * that the cache manager maintains to be killed on shutdown.
 * <p>
 * Perhaps the composite cache itself should be the observable object.
 * It doesn't make much of a difference.  There are some problems with
 * region by region shutdown.  Some auxiliaries are local.  They will
 * need to track when every region has shutdown before doing things like
 * closing the socket with a lateral.
 * <p>
 * @author Aaron Smuts
 *
 */
public interface IShutdownObservable
{

    /**
     * Registers an observer with the observable object.
     * @param observer
     */
    void registerShutdownObserver( IShutdownObserver observer );

    /**
     * Deregisters the observer with the observable.
     *
     * @param observer
     */
    void deregisterShutdownObserver( IShutdownObserver observer );

}
