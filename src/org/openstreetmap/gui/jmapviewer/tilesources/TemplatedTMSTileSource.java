// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;

/**
 * Handles templated TMS Tile Source. Templated means, that some patterns within
 * URL gets substituted.
 *
 * Supported parameters
 * {zoom} - substituted with zoom level
 * {z} - as above
 * {NUMBER-zoom} - substituted with result of equation "NUMBER - zoom",
 *                  eg. {20-zoom} for zoom level 15 will result in 5 in this place
 * {zoom+number} - substituted with result of equation "zoom + number",
 *                 eg. {zoom+5} for zoom level 15 will result in 20.
 * {x} - substituted with X tile number
 * {y} - substituted with Y tile number
 * {!y} - substituted with Yahoo Y tile number
 * {-y} - substituted with reversed Y tile number
 * {switch:VAL_A,VAL_B,VAL_C,...} - substituted with one of VAL_A, VAL_B, VAL_C. Usually
 *                                  used to specify many tile servers
 * {header:(HEADER_NAME,HEADER_VALUE)} - sets the headers to be sent to tile server
 */
public class TemplatedTMSTileSource extends TMSTileSource implements TemplatedTileSource {

    private Random rand;
    private String[] randomParts;
    private final Map<String, String> headers = new HashMap<>();
    private boolean inverse_zoom = false;
    private int zoom_offset = 0;

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final String COOKIE_HEADER   = "Cookie";
    private static final Pattern PATTERN_ZOOM    = Pattern.compile("\\{(?:(\\d+)-)?z(?:oom)?([+-]\\d+)?\\}");
    private static final Pattern PATTERN_X       = Pattern.compile("\\{x\\}");
    private static final Pattern PATTERN_Y       = Pattern.compile("\\{y\\}");
    private static final Pattern PATTERN_Y_YAHOO = Pattern.compile("\\{!y\\}");
    private static final Pattern PATTERN_NEG_Y   = Pattern.compile("\\{-y\\}");
    private static final Pattern PATTERN_SWITCH  = Pattern.compile("\\{switch:([^}]+)\\}");
    private static final Pattern PATTERN_HEADER  = Pattern.compile("\\{header\\(([^,]+),([^}]+)\\)\\}");
    private static final Pattern PATTERN_PARAM  = Pattern.compile("\\{((?:\\d+-)?z(?:oom)?(:?[+-]\\d+)?|x|y|!y|-y|switch:([^}]+))\\}");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final Pattern[] ALL_PATTERNS = {
        PATTERN_HEADER, PATTERN_ZOOM, PATTERN_X, PATTERN_Y, PATTERN_Y_YAHOO, PATTERN_NEG_Y, PATTERN_SWITCH
    };

    /**
     * Creates Templated TMS Tile Source based on ImageryInfo
     * @param info imagery info
     */
    public TemplatedTMSTileSource(TileSourceInfo info) {
        super(info);
        String cookies = info.getCookies();
        if (cookies != null && !cookies.isEmpty()) {
            headers.put(COOKIE_HEADER, cookies);
        }
        handleTemplate();
    }

    private void handleTemplate() {
        // Capturing group pattern on switch values
        Matcher m = PATTERN_SWITCH.matcher(baseUrl);
        if (m.find()) {
            rand = new Random();
            randomParts = m.group(1).split(",");
        }
        StringBuffer output = new StringBuffer();
        Matcher matcher = PATTERN_HEADER.matcher(baseUrl);
        while (matcher.find()) {
            headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        baseUrl = output.toString();
        m = PATTERN_ZOOM.matcher(this.baseUrl);
        if (m.find()) {
            if (m.group(1) != null) {
                inverse_zoom = true;
                zoom_offset = Integer.parseInt(m.group(1));
            }
            if (m.group(2) != null) {
                String ofs = m.group(2);
                if (ofs.startsWith("+"))
                    ofs = ofs.substring(1);
                zoom_offset += Integer.parseInt(ofs);
            }
        }

    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        StringBuffer url = new StringBuffer(baseUrl.length());
        Matcher matcher = PATTERN_PARAM.matcher(baseUrl);
        while (matcher.find()) {
            String replacement = "replace";
            switch (matcher.group(1)) {
            case "z": // PATTERN_ZOOM
            case "zoom":
                replacement = Integer.toString((inverse_zoom ? -1 * zoom : zoom) + zoom_offset);
                break;
            case "x": // PATTERN_X
                replacement = Integer.toString(tilex);
                break;
            case "y": // PATTERN_Y
                replacement = Integer.toString(tiley);
                break;
            case "!y": // PATTERN_Y_YAHOO
                replacement = Integer.toString((int) Math.pow(2, zoom-1)-1-tiley);
                break;
            case "-y": // PATTERN_NEG_Y
                replacement = Integer.toString((int) Math.pow(2, zoom)-1-tiley);
                break;
            case "switch:":
                replacement = randomParts[rand.nextInt(randomParts.length)];
                break;
            default:
                // handle switch/zoom here, as group will contain parameters and switch will not work
                if (PATTERN_ZOOM.matcher("{" + matcher.group(1) + "}").matches()) {
                    replacement = Integer.toString((inverse_zoom ? -1 * zoom : zoom) + zoom_offset);
                } else if (PATTERN_SWITCH.matcher("{" + matcher.group(1) + "}").matches()) {
                    replacement = randomParts[rand.nextInt(randomParts.length)];
                } else {
                    replacement = '{' + matcher.group(1) + '}';
                }
            }
            matcher.appendReplacement(url, replacement);
        }
        matcher.appendTail(url);
        return url.toString().replace(" ", "%20");
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * @param url URL to check
     */
    public static void checkUrl(String url) {
        assert url != null && !"".equals(url) : "URL cannot be null or empty";
        Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (Pattern pattern : ALL_PATTERNS) {
                if (pattern.matcher(m.group()).matches()) {
                    isSupportedPattern = true;
                    break;
                }
            }
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(
                        m.group() + " is not a valid TMS argument. Please check this server URL:\n" + url);
            }
        }
    }
}
