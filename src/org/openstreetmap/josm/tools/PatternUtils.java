// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.engine.ElementAttributes;
import org.apache.commons.jcs3.engine.behavior.IElementAttributes;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A class that caches compiled patterns.
 * @author Taylor Smock
 * @since 18475
 */
public final class PatternUtils {
    /** A string that is highly unlikely to appear in regexes to split a regex from its flags */
    private static final String MAGIC_STRING = "========";
    /** A cache for Java Patterns (no flags) */
    private static final CacheAccess<String, Pattern> cache = JCSCacheManager.getCache("java:pattern",
            Config.getPref().getInt("java.pattern.cache", 1024), 0, null);

    static {
        // We don't want to keep these around forever, so set a reasonablish max idle life.
        final IElementAttributes defaultAttributes = Optional.ofNullable(cache.getDefaultElementAttributes()).orElseGet(
                ElementAttributes::new);
        defaultAttributes.setIdleTime(TimeUnit.HOURS.toSeconds(1));
        cache.setDefaultElementAttributes(defaultAttributes);
    }

    private PatternUtils() {
        // Hide the constructor
    }

    /**
     * Compile a regex into a pattern. This may return a {@link Pattern} used elsewhere. This is safe.
     * @param regex The regex to compile
     * @return The immutable {@link Pattern}.
     * @see Pattern#compile(String)
     */
    public static Pattern compile(final String regex) {
        return compile(regex, 0);
    }

    /**
     * Compile a regex into a pattern. This may return a {@link Pattern} used elsewhere. This is safe.
     * @param regex The regex to compile
     * @param flags The flags from {@link Pattern} to apply
     * @return The immutable {@link Pattern}.
     * @see Pattern#compile(String, int)
     */
    public static Pattern compile(String regex, int flags) {
        // Right now, the maximum value of flags is 511 (3 characters). This should avoid unnecessary array copying.
        final StringBuilder sb = new StringBuilder(3 + MAGIC_STRING.length() + regex.length());
        return cache.get(sb.append(flags).append(MAGIC_STRING).append(regex).toString(), () -> Pattern.compile(regex, flags));
    }
}
