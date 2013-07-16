// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;

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
public class InvalidXmlCharacterFilter extends FilterInputStream {

    public static boolean firstWarning = true;

    public static final boolean[] INVALID_CHARS;

    static {
        INVALID_CHARS = new boolean[0x20];
        for (int i = 0; i < INVALID_CHARS.length; ++i) {
            INVALID_CHARS[i] = true;
        }
        INVALID_CHARS[0x9] = false; // tab
        INVALID_CHARS[0xA] = false; // LF
        INVALID_CHARS[0xD] = false; // CR
    }

    public InvalidXmlCharacterFilter(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        return filter((byte)super.read());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n == -1) {
            return -1;
        }
        for (int i = off; i < off + n; ++i) {
            b[i] = filter(b[i]);
        }
        return n;
    }

    private byte filter(byte in) {
        if (in < 0x20 && in >= 0 && INVALID_CHARS[in]) {
            if (firstWarning) {
                Main.warn("Invalid xml character encountered.");
                firstWarning = false;
            }
            return 0x20;
        }
        return in;
    }

}
