// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.CustomProjection.Param;
import org.openstreetmap.josm.data.projection.datum.CentricDatum;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.NullDatum;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.tools.Utils;

/**
 * Custom projection
 *
 * Inspired by PROJ.4 and Proj4J.
 */
public class CustomProjection extends AbstractProjection {

    /**
     * pref String that defines the projection
     *
     * null means fall back mode (Mercator)
     */
    protected String pref;
    protected Bounds bounds;

    protected static class Param {
        public String key;
        public boolean hasValue;

        public final static Param x_0 = new Param("x_0", true);
        public final static Param y_0 = new Param("y_0", true);
        public final static Param lon_0 = new Param("lon_0", true);
        public final static Param k_0 = new Param("k_0", true);
        public final static Param ellps = new Param("ellps", true);
        public final static Param a = new Param("a", true);
        public final static Param es = new Param("es", true);
        public final static Param rf = new Param("rf", true);
        public final static Param f = new Param("f", true);
        public final static Param b = new Param("b", true);
        public final static Param datum = new Param("datum", true);
        public final static Param towgs84 = new Param("towgs84", true);
        public final static Param nadgrids = new Param("nadgrids", true);
        public final static Param proj = new Param("proj", true);
        public final static Param lat_0 = new Param("lat_0", true);
        public final static Param lat_1 = new Param("lat_1", true);
        public final static Param lat_2 = new Param("lat_2", true);
        public final static Param wktext = new Param("wktext", false);  // ignored
        public final static Param units = new Param("units", true);     // ignored
        public final static Param no_defs = new Param("no_defs", false);
        public final static Param init = new Param("init", true);
        // JOSM extension, not present in PROJ.4
        public final static Param bounds = new Param("bounds", true);

        public final static Set<Param> params = new HashSet<Param>(Arrays.asList(
                x_0, y_0, lon_0, k_0, ellps, a, es, rf, f, b, datum, towgs84,
                nadgrids, proj, lat_0, lat_1, lat_2, wktext, units, no_defs, 
                init, bounds
        ));

        public final static Map<String, Param> paramsByKey = new HashMap<String, Param>();
        static {
            for (Param p : params) {
                paramsByKey.put(p.key, p);
            }
        }

        public Param(String key, boolean hasValue) {
            this.key = key;
            this.hasValue = hasValue;
        }
    }

    public CustomProjection() {
        this.pref = null;
    }

    public CustomProjection(String pref) {
        try {
            this.pref = pref;
            update(pref);
        } catch (ProjectionConfigurationException ex) {
            try {
                update(null);
            } catch (ProjectionConfigurationException ex1) {
                throw new RuntimeException();
            }
        }
    }

    public void update(String pref) throws ProjectionConfigurationException {
        this.pref = pref;
        if (pref == null) {
            ellps = Ellipsoid.WGS84;
            datum = WGS84Datum.INSTANCE;
            proj = new org.openstreetmap.josm.data.projection.proj.Mercator();
            bounds = new Bounds(
                    new LatLon(-85.05112877980659, -180.0),
                    new LatLon(85.05112877980659, 180.0));
        } else {
            Map<String, String> parameters = parseParameterList(pref);
            ellps = parseEllipsoid(parameters);
            datum = parseDatum(parameters, ellps);
            proj = parseProjection(parameters, ellps);
            String s = parameters.get(Param.x_0.key);
            if (s != null) {
                this.x_0 = parseDouble(s, Param.x_0.key);
            }
            s = parameters.get(Param.y_0.key);
            if (s != null) {
                this.y_0 = parseDouble(s, Param.y_0.key);
            }
            s = parameters.get(Param.lon_0.key);
            if (s != null) {
                this.lon_0 = parseAngle(s, Param.lon_0.key);
            }
            s = parameters.get(Param.k_0.key);
            if (s != null) {
                this.k_0 = parseDouble(s, Param.k_0.key);
            }
            s = parameters.get(Param.bounds.key);
            if (s != null) {
                this.bounds = parseBounds(s);
            }
        }
    }

    private Map<String, String> parseParameterList(String pref) throws ProjectionConfigurationException {
        Map<String, String> parameters = new HashMap<String, String>();
        String[] parts = pref.trim().split("\\s+");
        if (pref.trim().isEmpty()) {
            parts = new String[0];
        }
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.charAt(0) != '+')
                throw new ProjectionConfigurationException(tr("Parameter must begin with a ''+'' character (found ''{0}'')", part));
            Matcher m = Pattern.compile("\\+([a-zA-Z0-9_]+)(=(.*))?").matcher(part);
            if (m.matches()) {
                String key = m.group(1);
                // alias
                if (key.equals("k")) {
                    key = Param.k_0.key;
                }
                String value = null;
                if (m.groupCount() >= 3) {
                    value = m.group(3);
                    // same aliases
                    if (key.equals(Param.proj.key)) {
                        if (value.equals("longlat") || value.equals("latlon") || value.equals("latlong")) {
                            value = "lonlat";
                        }
                    }
                }
                if (!Param.paramsByKey.containsKey(key))
                    throw new ProjectionConfigurationException(tr("Unkown parameter: ''{0}''.", key));
                if (Param.paramsByKey.get(key).hasValue && value == null)
                    throw new ProjectionConfigurationException(tr("Value expected for parameter ''{0}''.", key));
                if (!Param.paramsByKey.get(key).hasValue && value != null)
                    throw new ProjectionConfigurationException(tr("No value expected for parameter ''{0}''.", key));
                parameters.put(key, value);
            } else
                throw new ProjectionConfigurationException(tr("Unexpected parameter format (''{0}'')", part));
        }
        // recursive resolution of +init includes
        String initKey = parameters.get(Param.init.key);
        if (initKey != null) {
            String init = Projections.getInit(initKey);
            if (init == null)
                throw new ProjectionConfigurationException(tr("Value ''{0}'' for option +init not supported.", initKey));
            Map<String, String> initp = null;
            try {
                initp = parseParameterList(init);
            } catch (ProjectionConfigurationException ex) {
                throw new ProjectionConfigurationException(tr(initKey+": "+ex.getMessage()));
            }
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                initp.put(e.getKey(), e.getValue());
            }
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
        if (parameters.containsKey(Param.no_defs.key))
            throw new ProjectionConfigurationException(tr("Ellipsoid required (+ellps=* or +a=*, +b=*)"));
        // nothing specified, use WGS84 as default
        return Ellipsoid.WGS84;
    }

    public Datum parseDatum(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        String nadgridsId = parameters.get(Param.nadgrids.key);
        if (nadgridsId != null) {
            if (nadgridsId.startsWith("@")) {
                nadgridsId = nadgridsId.substring(1);
            }
            if (nadgridsId.equals("null"))
                return new NullDatum(null, ellps);
            NTV2GridShiftFileWrapper nadgrids = Projections.getNTV2Grid(nadgridsId);
            if (nadgrids == null)
                throw new ProjectionConfigurationException(tr("Grid shift file ''{0}'' for option +nadgrids not supported.", nadgridsId));
            return new NTV2Datum(nadgridsId, null, ellps, nadgrids);
        }

        String towgs84 = parameters.get(Param.towgs84.key);
        if (towgs84 != null)
            return parseToWGS84(towgs84, ellps);

        String datumId = parameters.get(Param.datum.key);
        if (datumId != null) {
            Datum datum = Projections.getDatum(datumId);
            if (datum == null) throw new ProjectionConfigurationException(tr("Unkown datum identifier: ''{0}''", datumId));
            return datum;
        }
        if (parameters.containsKey(Param.no_defs.key))
            throw new ProjectionConfigurationException(tr("Datum required (+datum=*, +towgs84=* or +nadgirds=*)"));
        return new CentricDatum(null, null, ellps);
    }

    public Datum parseToWGS84(String paramList, Ellipsoid ellps) throws ProjectionConfigurationException {
        String[] numStr = paramList.split(",");

        if (numStr.length != 3 && numStr.length != 7)
            throw new ProjectionConfigurationException(tr("Unexpected number of arguments for parameter ''towgs84'' (must be 3 or 7)"));
        List<Double> towgs84Param = new ArrayList<Double>();
        for (int i = 0; i < numStr.length; i++) {
            try {
                towgs84Param.add(Double.parseDouble(numStr[i]));
            } catch (NumberFormatException e) {
                throw new ProjectionConfigurationException(tr("Unable to parse value of parameter ''towgs84'' (''{0}'')", numStr[i]));
            }
        }
        boolean isCentric = true;
        for (int i = 0; i<towgs84Param.size(); i++) {
            if (towgs84Param.get(i) != 0.0) {
                isCentric = false;
                break;
            }
        }
        if (isCentric)
            return new CentricDatum(null, null, ellps);
        boolean is3Param = true;
        for (int i = 3; i<towgs84Param.size(); i++) {
            if (towgs84Param.get(i) != 0.0) {
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
        String id = (String) parameters.get(Param.proj.key);
        if (id == null) throw new ProjectionConfigurationException(tr("Projection required (+proj=*)"));

        Proj proj =  Projections.getBaseProjection(id);
        if (proj == null) throw new ProjectionConfigurationException(tr("Unkown projection identifier: ''{0}''", id));

        ProjParameters projParams = new ProjParameters();

        projParams.ellps = ellps;

        String s;
        s = parameters.get(Param.lat_0.key);
        if (s != null) {
            projParams.lat_0 = parseAngle(s, Param.lat_0.key);
        }
        s = parameters.get(Param.lat_1.key);
        if (s != null) {
            projParams.lat_1 = parseAngle(s, Param.lat_1.key);
        }
        s = parameters.get(Param.lat_2.key);
        if (s != null) {
            projParams.lat_2 = parseAngle(s, Param.lat_2.key);
        }
        proj.initialize(projParams);
        return proj;
    }

    public Bounds parseBounds(String boundsStr) throws ProjectionConfigurationException {
        String[] numStr = boundsStr.split(",");
        if (numStr.length != 4)
            throw new ProjectionConfigurationException(tr("Unexpected number of arguments for parameter ''+bounds'' (must be 4)"));
        return new Bounds(parseAngle(numStr[1], "minlat (+bounds)"),
                parseAngle(numStr[0], "minlon (+bounds)"),
                parseAngle(numStr[3], "maxlat (+bounds)"),
                parseAngle(numStr[2], "maxlon (+bounds)"), false);
    }

    public double parseDouble(Map<String, String> parameters, String parameterName) throws ProjectionConfigurationException {
        String doubleStr = parameters.get(parameterName);
        if (doubleStr == null && parameters.containsKey(parameterName))
            throw new ProjectionConfigurationException(
                    tr("Expected number argument for parameter ''{0}''", parameterName));
        return parseDouble(doubleStr, parameterName);
    }

    public double parseDouble(String doubleStr, String parameterName) throws ProjectionConfigurationException {
        try {
            return Double.parseDouble(doubleStr);
        } catch (NumberFormatException e) {
            throw new ProjectionConfigurationException(
                    tr("Unable to parse value ''{1}'' of parameter ''{0}'' as number.", parameterName, doubleStr));
        }
    }

    public double parseAngle(String angleStr, String parameterName) throws ProjectionConfigurationException {
        String s = angleStr;
        double value = 0;
        boolean neg = false;
        Matcher m = Pattern.compile("^-").matcher(s);
        if (m.find()) {
            neg = true;
            s = s.substring(m.end());
        }
        final String FLOAT = "(\\d+(\\.\\d*)?)";
        boolean dms = false;
        // degrees
        m = Pattern.compile("^"+FLOAT+"d").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            value += Double.parseDouble(m.group(1));
            dms = true;
        }
        // minutes
        m = Pattern.compile("^"+FLOAT+"'").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            value += Double.parseDouble(m.group(1)) / 60.0;
            dms = true;
        }
        // seconds
        m = Pattern.compile("^"+FLOAT+"\"").matcher(s);
        if (m.find()) {
            s = s.substring(m.end());
            value += Double.parseDouble(m.group(1)) / 3600.0;
            dms = true;
        }
        // plain number (in degrees)
        if (!dms) {
            m = Pattern.compile("^"+FLOAT).matcher(s);
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

    public void dump() {
        System.err.println("x_0="+x_0);
        System.err.println("y_0="+y_0);
        System.err.println("lon_0="+lon_0);
        System.err.println("k_0="+k_0);
        System.err.println("ellps="+ellps);
        System.err.println("proj="+proj);
        System.err.println("datum="+datum);
    }

    @Override
    public Integer getEpsgCode() {
        return null;
    }

    @Override
    public String toCode() {
        return "proj:" + (pref == null ? "ERROR" : pref);
    }

    @Override
    public String getCacheDirectoryName() {
        return "proj-"+Utils.md5Hex(pref == null ? "" : pref).substring(0, 4);
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        if (bounds != null) return bounds;
        return new Bounds(
            new LatLon(-90.0, -180.0),
            new LatLon(90.0, 180.0));
    }

    @Override
    public String toString() {
        return tr("Custom Projection");
    }
}
