// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * An enumeration  of coordinate formats
 *
 */
public enum CoordinateFormat {

    /**
     * the decimal format 999.999
     */
    DECIMAL_DEGREES (tr("Decimal Degrees")),

    /**
     * the minutes/seconds format 99" 99'
     */
    DEGREES_MINUTES_SECONDS (tr("Degrees Minutes Seconds"));

    private String displayName;
    private CoordinateFormat(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Replies the display name of the format
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    private static CoordinateFormat defaultCoordinateFormat = DECIMAL_DEGREES;

    /**
     * Replies the default coordinate format to be use
     *
     * @return the default coordinate format
     */
    static public CoordinateFormat getDefaultFormat() {
        return defaultCoordinateFormat;
    }

    /**
     * Sets the default coordinate format
     *
     * @param format the default coordinate format
     */
    static public void setCoordinateFormat(CoordinateFormat format) {
        if (format != null) {
            defaultCoordinateFormat = format;
        }
    }
}
