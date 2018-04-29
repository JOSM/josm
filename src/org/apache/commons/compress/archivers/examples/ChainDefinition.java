/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.examples;

import java.util.Deque;
import java.util.LinkedList;

/**
 * The recipe for building a {@link Chain}.
 * @since 1.17
 */
public class ChainDefinition<T> {
    private final Deque<ChainStep<T>> steps = new LinkedList<>();
    private volatile boolean frozen = false;

    /**
     * Adds a step.
     * @throws IllegalStateException if the definition is already frozen.
     */
    public void add(ChainStep<T> step) {
        if (frozen) {
            throw new IllegalStateException("the definition is already frozen");
        }
        steps.addLast(step);
    }

    /**
     * Freezes the definition.
     *
     * <p>Once this method has been invoked {@link #add} can no longer be invoked.</p>
     *
     * @throws IllegalStateException if the last step of the definition is not a sink.
     */
    public void freeze() {
        if (!frozen) {
            frozen = true;
            if (!(steps.getLast() instanceof Sink)) {
                throw new IllegalStateException("this definition doesn't end in a sink");
            }
        }
    }

    /**
     * Returns a chain for this definition.
     *
     * @throws IllegalStateException if the definition is not frozen.
     */
    public Chain<T> chain() {
        if (!frozen) {
            throw new IllegalStateException("the definition hasn't been frozen, yet");
        }
        return new Chain(steps.iterator());
    }
}
