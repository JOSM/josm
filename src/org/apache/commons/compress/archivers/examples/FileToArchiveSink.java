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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Sink that creates an archive from files.
 * @since 1.17
 */
public class FileToArchiveSink extends Sink<File> {
    private final ArchiveOutputStream os;

    /**
     * Wraps an ArchiveOutputStream.
     *
     * @param os the stream to write to
     */
    public FileToArchiveSink(ArchiveOutputStream os) {
        this.os = os;
    }

    @Override
    public void consume(ChainPayload<File> payload) throws IOException, ArchiveException {
        ArchiveEntry e = os.createArchiveEntry(payload.getEntry(), payload.getEntryName());
        os.putArchiveEntry(e);
        if (!payload.getEntry().isDirectory()) {
            try (InputStream in = new BufferedInputStream(payload.getInput().get())) {
                IOUtils.copy(in, os);
            }
        }
        os.closeArchiveEntry();
    }

    @Override
    public void finish() throws IOException {
        os.finish();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

}
