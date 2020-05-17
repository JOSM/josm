// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.projection.datum.CentricDatum;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2Datum;
import org.openstreetmap.josm.data.projection.datum.NullDatum;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.ICentralMeridianProvider;
import org.openstreetmap.josm.data.projection.proj.IScaleFactorProvider;
import org.openstreetmap.josm.data.projection.proj.Mercator;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

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
    protected Bounds bounds;
    private double metersPerUnitWMTS;
    /**
     * Starting in PROJ 4.8.0, the {@code +axis} argument can be used to control the axis orientation of the coordinate system.
     * The default orientation is "easting, northing, up" but directions can be flipped, or axes flipped using
     * combinations of the axes in the {@code +axis} switch. The values are: {@code e} (Easting), {@code w} (Westing),
     * {@code n} (Northing), {@code s} (Southing), {@code u} (Up), {@code d} (Down);
     * Examples: {@code +axis=enu} (the default easting, northing, elevation), {@code +axis=neu} (northing, easting, up;
     * useful for "lat/long" geographic coordinates, or south orientated transverse mercator), {@code +axis=wnu}
     * (westing, northing, up - some planetary coordinate systems have "west positive" coordinate systems)<p>
     * See <a href="https://proj4.org/usage/projections.html#axis-orientation">proj4.org</a>
     */
    private String axis = "enu"; // default axis orientation is East, North, Up

    private static final List<String> LON_LAT_VALUES = Arrays.asList("longlat", "latlon", "latlong");

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
            // alias
            paramsByKey.put("k", Param.k_0);
        }

        Param(String key, boolean hasValue) {
            this.key = key;
            this.hasValue = hasValue;
        }
    }

    enum Polarity {
        NORTH(LatLon.NORTH_POLE),
        SOUTH(LatLon.SOUTH_POLE);

        private final LatLon latlon;

        Polarity(LatLon latlon) {
            this.latlon = latlon;
        }

        LatLon getLatLon() {
            return latlon;
        }
    }

    private EnumMap<Polarity, EastNorth> polesEN;

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
        this(null, null, pref);
    }

    /**
     * Constructs a new {@code CustomProjection} with given name, code and parameters.
     *
     * @param name describe projection in one or two words
     * @param code unique code for this projection - may be null
     * @param pref the string that defines the custom projection
     */
    public CustomProjection(String name, String code, String pref) {
        this.name = name;
        this.code = code;
        this.pref = pref;
        try {
            update(pref);
        } catch (ProjectionConfigurationException ex) {
            Logging.trace(ex);
            try {
                update(null);
            } catch (ProjectionConfigurationException ex1) {
                throw BugReport.intercept(ex1).put("name", name).put("code", code).put("pref", pref);
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
                Integer zone;
                try {
                    zone = Integer.valueOf(Optional.ofNullable(parameters.get(Param.zone.key)).orElseThrow(
                            () -> new ProjectionConfigurationException(tr("UTM projection (''+proj=utm'') requires ''+zone=...'' parameter."))));
                } catch (NumberFormatException e) {
                    zone = null;
                }
                if (zone == null || zone < 1 || zone > 60)
                    throw new ProjectionConfigurationException(tr("Expected integer value in range 1-60 for ''+zone=...'' parameter."));
                this.lon0 = 6d * zone - 183d;
                this.k0 = 0.9996;
                this.x0 = 500_000;
                this.y0 = parameters.containsKey(Param.south.key) ? 10_000_000 : 0;
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
            this.bounds = s != null ? parseBounds(s) : null;
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
        String trimmedPref = pref.trim();
        if (trimmedPref.isEmpty()) {
            return parameters;
        }

        Pattern keyPattern = Pattern.compile("\\+(?<key>[a-zA-Z0-9_]+)(=(?<value>.*))?");
        String[] parts = Utils.WHITE_SPACES_PATTERN.split(trimmedPref);
        for (String part : parts) {
            Matcher m = keyPattern.matcher(part);
            if (m.matches()) {
                String key = m.group("key");
                String value = m.group("value");
                // some aliases
                if (key.equals(Param.proj.key) && LON_LAT_VALUES.contains(value)) {
                    value = "lonlat";
                }
                Param param = Param.paramsByKey.get(key);
                if (param == null) {
                    if (!ignoreUnknownParameter)
                        throw new ProjectionConfigurationException(tr("Unknown parameter: ''{0}''.", key));
                } else {
                    if (param.hasValue && value == null)
                        throw new ProjectionConfigurationException(tr("Value expected for parameter ''{0}''.", key));
                    if (!param.hasValue && value != null)
                        throw new ProjectionConfigurationException(tr("No value expected for parameter ''{0}''.", key));
                    key = param.key; // To be really sure, we might have an alias.
                }
                parameters.put(key, value);
            } else if (!part.startsWith("+")) {
                throw new ProjectionConfigurationException(tr("Parameter must begin with a ''+'' character (found ''{0}'')", part));
            } else {
                throw new ProjectionConfigurationException(tr("Unexpected parameter format (''{0}'')", part));
            }
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
            Map<String, String> initp;
            try {
                initp = parseParameterList(Optional.ofNullable(Projections.getInit(initKey)).orElseThrow(
                        () -> new ProjectionConfigurationException(tr("Value ''{0}'' for option +init not supported.", initKey))),
                        ignoreUnknownParameter);
                initp = resolveInits(initp, ignoreUnknownParameter);
            } catch (ProjectionConfigurationException ex) {
                throw new ProjectionConfigurationException(initKey+": "+ex.getMessage(), ex);
            }
            initp.putAll(parameters);
            return initp;
        }
        return parameters;
    }

    /**
     * Gets the ellipsoid
     * @param parameters The parameters to get the value from
     * @return The Ellipsoid as specified with the parameters
     * @throws ProjectionConfigurationException in case of invalid parameters
     */
    public Ellipsoid parseEllipsoid(Map<String, String> parameters) throws ProjectionConfigurationException {
        String code = parameters.get(Param.ellps.key);
        if (code != null) {
            return Optional.ofNullable(Projections.getEllipsoid(code)).orElseThrow(
                () -> new ProjectionConfigurationException(tr("Ellipsoid ''{0}'' not supported.", code)));
        }
        String s = parameters.get(Param.a.key);
        if (s != null) {
            double a = parseDouble(s, Param.a.key);
            if (parameters.get(Param.es.key) != null) {
                double es = parseDouble(parameters, Param.es.key);
                return Ellipsoid.createAes(a, es);
            }
            if (parameters.get(Param.rf.key) != null) {
                double rf = parseDouble(parameters, Param.rf.key);
                return Ellipsoid.createArf(a, rf);
            }
            if (parameters.get(Param.f.key) != null) {
                double f = parseDouble(parameters, Param.f.key);
                return Ellipsoid.createAf(a, f);
            }
            if (parameters.get(Param.b.key) != null) {
                double b = parseDouble(parameters, Param.b.key);
                return Ellipsoid.createAb(a, b);
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

    /**
     * Gets the datum
     * @param parameters The parameters to get the value from
     * @param ellps The ellisoid that was previously computed
     * @return The Datum as specified with the parameters
     * @throws ProjectionConfigurationException in case of invalid parameters
     */
    public Datum parseDatum(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        Datum result = null;
        String datumId = parameters.get(Param.datum.key);
        if (datumId != null) {
            result = Optional.ofNullable(Projections.getDatum(datumId)).orElseThrow(
                    () -> new ProjectionConfigurationException(tr("Unknown datum identifier: ''{0}''", datumId)));
        }
        if (ellps == null) {
            if (result == null && parameters.containsKey(Param.no_defs.key))
                throw new ProjectionConfigurationException(tr("Ellipsoid required (+ellps=* or +a=*, +b=*)"));
            // nothing specified, use WGS84 as default
            ellps = result != null ? result.getEllipsoid() : Ellipsoid.WGS84;
        }

        String nadgridsId = parameters.get(Param.nadgrids.key);
        if (nadgridsId != null) {
            if (nadgridsId.startsWith("@")) {
                nadgridsId = nadgridsId.substring(1);
            }
            if ("null".equals(nadgridsId))
                return new NullDatum(null, ellps);
            final String fNadgridsId = nadgridsId;
            return new NTV2Datum(fNadgridsId, null, ellps, Optional.ofNullable(Projections.getNTV2Grid(fNadgridsId)).orElseThrow(
                    () -> new ProjectionConfigurationException(tr("Grid shift file ''{0}'' for option +nadgrids not supported.", fNadgridsId))));
        }

        String towgs84 = parameters.get(Param.towgs84.key);
        if (towgs84 != null) {
            Datum towgs84Datum = parseToWGS84(towgs84, ellps);
            if (result == null || towgs84Datum instanceof ThreeParameterDatum || towgs84Datum instanceof SevenParameterDatum) {
                // +datum has priority over +towgs84=0,0,0[,0,0,0,0]
                return towgs84Datum;
            }
        }

        return result != null ? result : new NullDatum(null, ellps);
    }

    /**
     * Parse {@code towgs84} parameter.
     * @param paramList List of parameter arguments (expected: 3 or 7)
     * @param ellps ellipsoid
     * @return parsed datum ({@link ThreeParameterDatum} or {@link SevenParameterDatum})
     * @throws ProjectionConfigurationException if the arguments cannot be parsed
     */
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
        boolean isCentric = towgs84Param.stream().noneMatch(param -> param != 0);
        if (isCentric)
            return Ellipsoid.WGS84.equals(ellps) ? WGS84Datum.INSTANCE : new CentricDatum(null, null, ellps);
        boolean is3Param = IntStream.range(3, towgs84Param.size()).noneMatch(i -> towgs84Param.get(i) != 0);
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

    /**
     * Gets a projection using the given ellipsoid
     * @param parameters Additional parameters
     * @param ellps The {@link Ellipsoid}
     * @return The projection
     * @throws ProjectionConfigurationException in case of invalid parameters
     */
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
        s = parameters.get(Param.lon_0.key);
        if (s != null) {
            projParams.lon0 = parseAngle(s, Param.lon_0.key);
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

    /**
     * Converts a string to a bounds object
     * @param boundsStr The string as comma separated list of angles.
     * @return The bounds.
     * @throws ProjectionConfigurationException in case of invalid parameter
     * @see CustomProjection#parseAngle(String, String)
     */
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
            throw new ProjectionConfigurationException(tr("Unknown parameter: ''{0}''.", parameterName));
        return parseDouble(Optional.ofNullable(parameters.get(parameterName)).orElseThrow(
                () -> new ProjectionConfigurationException(tr("Expected number argument for parameter ''{0}''", parameterName))),
                parameterName);
    }

    public static double parseDouble(String doubleStr, String parameterName) throws ProjectionConfigurationException {
        try {
            return Double.parseDouble(doubleStr);
        } catch (NumberFormatException e) {
            throw new ProjectionConfigurationException(
                    tr("Unable to parse value ''{1}'' of parameter ''{0}'' as number.", parameterName, doubleStr), e);
        }
    }

    /**
     * Convert an angle string to a double value
     * @param angleStr The string. e.g. -1.1 or 50d10'3"
     * @param parameterName Only for error message.
     * @return The angle value, in degrees.
     * @throws ProjectionConfigurationException in case of invalid parameter
     */
    public static double parseAngle(String angleStr, String parameterName) throws ProjectionConfigurationException {
        try {
            return LatLonParser.parseCoordinate(angleStr);
        } catch (IllegalArgumentException e) {
            throw new ProjectionConfigurationException(
                    tr("Unable to parse value ''{1}'' of parameter ''{0}'' as coordinate value.", parameterName, angleStr), e);
        }
    }

    @Override
    public Integer getEpsgCode() {
        if (code != null && code.startsWith("EPSG:")) {
            try {
                return Integer.valueOf(code.substring(5));
            } catch (NumberFormatException e) {
                Logging.warn(e);
            }
        }
        return null;
    }

    @Override
    public String toCode() {
        if (code != null) {
            return code;
        } else if (pref != null) {
            return "proj:" + pref;
        } else {
            return "proj:ERROR";
        }
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        if (bounds == null) {
            Bounds ab = proj.getAlgorithmBounds();
            if (ab != null) {
                double minlon = Math.max(ab.getMinLon() + lon0 + pm, -180);
                double maxlon = Math.min(ab.getMaxLon() + lon0 + pm, 180);
                bounds = new Bounds(ab.getMinLat(), minlon, ab.getMaxLat(), maxlon, false);
            } else {
                bounds = new Bounds(
                    new LatLon(-90.0, -180.0),
                    new LatLon(90.0, 180.0));
            }
        }
        return bounds;
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
        // +axis=neu
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
                LatLon ll = p.getLatLon();
                try {
                    EastNorth enPole = latlon2eastNorth(ll);
                    if (enPole.isValid()) {
                        // project back and check if the result is somewhat reasonable
                        LatLon llBack = eastNorth2latlon(enPole);
                        if (llBack.isValid() && ll.greatCircleDistance(llBack) < 1000) {
                            polesEN.put(p, enPole);
                        }
                    }
                } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    Logging.error(e);
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
                result.extend(p.getLatLon());
            }
        }
        return result;
    }

    @Override
    public ProjectionBounds getEastNorthBoundsBox(ProjectionBounds box, Projection boxProjection) {
        final int n = 8;
        ProjectionBounds result = null;
        for (int i = 0; i < 4*n; i++) {
            EastNorth en = latlon2eastNorth(boxProjection.eastNorth2latlon(getPointAlong(i, n, box)));
            if (result == null) {
                result = new ProjectionBounds(en);
            } else {
                result.extend(en);
            }
        }
        return result;
    }

    /**
     * Return true, if a geographic coordinate reference system is represented.
     *
     * I.e. if it returns latitude/longitude values rather than Cartesian
     * east/north coordinates on a flat surface.
     * @return true, if it is geographic
     * @since 12792
     */
    public boolean isGeographic() {
        return proj.isGeographic();
    }

}
