// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.Bounds;

/**
 * Parses a Geo URL (as specified in <a href="https://tools.ietf.org/html/rfc5870">RFC 5870</a>) into {@link Bounds}.
 *
 * Note that Geo URLs are also handled by {@link OsmUrlToBounds}.
 */
public final class GeoUrlToBounds {

    /**
     * The pattern of a geo: url, having named match groups.
     */
    public static final Pattern PATTERN = Pattern.compile("geo:(?<lat>[+-]?[0-9.]+),(?<lon>[+-]?[0-9.]+)(\\?z=(?<zoom>[0-9]+))?");

    private GeoUrlToBounds() {
        // Hide default constructor for utils classes
    }

    /**
     * Parses a Geo URL (as specified in <a href="https://tools.ietf.org/html/rfc5870">RFC 5870</a>) into {@link Bounds}.
     * @param url the URL to be parsed
     * @return the parsed {@link Bounds}
     */
    public static Bounds parse(final String url) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        final Matcher m = PATTERN.matcher(url);
        if (m.matches()) {
            final double lat;
            final double lon;
            final int zoom;
            try {
                lat = Double.parseDouble(m.group("lat"));
            } catch (NumberFormatException e) {
                Logging.warn(tr("URL does not contain valid {0}", tr("latitude")), e);
                return null;
            }
            try {
                lon = Double.parseDouble(m.group("lon"));
            } catch (NumberFormatException e) {
                Logging.warn(tr("URL does not contain valid {0}", tr("longitude")), e);
                return null;
            }
            try {
                zoom = m.group("zoom") != null ? Integer.parseInt(m.group("zoom")) : 18;
            } catch (NumberFormatException e) {
                Logging.warn(tr("URL does not contain valid {0}", tr("zoom")), e);
                return null;
            }
            return OsmUrlToBounds.positionToBounds(lat, lon, zoom);
        } else {
            return null;
        }
    }
}
