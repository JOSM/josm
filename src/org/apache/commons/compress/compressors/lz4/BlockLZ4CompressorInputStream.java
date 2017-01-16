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
package org.apache.commons.compress.compressors.lz4;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.IOUtils;

/**
 * CompressorInputStream for the LZ4 block format.
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Block_format.html">LZ4 Block Format Description</a>
 * @since 1.14
 */
public class BlockLZ4CompressorInputStream extends CompressorInputStream {

    private static final int WINDOW_SIZE = 1 << 16;
    private static final int SIZE_BITS = 4;
    private static final int COPY_SIZE_MASK = (1 << SIZE_BITS) - 1;
    private static final int LITERAL_SIZE_MASK = COPY_SIZE_MASK << SIZE_BITS;

    /** Buffer to write decompressed bytes to for back-references */
    private final byte[] buf = new byte[3 * WINDOW_SIZE];

    /** One behind the index of the last byte in the buffer that was written */
    private int writeIndex;

    /** Index of the next byte to be read. */
    private int readIndex;

    /** The underlying stream to read compressed data from */
    private final InputStream in;

    /** Number of bytes still to be read from the current literal or copy. */
    private long bytesRemaining;

    /** Copy-size part of the block starting byte. */
    private int nextCopySize;

    /** Offset of the current copy. */
    private int copyOffset;

    /** Current state of the stream */
    private State state = State.NO_BLOCK;

    /** uncompressed size */
    private int size = 0;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    private final ByteUtils.ByteSupplier supplier = new ByteUtils.ByteSupplier() {
        @Override
        public int getAsByte() throws IOException {
            return readOneByte();
        }
    };

    /**
     * Creates a new LZ4 input stream.
     *
     * @param is
     *            An InputStream to read compressed data from
     *
     * @throws IOException if reading fails
     */
    public BlockLZ4CompressorInputStream(final InputStream is) throws IOException {
        this.in = is;
        writeIndex = readIndex = 0;
        bytesRemaining = 0;
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        in.close();
    }

    /** {@inheritDoc} */
    @Override
    public int available() {
        return writeIndex - readIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (state == State.EOF) {
            return -1;
        }
        switch (state) {
        case NO_BLOCK:
            readSizes();
            /*FALLTHROUGH*/
        case IN_LITERAL:
            int litLen = readLiteral(b, off, len);
            if (bytesRemaining == 0) {
                state = State.LOOKING_FOR_COPY;
            }
            return litLen;
        case LOOKING_FOR_COPY:
            if (!initializeCopy()) {
                state = State.EOF;
                return -1;
            }
            /*FALLTHROUGH*/
        case IN_COPY:
            int copyLen = readCopy(b, off, len);
            if (bytesRemaining == 0) {
                state = State.NO_BLOCK;
            }
            return copyLen;
        default:
            throw new IOException("Unknown stream state " + state);
        }
    }

    /**
     * Get the uncompressed size of the stream
     *
     * @return the uncompressed size
     */
    public int getSize() {
        return size;
    }

    private void readSizes() throws IOException {
        int nextBlock = readOneByte();
        if (nextBlock == -1) {
            throw new IOException("Premature end of stream while looking for next block");
        }
        nextCopySize = nextBlock & COPY_SIZE_MASK;
        long literalSizePart = (nextBlock & LITERAL_SIZE_MASK) >> SIZE_BITS;
        if (literalSizePart == COPY_SIZE_MASK) {
            literalSizePart += readSizeBytes();
        }
        bytesRemaining = literalSizePart;
        state = State.IN_LITERAL;
    }

    private long readSizeBytes() throws IOException {
        long accum = 0;
        int nextByte;
        do {
            nextByte = readOneByte();
            if (nextByte == -1) {
                throw new IOException("Premature end of stream while parsing length");
            }
            accum += nextByte;
        } while (nextByte == 255);
        return accum;
    }

    private int readLiteral(final byte[] b, final int off, final int len) throws IOException {
        final int avail = available();
        if (len > avail) {
            tryToReadLiteral(len - avail);
        }
        return readFromBuffer(b, off, len);
    }

    private void tryToReadLiteral(int bytesToRead) throws IOException {
        final int reallyTryToRead = (int) Math.min(Math.min(bytesToRead, bytesRemaining),
                                                   buf.length - writeIndex);
        final int bytesRead = reallyTryToRead > 0
            ? IOUtils.readFully(in, buf, writeIndex, reallyTryToRead)
            : 0 /* happens for bytesRemaining == 0 */;
        count(bytesRead);
        if (reallyTryToRead != bytesRead) {
            throw new IOException("Premature end of stream reading literal");
        }
        writeIndex += reallyTryToRead;
        bytesRemaining -= reallyTryToRead;
    }

    private int readFromBuffer(final byte[] b, final int off, final int len) throws IOException {
        final int readable = Math.min(len, available());
        if (readable > 0) {
            System.arraycopy(buf, readIndex, b, off, readable);
            readIndex += readable;
            if (readIndex > 2 * WINDOW_SIZE) {
                slideBuffer();
            }
        }
        size += readable;
        return readable;
    }

    private void slideBuffer() {
        System.arraycopy(buf, WINDOW_SIZE, buf, 0, WINDOW_SIZE);
        writeIndex -= WINDOW_SIZE;
        readIndex -= WINDOW_SIZE;
    }

    /**
     * @return false if there is no more copy - this means this is the
     * last block of the stream.
     */
    private boolean initializeCopy() throws IOException {
        try {
            copyOffset = (int) ByteUtils.fromLittleEndian(supplier, 2);
        } catch (IOException ex) {
            if (nextCopySize == 0) { // the last block has no copy
                return false;
            }
            throw ex;
        }
        long copySize = nextCopySize;
        if (nextCopySize == COPY_SIZE_MASK) {
            copySize += readSizeBytes();
        }
        bytesRemaining = copySize + 4; // minimal match length 4 is encoded as 0
        state = State.IN_COPY;
        return true;
    }

    private int readCopy(final byte[] b, final int off, final int len) throws IOException {
        final int avail = available();
        if (len > avail) {
            tryToCopy(len - avail);
        }
        return readFromBuffer(b, off, len);
    }

    private void tryToCopy(int bytesToCopy) throws IOException {
        // this will fit into the buffer without sliding and not
        // require more than is available inside the copy
        int copy = (int) Math.min(Math.min(bytesToCopy, bytesRemaining),
                                  buf.length - writeIndex);
        if (copy == 0) {
            // NOP
        } else if (copyOffset == 1) { // pretty common special case
            final byte last = buf[writeIndex - 1];
            for (int i = 0; i < copy; i++) {
                buf[writeIndex++] = last;
            }
        } else if (copy < copyOffset) {
            System.arraycopy(buf, writeIndex - copyOffset, buf, writeIndex, copy);
            writeIndex += copy;
        } else {
            final int fullRots = copy / copyOffset;
            for (int i = 0; i < fullRots; i++) {
                System.arraycopy(buf, writeIndex - copyOffset, buf, writeIndex, copyOffset);
                writeIndex += copyOffset;
            }

            final int pad = copy - (copyOffset * fullRots);
            if (pad > 0) {
                System.arraycopy(buf, writeIndex - copyOffset, buf, writeIndex, pad);
                writeIndex += pad;
            }
        }
        bytesRemaining -= copy;
    }

    private int readOneByte() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
            return b & 0xFF;
        }
        return -1;
    }

    private enum State {
        NO_BLOCK, IN_LITERAL, LOOKING_FOR_COPY, IN_COPY, EOF
    }
}
