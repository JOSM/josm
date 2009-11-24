// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.util.HashMap;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

public class OsmUrlToBounds {

    public static Bounds parse(String url) {
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

        Bounds b = null;
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
                double size = 180.0 / Math.pow(2, Integer.parseInt(map.get("zoom")));
                b = new Bounds(
                    new LatLon(parseDouble(map, "lat") - size/2, parseDouble(map, "lon") - size),
                    new LatLon(parseDouble(map, "lat") + size/2, parseDouble(map, "lon") + size));
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

    static public int getZoom(Bounds b) {
        // convert to mercator (for calculation of zoom only)
        double latMin = Math.log(Math.tan(Math.PI/4.0+b.getMin().lat()/180.0*Math.PI/2.0))*180.0/Math.PI;
        double latMax = Math.log(Math.tan(Math.PI/4.0+b.getMax().lat()/180.0*Math.PI/2.0))*180.0/Math.PI;
        double size = Math.max(Math.abs(latMax-latMin), Math.abs(b.getMax().lon()-b.getMin().lon()));
        int zoom = 0;
        while (zoom <= 20) {
            if (size >= 180)
                break;
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
        return new String("http://www.openstreetmap.org/?lat="+lat+"&lon="+lon+"&zoom="+zoom);
    }
}
