// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;

/**
 * Detects the different UTF encodings from byte order mark
 */
public final class UTFInputStreamReader extends InputStreamReader {
    
    /**
     * Creates a new {@link InputStreamReader} from the {@link InputStream} with UTF-8 as default encoding.
     * @return A reader with the correct encoding. Starts to read after the BOM.
     * @see #create(java.io.InputStream, String)
     */
    public static UTFInputStreamReader create(InputStream input) throws IOException {
        return create(input, "UTF-8");
    }

    /**
     * Creates a new {@link InputStreamReader} from the {@link InputStream}.
     * @param defaultEncoding Used, when no BOM was recognized. Can be null.
     * @return A reader with the correct encoding. Starts to read after the BOM.
     */
    public static UTFInputStreamReader create(InputStream input, String defaultEncoding) throws IOException {
        byte[] bom = new byte[4];
        String encoding = defaultEncoding;
        int unread;
        PushbackInputStream pushbackStream = new PushbackInputStream(input, 4);
        int n = pushbackStream.read(bom, 0, 4);

        if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            encoding = "UTF-8";
            unread = n - 3;
        } else if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
            encoding = "UTF-32BE";
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
            encoding = "UTF-32LE";
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
            encoding = "UTF-16BE";
            unread = n - 2;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
            encoding = "UTF-16LE";
            unread = n - 2;
        } else {
            unread = n;
        }

        if (unread > 0) {
            pushbackStream.unread(bom, (n - unread), unread);
        } else if (unread < -1) {
            pushbackStream.unread(bom, 0, 0);
        }

        if (encoding == null) {
            return new UTFInputStreamReader(pushbackStream);
        } else {
            return new UTFInputStreamReader(pushbackStream, encoding);
        }
    }

    private UTFInputStreamReader(InputStream in) {
        super(in);
    }
    private UTFInputStreamReader(InputStream in, String cs) throws UnsupportedEncodingException {
        super(in, cs);
    }
}
