// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

public class OsmUrlToBounds {
    private static final String SHORTLINK_PREFIX = "http://osm.org/go/";

    public static Bounds parse(String url) {
        Bounds b = parseShortLink(url);
        if (b != null)
            return b;
        int i = url.indexOf('?');
        if (i == -1)
            return null;
        String[] args = url.substring(i+1).split("&");
        HashMap<String, String> map = new HashMap<String, String>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq != -1) {
                map.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }

        try {
            if (map.containsKey("bbox")) {
                String bbox[] = map.get("bbox").split(",");
                b = new Bounds(
                        new LatLon(Double.parseDouble(bbox[1]), Double.parseDouble(bbox[0])),
                        new LatLon(Double.parseDouble(bbox[3]), Double.parseDouble(bbox[2])));
            } else if (map.containsKey("minlat")) {
                String s = map.get("minlat");
                Double minlat = Double.parseDouble(s);
                s = map.get("minlon");
                Double minlon = Double.parseDouble(s);
                s = map.get("maxlat");
                Double maxlat = Double.parseDouble(s);
                s = map.get("maxlon");
                Double maxlon = Double.parseDouble(s);
                b = new Bounds(new LatLon(minlat, minlon), new LatLon(maxlat, maxlon));
            } else {
                b = positionToBounds(parseDouble(map, "lat"),
                        parseDouble(map, "lon"),
                        Integer.parseInt(map.get("zoom")));
            }
        } catch (NumberFormatException x) {
        } catch (NullPointerException x) {
        }
        return b;
    }

    private static double parseDouble(HashMap<String, String> map, String key) {
        if (map.containsKey(key))
            return Double.parseDouble(map.get(key));
        return Double.parseDouble(map.get("m"+key));
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
     * p
     *
     * @param url string for parsing
     *
     * @return Bounds if shortlink, null otherwise
     *
     * @see http://trac.openstreetmap.org/browser/sites/rails_port/lib/short_link.rb
     */
    private static Bounds parseShortLink(final String url) {
        if (!url.startsWith(SHORTLINK_PREFIX))
            return null;
        final String shortLink = url.substring(SHORTLINK_PREFIX.length());

        final Map<Character, Integer> array = new HashMap<Character, Integer>();

        for (int i=0; i<SHORTLINK_CHARS.length; ++i) {
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
                for (int i=0; i<3; ++i) {
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

    public static Bounds positionToBounds(final double lat, final double lon, final int zoom) {
        final double size = 180.0 / Math.pow(2, zoom);
        return new Bounds(
                new LatLon(lat - size/2, lon - size),
                new LatLon(lat + size/2, lon + size));
    }

    static public int getZoom(Bounds b) {
        // convert to mercator (for calculation of zoom only)
        double latMin = Math.log(Math.tan(Math.PI/4.0+b.getMin().lat()/180.0*Math.PI/2.0))*180.0/Math.PI;
        double latMax = Math.log(Math.tan(Math.PI/4.0+b.getMax().lat()/180.0*Math.PI/2.0))*180.0/Math.PI;
        double size = Math.max(Math.abs(latMax-latMin), Math.abs(b.getMax().lon()-b.getMin().lon()));
        int zoom = 0;
        while (zoom <= 20) {
            if (size >= 180) {
                break;
            }
            size *= 2;
            zoom++;
        }
        return zoom;
    }

    static public String getURL(Bounds b) {
        return getURL(b.getCenter(), getZoom(b));
    }

    static public String getURL(LatLon pos, int zoom) {
        // Truncate lat and lon to something more sensible
        int decimals = (int) Math.pow(10, (zoom / 3));
        double lat = (Math.round(pos.lat() * decimals));
        lat /= decimals;
        double lon = (Math.round(pos.lon() * decimals));
        lon /= decimals;
        return "http://www.openstreetmap.org/?lat="+lat+"&lon="+lon+"&zoom="+zoom;
    }
}
