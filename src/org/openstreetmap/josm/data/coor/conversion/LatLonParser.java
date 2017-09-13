// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Support for parsing a {@link LatLon} object from a string.
 * @since 12792
 */
public final class LatLonParser {

    /** Character denoting South, as string */
    public static final String SOUTH = trc("compass", "S");
    /** Character denoting North, as string */
    public static final String NORTH = trc("compass", "N");
    /** Character denoting West, as string */
    public static final String WEST = trc("compass", "W");
    /** Character denoting East, as string */
    public static final String EAST = trc("compass", "E");

    private static final char N_TR = NORTH.charAt(0);
    private static final char S_TR = SOUTH.charAt(0);
    private static final char E_TR = EAST.charAt(0);
    private static final char W_TR = WEST.charAt(0);

    private static final String DEG = "\u00B0";
    private static final String MIN = "\u2032";
    private static final String SEC = "\u2033";

    private static final Pattern P = Pattern.compile(
            "([+|-]?\\d+[.,]\\d+)|"             // (1)
            + "([+|-]?\\d+)|"                   // (2)
            + "("+DEG+"|o|deg)|"                // (3)
            + "('|"+MIN+"|min)|"                // (4)
            + "(\"|"+SEC+"|sec)|"               // (5)
            + "(,|;)|"                          // (6)
            + "([NSEW"+N_TR+S_TR+E_TR+W_TR+"])|"// (7)
            + "\\s+|"
            + "(.+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern P_XML = Pattern.compile(
            "lat=[\"']([+|-]?\\d+[.,]\\d+)[\"']\\s+lon=[\"']([+|-]?\\d+[.,]\\d+)[\"']");

    private static class LatLonHolder {
        private double lat = Double.NaN;
        private double lon = Double.NaN;
    }

    private LatLonParser() {
        // private constructor
    }

    /**
     * Parses the given string as lat/lon.
     * @param coord String to parse
     * @return parsed lat/lon
     * @since 12792 (moved from {@link LatLon}, there since 11045)
     */
    public static LatLon parse(String coord) {
        final LatLonHolder latLon = new LatLonHolder();
        final Matcher mXml = P_XML.matcher(coord);
        if (mXml.matches()) {
            setLatLonObj(latLon,
                    Double.valueOf(mXml.group(1).replace(',', '.')), 0.0, 0.0, "N",
                    Double.valueOf(mXml.group(2).replace(',', '.')), 0.0, 0.0, "E");
        } else {
            final Matcher m = P.matcher(coord);

            final StringBuilder sb = new StringBuilder();
            final List<Object> list = new ArrayList<>();

            while (m.find()) {
                if (m.group(1) != null) {
                    sb.append('R');     // floating point number
                    list.add(Double.valueOf(m.group(1).replace(',', '.')));
                } else if (m.group(2) != null) {
                    sb.append('Z');     // integer number
                    list.add(Double.valueOf(m.group(2)));
                } else if (m.group(3) != null) {
                    sb.append('o');     // degree sign
                } else if (m.group(4) != null) {
                    sb.append('\'');    // seconds sign
                } else if (m.group(5) != null) {
                    sb.append('"');     // minutes sign
                } else if (m.group(6) != null) {
                    sb.append(',');     // separator
                } else if (m.group(7) != null) {
                    sb.append('x');     // cardinal direction
                    String c = m.group(7).toUpperCase(Locale.ENGLISH);
                    if ("N".equalsIgnoreCase(c) || "S".equalsIgnoreCase(c) || "E".equalsIgnoreCase(c) || "W".equalsIgnoreCase(c)) {
                        list.add(c);
                    } else {
                        list.add(c.replace(N_TR, 'N').replace(S_TR, 'S')
                                  .replace(E_TR, 'E').replace(W_TR, 'W'));
                    }
                } else if (m.group(8) != null) {
                    throw new IllegalArgumentException("invalid token: " + m.group(8));
                }
            }

            final String pattern = sb.toString();

            final Object[] params = list.toArray();

            if (pattern.matches("Ro?,?Ro?")) {
                setLatLonObj(latLon,
                        params[0], 0.0, 0.0, "N",
                        params[1], 0.0, 0.0, "E");
            } else if (pattern.matches("xRo?,?xRo?")) {
                setLatLonObj(latLon,
                        params[1], 0.0, 0.0, params[0],
                        params[3], 0.0, 0.0, params[2]);
            } else if (pattern.matches("Ro?x,?Ro?x")) {
                setLatLonObj(latLon,
                        params[0], 0.0, 0.0, params[1],
                        params[2], 0.0, 0.0, params[3]);
            } else if (pattern.matches("Zo[RZ]'?,?Zo[RZ]'?|Z[RZ],?Z[RZ]")) {
                setLatLonObj(latLon,
                        params[0], params[1], 0.0, "N",
                        params[2], params[3], 0.0, "E");
            } else if (pattern.matches("xZo[RZ]'?,?xZo[RZ]'?|xZo?[RZ],?xZo?[RZ]")) {
                setLatLonObj(latLon,
                        params[1], params[2], 0.0, params[0],
                        params[4], params[5], 0.0, params[3]);
            } else if (pattern.matches("Zo[RZ]'?x,?Zo[RZ]'?x|Zo?[RZ]x,?Zo?[RZ]x")) {
                setLatLonObj(latLon,
                        params[0], params[1], 0.0, params[2],
                        params[3], params[4], 0.0, params[5]);
            } else if (pattern.matches("ZoZ'[RZ]\"?x,?ZoZ'[RZ]\"?x|ZZ[RZ]x,?ZZ[RZ]x")) {
                setLatLonObj(latLon,
                        params[0], params[1], params[2], params[3],
                        params[4], params[5], params[6], params[7]);
            } else if (pattern.matches("xZoZ'[RZ]\"?,?xZoZ'[RZ]\"?|xZZ[RZ],?xZZ[RZ]")) {
                setLatLonObj(latLon,
                        params[1], params[2], params[3], params[0],
                        params[5], params[6], params[7], params[4]);
            } else if (pattern.matches("ZZ[RZ],?ZZ[RZ]")) {
                setLatLonObj(latLon,
                        params[0], params[1], params[2], "N",
                        params[3], params[4], params[5], "E");
            } else {
                throw new IllegalArgumentException("invalid format: " + pattern);
            }
        }

        return new LatLon(latLon.lat, latLon.lon);
    }

    private static void setLatLonObj(final LatLonHolder latLon,
            final Object coord1deg, final Object coord1min, final Object coord1sec, final Object card1,
            final Object coord2deg, final Object coord2min, final Object coord2sec, final Object card2) {

        setLatLon(latLon,
                (Double) coord1deg, (Double) coord1min, (Double) coord1sec, (String) card1,
                (Double) coord2deg, (Double) coord2min, (Double) coord2sec, (String) card2);
    }

    private static void setLatLon(final LatLonHolder latLon,
            final double coord1deg, final double coord1min, final double coord1sec, final String card1,
            final double coord2deg, final double coord2min, final double coord2sec, final String card2) {

        setLatLon(latLon, coord1deg, coord1min, coord1sec, card1);
        setLatLon(latLon, coord2deg, coord2min, coord2sec, card2);
        if (Double.isNaN(latLon.lat) || Double.isNaN(latLon.lon)) {
            throw new IllegalArgumentException("invalid lat/lon parameters");
        }
    }

    private static void setLatLon(final LatLonHolder latLon, final double coordDeg, final double coordMin, final double coordSec,
            final String card) {
        if (coordDeg < -180 || coordDeg > 180 || coordMin < 0 || coordMin >= 60 || coordSec < 0 || coordSec > 60) {
            throw new IllegalArgumentException("out of range");
        }

        double coord = (coordDeg < 0 ? -1 : 1) * (Math.abs(coordDeg) + coordMin / 60 + coordSec / 3600);
        coord = "N".equals(card) || "E".equals(card) ? coord : -coord;
        if ("N".equals(card) || "S".equals(card)) {
            latLon.lat = coord;
        } else {
            latLon.lon = coord;
        }
    }

    /**
     * Parse string coordinate from floating point or DMS format.
     * @param angleStr the string to parse as coordinate e.g. -1.1 or 50d10'3"W
     * @return the value, in degrees
     * @throws IllegalArgumentException in case parsing fails
     * @since 12792
     */
    public static double parseCoordinate(String angleStr) {
        final String floatPattern = "(\\d+(\\.\\d*)?)";
        // pattern does all error handling.
        Matcher in = Pattern.compile("^(?<neg1>-)?"
                + "(?=\\d)(?:(?<single>" + floatPattern + ")|"
                + "((?<degree>" + floatPattern + ")d)?"
                + "((?<minutes>" + floatPattern + ")\')?"
                + "((?<seconds>" + floatPattern + ")\")?)"
                + "(?:[NE]|(?<neg2>[SW]))?$").matcher(angleStr);

        if (!in.find()) {
            throw new IllegalArgumentException(
                    tr("Unable to parse as coordinate value: ''{0}''", angleStr));
        }

        double value = 0;
        if (in.group("single") != null) {
            value += Double.parseDouble(in.group("single"));
        }
        if (in.group("degree") != null) {
            value += Double.parseDouble(in.group("degree"));
        }
        if (in.group("minutes") != null) {
            value += Double.parseDouble(in.group("minutes")) / 60;
        }
        if (in.group("seconds") != null) {
            value += Double.parseDouble(in.group("seconds")) / 3600;
        }

        if (in.group("neg1") != null ^ in.group("neg2") != null) {
            value = -value;
        }
        return value;
    }

}
