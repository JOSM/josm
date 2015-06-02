// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.ClassProjFactory;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.LonLat;
import org.openstreetmap.josm.data.projection.proj.Mercator;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjFactory;
import org.openstreetmap.josm.data.projection.proj.SwissObliqueMercator;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Pair;

/**
 * Class to handle projections
 *
 */
public final class Projections {

    private Projections() {
        // Hide default constructor for utils classes
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
    public static final Map<String, ProjFactory> projs = new HashMap<>();
    public static final Map<String, Ellipsoid> ellipsoids = new HashMap<>();
    public static final Map<String, Datum> datums = new HashMap<>();
    public static final Map<String, NTV2GridShiftFileWrapper> nadgrids = new HashMap<>();
    public static final Map<String, Pair<String, String>> inits = new HashMap<>();

    static {
        registerBaseProjection("lonlat", LonLat.class, "core");
        registerBaseProjection("josm:smerc", Mercator.class, "core");
        registerBaseProjection("lcc", LambertConformalConic.class, "core");
        registerBaseProjection("somerc", SwissObliqueMercator.class, "core");
        registerBaseProjection("tmerc", TransverseMercator.class, "core");

        ellipsoids.put("airy", Ellipsoid.Airy);
        ellipsoids.put("mod_airy", Ellipsoid.AiryMod);
        ellipsoids.put("aust_SA", Ellipsoid.AustSA);
        ellipsoids.put("bessel", Ellipsoid.Bessel1841);
        ellipsoids.put("clrk66", Ellipsoid.Clarke1866);
        ellipsoids.put("clarkeIGN", Ellipsoid.ClarkeIGN);
        ellipsoids.put("intl", Ellipsoid.Hayford);
        ellipsoids.put("helmert", Ellipsoid.Helmert);
        ellipsoids.put("krass", Ellipsoid.Krassowsky);
        ellipsoids.put("GRS67", Ellipsoid.GRS67);
        ellipsoids.put("GRS80", Ellipsoid.GRS80);
        ellipsoids.put("WGS72", Ellipsoid.WGS72);
        ellipsoids.put("WGS84", Ellipsoid.WGS84);

        datums.put("WGS84", WGS84Datum.INSTANCE);
        datums.put("GRS80", GRS80Datum.INSTANCE);

        nadgrids.put("BETA2007.gsb", NTV2GridShiftFileWrapper.BETA2007);
        nadgrids.put("ntf_r93_b.gsb", NTV2GridShiftFileWrapper.ntf_rgf93);

        loadInits();
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

    public static NTV2GridShiftFileWrapper getNTV2Grid(String id) {
        return nadgrids.get(id);
    }

    /**
     * Get the projection definition string for the given id.
     * @param id the id
     * @return the string that can be processed by #{link CustomProjection}.
     * Null, if the id isn't supported.
     */
    public static String getInit(String id) {
        Pair<String, String> r = inits.get(id.toUpperCase(Locale.ENGLISH));
        if (r == null) return null;
        return r.b;
    }

    /**
     * Load +init "presets" from file
     */
    private static void loadInits() {
        Pattern epsgPattern = Pattern.compile("<(\\d+)>(.*)<>");
        try (
            InputStream in = new CachedFile("resource://data/projection/epsg").getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        ) {
            String line, lastline = "";
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    if (!lastline.startsWith("#")) throw new AssertionError("EPSG file seems corrupted");
                    String name = lastline.substring(1).trim();
                    Matcher m = epsgPattern.matcher(line);
                    if (m.matches()) {
                        inits.put("EPSG:" + m.group(1), Pair.create(name, m.group(2).trim()));
                    } else {
                        Main.warn("Failed to parse line from the EPSG projection definition: "+line);
                    }
                }
                lastline = line;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final Set<String> allCodes = new HashSet<>();
    private static final Map<String, ProjectionChoice> allProjectionChoicesByCode = new HashMap<>();
    private static final Map<String, Projection> projectionsByCode_cache = new HashMap<>();

    static {
        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            for (String code : pc.allCodes()) {
                allProjectionChoicesByCode.put(code, pc);
            }
        }
        allCodes.addAll(inits.keySet());
        allCodes.addAll(allProjectionChoicesByCode.keySet());
    }

    public static Projection getProjectionByCode(String code) {
        Projection proj = projectionsByCode_cache.get(code);
        if (proj != null) return proj;
        ProjectionChoice pc = allProjectionChoicesByCode.get(code);
        if (pc != null) {
            Collection<String> pref = pc.getPreferencesFromCode(code);
            pc.setPreferences(pref);
            try {
                proj = pc.getProjection();
            } catch (Exception e) {
                String cause = e.getMessage();
                Main.warn("Unable to get projection "+code+" with "+pc + (cause != null ? ". "+cause : ""));
            }
        }
        if (proj == null) {
            Pair<String, String> pair = inits.get(code);
            if (pair == null) return null;
            String name = pair.a;
            String init = pair.b;
            proj = new CustomProjection(name, code, init, null);
        }
        projectionsByCode_cache.put(code, proj);
        return proj;
    }

    public static Collection<String> getAllProjectionCodes() {
        return Collections.unmodifiableCollection(allCodes);
    }
}
