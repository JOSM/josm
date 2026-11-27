// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Text/String utils.
 * @since 13978
 */
public final class TextUtils {

    /** What private information is replaced with */
    private static final String STRIPPED = "...stripped...";
    /**
     * The parameters which are private: API keys and tokens, and OAuth 2.0 client secrets, authorization codes and
     * their verifiers, and passwords. {@code code} is only matched as a whole name, so as not to strip e.g. a postcode.
     * A value ends at white space too, so that what follows a URL in a log line is kept.
     */
    private static final Pattern PRIVATE_PARAMETERS =
            Pattern.compile("(token|key|connectId|secret|password|(?<![\\w-])code|verifier)=[^&\\s]+");

    private TextUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Inserts zero width space character (U+8203) after each slash/ampersand to wrap long URLs.
     * @param url URL
     * @return wrapped URL
     * @since 13978
     */
    public static String wrapLongUrl(String url) {
        return url.replace("/", "/\u200b").replace("&", "&\u200b");
    }

    /**
     * Remove privacy related parts from output URL.
     * <p>
     * This works for a form-encoded ({@code application/x-www-form-urlencoded}) request body as well, which has the
     * syntax of a query, such as an OAuth 2.0 token request with its client secret.
     * @param url Unmodified URL
     * @return Stripped URL (privacy related issues removed)
     * @since 18652
     */
    public static String stripUrl(String url) {
        return PRIVATE_PARAMETERS.matcher(url).replaceAll("$1=" + STRIPPED);
    }

    /**
     * Remove privacy related values from output HTTP headers: those of {@code Authorization} and
     * {@code Proxy-Authorization}, but for their scheme, so that the output still tells which kind of authentication
     * was used, and those of {@code Cookie} and {@code Set-Cookie}.
     * @param headers Unmodified headers, with a single value or a list of values for each
     * @return Stripped copy of the headers, in the same order (privacy related issues removed)
     * @since 19711
     */
    public static Map<String, Object> stripHeaders(Map<String, ?> headers) {
        final Map<String, Object> stripped = new LinkedHashMap<>(headers);
        stripped.replaceAll((key, value) -> {
            final String name = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if ("authorization".equals(name) || "proxy-authorization".equals(name)) {
                return stripHeaderValue(value, true);
            } else if ("cookie".equals(name) || "set-cookie".equals(name)) {
                return stripHeaderValue(value, false);
            }
            return value;
        });
        return stripped;
    }

    private static Object stripHeaderValue(Object value, boolean keepScheme) {
        if (value instanceof List) {
            return ((List<?>) value).stream().map(v -> stripHeaderValue(v, keepScheme)).collect(Collectors.toList());
        }
        final String text = String.valueOf(value);
        final int space = text.indexOf(' ');
        return (keepScheme && space > 0 ? text.substring(0, space + 1) : "") + STRIPPED;
    }

}
