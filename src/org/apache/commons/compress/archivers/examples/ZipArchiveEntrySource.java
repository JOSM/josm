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
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Supplier based on {@link ZipFile}s.
 * @since 1.17
 */
public class ZipArchiveEntrySource implements ArchiveEntrySource {

    private final ZipFile zf;

    public ZipArchiveEntrySource(File f) throws IOException {
        this(new ZipFile(f));
    }

    public ZipArchiveEntrySource(SeekableByteChannel c) throws IOException {
        this(new ZipFile(c));
    }

    public ZipArchiveEntrySource(ZipFile file) {
        zf = file;
    }

    @Override
    public ThrowingIterator<ChainPayload<ArchiveEntry>> get() throws IOException {
        return new ZipFileIterator(zf, zf.getEntries());
    }

    @Override
    public void close() throws IOException {
        zf.close();
    }

    @Override
    public Filter<ArchiveEntry> skipUnreadable() {
        return new Filter<ArchiveEntry>() {
            @Override
            public boolean accept(String entryName, ArchiveEntry entry) {
                return entry instanceof ZipArchiveEntry && zf.canReadEntryData((ZipArchiveEntry) entry);
            }
        };
    }

    private static class ZipFileIterator implements ThrowingIterator<ChainPayload<ArchiveEntry>> {
        private final ZipFile zf;
        private final Enumeration<ZipArchiveEntry> iter;
        ZipFileIterator(ZipFile zf, Enumeration<ZipArchiveEntry> iter) {
            this.zf = zf;
            this.iter = iter;
        }

        @Override
        public boolean hasNext() throws IOException {
            return iter.hasMoreElements();
        }

        @Override
        public ChainPayload<ArchiveEntry> next() throws IOException {
            final ZipArchiveEntry z = iter.nextElement();
            return new ChainPayload(z, z.getName(), new Supplier<InputStream>() {
                    @Override
                    public InputStream get() throws IOException {
                        return zf.getInputStream(z);
                    }
                });
        }

    }
}
