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
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/**
 * Supplies factory methods for file sinks that write to archives,
 * @since 1.17
 */
public class ArchiveSinks {
    /**
     * Uses {@link ArchiveStreamFactory#createArchiveOutputStream}.
     *
     * <p>Will not support 7z.</p>
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param os the stream to write to.
     * @return a sink that consumes the files
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public static Sink<File> forStream(String format, OutputStream os) throws IOException, ArchiveException {
        return new FileToArchiveSink(new ArchiveStreamFactory().createArchiveOutputStream(format, os));
    }

    /**
     * Uses {@link ArchiveStreamFactory#createArchiveOutputStream} unless
     * special handling for ZIP or 7z is required.
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param target the file to write to.
     * @return a sink that consumes the files
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public static Sink<File> forFile(String format, File target) throws IOException, ArchiveException {
        if (prefersSeekableByteChannel(format)) {
            return forChannel(format, FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        }
        return new FileToArchiveSink(new ArchiveStreamFactory()
            .createArchiveOutputStream(format, new FileOutputStream(target)));
    }

    /**
     * Uses {@link ArchiveStreamFactory#createArchiveOutputStream} unless
     * special handling for ZIP or 7z is required.
     *
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param c the channel to write to.
     * @return a sink that consumes the files
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public static Sink<File> forChannel(String format, SeekableByteChannel c) throws IOException, ArchiveException {
        if (!prefersSeekableByteChannel(format)) {
            return forStream(format, Channels.newOutputStream(c));
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            return new FileToArchiveSink(new ZipArchiveOutputStream(c));
        } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            return new SevenZOutputFileSink(c);
        } else {
            throw new ArchiveException("don't know how to handle format " + format);
        }
    }

    private static boolean prefersSeekableByteChannel(String format) {
        return ArchiveStreamFactory.ZIP.equalsIgnoreCase(format) || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }
}
