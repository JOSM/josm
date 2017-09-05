// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.ILatLon;

/**
 * Coordinate format that projects a coordinate and returns northing and easting in
 * decimal format.
 * @since 12735
 */
public class ProjectedCoordinateFormat extends AbstractCoordinateFormat {

    public static ProjectedCoordinateFormat INSTANCE = new ProjectedCoordinateFormat();

    protected ProjectedCoordinateFormat() {
        super("EAST_NORTH", tr("Projected Coordinates"));
    }

    @Override
    public String latToString(ILatLon ll) {
        return cDdFormatter.format(ll.getEastNorth(Main.getProjection()).north());
    }

    @Override
    public String lonToString(ILatLon ll) {
        return cDdFormatter.format(ll.getEastNorth(Main.getProjection()).east());
    }
}
