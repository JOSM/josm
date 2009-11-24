// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

/**
 * PointInTimeType enumerates two points in time in the {@see History} of an {@see OsmPrimitive}.
 * @author karl
 *
 */
public enum PointInTimeType {
    /** the point in time selected as reference point when comparing two version */
    REFERENCE_POINT_IN_TIME,

    /** the point in time selected as current point when comparing two version */
    CURRENT_POINT_IN_TIME;

    public PointInTimeType opposite() {
        if (this.equals(REFERENCE_POINT_IN_TIME))
            return CURRENT_POINT_IN_TIME;
        else
            return REFERENCE_POINT_IN_TIME;
    }
}
