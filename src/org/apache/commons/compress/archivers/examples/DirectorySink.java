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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.IOUtils;

/**
 * A sink that expands archive entries into a directory.
 * @since 1.17
 */
public class DirectorySink extends Sink<ArchiveEntry> {
    private final File dir;
    private final String dirPath;

    /**
     * Sets up a directory as sink.
     *
     * @param dir the directory to provide entries from.
     * @throws IOException if the canonical path of the directory cannot be determined
     * @throws IllegalArgumentException if dir doesn't exist or is not a directory 
     */
    public DirectorySink(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("dir is not a readable directory");
        }
        this.dir = dir;
        dirPath = dir.getCanonicalPath();
    }

    @Override
    public void consume(ChainPayload<ArchiveEntry> payload) throws IOException, ArchiveException {
        File f = new File(dir, payload.getEntryName());
        if (!f.getCanonicalPath().startsWith(dirPath)) {
            throw new IOException("expanding " + payload.getEntryName() + " would create file outside of " + dir);
        }
        if (payload.getEntry().isDirectory()) {
            f.mkdirs();
        } else {
            f.getParentFile().mkdirs();
            try (OutputStream o = new FileOutputStream(f);
                 InputStream i = payload.getInput().get()) {
                IOUtils.copy(i, o);
            }
        }
    }

    @Override
    public void close() {
    }
}
