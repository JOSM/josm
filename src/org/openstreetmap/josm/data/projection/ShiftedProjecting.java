// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * This is a projecting instance that shifts the projection by a given eastnorth offset.
 * @author Michael Zangl
 * @since 10805
 */
public class ShiftedProjecting implements Projecting {
    private final Projecting base;
    private final EastNorth offset;

    /**
     * Create a new {@link ShiftedProjecting}
     * @param base The base to use
     * @param offset The offset to move base. Subtracted when converting lat/lon-&gt;east/north.
     */
    public ShiftedProjecting(Projecting base, EastNorth offset) {
        this.base = base;
        this.offset = offset;
    }

    @Override
    public EastNorth latlon2eastNorth(ILatLon ll) {
        return base.latlon2eastNorth(ll).add(offset);
    }

    @Override
    public LatLon eastNorth2latlonClamped(EastNorth en) {
        return base.eastNorth2latlonClamped(en.subtract(offset));
    }

    @Override
    public Projection getBaseProjection() {
        return base.getBaseProjection();
    }

    @Override
    public Map<ProjectionBounds, Projecting> getProjectingsForArea(ProjectionBounds area) {
        Map<ProjectionBounds, Projecting> forArea = base
                .getProjectingsForArea(new ProjectionBounds(area.getMin().subtract(offset), area.getMax().subtract(offset)));
        HashMap<ProjectionBounds, Projecting> ret = new HashMap<>();
        forArea.forEach((pb, projecting) -> ret.put(
                new ProjectionBounds(pb.getMin().add(offset), pb.getMax().add(offset)),
                new ShiftedProjecting(projecting, offset)));
        return ret;
    }
}
