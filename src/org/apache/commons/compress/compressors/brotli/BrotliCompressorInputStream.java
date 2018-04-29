/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.compressors.brotli;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.brotli.dec.BrotliInputStream;

/**
 * {@link CompressorInputStream} implementation to decode Brotli encoded stream.
 * Library relies on <a href="https://github.com/google/brotli">Google brotli</a>
 *
 * @since 1.14
 */
public class BrotliCompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    private final CountingInputStream countingStream;
    private final BrotliInputStream decIS;

    public BrotliCompressorInputStream(final InputStream in) throws IOException {
        decIS = new BrotliInputStream(countingStream = new CountingInputStream(in));
    }

    @Override
    public int available() throws IOException {
        return decIS.available();
    }

    @Override
    public void close() throws IOException {
        decIS.close();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return decIS.read(b);
    }

    @Override
    public long skip(final long n) throws IOException {
        return IOUtils.skip(decIS, n);
    }

    @Override
    public void mark(final int readlimit) {
        decIS.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return decIS.markSupported();
    }

    @Override
    public int read() throws IOException {
        final int ret = decIS.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int ret = decIS.read(buf, off, len);
        count(ret);
        return ret;
    }

    @Override
    public String toString() {
        return decIS.toString();
    }

    @Override
    public void reset() throws IOException {
        decIS.reset();
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getBytesRead();
    }
}
