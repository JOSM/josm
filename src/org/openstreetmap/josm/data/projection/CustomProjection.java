// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.CentricDatum;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.tools.Utils;

/**
 * Custom projection
 *
 * Inspired by PROJ.4 and Proj4J.
 */
public class CustomProjection extends AbstractProjection implements ProjectionSubPrefs {

    private String pref = "";

    public void update(String pref) {
        try {
            Map<String, String> parameters = new HashMap<String, String>();
            String[] parts = pref.trim().split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.charAt(0) != '+')
                    throw new ProjectionConfigurationException(tr("Parameter must begin with a ''+'' sign (found ''{0}'')", part));
                Matcher m = Pattern.compile("\\+([a-zA-Z0-9_]+)(=(.*))?").matcher(part);
                if (m.matches()) {
                    String key = m.group(1);
                    String value = null;
                    if (m.groupCount() >= 3) {
                        value = m.group(3);
                    }
                    parameters.put(key, value);
                } else
                    throw new ProjectionConfigurationException(tr("Unexpected parameter format (''{0}'')", part));
            }
            ellps = parseEllipsoid(parameters);
            datum = parseDatum(parameters, ellps);
            proj = parseProjection(parameters, ellps);
            String s = parameters.get("x_0");
            if (s != null) {
                this.x_0 = parseDouble(s, "x_0");
            }
            s = parameters.get("y_0");
            if (s != null) {
                this.y_0 = parseDouble(s, "y_0");
            }
            s = parameters.get("lon_0");
            if (s != null) {
                this.lon_0 = parseAngle(s, "lon_0");
            }
            s = parameters.get("k_0");
            if (s != null) {
                this.k_0 = parseDouble(s, "k_0");
            }
        } catch (ProjectionConfigurationException e) {
            System.err.println(e.toString()); // FIXME
        }
        this.pref = pref;
    }

    public Ellipsoid parseEllipsoid(Map<String, String> parameters) throws ProjectionConfigurationException {
        String code = parameters.get("ellps");
        if (code != null) {
            Ellipsoid ellipsoid = Projections.getEllipsoid(code);
            if (ellipsoid == null) {
                throw new ProjectionConfigurationException(tr("Ellipsoid ''{0}'' not supported.", code));
            } else {
                return ellipsoid;
            }
        }
        String s = parameters.get("a");
        if (s != null) {
            double a = parseDouble(s, "a");
            if (parameters.get("es") != null) {
                double es = parseDouble(parameters, "es");
                return Ellipsoid.create_a_es(a, es);
            }
            if (parameters.get("rf") != null) {
                double rf = parseDouble(parameters, "rf");
                return Ellipsoid.create_a_rf(a, rf);
            }
            if (parameters.get("f") != null) {
                double f = parseDouble(parameters, "f");
                return Ellipsoid.create_a_f(a, f);
            }
            if (parameters.get("b") != null) {
                double b = parseDouble(parameters, "b");
                return Ellipsoid.create_a_b(a, b);
            }
        }
        if (parameters.containsKey("a") ||
                parameters.containsKey("es") ||
                parameters.containsKey("rf") ||
                parameters.containsKey("f") ||
                parameters.containsKey("b"))
            throw new ProjectionConfigurationException(tr("Combination of ellipsoid parameters is not supported."));
        // nothing specified, use WGS84 as default
        return Ellipsoid.WGS84;
    }

    public Datum parseDatum(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        String towgs84 = parameters.get("towgs84");
        if (towgs84 != null)
            return parseToWGS84(towgs84, ellps);

        String datumId = parameters.get("datum");
        if (datumId != null) {
            Datum datum = Projections.getDatum(datumId);
            if (datum == null) throw new ProjectionConfigurationException(tr("Unkown datum identifier: ''{0}''", datumId));
            return datum;
        } else
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
        boolean is3Param = true;
        for (int i = 3; i<towgs84Param.size(); i++) {
            if (towgs84Param.get(i) != 0.0) {
                is3Param = false;
                break;
            }
        }
        Datum datum = null;
        if (is3Param) {
            datum = new ThreeParameterDatum(null, null, ellps,
                    towgs84Param.get(0),
                    towgs84Param.get(1),
                    towgs84Param.get(2)
            );
        } else {
            datum = new SevenParameterDatum(null, null, ellps,
                    towgs84Param.get(0),
                    towgs84Param.get(1),
                    towgs84Param.get(2),
                    towgs84Param.get(3),
                    towgs84Param.get(4),
                    towgs84Param.get(5),
                    towgs84Param.get(6)
            );
        }
        return datum;
    }

    public Proj parseProjection(Map<String, String> parameters, Ellipsoid ellps) throws ProjectionConfigurationException {
        String id = (String) parameters.get("proj");
        if (id == null) throw new ProjectionConfigurationException(tr("Projection required (+proj=*)"));

        Proj proj =  Projections.getProjection(id);
        if (proj == null) throw new ProjectionConfigurationException(tr("Unkown projection identifier: ''{0}''", id));

        ProjParameters projParams = new ProjParameters();

        projParams.ellps = ellps;

        String s;
        s = parameters.get("lat_0");
        if (s != null) {
            projParams.lat_0 = parseAngle(s, "lat_0");
        }
        s = parameters.get("lat_1");
        if (s != null) {
            projParams.lat_1 = parseAngle(s, "lat_1");
        }
        s = parameters.get("lat_2");
        if (s != null) {
            projParams.lat_2 = parseAngle(s, "lat_2");
        }
        proj.initialize(projParams);
        return proj;

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
        return Utils.md5Hex(pref).substring(0, 10);
    }

    @Override
    public String getCacheDirectoryName() {
        return toCode();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
            new LatLon(-85.05112877980659, -180.0),
            new LatLon(85.05112877980659, 180.0)); // FIXME
    }

    @Override
    public String toString() {
        return tr("Custom Projection (from PROJ.4 string)");
    }

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JTextField input = new JTextField(pref, 50);
        p.setLayout(new GridBagLayout());
        p.add(input);
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        Object prefTf = p.getComponent(0);
        if (!(prefTf instanceof JTextField))
            return null;
        String pref = ((JTextField) prefTf).getText();
        return Collections.singleton(pref);
    }

    @Override
    public void setPreferences(Collection<String> args) {
        update(args.iterator().next());
    }

    @Override
    public String[] allCodes() {
        return new String[0];
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        return null;
    }

}
