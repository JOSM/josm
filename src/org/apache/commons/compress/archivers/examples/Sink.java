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

import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Final stage of a {@link Expand} or {@link Archive} chain.
 * @since 1.17
 */
public abstract class Sink<T> implements ChainStep<T>, Closeable {
    /**
     * Consume a single entry.
     *
     * @param payload the entry to consume
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if an archive format related error occurs
     */
    public abstract void consume(ChainPayload<T> payload) throws IOException, ArchiveException;

    /**
     * Is invoked once all entries have been processed.
     *
     * <p>This implementation is empty.
     *
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if an archive format related error occurs
     */
    public void finish() throws IOException, ArchiveException {
    }

    @Override
    public void process(ChainPayload<T> payload, Chain<T> chain) throws IOException, ArchiveException {
        consume(payload);
    }
}
