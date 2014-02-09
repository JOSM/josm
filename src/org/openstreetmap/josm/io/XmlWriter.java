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
     */
    public static String encode(String unencoded, boolean keepApos) {
        StringBuilder buffer = null;
        for (int i = 0; i < unencoded.length(); ++i) {
            String encS = null;
            if (!keepApos || unencoded.charAt(i) != '\'') {
                encS = XmlWriter.encoding.get(unencoded.charAt(i));
            }
            if (encS != null) {
                if (buffer == null) {
                    buffer = new StringBuilder(unencoded.substring(0,i));
                }
                buffer.append(encS);
            } else if (buffer != null) {
                buffer.append(unencoded.charAt(i));
            }
        }
        return (buffer == null) ? unencoded : buffer.toString();
    }

    /**
     * The output writer to save the values to.
     */
    private static final Map<Character, String> encoding = new HashMap<Character, String>();
    static {
        encoding.put('<', "&lt;");
        encoding.put('>', "&gt;");
        encoding.put('"', "&quot;");
        encoding.put('\'', "&apos;");
        encoding.put('&', "&amp;");
        encoding.put('\n', "&#xA;");
        encoding.put('\r', "&#xD;");
        encoding.put('\t', "&#x9;");
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
    }
}
