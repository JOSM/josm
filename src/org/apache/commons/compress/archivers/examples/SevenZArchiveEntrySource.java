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
import java.util.NoSuchElementException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.NoCloseInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/**
 * Supplier based on {@link SevenZFile}s.
 * @since 1.17
 */
public class SevenZArchiveEntrySource implements ArchiveEntrySource {

    private final SevenZFile sf;

    public SevenZArchiveEntrySource(File f) throws IOException {
        this(new SevenZFile(f));
    }

    public SevenZArchiveEntrySource(SeekableByteChannel c) throws IOException {
        this(new SevenZFile(c));
    }

    public SevenZArchiveEntrySource(SevenZFile sf) {
        this.sf = sf;
    }

    @Override
    public ThrowingIterator<ChainPayload<ArchiveEntry>> get() throws IOException {
        return new SevenZFileIterator(sf);
    }

    @Override
    public void close() throws IOException {
        sf.close();
    }

    @Override
    public Filter<ArchiveEntry> skipUnreadable() {
        return new Filter<ArchiveEntry>() {
            @Override
            public boolean accept(String entryName, ArchiveEntry entry) {
                return true;
            }
        };
    }

    private static class SevenZFileIterator implements ThrowingIterator<ChainPayload<ArchiveEntry>> {
        private final SevenZFile sf;
        private ArchiveEntry nextEntry;
        private boolean nextEntryConsumed;
        SevenZFileIterator(SevenZFile sf) throws IOException {
            this.sf = sf;
            nextEntry = sf.getNextEntry();
            nextEntryConsumed = false;
        }

        @Override
        public boolean hasNext() throws IOException {
            if (nextEntry == null || nextEntryConsumed) {
                nextEntry = sf.getNextEntry();
                nextEntryConsumed = false;
            }
            return nextEntry != null && !nextEntryConsumed;
        }

        @Override
        public ChainPayload<ArchiveEntry> next() throws IOException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextEntryConsumed = true;
            return new ChainPayload(nextEntry, nextEntry.getName(), new Supplier<InputStream>() {
                    @Override
                    public InputStream get() throws IOException {
                        return new SevenZFileInputStream(sf);
                    }
                });
        }

    }

    private static class SevenZFileInputStream extends InputStream {
        private final SevenZFile sf;
        SevenZFileInputStream(SevenZFile sf) {
            this.sf = sf;
        }
        @Override
        public int read() throws IOException {
            return sf.read();
        }
        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return sf.read(b, off, len);
        }
    }
}
