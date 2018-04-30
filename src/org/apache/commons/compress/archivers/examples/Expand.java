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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Consumes archive entries and passes them to a sink, usually used to
 * expand an archive.
 * @since 1.17
 */
public class Expand {
    /**
     * Sets up a chain of operations and consumes the entries from a source of archive entries.
     * @since 1.17
     */
    public interface ChainBuilder {
        /**
         * Adds a filter to the chain.
         * @param filter the filter to apply
         * @return an updated builder
         */
        ChainBuilder filter(Filter<ArchiveEntry> filter);
        /**
         * Adds a filter to the chain that filters out entries that cannot be read.
         * @return an updated builder
         */
        ChainBuilder skipUnreadable();
        /**
         * Adds a filter to the chain that suppresses all directory entries.
         * @return an updated builder
         */
        ChainBuilder skipDirectories();
        /**
         * Adds a transformer to the chain.
         * @param transformer transformer to apply
         * @return an updated builder
         */
        ChainBuilder map(Transformer<ArchiveEntry> transformer);
        /**
         * Adds a generic step to the chain.
         * @return an updated builder
         * @param step step to perform
         */
        ChainBuilder withStep(ChainStep<ArchiveEntry> step);
        /**
         * Actually consumes all the entries supplied.
         * @param sink sink that the entries will be sent to
         * @throws IOException if an I/O error occurs
         * @throws ArchiveException if the source archive cannot be read for other reasons
         */
        void to(Sink<ArchiveEntry> sink) throws IOException, ArchiveException;
    }

    /**
     * Sets the source of entries to process.
     * @param source the source
     * @return a builder for the chain to be created and run
     */
    public static ChainBuilder source(ArchiveEntrySource source) {
        return new Builder(source);
    }

    private static class Builder implements ChainBuilder {
        private final ArchiveEntrySource source;
        private ChainDefinition<ArchiveEntry> chainDef = new ChainDefinition<>();

        Builder(ArchiveEntrySource source) {
            this.source = source;
        }

        @Override
        public ChainBuilder filter(Filter<ArchiveEntry> filter) {
            return withStep(filter);
        }
        @Override
        public ChainBuilder skipUnreadable() {
            return filter(source.skipUnreadable());
        }
        @Override
        public ChainBuilder skipDirectories() {
            return filter(new Filter<ArchiveEntry>() {
                @Override
                public boolean accept(String entryName, ArchiveEntry e) {
                    return !e.isDirectory();
                }
            });
        }
        @Override
        public ChainBuilder map(Transformer<ArchiveEntry> transformer) {
            return withStep(transformer);
        }
        @Override
        public ChainBuilder withStep(ChainStep<ArchiveEntry> step) {
            chainDef.add(step);
            return this;
        }
        @Override
        public void to(Sink<ArchiveEntry> sink) throws IOException, ArchiveException {
            chainDef.add(sink);
            chainDef.freeze();
            new ChainRunner<ArchiveEntry>(source, chainDef, sink).run();
        }
    }
}
