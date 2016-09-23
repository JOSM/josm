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
package org.apache.commons.compress.compressors.bzip2;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.compress.compressors.FileNameUtil;

/**
 * Utility code for the BZip2 compression format.
 * @ThreadSafe
 * @since 1.1
 */
public abstract class BZip2Utils {

    private static final FileNameUtil fileNameUtil;

    static {
        final Map<String, String> uncompressSuffix =
            new LinkedHashMap<String, String>();
        // backwards compatibilty: BZip2Utils never created the short
        // tbz form, so .tar.bz2 has to be added explicitly
        uncompressSuffix.put(".tar.bz2", ".tar");
        uncompressSuffix.put(".tbz2", ".tar");
        uncompressSuffix.put(".tbz", ".tar");
        uncompressSuffix.put(".bz2", "");
        uncompressSuffix.put(".bz", "");
        fileNameUtil = new FileNameUtil(uncompressSuffix, ".bz2");
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private BZip2Utils() {
    }

    /**
     * Detects common bzip2 suffixes in the given filename.
     *
     * @param filename name of a file
     * @return {@code true} if the filename has a common bzip2 suffix,
     *         {@code false} otherwise
     */
    public static boolean isCompressedFilename(final String filename) {
        return fileNameUtil.isCompressedFilename(filename);
    }

    /**
     * Maps the given name of a bzip2-compressed file to the name that the
     * file should have after uncompression. Commonly used file type specific
     * suffixes like ".tbz" or ".tbz2" are automatically detected and
     * correctly mapped. For example the name "package.tbz2" is mapped to
     * "package.tar". And any filenames with the generic ".bz2" suffix
     * (or any other generic bzip2 suffix) is mapped to a name without that
     * suffix. If no bzip2 suffix is detected, then the filename is returned
     * unmapped.
     *
     * @param filename name of a file
     * @return name of the corresponding uncompressed file
     */
    public static String getUncompressedFilename(final String filename) {
        return fileNameUtil.getUncompressedFilename(filename);
    }

    /**
     * Maps the given filename to the name that the file should have after
     * compression with bzip2. Currently this method simply appends the suffix
     * ".bz2" to the filename based on the standard behaviour of the "bzip2"
     * program, but a future version may implement a more complex mapping if
     * a new widely used naming pattern emerges.
     *
     * @param filename name of a file
     * @return name of the corresponding compressed file
     */
    public static String getCompressedFilename(final String filename) {
        return fileNameUtil.getCompressedFilename(filename);
    }

}
