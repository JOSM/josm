// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.gui.jmapviewer.FeatureAdapter;

/**
 * Patterns that can be replaced in imagery URLs.
 * @since 17578
 */
public final class ImageryPatterns {

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    static final Pattern PATTERN_HEADER = Pattern.compile("\\{header\\(([^,]+),([^}]+)\\)\\}");
    static final Pattern PATTERN_PROJ   = Pattern.compile("\\{proj\\}");
    static final Pattern PATTERN_WKID   = Pattern.compile("\\{wkid\\}");
    static final Pattern PATTERN_BBOX   = Pattern.compile("\\{bbox\\}");
    static final Pattern PATTERN_W      = Pattern.compile("\\{w\\}");
    static final Pattern PATTERN_S      = Pattern.compile("\\{s\\}");
    static final Pattern PATTERN_E      = Pattern.compile("\\{e\\}");
    static final Pattern PATTERN_N      = Pattern.compile("\\{n\\}");
    static final Pattern PATTERN_WIDTH  = Pattern.compile("\\{width\\}");
    static final Pattern PATTERN_HEIGHT = Pattern.compile("\\{height\\}");
    static final Pattern PATTERN_TIME   = Pattern.compile("\\{time\\}"); // Sentinel-2
    static final Pattern PATTERN_PARAM  = Pattern.compile("\\{([^}]+)\\}");
    /**
     * The api key pattern is used to allow us to quickly switch apikeys. This is functionally the same as the pattern
     * in {@link org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource}.
     */
    static final Pattern PATTERN_API_KEY = Pattern.compile("\\{apikey}");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final Pattern[] ALL_WMS_PATTERNS = {
            PATTERN_HEADER, PATTERN_PROJ, PATTERN_WKID, PATTERN_BBOX,
            PATTERN_W, PATTERN_S, PATTERN_E, PATTERN_N,
            PATTERN_WIDTH, PATTERN_HEIGHT, PATTERN_TIME,
            PATTERN_API_KEY
    };

    private static final Pattern[] ALL_WMTS_PATTERNS = {
            PATTERN_HEADER, PATTERN_API_KEY
    };

    private ImageryPatterns() {
        // Hide public constructor
    }

    private static void checkUrlPatterns(String url, Pattern[] allPatterns, String errMessage) {
        Matcher m = PATTERN_PARAM.matcher(Objects.requireNonNull(url, "url"));
        while (m.find()) {
            if (Arrays.stream(allPatterns).noneMatch(pattern -> pattern.matcher(m.group()).matches())) {
                throw new IllegalArgumentException(tr(errMessage, m.group(), url));
            }
        }
    }

    static void checkWmsUrlPatterns(String url) {
        checkUrlPatterns(url, ALL_WMS_PATTERNS,
                marktr("{0} is not a valid WMS argument. Please check this server URL:\n{1}"));
    }

    static void checkWmtsUrlPatterns(String url) {
        checkUrlPatterns(url, ALL_WMTS_PATTERNS,
                marktr("{0} is not a valid WMTS argument. Please check this server URL:\n{1}"));
    }

    static String handleHeaderTemplate(String url, Map<String, String> headers) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = PATTERN_HEADER.matcher(url);
        while (matcher.find()) {
            headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        return output.toString();
    }

    /**
     * Handle the {@link #PATTERN_API_KEY} replacement
     * @param id The id of the info
     * @param url The templated url
     * @return The templated url with {@link #PATTERN_API_KEY} replaced
     */
    static String handleApiKeyTemplate(final String id, final String url) {
        if (id != null && url != null) {
            final Matcher matcher = PATTERN_API_KEY.matcher(url);
            if (matcher.matches()) {
                try {
                    final String apiKey = FeatureAdapter.retrieveApiKey(id);
                    return matcher.replaceAll(apiKey);
                } catch (IOException | NullPointerException e) {
                    // Match rough behavior in JMapViewer TemplatedTMSTileSource, but with better error message.
                    throw new IllegalArgumentException(tr("Could not retrieve API key for imagery with id={0}. Cannot add layer.", id), e);
                }
            }
        }
        return url;
    }
}
