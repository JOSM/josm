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
     * Sets up a chain of operations and consumes the files from a source of files.
     * @since 1.17
     */
    public interface ChainBuilder {
        /**
         * Adds a filter to the chain.
         * @param filter the filter to apply
         * @return an updated builder
         */
        ChainBuilder filter(Filter<File> filter);
        /**
         * Adds a filter to the chain.
         * @param filter the filter to apply
         * @return an updated builder
         */
        ChainBuilder filter(FileFilter filter);
        /**
         * Adds a filter to the chain that filters out entries that cannot be read.
         * @return an updated builder
         */
        ChainBuilder skipUnreadable();
        /**
         * Adds a filter to the chain that filters out everything that is not a file.
         * @return an updated builder
         */
        ChainBuilder skipNonFiles();
        /**
         * Adds a transformer to the chain.
         * @param transformer transformer to apply
         * @return an updated builder
         */
        ChainBuilder map(Transformer<File> transformer);
        /**
         * Adds a generic step to the chain.
         * @param step step to perform
         * @return an updated builder
         */
        ChainBuilder withStep(ChainStep<File> step);
        /**
         * Actually consumes all the files supplied.
         * @param sink sink that the entries will be sent to
         * @throws IOException if an I/O error occurs
         * @throws ArchiveException if the archive cannot be written for other reasons
         */
        void to(Sink<File> sink) throws IOException, ArchiveException;
    }

    /**
     * Sets the source of files to be a directory.
     * @param f the source directory
     * @return a builder for the chain to be created and run
     */
    public static ChainBuilder directory(File f) {
        return source(new DirectoryBasedSource(f));
    }

    /**
     * Sets the source of files to process.
     * @param source the source directory
     * @return a builder for the chain to be created and run
     */
    public static ChainBuilder source(Source<File> source) {
        return new Builder(source);
    }

    private static class Builder implements ChainBuilder {
        private final Source<File> source;
        private ChainDefinition<File> chainDef = new ChainDefinition<>();

        Builder(Source<File> source) {
            this.source = source;
        }

        @Override
        public ChainBuilder filter(Filter<File> filter) {
            return withStep(filter);
        }
        @Override
        public ChainBuilder filter(FileFilter filter) {
            return filter(new FileFilterAdapter(filter));
        }
        @Override
        public ChainBuilder skipUnreadable() {
            return filter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.canRead();
                }
            });
        }
        @Override
        public ChainBuilder skipNonFiles() {
            return filter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile();
                }
            });
        }
        @Override
        public ChainBuilder map(Transformer<File> transformer) {
            return withStep(transformer);
        }
        @Override
        public ChainBuilder withStep(ChainStep<File> step) {
            chainDef.add(step);
            return this;
        }
        @Override
        public void to(Sink<File> sink) throws IOException, ArchiveException {
            chainDef.add(sink);
            chainDef.freeze();
            new ChainRunner<File>(source, chainDef, sink).run();
        }
    }

}
