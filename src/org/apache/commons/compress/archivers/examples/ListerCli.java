/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.examples;

import java.io.File;
import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Simple command line application that lists the contents of an archive.
 *
 * <p>The name of the archive must be given as a command line argument.</p>
 * <p>The optional second argument defines the archive type, in case the format is not recognized.</p>
 *
 * @since 1.17
 */
public final class ListerCli {

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        System.out.println("Analysing " + args[0]);
        final Sink<ArchiveEntry> sink = new Sink<ArchiveEntry>() {
            @Override
            public void consume(ChainPayload<ArchiveEntry> payload) {
                System.out.println(payload.getEntry().getName());
            }
            @Override
            public void close() {
            }
        };

        final File f = new File(args[0]);
        if (!f.isFile()) {
            System.err.println(f + " doesn't exist or is a directory");
        } else if (args.length == 1) {
            try (ArchiveEntrySource source = ArchiveSources.forFile(f).detectFormat()) {
                Expand.source(source).to(sink);
            }
        } else {
            try (ArchiveEntrySource source = ArchiveSources.forFile(f).withFormat(args[1])) {
                Expand.source(source).to(sink);
            }
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [archive-type]");
    }

}
