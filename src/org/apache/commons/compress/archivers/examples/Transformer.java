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
 * Transforming stage of a {@link Expand} or {@link Archive} chain.
 * @since 1.17
 */
public abstract class Transformer<T> implements ChainStep<T> {
    /**
     * Transforms an entry.
     *
     * @param entry the entry
     * @return the transformed entry
     */
    public abstract ChainPayload<T> transform(ChainPayload<T> entry);

    @Override
    public void process(ChainPayload<T> payload, Chain<T> chain) throws IOException, ArchiveException {
        chain.next(transform(payload));
    }
}
