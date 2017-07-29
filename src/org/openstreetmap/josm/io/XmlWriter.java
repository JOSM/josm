// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to use for xml outputting classes.
 *
 * @author imi
 */
public class XmlWriter implements Closeable {

    protected final PrintWriter out;

    /**
     * Constructs a new {@code XmlWriter}.
     * @param out print writer
     */
    public XmlWriter(PrintWriter out) {
        this.out = out;
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Encode the given string in XML1.0 format.
     * Optimized to fast pass strings that don't need encoding (normal case).
     *
     * @param unencoded the unencoded input string
     * @return XML1.0 string
     */
    public static String encode(String unencoded) {
        return encode(unencoded, false);
    }

    /**
     * Encode the given string in XML1.0 format.
     * Optimized to fast pass strings that don't need encoding (normal case).
     *
     * @param unencoded the unencoded input string
     * @param keepApos true if apostrophe sign should stay as it is (in order to work around
     * a Java bug that renders
     *     new JLabel("&lt;html&gt;&amp;apos;&lt;/html&gt;")
     * literally as 6 character string, see #7558)
     * @return XML1.0 string
     */
    public static String encode(String unencoded, boolean keepApos) {
        StringBuilder buffer = null;
        if (unencoded != null) {
            for (int i = 0; i < unencoded.length(); ++i) {
                String encS = null;
                if (!keepApos || unencoded.charAt(i) != '\'') {
                    encS = ENCODING.get(unencoded.charAt(i));
                }
                if (encS != null) {
                    if (buffer == null) {
                        buffer = new StringBuilder(unencoded.substring(0, i));
                    }
                    buffer.append(encS);
                } else if (buffer != null) {
                    buffer.append(unencoded.charAt(i));
                }
            }
        }
        return (buffer == null) ? unencoded : buffer.toString();
    }

    /**
     * The output writer to save the values to.
     */
    private static final Map<Character, String> ENCODING = new HashMap<>();
    static {
        ENCODING.put('<', "&lt;");
        ENCODING.put('>', "&gt;");
        ENCODING.put('"', "&quot;");
        ENCODING.put('\'', "&apos;");
        ENCODING.put('&', "&amp;");
        ENCODING.put('\n', "&#xA;");
        ENCODING.put('\r', "&#xD;");
        ENCODING.put('\t', "&#x9;");
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
    }
}
