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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Supplies factory methods for ArchiveEntry sources that read from archives,
 * @since 1.17
 */
public class ArchiveSources {
    /**
     * Builder for {@link ArchiveEntrySource} that needs to know its format.
     * @since 1.17
     */
    public interface PendingFormat {
        ArchiveEntrySource detectFormat() throws IOException, ArchiveException;
        ArchiveEntrySource withFormat(String format) throws IOException, ArchiveException;
    }

    /**
     * Uses {@link ArchiveFactory#createArchiveInputStream} unless special handling for ZIP or /z is required.
     */
    public static PendingFormat forFile(final File f) {
        return new PendingFormat() {
            @Override
            public ArchiveEntrySource detectFormat() throws IOException, ArchiveException {
                String format = null;
                try (InputStream i = new BufferedInputStream(new FileInputStream(f))) {
                    format = new ArchiveStreamFactory().detect(i);
                }
                return withFormat(format);
            }
            @Override
            public ArchiveEntrySource withFormat(String format) throws IOException, ArchiveException {
                if (prefersSeekableByteChannel(format)) {
                    return forChannel(format, FileChannel.open(f.toPath(), StandardOpenOption.READ));
                }
                return new StreamBasedArchiveEntrySource(new ArchiveStreamFactory()
                    .createArchiveInputStream(format, new BufferedInputStream(new FileInputStream(f))));
            }
        };
    }

    /**
     * Uses {@link ArchiveFactory#createArchiveInputStream} unless special handling for ZIP or /z is required.
     */
    public static ArchiveEntrySource forChannel(String format, SeekableByteChannel c)
        throws IOException, ArchiveException {
        if (!prefersSeekableByteChannel(format)) {
            return new StreamBasedArchiveEntrySource(new ArchiveStreamFactory()
                .createArchiveInputStream(format, Channels.newInputStream(c)));
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            return new ZipArchiveEntrySource(c);
        } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            return new SevenZArchiveEntrySource(c);
        }
        throw new ArchiveException("don't know how to handle format " + format);
    }

    /**
     * Uses {@link ArchiveFactory#createArchiveInputStream}.
     *
     * <p>Will not support 7z.</p>
     */
    public static PendingFormat forStream(final InputStream in) {
        return new PendingFormat() {
            @Override
            public ArchiveEntrySource detectFormat() throws IOException, ArchiveException {
                return new StreamBasedArchiveEntrySource(new ArchiveStreamFactory().createArchiveInputStream(in));
            }
            @Override
            public ArchiveEntrySource withFormat(String format) throws IOException, ArchiveException {
                return new StreamBasedArchiveEntrySource(new ArchiveStreamFactory()
                    .createArchiveInputStream(format, in));
            }
        };
    }

    private static boolean prefersSeekableByteChannel(String format) {
        return ArchiveStreamFactory.ZIP.equalsIgnoreCase(format) || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }
}
