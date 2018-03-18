// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class that analyzes the text and attempts to parse tags from it
 * @since 13544 (extracted from {@link TextTagParser})
 */
public class TextAnalyzer {
    private boolean quotesStarted;
    private boolean esc;
    private final StringBuilder s = new StringBuilder(200);
    private String valueStops = "\n\r\t";
    private int pos;
    private final String data;
    private final int n;

    /**
     * Create a new {@link TextAnalyzer}
     * @param text The text to parse
     */
    public TextAnalyzer(String text) {
        pos = 0;
        data = Utils.strip(text);
        n = data.length();
        // fix #1604: allow space characters as value stops for single-line input only
        if (data.indexOf('\r') == -1 && data.indexOf('\n') == -1) {
            valueStops += " ";
        }
    }

    /**
     * Read tags from "Free format"
     * @return map of tags
     */
    public Map<String, String> getFreeParsedTags() {
        String k, v;
        Map<String, String> tags = new HashMap<>();

        while (true) {
            skipEmpty();
            if (pos == n) {
                break;
            }
            k = parseString("\n\r\t= ");
            if (pos == n) {
                tags.clear();
                break;
            }
            skipSign();
            if (pos == n) {
                tags.clear();
                break;
            }
            v = parseString(valueStops);
            tags.put(k, v);
        }
        return tags;
    }

    /**
     * Parses current text to extract a key or value depending on given stop characters.
     * @param stopChars Parsing will stop when one character of this string is found
     * @return key or value extracted from current text
     */
    public String parseString(String stopChars) {
        char[] stop = stopChars.toCharArray();
        Arrays.sort(stop);
        char c;
        while (pos < n) {
            c = data.charAt(pos);
            if (esc) {
                esc = false;
                s.append(c); //  \" \\
            } else if (c == '\\') {
                esc = true;
            } else if (c == '\"' && !quotesStarted) { // opening "
                if (!s.toString().trim().isEmpty()) { // we had   ||some text"||
                    s.append(c); // just add ", not open
                } else {
                    s.delete(0, s.length()); // forget that empty characthers and start reading "....
                    quotesStarted = true;
                }
            } else if (c == '\"' && quotesStarted) {  // closing "
                quotesStarted = false;
                pos++;
                break;
            } else if (!quotesStarted && (Arrays.binarySearch(stop, c) >= 0)) {
                // stop-symbol found
                pos++;
                break;
            } else {
                // skip non-printable characters
                if (c >= 32) s.append(c);
            }
            pos++;
        }

        String res = s.toString();
        s.delete(0, s.length());
        return res.trim();
    }

    private void skipSign() {
        char c;
        boolean signFound = false;
        while (pos < n) {
            c = data.charAt(pos);
            if (c == '\t' || c == '\n' || c == ' ') {
                pos++;
            } else if (c == '=') {
                if (signFound) break; // a  =  =qwerty means "a"="=qwerty"
                signFound = true;
                pos++;
            } else {
                break;
            }
        }
    }

    private void skipEmpty() {
        char c;
        while (pos < n) {
            c = data.charAt(pos);
            if (c == '\t' || c == '\n' || c == '\r' || c == ' ') {
                pos++;
            } else {
                break;
            }
        }
    }
}
