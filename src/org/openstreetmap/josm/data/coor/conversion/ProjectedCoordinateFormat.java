// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

/**
 * Coordinate format that projects a coordinate and returns northing and easting in
 * decimal format.
 * @since 12735
 */
public class ProjectedCoordinateFormat extends AbstractCoordinateFormat {

    /**
     * The unique instance.
     */
    public static final ProjectedCoordinateFormat INSTANCE = new ProjectedCoordinateFormat();

    protected ProjectedCoordinateFormat() {
        super("EAST_NORTH", tr("Projected Coordinates"));
    }

    @Override
    public String latToString(ILatLon ll) {
        return cDdFormatter.format(ll.getEastNorth(ProjectionRegistry.getProjection()).north());
    }

    @Override
    public String lonToString(ILatLon ll) {
        return cDdFormatter.format(ll.getEastNorth(ProjectionRegistry.getProjection()).east());
    }
}
