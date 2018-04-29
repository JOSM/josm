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
import java.nio.channels.SeekableByteChannel;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Sink that creates a 7z archive from files.
 * @since 1.17
 */
public class SevenZOutputFileSink extends Sink<File> {

    private final SevenZOutputFile outFile;

    public SevenZOutputFileSink(File f) throws IOException {
        this(new SevenZOutputFile(f));
    }

    public SevenZOutputFileSink(SeekableByteChannel c) throws IOException {
        this(new SevenZOutputFile(c));
    }

    public SevenZOutputFileSink(SevenZOutputFile outFile) {
        this.outFile = outFile;
    }

    @Override
    public void consume(ChainPayload<File> payload) throws IOException, ArchiveException {
        ArchiveEntry e = outFile.createArchiveEntry(payload.getEntry(), payload.getEntryName());
        outFile.putArchiveEntry(e);
        if (!payload.getEntry().isDirectory()) {
            final byte[] buffer = new byte[8024];
            int n = 0;
            long count = 0;
            try (InputStream in = new BufferedInputStream(payload.getInput().get())) {
                while (-1 != (n = in.read(buffer))) {
                    outFile.write(buffer, 0, n);
                    count += n;
                }
            }
        }
        outFile.closeArchiveEntry();
    }

    @Override
    public void finish() throws IOException {
        outFile.finish();
    }

    @Override
    public void close() throws IOException {
        outFile.close();
    }
}
