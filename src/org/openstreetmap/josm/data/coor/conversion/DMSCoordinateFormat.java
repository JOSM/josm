// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DecimalFormat;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Coordinate format that converts a coordinate to degrees, minutes and seconds.
 * @since 12735
 */
public class DMSCoordinateFormat extends AbstractCoordinateFormat {

    private static final DecimalFormat DMS_MINUTE_FORMATTER = new DecimalFormat("00");
    private static final DecimalFormat DMS_SECOND_FORMATTER = new DecimalFormat(
            Config.getPref() == null ? "00.0" : Config.getPref().get("latlon.dms.decimal-format", "00.0"));
    private static final String DMS60 = DMS_SECOND_FORMATTER.format(60.0);
    private static final String DMS00 = DMS_SECOND_FORMATTER.format(0.0);

    /**
     * The unique instance.
     */
    public static final DMSCoordinateFormat INSTANCE = new DMSCoordinateFormat();

    protected DMSCoordinateFormat() {
        super("DEGREES_MINUTES_SECONDS", tr("deg\u00B0 min'' sec\""));
    }

    @Override
    public String latToString(ILatLon ll) {
        return degreesMinutesSeconds(ll.lat()) + ((ll.lat() < 0) ? SOUTH : NORTH);
    }

    @Override
    public String lonToString(ILatLon ll) {
       return degreesMinutesSeconds(ll.lon()) + ((ll.lon() < 0) ? WEST : EAST);
    }

    /**
     * Replies the coordinate in degrees/minutes/seconds format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes/seconds format
     * @since 12561
     */
    public static String degreesMinutesSeconds(double pCoordinate) {

        double tAbsCoord = Math.abs(pCoordinate);
        int tDegree = (int) tAbsCoord;
        double tTmpMinutes = (tAbsCoord - tDegree) * 60;
        int tMinutes = (int) tTmpMinutes;
        double tSeconds = (tTmpMinutes - tMinutes) * 60;

        String sDegrees = Integer.toString(tDegree);
        String sMinutes = DMS_MINUTE_FORMATTER.format(tMinutes);
        String sSeconds = DMS_SECOND_FORMATTER.format(tSeconds);

        if (DMS60.equals(sSeconds)) {
            sSeconds = DMS00;
            sMinutes = DMS_MINUTE_FORMATTER.format(tMinutes+1L);
        }
        if ("60".equals(sMinutes)) {
            sMinutes = "00";
            sDegrees = Integer.toString(tDegree+1);
        }

        return sDegrees + '\u00B0' + sMinutes + '\'' + sSeconds + '\"';
    }

}
