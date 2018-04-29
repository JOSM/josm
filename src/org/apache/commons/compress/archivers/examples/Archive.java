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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Consumes files and passes them to a sink, usually used to create an archive of them.
 * @since 1.17
 */
public class Archive {
    /**
     * Sets up a chain of operations and consumes the files from a supplier of files.
     * @since 1.17
     */
    public interface ChainBuilder {
        /**
         * Adds a filter to the chain.
         */
        ChainBuilder filter(Filter<File> filter);
        /**
         * Adds a filter to the chain.
         */
        ChainBuilder filter(FileFilter filter);
        /**
         * Adds a filter to the chain that filters out entries that cannot be read.
         */
        ChainBuilder skipUnreadable();
        /**
         * Adds a filter to the chain that filters out everything that is not a file.
         */
        ChainBuilder skipNonFiles();
        /**
         * Adds a transformer to the chain.
         */
        ChainBuilder map(Transformer<File> transformer);
        /**
         * Actually consumes all the files supplied.
         */
        void to(Sink<File> sink) throws IOException, ArchiveException;
    }

    /**
     * Sets the source of files to be a directory.
     */
    public static ChainBuilder directory(File f) {
        return source(new DirectoryBasedSupplier(f));
    }

    /**
     * Sets the source of files to process.
     */
    public static ChainBuilder source(Supplier<ThrowingIterator<ChainPayload<File>>> supplier) {
        return new Builder(supplier);
    }

    private static class Builder implements ChainBuilder {
        private final Supplier<ThrowingIterator<ChainPayload<File>>> supplier;
        private ChainDefinition<File> chainDef = new ChainDefinition<>();

        Builder(Supplier<ThrowingIterator<ChainPayload<File>>> supplier) {
            this.supplier = supplier;
        }

        public ChainBuilder filter(Filter<File> filter) {
            chainDef.add(filter);
            return this;
        }
        public ChainBuilder filter(FileFilter filter) {
            return filter(new FileFilterAdapter(filter));
        }
        public ChainBuilder skipUnreadable() {
            return filter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.canRead();
                }
            });
        }
        public ChainBuilder skipNonFiles() {
            return filter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile();
                }
            });
        }
        public ChainBuilder map(Transformer transformer) {
            chainDef.add(transformer);
            return this;
        }
        public void to(Sink<File> sink) throws IOException, ArchiveException {
            chainDef.add(sink);
            chainDef.freeze();
            new Archive(supplier, chainDef, sink).run();
        }
    }

    private final Supplier<ThrowingIterator<ChainPayload<File>>> supplier;
    private final ChainDefinition<File> chainDef;
    private final Sink<File> sink;

    private Archive(Supplier<ThrowingIterator<ChainPayload<File>>> supplier, ChainDefinition<File> chainDef,
        Sink<File> sink) {
        this.supplier = supplier;
        this.chainDef = chainDef;
        this.sink = sink;
    }

    private void run() throws IOException, ArchiveException {
        ThrowingIterator<ChainPayload<File>> iter = supplier.get();
        while (iter.hasNext()) {
            chainDef.chain().next(iter.next());
        }
        sink.finish();
    }
}
