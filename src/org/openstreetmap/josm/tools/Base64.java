// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.nio.ByteBuffer;

/**
 * This class implements an encoder for encoding byte and character data using the
 * <a href="https://en.wikipedia.org/wiki/Base64">Base64</a> encoding scheme as specified in
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a> ("base64", default) and
 * <a href="http://tools.ietf.org/html/rfc4648#section-4">RFC 4648</a> ("base64url").
 * @since 195
 */
public final class Base64 {
    // TODO: Remove this class when switching to Java 8 (finally integrated in Java SE as java.util.Base64.Encoder)

    private Base64() {
        // Hide default constructor for utils classes
    }

    /** "base64": RFC 2045 default encoding */
    private static String encDefault = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    /** "base64url": RFC 4648 url-safe encoding */
    private static String encUrlSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    /**
     * Encodes all characters from the specified string into a new String using the Base64 default encoding scheme.
     * @param s the string to encode
     * @return A new string containing the resulting encoded characters.
     */
    public static String encode(String s) {
        return encode(s, false);
    }

    /**
     * Encodes all characters from the specified string into a new String using a supported Base64 encoding scheme.
     * @param s the string to encode
     * @param urlsafe if {@code true}, uses "base64url" encoding, otherwise use the "base64" default encoding
     * @return A new string containing the resulting encoded characters.
     * @since 3840
     */
    public static String encode(String s, boolean urlsafe) {
        StringBuilder out = new StringBuilder();
        String enc = urlsafe ? encUrlSafe : encDefault;
        for (int i = 0; i < (s.length()+2)/3; ++i) {
            int l = Math.min(3, s.length()-i*3);
            String buf = s.substring(i*3, i*3+l);
            out.append(enc.charAt(buf.charAt(0) >> 2));
            out.append(enc.charAt(
                                  (buf.charAt(0) & 0x03) << 4 |
                                  (l == 1 ? 0 : (buf.charAt(1) & 0xf0) >> 4)));
            out.append(l > 1 ? enc.charAt((buf.charAt(1) & 0x0f) << 2 | (l == 2 ? 0 : (buf.charAt(2) & 0xc0) >> 6)) : '=');
            out.append(l > 2 ? enc.charAt(buf.charAt(2) & 0x3f) : '=');
        }
        return out.toString();
    }

    /**
     * Encodes all remaining bytes from the specified byte buffer into a new string using the Base64 default encoding scheme.
     * Upon return, the source buffer's position will be updated to its limit; its limit will not have been changed.
     * @param s the source ByteBuffer to encode
     * @return A new string containing the resulting encoded characters.
     */
    public static String encode(ByteBuffer s) {
        return encode(s, false);
    }

    /**
     * Encodes all remaining bytes from the specified byte buffer into a new string using a supported Base64 encoding scheme.
     * Upon return, the source buffer's position will be updated to its limit; its limit will not have been changed.
     * @param s the source ByteBuffer to encode
     * @param urlsafe if {@code true}, uses "base64url" encoding, otherwise use the "base64" default encoding
     * @return A new string containing the resulting encoded characters.
     * @since 3840
     */
    public static String encode(ByteBuffer s, boolean urlsafe) {
        StringBuilder out = new StringBuilder();
        String enc = urlsafe ? encUrlSafe : encDefault;
        // Read 3 bytes at a time.
        for (int i = 0; i < (s.limit()+2)/3; ++i) {
            int l = Math.min(3, s.limit()-i*3);
            int byte0 = s.get() & 0xff;
            int byte1 = l > 1 ? s.get() & 0xff : 0;
            int byte2 = l > 2 ? s.get() & 0xff : 0;

            out.append(enc.charAt(byte0 >> 2));
            out.append(enc.charAt(
                                  (byte0 & 0x03) << 4 |
                                  (l == 1 ? 0 : (byte1 & 0xf0) >> 4)));
            out.append(l > 1 ? enc.charAt((byte1 & 0x0f) << 2 | (l == 2 ? 0 : (byte2 & 0xc0) >> 6)) : '=');
            out.append(l > 2 ? enc.charAt(byte2 & 0x3f) : '=');
        }
        return out.toString();
    }
}
