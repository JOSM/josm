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
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Provides a high level API for creating archives.
 * @since 1.17
 */
public class Archiver {

    private interface ArchiveEntryCreator {
        ArchiveEntry create(File f, String entryName) throws IOException;
    }

    private interface ArchiveEntryConsumer {
        void accept(File source, ArchiveEntry entry) throws IOException;
    }

    private interface Finisher {
        void finish() throws IOException;
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in
     * {@code directory}.
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public void create(String format, File target, File directory) throws IOException, ArchiveException {
        if (prefersSeekableByteChannel(format)) {
            try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                create(format, c, directory);
            }
            return;
        }
        try (OutputStream o = Files.newOutputStream(target.toPath())) {
            create(format, o, directory);
        }
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in
     * {@code directory}.
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public void create(String format, OutputStream target, File directory) throws IOException, ArchiveException {
        create(new ArchiveStreamFactory().createArchiveOutputStream(format, target), directory);
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in
     * {@code directory}.
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param target the channel to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public void create(String format, SeekableByteChannel target, File directory)
        throws IOException, ArchiveException {
        if (!prefersSeekableByteChannel(format)) {
            create(format, Channels.newOutputStream(target), directory);
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            create(new ZipArchiveOutputStream(target), directory);
        } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            create(new SevenZOutputFile(target), directory);
        } else {
            // never reached as prefersSeekableByteChannel only returns true for ZIP and 7z
            throw new ArchiveException("don't know how to handle format " + format);
        }
    }

    /**
     * Creates an archive {@code target} by recursively including all
     * files and directories in {@code directory}.
     *
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public void create(final ArchiveOutputStream target, File directory)
        throws IOException, ArchiveException {
        create(directory, new ArchiveEntryCreator() {
            public ArchiveEntry create(File f, String entryName) throws IOException {
                return target.createArchiveEntry(f, entryName);
            }
        }, new ArchiveEntryConsumer() {
            public void accept(File source, ArchiveEntry e) throws IOException {
                target.putArchiveEntry(e);
                if (!e.isDirectory()) {
                    try (InputStream in = new BufferedInputStream(Files.newInputStream(source.toPath()))) {
                        IOUtils.copy(in, target);
                    }
                }
                target.closeArchiveEntry();
            }
        }, new Finisher() {
            public void finish() throws IOException {
                target.finish();
            }
        });
    }

    /**
     * Creates an archive {@code target} by recursively including all
     * files and directories in {@code directory}.
     *
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     */
    public void create(final SevenZOutputFile target, File directory) throws IOException {
        create(directory, new ArchiveEntryCreator() {
            public ArchiveEntry create(File f, String entryName) throws IOException {
                return target.createArchiveEntry(f, entryName);
            }
        }, new ArchiveEntryConsumer() {
            public void accept(File source, ArchiveEntry e) throws IOException {
                target.putArchiveEntry(e);
                if (!e.isDirectory()) {
                    final byte[] buffer = new byte[8024];
                    int n = 0;
                    long count = 0;
                    try (InputStream in = new BufferedInputStream(Files.newInputStream(source.toPath()))) {
                        while (-1 != (n = in.read(buffer))) {
                            target.write(buffer, 0, n);
                            count += n;
                        }
                    }
                }
                target.closeArchiveEntry();
            }
        }, new Finisher() {
            public void finish() throws IOException {
                target.finish();
            }
        });
    }

    private boolean prefersSeekableByteChannel(String format) {
        return ArchiveStreamFactory.ZIP.equalsIgnoreCase(format) || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }

    private void create(File directory, ArchiveEntryCreator creator, ArchiveEntryConsumer consumer,
        Finisher finisher) throws IOException {
        create("", directory, creator, consumer);
        finisher.finish();
    }

    private void create(String prefix, File directory, ArchiveEntryCreator creator, ArchiveEntryConsumer consumer)
        throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            String entryName = prefix + f.getName() + (f.isDirectory() ? "/" : "");
            consumer.accept(f, creator.create(f, entryName));
            if (f.isDirectory()) {
                create(entryName, f, creator, consumer);
            }
        }
    }
}
