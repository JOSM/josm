// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.DMSCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.NauticalCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ProjectedCoordinateFormat;

/**
 * An enumeration  of coordinate formats
 * @since 1990
 * @deprecated use {@link CoordinateFormatManager}
 */
@Deprecated
public enum CoordinateFormat {

    /**
     * the decimal format 999.999
     */
    DECIMAL_DEGREES(tr("Decimal Degrees"), DecimalDegreesCoordinateFormat.INSTANCE),

    /**
     * the degrees/minutes/seconds format 9 deg 99 min 99 sec
     */
    DEGREES_MINUTES_SECONDS(tr("deg\u00B0 min'' sec\""), DMSCoordinateFormat.INSTANCE),

    /**
     * the nautical format
     */
    NAUTICAL(tr("deg\u00B0 min'' (Nautical)"), NauticalCoordinateFormat.INSTANCE),

    /**
     * coordinates East/North
     */
    EAST_NORTH(tr("Projected Coordinates"), ProjectedCoordinateFormat.INSTANCE);

    private final String displayName;
    private final ICoordinateFormat migration;

    CoordinateFormat(String displayName, ICoordinateFormat migration) {
        this.displayName = displayName;
        this.migration = migration;
    }

    /**
     * Replies the display name of the format
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the corresponding {@link ICoordinateFormat} instance for
     * migration.
     * @return the corresponding {@link ICoordinateFormat} instance for
     * migration
     */
    public ICoordinateFormat getICoordinateFormat() {
        return migration;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    private static volatile CoordinateFormat defaultCoordinateFormat = DECIMAL_DEGREES;

    /**
     * Replies the default coordinate format to be use
     *
     * @return the default coordinate format
     */
    public static CoordinateFormat getDefaultFormat() {
        return defaultCoordinateFormat;
    }

    /**
     * Sets the default coordinate format
     *
     * @param format the default coordinate format
     */
    public static void setCoordinateFormat(CoordinateFormat format) {
        if (format != null) {
            defaultCoordinateFormat = format;
        }
    }
}
