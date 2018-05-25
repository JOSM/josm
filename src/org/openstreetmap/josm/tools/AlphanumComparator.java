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
import java.util.Comparator;

/**
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers: Instead of sorting numbers in ASCII order like a standard
 * sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * This is an updated version with enhancements made by Daniel Migowski, Andre
 * Bogus, David Koelle and others.
 *
 */
public final class AlphanumComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final AlphanumComparator INSTANCE = new AlphanumComparator();

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
     * Returns an alphanum chunk.
     * Length of string is passed in for improved efficiency (only need to calculate it once).
     * @param s string
     * @param slength string length
     * @param marker position
     * @return alphanum chunk found at given position
     */
    private static String getChunk(String s, int slength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (Character.isDigit(c)) {
            while (marker < slength) {
                c = s.charAt(marker);
                if (!Character.isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        } else {
            while (marker < slength) {
                c = s.charAt(marker);
                if (Character.isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
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
            String thisChunk = getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();

            String thatChunk = getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();

            // If both chunks contain numeric characters, sort them numerically
            int result;
            if (Character.isDigit(thisChunk.charAt(0)) && Character.isDigit(thatChunk.charAt(0))) {
                // Simple chunk comparison by length.
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
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
                // Instantiate the collator
                Collator compareOperator = Collator.getInstance();
                // Compare regardless of accented letters
                compareOperator.setStrength(Collator.SECONDARY);
                result = compareOperator.compare(thisChunk, thatChunk);
            }

            if (result != 0) {
                return result;
            }
        }

        return s1Length - s2Length;
    }
}
