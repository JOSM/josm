// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.tools.Logging;

/**
 * A basic Protobuf parser
 *
 * @author Taylor Smock
 * @since 17862
 */
public class ProtobufParser implements AutoCloseable {
    /**
     * The default byte size (see {@link #VAR_INT_BYTE_SIZE} for var ints)
     */
    public static final byte BYTE_SIZE = 8;
    /**
     * The byte size for var ints (since the first byte is just an indicator for if the var int is done)
     */
    public static final byte VAR_INT_BYTE_SIZE = BYTE_SIZE - 1;
    /**
     * Used to get the most significant byte
     */
    static final byte MOST_SIGNIFICANT_BYTE = (byte) (1 << 7);
    /**
     * Convert a byte array to a number (little endian)
     *
     * @param bytes    The bytes to convert
     * @param byteSize The size of the byte. For var ints, this is 7, for other ints, this is 8.
     * @return An appropriate {@link Number} class.
     */
    public static Number convertByteArray(byte[] bytes, byte byteSize) {
        long number = 0;
        for (int i = 0; i < bytes.length; i++) {
            // Need to convert to uint64 in order to avoid bit operation from filling in 1's and overflow issues
            number += Byte.toUnsignedLong(bytes[i]) << (byteSize * i);
        }
        return convertLong(number);
    }

    /**
     * Convert a long to an appropriate {@link Number} class
     *
     * @param number The long to convert
     * @return A {@link Number}
     */
    public static Number convertLong(long number) {
        // TODO deal with booleans
        if (number <= Byte.MAX_VALUE && number >= Byte.MIN_VALUE) {
            return (byte) number;
        } else if (number <= Short.MAX_VALUE && number >= Short.MIN_VALUE) {
            return (short) number;
        } else if (number <= Integer.MAX_VALUE && number >= Integer.MIN_VALUE) {
            return (int) number;
        }
        return number;
    }

    /**
     * Decode a zig-zag encoded value
     *
     * @param signed The value to decode
     * @return The decoded value
     */
    public static Number decodeZigZag(Number signed) {
        final long value = signed.longValue();
        return convertLong((value >> 1) ^ -(value & 1));
    }

    /**
     * Encode a number to a zig-zag encode value
     *
     * @param signed The number to encode
     * @return The encoded value
     */
    public static Number encodeZigZag(Number signed) {
        final long value = signed.longValue();
        // This boundary condition could be >= or <= or both. Tests indicate that it doesn't actually matter.
        // The only difference would be the number type returned, except it is always converted to the most basic type.
        final int shift = (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE ? Long.BYTES : Integer.BYTES) * 8 - 1;
        return convertLong((value << 1) ^ (value >> shift));
    }

    private final InputStream inputStream;

    /**
     * Create a new parser
     *
     * @param bytes The bytes to parse
     */
    public ProtobufParser(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * Create a new parser
     *
     * @param inputStream The InputStream (will be fully read at this time)
     */
    public ProtobufParser(InputStream inputStream) {
        if (inputStream.markSupported()) {
            this.inputStream = inputStream;
        } else {
            this.inputStream = new BufferedInputStream(inputStream);
        }
    }

    /**
     * Read all records
     *
     * @return A collection of all records
     * @throws IOException - if an IO error occurs
     */
    public Collection<ProtobufRecord> allRecords() throws IOException {
        Collection<ProtobufRecord> records = new ArrayList<>();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4);
        while (this.hasNext()) {
            records.add(new ProtobufRecord(byteArrayOutputStream, this));
        }
        return records;
    }

    @Override
    public void close() {
        try {
            this.inputStream.close();
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    /**
     * Check if there is more data to read
     *
     * @return {@code true} if there is more data to read
     * @throws IOException - if an IO error occurs
     */
    public boolean hasNext() throws IOException {
        return this.inputStream.available() > 0;
    }

    /**
     * Get the "next" WireType
     *
     * @return {@link WireType} expected
     * @throws IOException - if an IO error occurs
     */
    public WireType next() throws IOException {
        this.inputStream.mark(16);
        try {
            return WireType.getAllValues()[this.inputStream.read() << 3];
        } finally {
            this.inputStream.reset();
        }
    }

    /**
     * Get the next byte
     *
     * @return The next byte
     * @throws IOException - if an IO error occurs
     */
    public int nextByte() throws IOException {
        return this.inputStream.read();
    }

    /**
     * Get the next 32 bits ({@link WireType#THIRTY_TWO_BIT})
     *
     * @return a byte array of the next 32 bits (4 bytes)
     * @throws IOException - if an IO error occurs
     */
    public byte[] nextFixed32() throws IOException {
        // 4 bytes == 32 bits
        return readNextBytes(4);
    }

    /**
     * Get the next 64 bits ({@link WireType#SIXTY_FOUR_BIT})
     *
     * @return a byte array of the next 64 bits (8 bytes)
     * @throws IOException - if an IO error occurs
     */
    public byte[] nextFixed64() throws IOException {
        // 8 bytes == 64 bits
        return readNextBytes(8);
    }

    /**
     * Get the next delimited message ({@link WireType#LENGTH_DELIMITED})
     *
     * @param byteArrayOutputStream A reusable stream to write bytes to. This can significantly reduce the allocations
     *                              (150 MB to 95 MB in a test area).
     * @return The next length delimited message
     * @throws IOException - if an IO error occurs
     */
    public byte[] nextLengthDelimited(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        int length = convertByteArray(this.nextVarInt(byteArrayOutputStream), VAR_INT_BYTE_SIZE).intValue();
        return readNextBytes(length);
    }

    /**
     * Get the next var int ({@code WireType#VARINT})
     *
     * @param byteArrayOutputStream A reusable stream to write bytes to. This can significantly reduce the allocations
     *                              (150 MB to 95 MB in a test area).
     * @return The next var int ({@code int32}, {@code int64}, {@code uint32}, {@code uint64}, {@code bool}, {@code enum})
     * @throws IOException - if an IO error occurs
     */
    public byte[] nextVarInt(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        // Using this reduces the allocations from 150 MB to 95 MB.
        int currentByte = this.nextByte();
        while ((byte) (currentByte & MOST_SIGNIFICANT_BYTE) == MOST_SIGNIFICANT_BYTE && currentByte > 0) {
            // Get rid of the leading bit (shift left 1, then shift right 1 unsigned)
            byteArrayOutputStream.write((currentByte ^ MOST_SIGNIFICANT_BYTE));
            currentByte = this.nextByte();
        }
        // The last byte doesn't drop the most significant bit
        byteArrayOutputStream.write(currentByte);
        try {
            return byteArrayOutputStream.toByteArray();
        } finally {
            byteArrayOutputStream.reset();
        }
    }

    /**
     * Read an arbitrary number of bytes
     *
     * @param size The number of bytes to read
     * @return a byte array of the specified size, filled with bytes read (unsigned)
     * @throws IOException - if an IO error occurs
     */
    private byte[] readNextBytes(int size) throws IOException {
        byte[] bytesRead = new byte[size];
        for (int i = 0; i < bytesRead.length; i++) {
            bytesRead[i] = (byte) this.nextByte();
        }
        return bytesRead;
    }
}
