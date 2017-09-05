// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DecimalFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.ILatLon;

/**
 * Coordinate format that converts a coordinate to degrees and minutes (nautical format).
 * @since 12735
 */
public class NauticalCoordinateFormat extends AbstractCoordinateFormat {
    private static final DecimalFormat DM_MINUTE_FORMATTER = new DecimalFormat(
            Main.pref == null ? "00.000" : Main.pref.get("latlon.dm.decimal-format", "00.000"));
    private static final String DM60 = DM_MINUTE_FORMATTER.format(60.0);
    private static final String DM00 = DM_MINUTE_FORMATTER.format(0.0);

    public static final NauticalCoordinateFormat INSTANCE = new NauticalCoordinateFormat();

    protected NauticalCoordinateFormat() {
        super("NAUTICAL", tr("deg\u00B0 min'' (Nautical)"));
    }

    @Override
    public String latToString(ILatLon ll) {
        return degreesMinutes(ll.lat()) + ((ll.lat() < 0) ? SOUTH : NORTH);
    }

    @Override
    public String lonToString(ILatLon ll) {
        return degreesMinutes(ll.lon()) + ((ll.lon() < 0) ? WEST : EAST);
    }

    /**
     * Replies the coordinate in degrees/minutes format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes format
     * @since 12537
     */
    public static String degreesMinutes(double pCoordinate) {

        double tAbsCoord = Math.abs(pCoordinate);
        int tDegree = (int) tAbsCoord;
        double tMinutes = (tAbsCoord - tDegree) * 60;

        String sDegrees = Integer.toString(tDegree);
        String sMinutes = DM_MINUTE_FORMATTER.format(tMinutes);

        if (sMinutes.equals(DM60)) {
            sMinutes = DM00;
            sDegrees = Integer.toString(tDegree+1);
        }

        return sDegrees + '\u00B0' + sMinutes + '\'';
    }
}
