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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Recursively returns all files and directories contained inside of a base directory.
 * @since 1.17
 */
public class DirectoryBasedSource implements Source<File> {

    private final File dir;

    /**
     * @param dir the directory to provide entries from.
     */
    public DirectoryBasedSource(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("dir is not a readable directory");
        }
        this.dir = dir;
    }

    @Override
    public ThrowingIterator<ChainPayload<File>> get() throws IOException {
        return new DirectoryIterator("", dir);
    }

    @Override
    public void close() {
    }

    private static class DirectoryIterator implements ThrowingIterator<ChainPayload<File>> {
        private final Iterator<File> files;
        private final String namePrefix;
        private DirectoryIterator nestedIterator;
        DirectoryIterator(String namePrefix, File dir) throws IOException {
            this.namePrefix = namePrefix;
            File[] fs = dir.listFiles();
            files = fs == null ? Collections.<File>emptyIterator() : Arrays.asList(fs).iterator();
        }

        @Override
        public boolean hasNext() throws IOException {
            if (nestedIterator != null && nestedIterator.hasNext()) {
                return true;
            }
            if (nestedIterator != null) {
                nestedIterator = null;
            }
            return files.hasNext();
        }

        @Override
        public ChainPayload<File> next() throws IOException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (nestedIterator != null) {
                return nestedIterator.next();
            }
            final File f = files.next();
            String entryName = namePrefix + f.getName();
            if (f.isDirectory()) {
                entryName += "/";
                nestedIterator = new DirectoryIterator(entryName, f);
            }
            return new ChainPayload(f, entryName, new Supplier<InputStream>() {
                    @Override
                    public InputStream get() throws IOException {
                        return new FileInputStream(f);
                    }
                });
        }

    }
}
