// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.SwissObliqueMercator;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;

/**
 * Class to handle projections
 *
 */
public class Projections {
    /**
     * List of all available projections.
     */
    private static ArrayList<Projection> allProjections =
        new ArrayList<Projection>(Arrays.asList(new Projection[] {
                // global projections
                new Epsg4326(),
                new Mercator(),
                new UTM(),
                // regional - alphabetical order by country code
                new BelgianLambert1972(),   // BE
                new BelgianLambert2008(),   // BE
                new SwissGrid(),            // CH
                new GaussKrueger(),         // DE
                new LambertEST(),           // EE
                new Lambert(),              // FR
                new Lambert93(),            // FR
                new LambertCC9Zones(),      // FR
                new UTM_France_DOM(),       // FR
                new TransverseMercatorLV(), // LV
                new Puwg(),                 // PL
                new Epsg3008(),             // SE
        }));

    static {
        if (Main.pref.getBoolean("customprojection")) {
            addProjection(new CustomProjectionPrefGui());
        }
    }

    public static ArrayList<Projection> getProjections() {
        return allProjections;
    }

    /**
     * Adds a new projection to the list of known projections.
     *
     * For Plugins authors: make sure your plugin is an early plugin, i.e. put
     * Plugin-Early=true in your Manifest.
     */
    public static void addProjection(Projection proj) {
        allProjections.add(proj);
    }

    public static EastNorth project(LatLon ll) {
        if (ll == null) return null;
        return Main.getProjection().latlon2eastNorth(ll);
    }

    public static LatLon inverseProject(EastNorth en) {
        if (en == null) return null;
        return Main.getProjection().eastNorth2latlon(en);
    }

    /*********************************
     * Registry for custom projection
     *
     * should be compatible to PROJ.4
     */

    private static Map<String, Ellipsoid> ellipsoids = new HashMap<String, Ellipsoid>();
    private static Map<String, Class<? extends Proj>> projs = new HashMap<String, Class<? extends Proj>>();
    private static Map<String, Datum> datums = new HashMap<String, Datum>();
    private static Map<String, NTV2GridShiftFileWrapper> nadgrids = new HashMap<String, NTV2GridShiftFileWrapper>();

    static {
        ellipsoids.put("intl", Ellipsoid.hayford);
        ellipsoids.put("GRS80", Ellipsoid.GRS80);
        ellipsoids.put("WGS84", Ellipsoid.WGS84);
        ellipsoids.put("bessel", Ellipsoid.Bessel1841);

        projs.put("merc", org.openstreetmap.josm.data.projection.proj.Mercator.class);
        projs.put("lcc", LambertConformalConic.class);
        projs.put("somerc", SwissObliqueMercator.class);
        projs.put("tmerc", TransverseMercator.class);

        datums.put("WGS84", WGS84Datum.INSTANCE);

        nadgrids.put("BETA2007.gsb", NTV2GridShiftFileWrapper.BETA2007);
        nadgrids.put("ntf_r93_b.gsb", NTV2GridShiftFileWrapper.ntf_rgf93);
    }

    public static Ellipsoid getEllipsoid(String id) {
        return ellipsoids.get(id);
    }

    public static Proj getProjection(String id) {
        Class<? extends Proj> projClass = projs.get(id);
        if (projClass == null) return null;
        Proj proj = null;
        try {
            proj = projClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return proj;
    }

    public static Datum getDatum(String id) {
        return datums.get(id);
    }

    public static NTV2GridShiftFileWrapper getNadgrids(String id) {
        return nadgrids.get(id);
    }
}
