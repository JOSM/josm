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
package org.apache.commons.compress.compressors.lzw;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BitInputStream;

/**
 * <p>Generic LZW implementation. It is used internally for
 * the Z decompressor and the Unshrinking Zip file compression method,
 * but may be useful for third-party projects in implementing their own LZW variations.</p>
 *
 * @NotThreadSafe
 * @since 1.10
 */
public abstract class LZWInputStream extends CompressorInputStream {
    protected static final int DEFAULT_CODE_SIZE = 9;
    protected static final int UNUSED_PREFIX = -1;

    private final byte[] oneByte = new byte[1];

    protected final BitInputStream in;
    private int clearCode = -1;
    private int codeSize = DEFAULT_CODE_SIZE;
    private byte previousCodeFirstChar;
    private int previousCode = UNUSED_PREFIX;
    private int tableSize;
    private int[] prefixes;
    private byte[] characters;
    private byte[] outputStack;
    private int outputStackLocation;

    protected LZWInputStream(final InputStream inputStream, final ByteOrder byteOrder) {
        this.in = new BitInputStream(inputStream, byteOrder);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
    
    @Override
    public int read() throws IOException {
        int ret = read(oneByte);
        if (ret < 0) {
            return ret;
        }
        return 0xff & oneByte[0];
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = readFromStack(b, off, len);
        while (len - bytesRead > 0) {
            int result = decompressNextSymbol();
            if (result < 0) {
                if (bytesRead > 0) {
                    count(bytesRead);
                    return bytesRead;
                }
                return result;
            }
            bytesRead += readFromStack(b, off + bytesRead, len - bytesRead);
        }
        count(bytesRead);
        return bytesRead;
    }

    /**
     * Read the next code and expand it.
     */
    protected abstract int decompressNextSymbol() throws IOException;

    /**
     * Add a new entry to the dictionary.
     */
    protected abstract int addEntry(int previousCode, byte character)
        throws IOException;

    /**
     * Sets the clear code based on the code size.
     */
    protected void setClearCode(int codeSize) {
        clearCode = (1 << (codeSize - 1));
    }

    /**
     * Initializes the arrays based on the maximum code size.
     */
    protected void initializeTables(int maxCodeSize) {
        final int maxTableSize = 1 << maxCodeSize;
        prefixes = new int[maxTableSize];
        characters = new byte[maxTableSize];
        outputStack = new byte[maxTableSize];
        outputStackLocation = maxTableSize;
        final int max = 1 << 8;
        for (int i = 0; i < max; i++) {
            prefixes[i] = -1;
            characters[i] = (byte) i;
        }
    }

    /**
     * Reads the next code from the stream.
     */
    protected int readNextCode() throws IOException {
        if (codeSize > 31) {
            throw new IllegalArgumentException("code size must not be bigger than 31");
        }
        return (int) in.readBits(codeSize);
    }
    
    /**
     * Adds a new entry if the maximum table size hasn't been exceeded
     * and returns the new index.
     */
    protected int addEntry(int previousCode, byte character, int maxTableSize) {
        if (tableSize < maxTableSize) {
            prefixes[tableSize] = previousCode;
            characters[tableSize] = character;
            return tableSize++;
        }
        return -1;
    }

    /**
     * Add entry for repeat of previousCode we haven't added, yet.
     */
    protected int addRepeatOfPreviousCode() throws IOException {
        if (previousCode == -1) {
            // can't have a repeat for the very first code
            throw new IOException("The first code can't be a reference to its preceding code");
        }
        return addEntry(previousCode, previousCodeFirstChar);
    }

    /**
     * Expands the entry with index code to the output stack and may
     * create a new entry
     */
    protected int expandCodeToOutputStack(int code, boolean addedUnfinishedEntry)
        throws IOException {
        for (int entry = code; entry >= 0; entry = prefixes[entry]) {
            outputStack[--outputStackLocation] = characters[entry];
        }
        if (previousCode != -1 && !addedUnfinishedEntry) {
            addEntry(previousCode, outputStack[outputStackLocation]);
        }
        previousCode = code;
        previousCodeFirstChar = outputStack[outputStackLocation];
        return outputStackLocation;
    }

    private int readFromStack(byte[] b, int off, int len) {
        int remainingInStack = outputStack.length - outputStackLocation;
        if (remainingInStack > 0) {
            int maxLength = Math.min(remainingInStack, len);
            System.arraycopy(outputStack, outputStackLocation, b, off, maxLength);
            outputStackLocation += maxLength;
            return maxLength;
        }
        return 0;
    }

    protected int getCodeSize() {
        return codeSize;
    }

    protected void resetCodeSize() {
        setCodeSize(DEFAULT_CODE_SIZE);
    }

    protected void setCodeSize(int cs) {
        this.codeSize = cs;
    }

    protected void incrementCodeSize() {
        codeSize++;
    }

    protected void resetPreviousCode() {
        this.previousCode = -1;
    }

    protected int getPrefix(int offset) {
        return prefixes[offset];
    }

    protected void setPrefix(int offset, int value) {
        prefixes[offset] = value;
    }

    protected int getPrefixesLength() {
        return prefixes.length;
    }

    protected int getClearCode() {
        return clearCode;
    }

    protected int getTableSize() {
        return tableSize;
    }

    protected void setTableSize(int newSize) {
        tableSize = newSize;
    }

}
