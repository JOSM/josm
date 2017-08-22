// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;

import org.openstreetmap.josm.tools.Logging;

/**
 * FilterInputStream that gets rid of characters that are invalid in an XML 1.0
 * document.
 *
 * Although these characters are forbidden, in the real wold they still appear
 * in XML files. Java's SAX parser throws an exception, so we have to filter
 * at a lower level.
 *
 * Only handles control characters (&lt;0x20). Invalid characters are replaced
 * by space (0x20).
 */
public class InvalidXmlCharacterFilter extends Reader {

    private final Reader reader;

    private static boolean firstWarning = true;

    private static final boolean[] INVALID_CHARS;

    static {
        INVALID_CHARS = new boolean[0x20];
        for (int i = 0; i < INVALID_CHARS.length; ++i) {
            INVALID_CHARS[i] = true;
        }
        INVALID_CHARS[0x9] = false; // tab
        INVALID_CHARS[0xA] = false; // LF
        INVALID_CHARS[0xD] = false; // CR
    }

    /**
     * Constructs a new {@code InvalidXmlCharacterFilter} for the given Reader.
     * @param reader The reader to filter
     */
    public InvalidXmlCharacterFilter(Reader reader) {
        this.reader = reader;
    }

    @Override
    public int read(char[] b, int off, int len) throws IOException {
        int n = reader.read(b, off, len);
        if (n == -1) {
            return -1;
        }
        for (int i = off; i < off + n; ++i) {
            b[i] = filter(b[i]);
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private static char filter(char in) {
        if (in < 0x20 && INVALID_CHARS[in]) {
            if (firstWarning) {
                Logging.warn("Invalid xml character encountered: '"+in+"'.");
                firstWarning = false;
            }
            return 0x20;
        }
        return in;
    }
}
