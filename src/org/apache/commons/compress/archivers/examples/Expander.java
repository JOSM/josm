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
import java.util.Enumeration;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Provides a high level API for expanding archives.
 * @since 1.17
 */
public class Expander {

    private interface ArchiveEntrySupplier {
        ArchiveEntry getNextReadableEntry() throws IOException;
    }

    private interface EntryWriter {
        void writeEntryDataTo(ArchiveEntry entry, OutputStream out) throws IOException;
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(File archive, File targetDirectory) throws IOException, ArchiveException {
        String format = null;
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            format = new ArchiveStreamFactory().detect(i);
        }
        expand(format, archive, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(String format, File archive, File targetDirectory) throws IOException, ArchiveException {
        if (prefersSeekableByteChannel(format)) {
            try (SeekableByteChannel c = FileChannel.open(archive.toPath(), StandardOpenOption.READ)) {
                expand(format, c, targetDirectory);
            }
            return;
        }
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            expand(format, i, targetDirectory);
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(InputStream archive, File targetDirectory) throws IOException, ArchiveException {
        expand(new ArchiveStreamFactory().createArchiveInputStream(archive), targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(String format, InputStream archive, File targetDirectory)
        throws IOException, ArchiveException {
        expand(new ArchiveStreamFactory().createArchiveInputStream(format, archive), targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(String format, SeekableByteChannel archive, File targetDirectory)
        throws IOException, ArchiveException {
        if (!prefersSeekableByteChannel(format)) {
            expand(format, Channels.newInputStream(archive), targetDirectory);
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            expand(new ZipFile(archive), targetDirectory);
        } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            expand(new SevenZFile(archive), targetDirectory);
        } else {
            // never reached as prefersSeekableByteChannel only returns true for ZIP and 7z
            throw new ArchiveException("don't know how to handle format " + format);
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(final ArchiveInputStream archive, File targetDirectory)
        throws IOException, ArchiveException {
        expand(new ArchiveEntrySupplier() {
            @Override
            public ArchiveEntry getNextReadableEntry() throws IOException {
                ArchiveEntry next = archive.getNextEntry();
                while (next != null && !archive.canReadEntryData(next)) {
                    next = archive.getNextEntry();
                }
                return next;
            }
        }, new EntryWriter() {
            @Override
            public void writeEntryDataTo(ArchiveEntry entry, OutputStream out) throws IOException {
                IOUtils.copy(archive, out);
            }
        }, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(final ZipFile archive, File targetDirectory)
        throws IOException, ArchiveException {
        final Enumeration<ZipArchiveEntry> entries = archive.getEntries();
        expand(new ArchiveEntrySupplier() {
            @Override
            public ArchiveEntry getNextReadableEntry() throws IOException {
                ZipArchiveEntry next = entries.hasMoreElements() ? entries.nextElement() : null;
                while (next != null && !archive.canReadEntryData(next)) {
                    next = entries.hasMoreElements() ? entries.nextElement() : null;
                }
                return next;
            }
        }, new EntryWriter() {
            @Override
            public void writeEntryDataTo(ArchiveEntry entry, OutputStream out) throws IOException {
                try (InputStream in = archive.getInputStream((ZipArchiveEntry) entry)) {
                    IOUtils.copy(in, out);
                }
            }
        }, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(final SevenZFile archive, File targetDirectory)
        throws IOException, ArchiveException {
        expand(new ArchiveEntrySupplier() {
            @Override
            public ArchiveEntry getNextReadableEntry() throws IOException {
                return archive.getNextEntry();
            }
        }, new EntryWriter() {
            @Override
            public void writeEntryDataTo(ArchiveEntry entry, OutputStream out) throws IOException {
                final byte[] buffer = new byte[8024];
                int n;
                while (-1 != (n = archive.read(buffer))) {
                    out.write(buffer, 0, n);
                }
            }
        }, targetDirectory);
    }

    private boolean prefersSeekableByteChannel(String format) {
        return ArchiveStreamFactory.ZIP.equalsIgnoreCase(format) || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }

    private void expand(ArchiveEntrySupplier supplier, EntryWriter writer, File targetDirectory)
        throws IOException {
        String targetDirPath = targetDirectory.getCanonicalPath();
        if (!targetDirPath.endsWith(File.separator)) {
            targetDirPath += File.separator;
        }
        ArchiveEntry nextEntry = supplier.getNextReadableEntry();
        while (nextEntry != null) {
            File f = new File(targetDirectory, nextEntry.getName());
            if (!f.getCanonicalPath().startsWith(targetDirPath)) {
                throw new IOException("expanding " + nextEntry.getName()
                    + " would create file outside of " + targetDirectory);
            }
            if (nextEntry.isDirectory()) {
                if (!f.isDirectory() && !f.mkdirs()) {
                    throw new IOException("failed to create directory " + f);
                }
            } else {
                File parent = f.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("failed to create directory " + parent);
                }
                try (OutputStream o = Files.newOutputStream(f.toPath())) {
                    writer.writeEntryDataTo(nextEntry, o);
                }
            }
            nextEntry = supplier.getNextReadableEntry();
        }
    }

}
