/*
 * Copyright (c) 2003 Objectix Pty Ltd  All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL OBJECTIX PTY LTD BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.data.projection.datum;

/**
 * A set of static utility methods for reading the NTv2 file format
 *
 * @author Peter Yuill
 */
public final class NTV2Util {

    private NTV2Util() {
    }

    /**
     * Get a Little Endian int from four bytes of a byte array
     * @param b the byte array
     * @param i the index of the first data byte in the array
     * @return the int
     */
    public static int getIntLE(byte[] b, int i) {
        return (b[i++] & 0x000000FF) | ((b[i++] << 8) & 0x0000FF00) | ((b[i++] << 16) & 0x00FF0000) | (b[i] << 24);
    }

    /**
     * Get a Big Endian int from four bytes of a byte array
     * @param b the byte array
     * @param i the index of the first data byte in the array
     * @return the int
     */
    public static int getIntBE(byte[] b, int i) {
        return (b[i++] << 24) | ((b[i++] << 16) & 0x00FF0000) | ((b[i++] << 8) & 0x0000FF00) | (b[i] & 0x000000FF);
    }

    /**
     * Get an int from the first 4 bytes of a byte array,
     * in either Big Endian or Little Endian format.
     * @param b the byte array
     * @param bigEndian is the byte array Big Endian?
     * @return the int
     */
    public static int getInt(byte[] b, boolean bigEndian) {
        if (bigEndian)
            return getIntBE(b, 0);
        else
            return getIntLE(b, 0);
    }

    /**
     * Get a float from the first 4 bytes of a byte array,
     * in either Big Endian or Little Endian format.
     * @param b the byte array
     * @param bigEndian is the byte array Big Endian?
     * @return the float
     */
    public static float getFloat(byte[] b, boolean bigEndian) {
        int i;
        if (bigEndian) {
            i = getIntBE(b, 0);
        } else {
            i = getIntLE(b, 0);
        }
        return Float.intBitsToFloat(i);
    }

    /**
     * Get a double from the first 8 bytes of a byte array,
     * in either Big Endian or Little Endian format.
     * @param b the byte array
     * @param bigEndian is the byte array Big Endian?
     * @return the double
     */
    public static double getDouble(byte[] b, boolean bigEndian) {
        int i;
        int j;
        if (bigEndian) {
            i = getIntBE(b, 0);
            j = getIntBE(b, 4);
        } else {
            i = getIntLE(b, 4);
            j = getIntLE(b, 0);
        }
        long l = ((long) i << 32) | (j & 0x0000_0000_FFFF_FFFFL);
        return Double.longBitsToDouble(l);
    }
}
