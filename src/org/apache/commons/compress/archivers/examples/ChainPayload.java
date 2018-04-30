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

import java.io.InputStream;

/**
 * The data that is pushed through a chain.
 * @since 1.17
 */
public class ChainPayload<T> {
    private final T entry;
    private final String entryName;
    private final Supplier<InputStream> input;
    /**
     * Constructs the payload.
     * @param entry entry the actual payload
     * @param entryName the local name of the entry. This may - for
     * example - be the file name relative to a directory.
     * @param input supplies an input stream to the entry's
     * content. Is not expected to be called more than once.
     */
    public ChainPayload(T entry, String entryName, Supplier<InputStream> input) {
        this.entry = entry;
        this.entryName = entryName;
        this.input = input;
    }
    /**
     * Provides the real payload.
     * @return the real playoad
     *
     */
    public T getEntry() {
        return entry;
    }
    /**
     * Provides the local name of the entry.
     *
     * <p>This may - for example - be the file name relative to a
     * directory.</p>
     *
     * @return local name of the entry
     */
    public String getEntryName() {
        return entryName;
    }
    /**
     * Returns a {@link Supplier} that can be used to read the entry's content.
     *
     * <p>The supplier is not required to be callable more than
     * once.</p>
     *
     * @return supplier of input
     */
    public Supplier<InputStream> getInput() {
        return input;
    }
}
