// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test projections using reference data. (Currently provided by proj.4)
 * 
 * The data file data_nodist/projection-reference-data.csv can be created like this:
 *      Fist run this file's main method to collect epsg codes and bounds data.
 *      Then pipe the result into test/generate-proj-data.pl.
 */
public class ProjectionRefTest {

    /**
     * create a list of epsg codes and bounds to be used by
     * @param args
     */
    public static void main(String[] args) {
        HashMap<String, Projection> allCodes = new LinkedHashMap<String, Projection>();
        List<Projection> projs = Projections.getProjections();
        for (Projection p: projs) {
            if (p instanceof ProjectionSubPrefs) {
                for (String code : ((ProjectionSubPrefs)p).allCodes()) {
                    ProjectionSubPrefs projSub = recreateProj((ProjectionSubPrefs)p);
                    Collection<String> prefs = projSub.getPreferencesFromCode(code);
                    projSub.setPreferences(prefs);
                    allCodes.put(code, projSub);
                }
            } else {
                allCodes.put(p.toCode(), p);
            }
        }
        for (Entry<String, Projection> e : allCodes.entrySet()) {
            System.out.println(String.format("%s %s", e.getKey(), e.getValue().getWorldBoundsLatLon()));
        }
    }

    private static ProjectionSubPrefs recreateProj(ProjectionSubPrefs proj) {
        try {
            return proj.getClass().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    tr("Cannot instantiate projection ''{0}''", proj.getClass().toString()), e);
        }
    }

    @Test
    public void test() throws IOException, FileNotFoundException {
        BufferedReader in = new BufferedReader(new FileReader("data_nodist/projection-reference-data.csv"));
        StringBuilder fail = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] f = line.split(",");
            String code = f[0];
            double lat = Double.parseDouble(f[1]);
            double lon = Double.parseDouble(f[2]);
            double east = Double.parseDouble(f[3]);
            double north = Double.parseDouble(f[4]);
            Projection p = ProjectionInfo.getProjectionByCode(code);
            {
                EastNorth en = p.latlon2eastNorth(new LatLon(lat, lon));
                String error = String.format("%s (%s): Projecting latlon(%s,%s):\n" +
                        "        expected: eastnorth(%s,%s),\n" +
                        "        but got:  eastnorth(%s,%s)!\n",
                        p.toString(), code, lat, lon, east, north, en.east(), en.north());
                double EPSILON = 1e-3; // 1 mm accuracy
                if (Math.abs(east - en.east()) > EPSILON || Math.abs(north - en.north()) > EPSILON) {
                    fail.append(error);
                }
            }
            {
                LatLon ll = p.eastNorth2latlon(new EastNorth(east, north));
                String error = String.format("%s (%s): Inverse projecting eastnorth(%s,%s):\n" +
                        "        expected: latlon(%s,%s),\n" +
                        "        but got:  latlon(%s,%s)!\n",
                        p.toString(), code, east, north, lat, lon, ll.lat(), ll.lon());
                double EPSILON = Math.toDegrees(1e-3 / 6378137); // 1 mm accuracy (or better)
                if (Math.abs(lat - ll.lat()) > EPSILON || Math.abs(lon - ll.lon()) > EPSILON) {
                    if (!("yes".equals(System.getProperty("suppressPermanentFailure")) && code.equals("EPSG:21781"))) {
                        fail.append(error);
                    }
                }
            }
        }
        Utils.close(in);
        if (fail.length() > 0) {
            System.err.println(fail.toString());
            throw new AssertionError(fail.toString());
        }

    }
}