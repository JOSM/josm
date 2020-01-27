// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Download URL pattern.
 * @since 15784
 */
public interface UrlPattern {
    /**
     * Returns the URL pattern.
     * @return the URL pattern
     */
    String pattern();

    /**
     * Creates a matcher that will match the given input against this pattern.
     * @param input The character sequence to be matched
     * @return A new matcher for this pattern
     */
    default Matcher matcher(String input) {
        return Pattern.compile(pattern()).matcher(input);
    }

    /**
     * Attempts to match the given input against the pattern.
     * @param input The character sequence to be matched
     * @return {@code true} if the given input matches this pattern
     */
    default boolean matches(String input) {
        return input != null && matcher(input).matches();
    }

    /**
     * Attempts to match the given URL external form against the pattern.
     * @param url URL to be matched
     * @return {@code true} if the given URL matches this pattern
     */
    default boolean matches(URL url) {
        return url != null && matches(url.toExternalForm());
    }
}
