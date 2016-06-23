// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.CentricDatum;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.NullDatum;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.ICentralMeridianProvider;
import org.openstreetmap.josm.data.projection.proj.IScaleFactorProvider;
import org.openstreetmap.josm.data.projection.proj.Mercator;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.tools.Utils;

/**
 * Custom projection.
 *
 * Inspired by PROJ.4 and Proj4J.
 * @since 5072
 */
public class CustomProjection extends AbstractProjection {

    /*
     * Equation for METER_PER_UNIT_DEGREE taken from:
     * https://github.com/openlayers/ol3/blob/master/src/ol/proj/epsg4326projection.js#L58
     * Value for Radius taken form:
     * https://github.com/openlayers/ol3/blob/master/src/ol/sphere/wgs84sphere.js#L11
     */
    private static final double METER_PER_UNIT_DEGREE = 2 * Math.PI * 6378137.0 / 360;
    private static final Map<String, Double> UNITS_TO_METERS = getUnitsToMeters();
    private static final Map<String, Double> PRIME_MERIDANS = getPrimeMeridians();

    /**
     * pref String that defines the projection
     *
     * null means fall back mode (Mercator)
     */
    protected String pref;
    protected String name;
    protected String code;
    protected String cacheDir;
    protected Bounds bounds;
    private double metersPerUnitWMTS;
    private String axis = "enu"; // default axis orientation is East, North, Up

    /**
     * Proj4-like projection parameters. See <a href="https://trac.osgeo.org/proj/wiki/GenParms">reference</a>.
     * @since 7370 (public)
     */
    public enum Param {

        /** False easting */
        x_0("x_0", true),
        /** False northing */
        y_0("y_0", true),
        /** Central meridian */
        lon_0("lon_0", true),
        /** Prime meridian */
        pm("pm", true),
        /** Scaling factor */
        k_0("k_0", true),
        /** Ellipsoid name (see {@code proj -le}) */
        ellps("ellps", true),
        /** Semimajor radius of the ellipsoid axis */
        a("a", true),
        /** Eccentricity of the ellipsoid squared */
        es("es", true),
        /** Reciprocal of the ellipsoid flattening term (e.g. 298) */
        rf("rf", true),
        /** Flattening of the ellipsoid = 1-sqrt(1-e^2) */
        f("f", true),
        /** Semiminor radius of the ellipsoid axis */
        b("b", true),
        /** Datum name (see {@code proj -ld}) */
        datum("datum", true),
        /** 3 or 7 term datum transform parameters */
        towgs84("towgs84", true),
        /** Filename of NTv2 grid file to use for datum transforms */
        nadgrids("nadgrids", true),
        /** Projection name (see {@code proj -l}) */
        proj("proj", true),
        /** Latitude of origin */
        lat_0("lat_0", true),
        /** Latitude of first standard parallel */
        lat_1("lat_1", true),
        /** Latitude of second standard parallel */
        lat_2("lat_2", true),
        /** Latitude of true scale (Polar Stereographic) */
        lat_ts("lat_ts", true),
        /** longitude of the center of the projection (Oblique Mercator) */
        lonc("lonc", true),
        /** azimuth (true) of the center line passing through the center of the
         * projection (Oblique Mercator) */
        alpha("alpha", true),
        /** rectified bearing of the center line (Oblique Mercator) */
        gamma("gamma", true),
        /** select "Hotine" variant of Oblique Mercator */
        no_off("no_off", false),
        /** legacy alias for no_off */
        no_uoff("no_uoff", false),
        /** longitude of first point (Oblique Mercator) */
        lon_1("lon_1", true),
        /** longitude of second point (Oblique Mercator) */
        lon_2("lon_2", true),
        /** the exact proj.4 string will be preserved in the WKT representation */
        wktext("wktext", false),  // ignored
        /** meters, US survey feet, etc. */
        units("units", true),
        /** Don't use the /usr/share/proj/proj_def.dat defaults file */
        no_defs("no_defs", false),
        init("init", true),
        /** crs units to meter multiplier */
        to_meter("to_meter", true),
        /** definition of axis for projection */
        axis("axis", true),
        /** UTM zone */
        zone("zone", true),
        /** indicate southern hemisphere for UTM */
        south("south", false),
        /** vertical units - ignore, as we don't use height information */
        vunits("vunits", true),
        // JOSM extensions, not present in PROJ.4
        wmssrs("wmssrs", true),
        bounds("bounds", true);

        /** Parameter key */
        public final String key;
        /** {@code true} if the parameter has a value */
        public final boolean hasValue;

        /** Map of all parameters by key */
        static final Map<String, Param> paramsByKey = new ConcurrentHashMap<>();
        static {
            for (Param p : Param.values()) {
                paramsByKey.put(p.key, p);
            }
        }

        Param(String key, boolean hasValue) {
            this.key = key;
            this.hasValue = hasValue;
        }
    }

    private enum Polarity { NORTH, SOUTH }

    private EnumMap<Polarity, EastNorth> polesEN;
    private EnumMap<Polarity, LatLon> polesLL;
    {
        polesLL = new EnumMap<>(Polarity.class);
        polesLL.put(Polarity.NORTH, LatLon.NORTH_POLE);
        polesLL.put(Polarity.SOUTH, LatLon.SOUTH_POLE);
    }

    /**
     * Constructs a new empty {@code CustomProjection}.
     */
    public CustomProjection() {
        // contents can be set later with update()
    }

    /**
     * Constructs a new {@code CustomProjection} with given parameters.
     * @param pref String containing projection parameters
     * (ex: "+proj=tmerc +lon_0=-3 +k_0=0.9996 +x_0=500000 +ellps=WGS84 +datum=WGS84 +bounds=-8,-5,2,85")
     */
    public CustomProjection(String pref) {
        this(null, null, pref, null);
    }

    /**
     * Constructs a new {@code CustomProjection} with given name, code and parameters.
     *
     * @param name describe projection in one or two words
     * @param code unique code for this projection - may be null
     * @param pref the string that defines the custom projection
     * @param cacheDir cache directory name
     */
    public CustomProjection(String name, String code, String pref, String cacheDir) {
        this.name = name;
        this.code = code;
        this.pref = pref;
        this.cacheDir = cacheDir;
        try {
            update(pref);
        } catch (ProjectionConfigurationException ex) {
            Main.trace(ex);
            try {
                update(null);
            } catch (ProjectionConfigurationException ex1) {
                throw new RuntimeException(ex1);
            }
        }
    }

    /**
     * Updates this {@code CustomProjection} with given parameters.
     * @param pref String containing projection parameters (ex: "+proj=lonlat +ellps=WGS84 +datum=WGS84 +bounds=-180,-90,180,90")
     * @throws ProjectionConfigurationException if {@code pref} cannot be parsed properly
     */
    public final void update(String pref) throws ProjectionConfigurationException {
        this.pref = pref;
        if (pref == null) {
            ellps = Ellipsoid.WGS84;
            datum = WGS84Datum.INSTANCE;
            proj = new Mercator();
            bounds = new Bounds(
                    -85.05112877980659, -180.0,
                    85.05112877980659, 180.0, true);
        } else {
            Map<String, String> parameters = parseParameterList(pref, false);
            parameters = resolveInits(parameters, false);
            ellps = parseEllipsoid(parameters);
            datum = parseDatum(parameters, ellps);
            if (ellps == null) {
                ellps = datum.getEllipsoid();
            }
            proj = parseProjection(parameters, ellps);
            // "utm" is a shortcut for a set of parameters
            if ("utm".equals(parameters.get(Param.proj.key))) {
                String zoneStr = parameters.get(Param.zone.key);
                Integer zone;
                if (zoneStr == null)
                    throw new ProjectionConfigurationException(tr("UTM projection (''+proj=utm'') requires ''+zone=...'' parameter."));
                try {
                    zone = Integer.valueOf(zoneStr);
                } catch (NumberFormatException e) {
                    zone = null;
                }
                if (zone == null || zone < 1 || zone > 60)
                    throw new ProjectionConfigurationException(tr("Expected integer value in range 1-60 for ''+zone=...'' parameter."));
                this.lon0 = 6d * zone - 183d;
                this.k0 = 0.9996;
                this.x0 = 500000;
                this.y0 = parameters.containsKey(Param.south.key) ? 10000000 : 0;
            }
            String s = parameters.get(Param.x_0.key);
            if (s != null) {
                this.x0 = parseDouble(s, Param.x_0.key);
            }
            s = parameters.get(Param.y_0.key);
            if (s != null) {
                this.y0 = parseDouble(s, Param.y_0.key);
            }
            s = parameters.get(Param.lon_0.key);
            if (s != null) {
                this.lon0 = parseAngle(s, Param.lon_0.key);
            }
            if (proj instanceof ICentralMeridianProvider) {
                this.lon0 = ((ICentralMeridianProvider) proj).getCentralMeridian();
            }
            s = parameters.get(Param.pm.key);
            if (s != null) {
                if (PRIME_MERIDANS.containsKey(s)) {
                    this.pm = PRIME_MERIDANS.get(s);
                } else {
                    this.pm = parseAngle(s, Param.pm.key);
                }
            }
            s = parameters.get(Param.k_0.key);
            if (s != null) {
                this.k0 = parseDouble(s, Param.k_0.key);
            }
            if (proj instanceof IScaleFactorProvider) {
                this.k0 *= ((IScaleFactorProvider) proj).getScaleFactor();
            }
            s = parameters.get(Param.bounds.key);
            if (s != null) {
                this.bounds = parseBounds(s);
            }
            s = parameters.get(Param.wmssrs.key);
            if (s != null) {
                this.code = s;
            }
            boolean defaultUnits = true;
            s = parameters.get(Param.units.key);
            if (s != null) {
                s = Utils.strip(s, "\"");
                if (UNITS_TO_METERS.containsKey(s)) {
                    this.toMeter = UNITS_TO_METERS.get(s);
                    this.metersPerUnitWMTS = this.toMeter;
                    defaultUnits = false;
                } else {
                    throw new ProjectionConfigurationException(tr("No unit found for: {0}", s));
                }
            }
            s = parameters.get(Param.to_meter.key);
            if (s != null) {
                this.toMeter = parseDouble(s, Param.to_meter.key);
                this.metersPerUnitWMTS = this.toMeter;
                defaultUnits = false;
            }
            if (defaultUnits) {
                this.toMeter = 1;
                this.metersPerUnitWMTS = proj.isGeographic() ? METER_PER_UNIT_DEGREE : 1;
            }
            s = parameters.get(Param.axis.key);
            if (s != null) {
                this.axis = s;
            }
        }
    }

    /**
     * Parse a parameter list to key=value pairs.
     *
     * @param pref the parameter list
     * @param ignoreUnknownParameter true, if unknown parameter should not raise exception
     * @return parameters map
     * @throws ProjectionConfigurationException in case of invalid parameter
     */
    public static Map<String, String> parseParameterList(String pref, boolean ignoreUnknownParameter) throws ProjectionConfigurationException {
        Map<String, String> parameters = new HashMap<>();
        String[] parts = Utils.WHITE_SPACES_PATTERN.split(pref.trim());
        if (pref.trim().isEmpty()) {
            parts = new String[0];
        }
        for (String part : parts) {
            if (part.isEmpty() || part.charAt(0) != '+')
                throw new ProjectionConfigurationException(tr("Parameter must begin with a ''+'' character (found ''{0}'')", part));
            Matcher m = Pattern.compile("\\+([a-zA-Z0-9_]+)(=(.*))?").matcher(part);
            if (m.matches()) {
                String key = m.group(1);
                // alias
                if ("k".equals(key)) {
                    key = Param.k_0.key;
                }
                String value = null;
                if (m.groupCount() >= 3) {
                    value = m.group(3);
                    // some aliases
                    if (key.equals(Param.proj.key)) {
                        if ("longlat".equals(value) || "latlon".equals(value) || "latlong".equals(value)) {
                            value = "lonlat";
                        }
                    }
                }
                if (!Param.paramsByKey.containsKey(key)) {
                    if (!ignoreUnknownParameter)
                        throw new ProjectionConfigurationException(tr("Unknown parameter: ''{0}''.", key));
                } else {
                    if (Param.paramsByKey.get(key).hasValue && value == null)
                        throw new ProjectionConfigurationException(tr("Value expected for parameter ''{0}''.", key));
                    if (!Param.paramsByKey.get(key).hasValue && value != null)
                        throw new ProjectionConfigurationException(tr("No value expected for parameter ''{0}''.", key));
                }
                parameters.put(key, value);
            } else
                throw new ProjectionConfigurationException(tr("Unexpected parameter format (''{0}'')", part));
        }
        return parameters;
    }

    /**
     * Recursive resolution of +init includes.
     *
     * @param parameters parameters map
     * @param ignoreUnknownParameter true, if unknown parameter should not raise exception
     * @return parameters map with +init includes resolved
     * @throws ProjectionConfigurationException in case of invalid parameter
     */
    public static Map<String, String> resolveInits(Map<String, String> parameters, boolean ignoreUnknownParameter)
            throws ProjectionConfigurationException {
        // recursive resolution of +init includes
        String initKey = parameters.get(Param.init.key);
        if (initKey != null) {
            String init = Projections.getInit(initKey);
            if (init == null)
                throw new ProjectionConfigurationException(tr("Value ''{0}'' for option +init not supported.", initKey));
            Map<String, String> initp;
            try {
                initp = parseParameterList(init, ignoreUnknownParameter);
                initp = resolveInits(initp, ignoreUnknownParameter);
            } catch (ProjectionConfigurationException ex) {
                throw new ProjectionConfigurationException(initKey+": "+ex.getMessage(), ex);
            }
            initp.putAll(parameters);
            return initp;
        }
        return parameters;
    }

    public Ellipsoid parseEllipsoid(Map<String, String> parameters) throws ProjectionConfigurationException {
        String code = parameters.get(Param.ellps.key);
        if (code != null) {
            Ellipsoid ellipsoid = Projections.getEllipsoid(code);
            if (ellipsoid == null) {
                throw new ProjectionConfigurationException(tr("Ellipsoid ''{0}'' not supported.", code));
            } else {
                return ellipsoid;
            }
        }
        String s = parameters.get(Param.a.key);
        if (s != null) {
            double a = parseDouble(s, Param.a.key);
            if (parameters.get(Param.es.key) != null) {
                double es = parseDouble(parameters, Param.es.key);
                return Ellipsoid.create_a_es(a, es);
            }
            if (parameters.get(Param.rf.key) != null) {
                double rf = parseDouble(parameters, Param.rf.key);
                return Ellipsoid.create_a_rf(a, rf);
            }
            if (parameters.get(Param.f.key) != null) {
                double f = parseDouble(parameters, Param.f.key);
                return Ellipsoid.create_a_f(a, f);
            }
            if (parameters.get(Param.b.key) != null) {
                double b = parseDouble(parameters, Param.b.key);
                return Ellipsoid.create_a_b(a, b);
            }
        }
        if (parameters.containsKey(Param.a.key) ||
                parameters.containsKey(Param.es.key) ||
                parameters.containsKey(Param.rf.key) ||
                parameters.containsKey(Param.f.key) ||
                parameters.containsKey(Param.b.key))
            throw new ProjectionConfigurationException(tr("Combination of ellipsoid parameters is not supported."));
        return null;
    }

    public Datum parseDatum(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        String datumId = parameters.get(Param.datum.key);
        if (datumId != null) {
            Datum datum = Projections.getDatum(datumId);
            if (datum == null) throw new ProjectionConfigurationException(tr("Unknown datum identifier: ''{0}''", datumId));
            return datum;
        }
        if (ellps == null) {
            if (parameters.containsKey(Param.no_defs.key))
                throw new ProjectionConfigurationException(tr("Ellipsoid required (+ellps=* or +a=*, +b=*)"));
            // nothing specified, use WGS84 as default
            ellps = Ellipsoid.WGS84;
        }

        String nadgridsId = parameters.get(Param.nadgrids.key);
        if (nadgridsId != null) {
            if (nadgridsId.startsWith("@")) {
                nadgridsId = nadgridsId.substring(1);
            }
            if ("null".equals(nadgridsId))
                return new NullDatum(null, ellps);
            NTV2GridShiftFileWrapper nadgrids = Projections.getNTV2Grid(nadgridsId);
            if (nadgrids == null)
                throw new ProjectionConfigurationException(tr("Grid shift file ''{0}'' for option +nadgrids not supported.", nadgridsId));
            return new NTV2Datum(nadgridsId, null, ellps, nadgrids);
        }

        String towgs84 = parameters.get(Param.towgs84.key);
        if (towgs84 != null)
            return parseToWGS84(towgs84, ellps);

        return new NullDatum(null, ellps);
    }

    public Datum parseToWGS84(String paramList, Ellipsoid ellps) throws ProjectionConfigurationException {
        String[] numStr = paramList.split(",");

        if (numStr.length != 3 && numStr.length != 7)
            throw new ProjectionConfigurationException(tr("Unexpected number of arguments for parameter ''towgs84'' (must be 3 or 7)"));
        List<Double> towgs84Param = new ArrayList<>();
        for (String str : numStr) {
            try {
                towgs84Param.add(Double.valueOf(str));
            } catch (NumberFormatException e) {
                throw new ProjectionConfigurationException(tr("Unable to parse value of parameter ''towgs84'' (''{0}'')", str), e);
            }
        }
        boolean isCentric = true;
        for (Double param : towgs84Param) {
            if (param != 0) {
                isCentric = false;
                break;
            }
        }
        if (isCentric)
            return new CentricDatum(null, null, ellps);
        boolean is3Param = true;
        for (int i = 3; i < towgs84Param.size(); i++) {
            if (towgs84Param.get(i) != 0) {
                is3Param = false;
                break;
            }
        }
        if (is3Param)
            return new ThreeParameterDatum(null, null, ellps,
                    towgs84Param.get(0),
                    towgs84Param.get(1),
                    towgs84Param.get(2));
        else
            return new SevenParameterDatum(null, null, ellps,
                    towgs84Param.get(0),
                    towgs84Param.get(1),
                    towgs84Param.get(2),
                    towgs84Param.get(3),
                    towgs84Param.get(4),
                    towgs84Param.get(5),
                    towgs84Param.get(6));
    }

    public Proj parseProjection(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        String id = parameters.get(Param.proj.key);
        if (id == null) throw new ProjectionConfigurationException(tr("Projection required (+proj=*)"));

        // "utm" is not a real projection, but a shortcut for a set of parameters
        if ("utm".equals(id)) {
            id = "tmerc";
        }
        Proj proj = Projections.getBaseProjection(id);
        if (proj == null) throw new ProjectionConfigurationException(tr("Unknown projection identifier: ''{0}''", id));

        ProjParameters projParams = new ProjParameters();

        projParams.ellps = ellps;

        String s;
        s = parameters.get(Param.lat_0.key);
        if (s != null) {
            projParams.lat0 = parseAngle(s, Param.lat_0.key);
        }
        s = parameters.get(Param.lat_1.key);
        if (s != null) {
            projParams.lat1 = parseAngle(s, Param.lat_1.key);
        }
        s = parameters.get(Param.lat_2.key);
        if (s != null) {
            projParams.lat2 = parseAngle(s, Param.lat_2.key);
        }
        s = parameters.get(Param.lat_ts.key);
        if (s != null) {
            projParams.lat_ts = parseAngle(s, Param.lat_ts.key);
        }
        s = parameters.get(Param.lonc.key);
        if (s != null) {
            projParams.lonc = parseAngle(s, Param.lonc.key);
        }
        s = parameters.get(Param.alpha.key);
        if (s != null) {
            projParams.alpha = parseAngle(s, Param.alpha.key);
        }
        s = parameters.get(Param.gamma.key);
        if (s != null) {
            projParams.gamma = parseAngle(s, Param.gamma.key);
        }
        s = parameters.get(Param.lon_1.key);
        if (s != null) {
            projParams.lon1 = parseAngle(s, Param.lon_1.key);
        }
        s = parameters.get(Param.lon_2.key);
        if (s != null) {
            projParams.lon2 = parseAngle(s, Param.lon_2.key);
        }
        if (parameters.containsKey(Param.no_off.key) || parameters.containsKey(Param.no_uoff.key)) {
            projParams.no_off = Boolean.TRUE;
        }
        proj.initialize(projParams);
        return proj;
    }

    public static Bounds parseBounds(String boundsStr) throws ProjectionConfigurationException {
        String[] numStr = boundsStr.split(",");
        if (numStr.length != 4)
            throw new ProjectionConfigurationException(tr("Unexpected number of arguments for parameter ''+bounds'' (must be 4)"));
        return new Bounds(parseAngle(numStr[1], "minlat (+bounds)"),
                parseAngle(numStr[0], "minlon (+bounds)"),
                parseAngle(numStr[3], "maxlat (+bounds)"),
                parseAngle(numStr[2], "maxlon (+bounds)"), false);
    }

    public static double parseDouble(Map<String, String> parameters, String parameterName) throws ProjectionConfigurationException {
        if (!parameters.containsKey(parameterName))
            throw new ProjectionConfigurationException(tr("Unknown parameter ''{0}''", parameterName));
        String doubleStr = parameters.get(parameterName);
        if (doubleStr == null)
            throw new ProjectionConfigurationException(
                    tr("Expected number argument for parameter ''{0}''", parameterName));
        return parseDouble(doubleStr, parameterName);
    }

    public static double parseDouble(String doubleStr, String parameterName) throws ProjectionConfigurationException {
        try {
            return Double.parseDouble(doubleStr);
        } catch (NumberFormatException e) {
            throw new ProjectionConfigurationException(
                    tr("Unable to parse value ''{1}'' of parameter ''{0}'' as number.", parameterName, doubleStr), e);
        }
    }

    public static double parseAngle(String angleStr, String parameterName) throws ProjectionConfigurationException {
        String s = angleStr;
        double value = 0;
        boolean neg = false;
        Matcher m = Pattern.compile("^-").matcher(s);
        if (m.find()) {
            neg = true;
            s = s.substring(m.end());
        }
        final String floatPattern = "(\\d+(\\.\\d*)?)";
        boolean dms = false;
        double deg = 0.0, min = 0.0, sec = 0.0;
        // degrees
        m = Pattern.compile("^"+floatPattern+"d").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            deg = Double.parseDouble(m.group(1));
            dms = true;
        }
        // minutes
        m = Pattern.compile("^"+floatPattern+"'").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            min = Double.parseDouble(m.group(1));
            dms = true;
        }
        // seconds
        m = Pattern.compile("^"+floatPattern+"\"").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            sec = Double.parseDouble(m.group(1));
            dms = true;
        }
        // plain number (in degrees)
        if (dms) {
            value = deg + (min/60.0) + (sec/3600.0);
        } else {
            m = Pattern.compile("^"+floatPattern).matcher(s);
            if (m.find()) {
                s = s.substring(m.end());
                value += Double.parseDouble(m.group(1));
            }
        }
        m = Pattern.compile("^(N|E)", Pattern.CASE_INSENSITIVE).matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
        } else {
            m = Pattern.compile("^(S|W)", Pattern.CASE_INSENSITIVE).matcher(s);
            if (m.find()) {
                s = s.substring(m.end());
                neg = !neg;
            }
        }
        if (neg) {
            value = -value;
        }
        if (!s.isEmpty()) {
            throw new ProjectionConfigurationException(
                    tr("Unable to parse value ''{1}'' of parameter ''{0}'' as coordinate value.", parameterName, angleStr));
        }
        return value;
    }

    @Override
    public Integer getEpsgCode() {
        if (code != null && code.startsWith("EPSG:")) {
            try {
                return Integer.valueOf(code.substring(5));
            } catch (NumberFormatException e) {
                Main.warn(e);
            }
        }
        return null;
    }

    @Override
    public String toCode() {
        return code != null ? code : "proj:" + (pref == null ? "ERROR" : pref);
    }

    @Override
    public String getCacheDirectoryName() {
        return cacheDir != null ? cacheDir : "proj-"+Utils.md5Hex(pref == null ? "" : pref).substring(0, 4);
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        if (bounds != null) return bounds;
        Bounds ab = proj.getAlgorithmBounds();
        if (ab != null) {
            double minlon = Math.max(ab.getMinLon() + lon0 + pm, -180);
            double maxlon = Math.min(ab.getMaxLon() + lon0 + pm, 180);
            return new Bounds(ab.getMinLat(), minlon, ab.getMaxLat(), maxlon, false);
        } else {
            return new Bounds(
                new LatLon(-90.0, -180.0),
                new LatLon(90.0, 180.0));
        }
    }

    @Override
    public String toString() {
        return name != null ? name : tr("Custom Projection");
    }

    /**
     * Factor to convert units of east/north coordinates to meters.
     *
     * When east/north coordinates are in degrees (geographic CRS), the scale
     * at the equator is taken, i.e. 360 degrees corresponds to the length of
     * the equator in meters.
     *
     * @return factor to convert units to meter
     */
    @Override
    public double getMetersPerUnit() {
        return metersPerUnitWMTS;
    }

    @Override
    public boolean switchXY() {
        // TODO: support for other axis orientation such as West South, and Up Down
        return this.axis.startsWith("ne");
    }

    private static Map<String, Double> getUnitsToMeters() {
        Map<String, Double> ret = new ConcurrentHashMap<>();
        ret.put("km", 1000d);
        ret.put("m", 1d);
        ret.put("dm", 1d/10);
        ret.put("cm", 1d/100);
        ret.put("mm", 1d/1000);
        ret.put("kmi", 1852.0);
        ret.put("in", 0.0254);
        ret.put("ft", 0.3048);
        ret.put("yd", 0.9144);
        ret.put("mi", 1609.344);
        ret.put("fathom", 1.8288);
        ret.put("chain", 20.1168);
        ret.put("link", 0.201168);
        ret.put("us-in", 1d/39.37);
        ret.put("us-ft", 0.304800609601219);
        ret.put("us-yd", 0.914401828803658);
        ret.put("us-ch", 20.11684023368047);
        ret.put("us-mi", 1609.347218694437);
        ret.put("ind-yd", 0.91439523);
        ret.put("ind-ft", 0.30479841);
        ret.put("ind-ch", 20.11669506);
        ret.put("degree", METER_PER_UNIT_DEGREE);
        return ret;
    }

    private static Map<String, Double> getPrimeMeridians() {
        Map<String, Double> ret = new ConcurrentHashMap<>();
        try {
            ret.put("greenwich", 0.0);
            ret.put("lisbon", parseAngle("9d07'54.862\"W", null));
            ret.put("paris", parseAngle("2d20'14.025\"E", null));
            ret.put("bogota", parseAngle("74d04'51.3\"W", null));
            ret.put("madrid", parseAngle("3d41'16.58\"W", null));
            ret.put("rome", parseAngle("12d27'8.4\"E", null));
            ret.put("bern", parseAngle("7d26'22.5\"E", null));
            ret.put("jakarta", parseAngle("106d48'27.79\"E", null));
            ret.put("ferro", parseAngle("17d40'W", null));
            ret.put("brussels", parseAngle("4d22'4.71\"E", null));
            ret.put("stockholm", parseAngle("18d3'29.8\"E", null));
            ret.put("athens", parseAngle("23d42'58.815\"E", null));
            ret.put("oslo", parseAngle("10d43'22.5\"E", null));
        } catch (ProjectionConfigurationException ex) {
            throw new IllegalStateException(ex);
        }
        return ret;
    }

    private static EastNorth getPointAlong(int i, int n, ProjectionBounds r) {
        double dEast = (r.maxEast - r.minEast) / n;
        double dNorth = (r.maxNorth - r.minNorth) / n;
        if (i < n) {
            return new EastNorth(r.minEast + i * dEast, r.minNorth);
        } else if (i < 2*n) {
            i -= n;
            return new EastNorth(r.maxEast, r.minNorth + i * dNorth);
        } else if (i < 3*n) {
            i -= 2*n;
            return new EastNorth(r.maxEast - i * dEast, r.maxNorth);
        } else if (i < 4*n) {
            i -= 3*n;
            return new EastNorth(r.minEast, r.maxNorth - i * dNorth);
        } else {
            throw new AssertionError();
        }
    }

    private EastNorth getPole(Polarity whichPole) {
        if (polesEN == null) {
            polesEN = new EnumMap<>(Polarity.class);
            for (Polarity p : Polarity.values()) {
                polesEN.put(p, null);
                LatLon ll = polesLL.get(p);
                try {
                    EastNorth enPole = latlon2eastNorth(ll);
                    if (enPole.isValid()) {
                        // project back and check if the result is somewhat reasonable
                        LatLon llBack = eastNorth2latlon(enPole);
                        if (llBack.isValid() && ll.greatCircleDistance(llBack) < 1000) {
                            polesEN.put(p, enPole);
                        }
                    }
                } catch (RuntimeException e) {
                    Main.error(e);
                }
            }
        }
        return polesEN.get(whichPole);
    }

    @Override
    public Bounds getLatLonBoundsBox(ProjectionBounds r) {
        final int n = 10;
        Bounds result = new Bounds(eastNorth2latlon(r.getMin()));
        result.extend(eastNorth2latlon(r.getMax()));
        LatLon llPrev = null;
        for (int i = 0; i < 4*n; i++) {
            LatLon llNow = eastNorth2latlon(getPointAlong(i, n, r));
            result.extend(llNow);
            // check if segment crosses 180th meridian and if so, make sure
            // to extend bounds to +/-180 degrees longitude
            if (llPrev != null) {
                double lon1 = llPrev.lon();
                double lon2 = llNow.lon();
                if (90 < lon1 && lon1 < 180 && -180 < lon2 && lon2 < -90) {
                    result.extend(new LatLon(llPrev.lat(), 180));
                    result.extend(new LatLon(llNow.lat(), -180));
                }
                if (90 < lon2 && lon2 < 180 && -180 < lon1 && lon1 < -90) {
                    result.extend(new LatLon(llNow.lat(), 180));
                    result.extend(new LatLon(llPrev.lat(), -180));
                }
            }
            llPrev = llNow;
        }
        // if the box contains one of the poles, the above method did not get
        // correct min/max latitude value
        for (Polarity p : Polarity.values()) {
            EastNorth pole = getPole(p);
            if (pole != null && r.contains(pole)) {
                result.extend(polesLL.get(p));
            }
        }
        return result;
    }
}
