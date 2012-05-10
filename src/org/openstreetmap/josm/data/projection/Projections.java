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
import org.openstreetmap.josm.data.projection.proj.ClassProjFactory;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.LonLat;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjFactory;
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
                new CustomProjectionPrefGui()
        }));

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
    public static Map<String, Ellipsoid> ellipsoids = new HashMap<String, Ellipsoid>();
    public static Map<String, ProjFactory> projs = new HashMap<String, ProjFactory>();
    public static Map<String, Datum> datums = new HashMap<String, Datum>();
    public static Map<String, NTV2GridShiftFileWrapper> nadgrids = new HashMap<String, NTV2GridShiftFileWrapper>();

    static {
        registerBaseProjection("lonlat", LonLat.class, "core");
        registerBaseProjection("merc", org.openstreetmap.josm.data.projection.proj.Mercator.class, "core");
        registerBaseProjection("lcc", LambertConformalConic.class, "core");
        registerBaseProjection("somerc", SwissObliqueMercator.class, "core");
        registerBaseProjection("tmerc", TransverseMercator.class, "core");

        ellipsoids.put("intl", Ellipsoid.hayford);
        ellipsoids.put("GRS80", Ellipsoid.GRS80);
        ellipsoids.put("WGS84", Ellipsoid.WGS84);
        ellipsoids.put("bessel", Ellipsoid.Bessel1841);

        datums.put("WGS84", WGS84Datum.INSTANCE);

        nadgrids.put("BETA2007.gsb", NTV2GridShiftFileWrapper.BETA2007);
        nadgrids.put("ntf_r93_b.gsb", NTV2GridShiftFileWrapper.ntf_rgf93);
    }

    /**
     * Plugins can register additional base projections.
     *
     * @param id The "official" PROJ.4 id. In case the projection is not supported
     * by PROJ.4, use some prefix, e.g. josm:myproj or gdal:otherproj.
     * @param fac The base projection factory.
     * @param origin Multiple plugins may implement the same base projection.
     * Provide plugin name or similar string, so it be differentiated.
     */
    public static void registerBaseProjection(String id, ProjFactory fac, String origin) {
        projs.put(id, fac);
    }

    public static void registerBaseProjection(String id, Class<? extends Proj> projClass, String origin) {
        registerBaseProjection(id, new ClassProjFactory(projClass), origin);
    }

    public static Proj getBaseProjection(String id) {
        ProjFactory fac = projs.get(id);
        if (fac == null) return null;
        return fac.createInstance();
    }

    public static Ellipsoid getEllipsoid(String id) {
        return ellipsoids.get(id);
    }

    public static Datum getDatum(String id) {
        return datums.get(id);
    }

    public static NTV2GridShiftFileWrapper getNadgrids(String id) {
        return nadgrids.get(id);
    }
}
