// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Text/String utils.
 * @since 13978
 */
public final class TextUtils {

    private TextUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Inserts zero width space character (U+8203) after each slash/amperand to wrap long URLs.
     * @param url URL
     * @return wrapped URL
     * @since 13978
     */
    public static String wrapLongUrl(String url) {
        return url.replace("/", "/\u200b").replace("&", "&\u200b");
    }
}
