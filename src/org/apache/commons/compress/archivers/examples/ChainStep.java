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
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * A step inside of a {@link Chain}.
 * @since 1.17
 */
public interface ChainStep<T> {
    /**
     * Process the chain's payload.
     *
     * <p>Any non-terminal step that invokes the {@link Supplier} of
     * the payload is responsible for providing a fresh supplier if
     * the chain is to be continued.</p>
     *
     * @param payload the payload.
     * @param chain chain to return control to once processing is done.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if an archive format related error occurs
     */
    void process(ChainPayload<T> payload, Chain<T> chain) throws IOException, ArchiveException;
}
