// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.ClassProjFactory;
import org.openstreetmap.josm.data.projection.proj.DoubleStereographic;
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
import org.openstreetmap.josm.tools.Utils;

/**
 * Class to handle projections
 *
 */
public final class Projections {

    /**
     * Class to hold information about one projection.
     */
    public static class ProjectionDefinition {
        public String code;
        public String name;
        public String definition;

        public ProjectionDefinition(String code, String name, String definition) {
            this.code = code;
            this.name = name;
            this.definition = definition;
        }
    }

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
    static final Map<String, ProjFactory> projs = new HashMap<>();
    static final Map<String, Ellipsoid> ellipsoids = new HashMap<>();
    static final Map<String, Datum> datums = new HashMap<>();
    static final Map<String, NTV2GridShiftFileWrapper> nadgrids = new HashMap<>();
    static final Map<String, ProjectionDefinition> inits;

    static {
        registerBaseProjection("lonlat", LonLat.class, "core");
        registerBaseProjection("josm:smerc", Mercator.class, "core");
        registerBaseProjection("lcc", LambertConformalConic.class, "core");
        registerBaseProjection("somerc", SwissObliqueMercator.class, "core");
        registerBaseProjection("tmerc", TransverseMercator.class, "core");
        registerBaseProjection("sterea", DoubleStereographic.class, "core");

        ellipsoids.put("airy", Ellipsoid.Airy);
        ellipsoids.put("mod_airy", Ellipsoid.AiryMod);
        ellipsoids.put("aust_SA", Ellipsoid.AustSA);
        ellipsoids.put("bessel", Ellipsoid.Bessel1841);
        ellipsoids.put("bess_nam", Ellipsoid.BesselNamibia);
        ellipsoids.put("clrk66", Ellipsoid.Clarke1866);
        ellipsoids.put("clrk80", Ellipsoid.Clarke1880);
        ellipsoids.put("clarkeIGN", Ellipsoid.ClarkeIGN);
        ellipsoids.put("evrstSS", Ellipsoid.EverestSabahSarawak);
        ellipsoids.put("intl", Ellipsoid.Hayford);
        ellipsoids.put("helmert", Ellipsoid.Helmert);
        ellipsoids.put("krass", Ellipsoid.Krassowsky);
        ellipsoids.put("GRS67", Ellipsoid.GRS67);
        ellipsoids.put("GRS80", Ellipsoid.GRS80);
        ellipsoids.put("WGS66", Ellipsoid.WGS66);
        ellipsoids.put("WGS72", Ellipsoid.WGS72);
        ellipsoids.put("WGS84", Ellipsoid.WGS84);

        datums.put("WGS84", WGS84Datum.INSTANCE);
        datums.put("GRS80", GRS80Datum.INSTANCE);
        datums.put("NAD83", GRS80Datum.INSTANCE);
        datums.put("carthage", new ThreeParameterDatum(
                "Carthage 1934 Tunisia", "carthage",
                Ellipsoid.Clarke1880, -263.0, 6.0, 431.0));
        datums.put("GGRS87", new ThreeParameterDatum(
                "Greek Geodetic Reference System 1987", "GGRS87",
                Ellipsoid.GRS80, -199.87, 74.79, 246.62));
        datums.put("hermannskogel", new ThreeParameterDatum(
                "Hermannskogel", "hermannskogel",
                Ellipsoid.Bessel1841, 653.0, -212.0, 449.0));
        datums.put("ire65", new SevenParameterDatum(
                "Ireland 1965", "ire65",
                Ellipsoid.AiryMod, 482.530, -130.596, 564.557, -1.042, -0.214, -0.631, 8.15));
        datums.put("nzgd49", new SevenParameterDatum(
                "New Zealand Geodetic Datum 1949", "nzgd49",
                Ellipsoid.Hayford, 59.47, -5.04, 187.44, 0.47, -0.1, 1.024, -4.5993));
        datums.put("OSGB36", new SevenParameterDatum(
                "Airy 1830", "OSGB36",
                Ellipsoid.Airy, 446.448, -125.157, 542.060, 0.1502, 0.2470, 0.8421, -20.4894));
        datums.put("potsdam", new SevenParameterDatum(
                "Potsdam Rauenberg 1950 DHDN", "potsdam",
                Ellipsoid.Bessel1841, 598.1, 73.7, 418.2, 0.202, 0.045, -2.455, 6.7));

        nadgrids.put("BETA2007.gsb", NTV2GridShiftFileWrapper.BETA2007);
        nadgrids.put("ntf_r93_b.gsb", NTV2GridShiftFileWrapper.ntf_rgf93);

        List<ProjectionDefinition> pds;
        try {
            pds = loadProjectionDefinitions("resource://data/projection/epsg");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        inits = new LinkedHashMap<>();
        for (ProjectionDefinition pd : pds) {
            inits.put(pd.code, pd);
        }
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
        ProjectionDefinition pd = inits.get(id.toUpperCase(Locale.ENGLISH));
        if (pd == null) return null;
        return pd.definition;
    }

    /**
     * Load projection definitions from file.
     * 
     * @param path the path
     * @return projection definitions
     * @throws java.io.IOException
     */
    public static List<ProjectionDefinition> loadProjectionDefinitions(String path) throws IOException {
        try (
            InputStream in = new CachedFile(path).getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        ) {
            return loadProjectionDefinitions(r);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Load projection definitions from file.
     * 
     * @param r the reader
     * @return projection definitions
     * @throws java.io.IOException
     */
    public static List<ProjectionDefinition> loadProjectionDefinitions(BufferedReader r) throws IOException {
        List<ProjectionDefinition> result = new ArrayList<>();
        Pattern epsgPattern = Pattern.compile("<(\\d+)>(.*)<>");
        String line, lastline = "";
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (!line.startsWith("#") && !line.isEmpty()) {
                if (!lastline.startsWith("#")) throw new AssertionError("EPSG file seems corrupted");
                String name = lastline.substring(1).trim();
                Matcher m = epsgPattern.matcher(line);
                if (m.matches()) {
                    String code = "EPSG:" + m.group(1);
                    String definition = m.group(2).trim();
                    result.add(new ProjectionDefinition(code, name, definition));
                } else {
                    Main.warn("Failed to parse line from the EPSG projection definition: "+line);
                }
            }
            lastline = line;
        }
        return result;
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
            ProjectionDefinition pd = inits.get(code);
            if (pd == null) return null;
            proj = new CustomProjection(pd.name, code, pd.definition, null);
        }
        projectionsByCode_cache.put(code, proj);
        return proj;
    }

    public static Collection<String> getAllProjectionCodes() {
        return Collections.unmodifiableCollection(allCodes);
    }

    private static String listKeys(Map<String, ?> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        return Utils.join(", ", keys);
    }

    /**
     * Replies the list of projections as string (comma separated).
     * @return the list of projections as string (comma separated)
     * @since 8533
     */
    public static String listProjs() {
        return listKeys(projs);
    }

    /**
     * Replies the list of ellipsoids as string (comma separated).
     * @return the list of ellipsoids as string (comma separated)
     * @since 8533
     */
    public static String listEllipsoids() {
        return listKeys(ellipsoids);
    }

    /**
     * Replies the list of datums as string (comma separated).
     * @return the list of datums as string (comma separated)
     * @since 8533
     */
    public static String listDatums() {
        return listKeys(datums);
    }

    /**
     * Replies the list of nadgrids as string (comma separated).
     * @return the list of nadgrids as string (comma separated)
     * @since 8533
     */
    public static String listNadgrids() {
        return listKeys(nadgrids);
    }
}
