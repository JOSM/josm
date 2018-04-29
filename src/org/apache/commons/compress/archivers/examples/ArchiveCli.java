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
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Simple command line tool that creates an archive from the contents of a directory.
 *
 * <p>Usage: <code>ArchiveCli dir format archive</code></p>
 * @since 1.17
 */
public class ArchiveCli {

    public static void main(String[] args) throws IOException, ArchiveException {
        if (args.length != 3) {
            System.err.println("Usage: ArchiveCli dir format target");
            System.exit(1);
        }
        try (Sink<File> sink = FileToArchiveSink.forFile(args[1], new File(args[2]))) {
            Archive.directory(new File(args[0]))
                .to(sink);
        }
    }

}
