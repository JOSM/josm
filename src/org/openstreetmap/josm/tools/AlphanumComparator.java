// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers. Instead of sorting numbers in ASCII order like a standard
 * sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * Released under the MIT License - https://opensource.org/licenses/MIT
 *
 * Copyright 2007-2017 David Koelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.io.Serializable;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers: Instead of sorting numbers in ASCII order like a standard
 * sort, this algorithm sorts numbers in numeric order.
 * <p>
 * The Alphanum Algorithm is discussed at
 * <a href="https://web.archive.org/web/20210602024123/http://www.davekoelle.com/alphanum.html">DaveKoelle.com</a>
 * <p>
 * This is an updated version with enhancements made by Daniel Migowski, Andre
 * Bogus, David Koelle and others.
 *
 */
public final class AlphanumComparator implements Comparator<String>, Serializable {
    /** {@code true} to use the faster ASCII sorting algorithm. Set to {@code false} when testing compatibility. */
    static boolean useFastASCIISort = true;
    /**
     * The sort order for the fast ASCII sort method.
     */
    static final String ASCII_SORT_ORDER =
            " \r\t\n\f\u000b-_,;:!?/.`^~'\"()[]{}@$*\\&#%+<=>|0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final long serialVersionUID = 1L;

    private static final AlphanumComparator INSTANCE = new AlphanumComparator();
    /**
     * A mapping from ASCII characters to the default {@link Collator} order.
     * At writing, the default rules can be found in CollationRules#DEFAULTRULES.
     */
    private static final byte[] ASCII_MAPPING = new byte[128];
    static {
        for (int i = 0; i < ASCII_MAPPING.length; i++) {
            ASCII_MAPPING[i] = (byte) i; // This is kind of pointless, but it is the default ASCII ordering.
        }
        // The control characters are "ignored"
        Arrays.fill(ASCII_MAPPING, 0, 32, (byte) 0);
        ASCII_MAPPING[127] = 0; // DEL is ignored.
        // We have 37 order overrides for symbols; ASCII tables has control characters through 31. 32-47 are symbols.
        // After the symbols, we have 0-9, and then aA-zZ.
        // The character order
        for (int i = 0; i < ASCII_SORT_ORDER.length(); i++) {
            char c = ASCII_SORT_ORDER.charAt(i);
            ASCII_MAPPING[c] = (byte) (i + 1);
        }
    }

    /**
     * Replies the unique instance.
     * @return the unique instance
     */
    public static AlphanumComparator getInstance() {
        return INSTANCE;
    }

    /**
     * Constructs a new Alphanum Comparator.
     */
    private AlphanumComparator() {
    }

    /**
     * Compare two ASCII strings in a manner compatible with the default {@link Collator}
     * @param string1 The first string to compare
     * @param len1 The length of the first string
     * @param string2 The second string to compare
     * @param len2 The length of the second string
     * @return See {@link String#compareToIgnoreCase(String)} (e.g. {@code string1.compareToIgnoreCase(string2)}).
     */
    private static int compareString(String string1, int len1, String string2, int len2) {
        int loc1 = 0;
        int loc2 = 0;
        while (loc1 < len1 && loc2 < len2) {
            // Ignore control symbols
            while (loc1 < len1 - 1 && string1.charAt(loc1) <= 32) {
                loc1++;
            }
            while (loc2 < len2 - 1 && string2.charAt(loc2) <= 32) {
                loc2++;
            }
            if (loc1 >= len1 || loc2 >= len2) break;

            char lower1 = Character.toLowerCase(string1.charAt(loc1));
            char lower2 = Character.toLowerCase(string2.charAt(loc2));

            final int c1 = ASCII_MAPPING[lower1];
            final int c2 = ASCII_MAPPING[lower2];
            if (c1 != c2) {
                return c1 - c2;
            }
            loc1++;
            loc2++;
        }
        return len1 - len2;
    }

    /**
     * Returns an alphanum chunk.
     * Length of string is passed in for improved efficiency (only need to calculate it once).
     * @param s string
     * @param slength string length
     * @param marker position
     * @return alphanum chunk found at given position
     */
    private static String getChunk(String s, int slength, int marker) {
        final int startMarker = marker;
        char c = s.charAt(marker);
        marker++;
        if (Character.isDigit(c)) {
            while (marker < slength) {
                c = s.charAt(marker);
                if (!Character.isDigit(c)) {
                    break;
                }
                marker++;
            }
        } else {
            while (marker < slength) {
                c = s.charAt(marker);
                if (Character.isDigit(c)) {
                    break;
                }
                marker++;
            }
        }
        return s.substring(startMarker, marker);
    }

    /**
     * Check if a string is ASCII only
     * @param string The string to check
     * @param stringLength The length of the string (for performance reasons)
     * @return {@code true} if the string only contains ascii characters
     */
    private static boolean isAscii(String string, int stringLength) {
        for (int i = 0; i < stringLength; i++) {
            char c = string.charAt(i);
            if (c > ASCII_MAPPING.length) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two string chunks
     * @param thisChunk The first chunk to compare
     * @param thisChunkLength The length of the first chunk (for performance reasons)
     * @param thatChunk The second chunk to compare
     * @param thatChunkLength The length of the second chunk (for performance reasons)
     * @return The {@link Comparator} result
     */
    private static int compareChunk(String thisChunk, int thisChunkLength, String thatChunk, int thatChunkLength) {
        int result;
        if (Character.isDigit(thisChunk.charAt(0)) && Character.isDigit(thatChunk.charAt(0))) {
            // Simple chunk comparison by length.
            result = thisChunkLength - thatChunkLength;
            // If equal, the first different number counts
            if (result == 0) {
                for (int i = 0; i < thisChunkLength; i++) {
                    result = thisChunk.charAt(i) - thatChunk.charAt(i);
                    if (result != 0) {
                        return result;
                    }
                }
            }
        } else {
            // Check if both chunks are ascii only; if so, use a much faster sorting algorithm.
            if (useFastASCIISort && isAscii(thisChunk, thisChunkLength) && isAscii(thatChunk, thatChunkLength)) {
                return Utils.clamp(compareString(thisChunk, thisChunkLength, thatChunk, thatChunkLength), -1, 1);
            }
            // Instantiate the collator
            Collator compareOperator = Collator.getInstance();
            // Compare regardless of accented letters
            compareOperator.setStrength(Collator.SECONDARY);
            result = compareOperator.compare(thisChunk, thatChunk);
        }
        return result;
    }

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        } else if (s1 == null) {
            return -1;
        } else if (s2 == null) {
            return 1;
        }

        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            final String thisChunk = getChunk(s1, s1Length, thisMarker);
            final int thisChunkLength = thisChunk.length();
            thisMarker += thisChunkLength;

            String thatChunk = getChunk(s2, s2Length, thatMarker);
            final int thatChunkLength = thatChunk.length();
            thatMarker += thatChunkLength;

            // If both chunks contain numeric characters, sort them numerically
            int result = compareChunk(thisChunk, thisChunkLength, thatChunk, thatChunkLength);

            if (result != 0) {
                return result;
            }
        }

        return s1Length - s2Length;
    }
}
