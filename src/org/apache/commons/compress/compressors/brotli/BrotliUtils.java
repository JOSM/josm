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
package org.apache.commons.compress.compressors.brotli;

/**
 * Utility code for the Brotli compression format.
 * @ThreadSafe
 * @since 1.14
 */
public class BrotliUtils {

    enum CachedAvailability {
        DONT_CACHE, CACHED_AVAILABLE, CACHED_UNAVAILABLE
    }

    private static volatile CachedAvailability cachedBrotliAvailability;

    static {
        cachedBrotliAvailability = CachedAvailability.DONT_CACHE;
        try {
            Class.forName("org.osgi.framework.BundleEvent");
        } catch (final Exception ex) { // NOSONAR
            setCacheBrotliAvailablity(true);
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private BrotliUtils() {
    }


    /**
     * Are the classes required to support Brotli compression available?
     * @return true if the classes required to support Brotli compression are available
     */
    public static boolean isBrotliCompressionAvailable() {
        final CachedAvailability cachedResult = cachedBrotliAvailability;
        if (cachedResult != CachedAvailability.DONT_CACHE) {
            return cachedResult == CachedAvailability.CACHED_AVAILABLE;
        }
        return internalIsBrotliCompressionAvailable();
    }

    private static boolean internalIsBrotliCompressionAvailable() {
        try {
            Class.forName("org.brotli.dec.BrotliInputStream");
            return true;
        } catch (NoClassDefFoundError | Exception error) { // NOSONAR
            return false;
        }
    }

    /**
     * Whether to cache the result of the Brotli for Java check.
     *
     * <p>This defaults to {@code false} in an OSGi environment and {@code true} otherwise.</p>
     * @param doCache whether to cache the result
     */
    public static void setCacheBrotliAvailablity(final boolean doCache) {
        if (!doCache) {
            cachedBrotliAvailability = CachedAvailability.DONT_CACHE;
        } else if (cachedBrotliAvailability == CachedAvailability.DONT_CACHE) {
            final boolean hasBrotli = internalIsBrotliCompressionAvailable();
            cachedBrotliAvailability = hasBrotli ? CachedAvailability.CACHED_AVAILABLE
                : CachedAvailability.CACHED_UNAVAILABLE;
        }
    }

    // only exists to support unit tests
    static CachedAvailability getCachedBrotliAvailability() {
        return cachedBrotliAvailability;
    }
}
