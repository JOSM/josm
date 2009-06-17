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
}
