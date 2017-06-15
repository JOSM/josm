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
package org.apache.commons.compress.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixedLengthBlockOutputStream extends OutputStream implements WritableByteChannel,
    AutoCloseable {

    private final WritableByteChannel out;
    private final int blockSize;
    private final ByteBuffer buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FixedLengthBlockOutputStream(OutputStream os, int blockSize) {
        if (os instanceof FileOutputStream) {
            FileOutputStream fileOutputStream = (FileOutputStream) os;
            out = fileOutputStream.getChannel();
            buffer = ByteBuffer.allocateDirect(blockSize);
        } else {
            out = new BufferAtATimeOutputChannel(os);
            buffer = ByteBuffer.allocate(blockSize);
        }
        this.blockSize = blockSize;
    }

    public FixedLengthBlockOutputStream(WritableByteChannel out, int blockSize) {
        this.out = out;
        this.blockSize = blockSize;
        this.buffer = ByteBuffer.allocateDirect(blockSize);
    }

    private void maybeFlush() throws IOException {
        if (!buffer.hasRemaining()) {
            writeBlock();
        }
    }

    private void writeBlock() throws IOException {
        buffer.flip();
        int i = out.write(buffer);
        boolean hasRemaining = buffer.hasRemaining();
        if (i != blockSize || hasRemaining) {
            String msg = String
                .format("Failed to write %,d bytes atomically. Only wrote  %,d",
                    blockSize, i);
            throw new IOException(msg);
        }
        buffer.clear();
    }

    @Override
    public void write(int b) throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        buffer.put((byte) b);
        maybeFlush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        while (len > 0) {
            int n = Math.min(len, buffer.remaining());
            buffer.put(b, off, n);
            maybeFlush();
            len -= n;
            off += n;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        int srcRemaining = src.remaining();

        if (srcRemaining < buffer.remaining()) {
            // if don't have enough bytes in src to fill up a block we must buffer
            buffer.put(src);
        } else {
            int srcLeft = srcRemaining;
            int savedLimit = src.limit();
            // If we're not at the start of buffer, we have some bytes already  buffered
            // fill up the reset of buffer and write the block.
            if (buffer.position() != 0) {
                int n = buffer.remaining();
                src.limit(src.position() + n);
                buffer.put(src);
                writeBlock();
                srcLeft -= n;
            }
            // whilst we have enough bytes in src for complete blocks,
            // write them directly from src without copying them to buffer
            while (srcLeft >= blockSize) {
                src.limit(src.position() + blockSize);
                out.write(src);
                srcLeft -= blockSize;
            }
            // copy any remaining bytes into buffer
            src.limit(savedLimit);
            buffer.put(src);
        }
        return srcRemaining;
    }

    @Override
    public boolean isOpen() {
        if(!out.isOpen()) {
            closed.set(true);
        }
        return !closed.get();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            if (buffer.position() != 0) {
                padLastBlock();
                writeBlock();
            }
            out.close();
        }
    }

    private void padLastBlock() {
        buffer.order(ByteOrder.nativeOrder());
        int bytesToWrite = buffer.remaining();
        if (bytesToWrite > 8) {
            int align = (buffer.position() & 7);
            if (align != 0) {
                int limit = 8 - align;
                for (int i = 0; i < limit; i++) {
                    buffer.put((byte) 0);
                }
                bytesToWrite -= limit;
            }

            while (bytesToWrite >= 8) {
                buffer.putLong(0L);
                bytesToWrite -= 8;
            }
        }
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
    }

    /**
     * Helper class to provide channel wrapper for arbitrary output stream that doesn't alter the
     * size of writes.  We can't use Channels.newChannel, because for non FileOutputStreams, it
     * breaks up writes into 8KB max chunks. Since the purpose of this class is to always write
     * complete blocks, we need to write a simple class to take care of it.
     */
    private static class BufferAtATimeOutputChannel implements WritableByteChannel {

        private final OutputStream out;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private BufferAtATimeOutputChannel(OutputStream out) {
            this.out = out;
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            assert isOpen() : "somehow trying to write to closed BufferAtATimeOutputChannel";
            assert buffer.hasArray() :
                "direct buffer somehow written to BufferAtATimeOutputChannel";

            try {
                int pos = buffer.position();
                int len = buffer.limit() - pos;
                out.write(buffer.array(), buffer.arrayOffset() + pos, len);
                buffer.position(buffer.limit());
                return len;
            } catch (IOException e) {
                  try {
                      close();
                  } finally {
                      throw e;
                  }
            }
        }

        @Override
        public boolean isOpen() {
            return !closed.get();
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                out.close();
            }
        }

    }


}
