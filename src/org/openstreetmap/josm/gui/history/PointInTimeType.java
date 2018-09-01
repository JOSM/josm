// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

/**
 * PointInTimeType enumerates two points in time in the {@link org.openstreetmap.josm.data.osm.history.History}
 * of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive}.
 * @author karl
 */
public enum PointInTimeType {
    /** the point in time selected as reference point when comparing two version */
    REFERENCE_POINT_IN_TIME,

    /** the point in time selected as current point when comparing two version */
    CURRENT_POINT_IN_TIME;

    /**
     * Returns the opposite point in time.
     * @return the opposite point in time
     */
    public PointInTimeType opposite() {
        if (this == REFERENCE_POINT_IN_TIME)
            return CURRENT_POINT_IN_TIME;
        else
            return REFERENCE_POINT_IN_TIME;
    }
}
