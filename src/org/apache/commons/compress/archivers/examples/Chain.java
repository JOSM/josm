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

import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Encapsulates the execution flow of a chain of operations.
 * @since 1.17
 */
public class Chain<T> {

    private final Iterator<ChainStep<T>> chain;

    /**
     * Instantiates a new chain.
     *
     * @param chain the steps to take in order.
     */
    public Chain(Iterator<ChainStep<T>> chain) {
        this.chain = chain;
    }

    /**
     * Invokes the next step of the chain.
     *
     * @param payload the payload to pass to the next step
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if an archive format related error occurs
     */
    public void next(ChainPayload<T> payload) throws IOException, ArchiveException {
        if (chain.hasNext()) {
            chain.next().process(payload, this);
        }
    }
}
