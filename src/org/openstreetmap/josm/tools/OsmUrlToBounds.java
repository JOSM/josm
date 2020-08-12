// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Parses various URL used in OpenStreetMap projects into {@link Bounds}.
 */
public final class OsmUrlToBounds {
    private static final String SHORTLINK_PREFIX = "http://osm.org/go/";

    private static volatile Supplier<Dimension> mapSize = () -> new Dimension(800, 600);

    private OsmUrlToBounds() {
        // Hide default constructor for utils classes
    }

    /**
     * Parses an URL into {@link Bounds}
     * @param url the URL to be parsed
     * @return the parsed {@link Bounds}, or {@code null}
     */
    public static Bounds parse(String url) {
        if (url.startsWith("geo:")) {
            return GeoUrlToBounds.parse(url);
        }
        try {
            // a percent sign indicates an encoded URL (RFC 1738).
            if (url.contains("%")) {
                url = Utils.decodeUrl(url);
            }
        } catch (IllegalArgumentException ex) {
            Logging.error(ex);
        }
        Bounds b = parseShortLink(url);
        if (b != null)
            return b;
        if (url.contains("#map") || url.contains("/#")) {
            // probably it's a URL following the new scheme?
            return parseHashURLs(url);
        }
        final int i = url.indexOf('?');
        if (i == -1) {
            return null;
        }
        String[] args = url.substring(i+1).split("&", -1);
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq != -1) {
                map.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }

        try {
            if (map.containsKey("bbox")) {
                String[] bbox = map.get("bbox").split(",", -1);
                b = new Bounds(
                        Double.parseDouble(bbox[1]), Double.parseDouble(bbox[0]),
                        Double.parseDouble(bbox[3]), Double.parseDouble(bbox[2]));
            } else if (map.containsKey("minlat")) {
                double minlat = Double.parseDouble(map.get("minlat"));
                double minlon = Double.parseDouble(map.get("minlon"));
                double maxlat = Double.parseDouble(map.get("maxlat"));
                double maxlon = Double.parseDouble(map.get("maxlon"));
                b = new Bounds(minlat, minlon, maxlat, maxlon);
            } else {
                String z = map.get("zoom");
                b = positionToBounds(parseDouble(map, "lat"), parseDouble(map, "lon"),
                        z == null ? 18 : Integer.parseInt(z));
            }
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ex) {
            Logging.log(Logging.LEVEL_ERROR, url, ex);
        }
        return b;
    }

    /**
     * Openstreetmap.org changed it's URL scheme in August 2013, which breaks the URL parsing.
     * The following function, called by the old parse function if necessary, provides parsing new URLs
     * the new URLs follow the scheme https://www.openstreetmap.org/#map=18/51.71873/8.76164&amp;layers=CN
     * @param url string for parsing
     * @return Bounds if hashurl, {@code null} otherwise
     */
    private static Bounds parseHashURLs(String url) {
        int startIndex = url.indexOf('#');
        if (startIndex == -1) return null;
        int endIndex = url.indexOf('&', startIndex);
        if (endIndex == -1) endIndex = url.length();
        String coordPart = url.substring(startIndex+(url.contains("#map=") ? "#map=".length() : "#".length()), endIndex);
        String[] parts = coordPart.split("/", -1);
        if (parts.length < 3) {
            Logging.warn(tr("URL does not contain {0}/{1}/{2}", tr("zoom"), tr("latitude"), tr("longitude")));
            return null;
        }
        int zoom;
        try {
            zoom = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            Logging.warn(tr("URL does not contain valid {0}", tr("zoom")), e);
            return null;
        }
        double lat, lon;
        try {
            lat = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            Logging.warn(tr("URL does not contain valid {0}", tr("latitude")), e);
            return null;
        }
        try {
            lon = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            Logging.warn(tr("URL does not contain valid {0}", tr("longitude")), e);
            return null;
        }
        return positionToBounds(lat, lon, zoom);
    }

    private static double parseDouble(Map<String, String> map, String key) {
        if (map.containsKey(key))
            return Double.parseDouble(map.get(key));
        if (map.containsKey('m'+key))
            return Double.parseDouble(map.get('m'+key));
        throw new IllegalArgumentException(map.toString() + " does not contain " + key);
    }

    private static final char[] SHORTLINK_CHARS = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '_', '@'
    };

    /**
     * Parse OSM short link
     *
     * @param url string for parsing
     * @return Bounds if shortlink, null otherwise
     * @see <a href="https://github.com/openstreetmap/openstreetmap-website/blob/master/lib/short_link.rb">short_link.rb</a>
     */
    private static Bounds parseShortLink(final String url) {
        if (!url.startsWith(SHORTLINK_PREFIX))
            return null;
        final String shortLink = url.substring(SHORTLINK_PREFIX.length());

        final Map<Character, Integer> array = new HashMap<>();

        for (int i = 0; i < SHORTLINK_CHARS.length; ++i) {
            array.put(SHORTLINK_CHARS[i], i);
        }

        // long is necessary (need 32 bit positive value is needed)
        long x = 0;
        long y = 0;
        int zoom = 0;
        int zoomOffset = 0;

        for (final char ch : shortLink.toCharArray()) {
            if (array.containsKey(ch)) {
                int val = array.get(ch);
                for (int i = 0; i < 3; ++i) {
                    x <<= 1;
                    if ((val & 32) != 0) {
                        x |= 1;
                    }
                    val <<= 1;

                    y <<= 1;
                    if ((val & 32) != 0) {
                        y |= 1;
                    }
                    val <<= 1;
                }
                zoom += 3;
            } else {
                zoomOffset--;
            }
        }

        x <<= 32 - zoom;
        y <<= 32 - zoom;

        // 2**32 == 4294967296
        return positionToBounds(y * 180.0 / 4294967296.0 - 90.0,
                x * 360.0 / 4294967296.0 - 180.0,
                // TODO: -2 was not in ruby code
                zoom - 8 - (zoomOffset % 3) - 2);
    }

    /**
     * Sets the map size supplier.
     * @param mapSizeSupplier returns the map size in pixels
     * @since 12796
     */
    public static void setMapSizeSupplier(Supplier<Dimension> mapSizeSupplier) {
        mapSize = Objects.requireNonNull(mapSizeSupplier, "mapSizeSupplier");
    }

    private static final int TILE_SIZE_IN_PIXELS = 256;

    /**
     * Compute the bounds for a given lat/lon position and the zoom level
     * @param lat The latitude
     * @param lon The longitude
     * @param zoom The current zoom level
     * @return The bounds the OSM server would display
     */
    public static Bounds positionToBounds(final double lat, final double lon, final int zoom) {
        final Dimension screenSize = mapSize.get();
        double scale = (1L << zoom) * TILE_SIZE_IN_PIXELS / (2.0 * Math.PI * Ellipsoid.WGS84.a);
        double deltaX = screenSize.getWidth() / 2.0 / scale;
        double deltaY = screenSize.getHeight() / 2.0 / scale;
        final Projection mercator = Projections.getProjectionByCode("EPSG:3857");
        final EastNorth projected = mercator.latlon2eastNorth(new LatLon(lat, lon));
        return new Bounds(
                mercator.eastNorth2latlon(projected.add(-deltaX, -deltaY)),
                mercator.eastNorth2latlon(projected.add(deltaX, deltaY)));
    }

    /**
     * Return OSM Zoom level for a given area
     *
     * @param b bounds of the area
     * @return matching zoom level for area
     */
    public static int getZoom(Bounds b) {
        final Projection mercator = Projections.getProjectionByCode("EPSG:3857");
        final EastNorth min = mercator.latlon2eastNorth(b.getMin());
        final EastNorth max = mercator.latlon2eastNorth(b.getMax());
        final double deltaX = max.getX() - min.getX();
        final double scale = mapSize.get().getWidth() / deltaX;
        final double x = scale * (2 * Math.PI * Ellipsoid.WGS84.a) / TILE_SIZE_IN_PIXELS;
        return (int) Math.round(Math.log(x) / Math.log(2));
    }

    /**
     * Return OSM URL for given area.
     *
     * @param b bounds of the area
     * @return link to display that area in OSM map
     */
    public static String getURL(Bounds b) {
        return getURL(b.getCenter(), getZoom(b));
    }

    /**
     * Return OSM URL for given position and zoom.
     *
     * @param pos center position of area
     * @param zoom zoom depth of display
     * @return link to display that area in OSM map
     */
    public static String getURL(LatLon pos, int zoom) {
        return getURL(pos.lat(), pos.lon(), zoom);
    }

    /**
     * Return OSM URL for given lat/lon and zoom.
     *
     * @param dlat center latitude of area
     * @param dlon center longitude of area
     * @param zoom zoom depth of display
     * @return link to display that area in OSM map
     *
     * @since 6453
     */
    public static String getURL(double dlat, double dlon, int zoom) {
        // Truncate lat and lon to something more sensible
        int decimals = (int) Math.pow(10, zoom / 3d);
        double lat = Math.round(dlat * decimals);
        lat /= decimals;
        double lon = Math.round(dlon * decimals);
        lon /= decimals;
        return Config.getUrls().getOSMWebsite() + "/#map="+zoom+'/'+lat+'/'+lon;
    }
}
