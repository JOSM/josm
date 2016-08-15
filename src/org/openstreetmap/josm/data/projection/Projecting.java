// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Map;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Classes implementing this are able to project between screen (east/north) and {@link LatLon} coordinates.
 * <p>
 * Each instance is backed by a base projection but may e.g. offset the resulting position.
 * @author Michael Zangl
 * @since 10805
 */
public interface Projecting {

    /**
     * Convert from lat/lon to easting/northing.
     *
     * @param ll the geographical point to convert (in WGS84 lat/lon)
     * @return the corresponding east/north coordinates
     */
    EastNorth latlon2eastNorth(LatLon ll);

    /**
     * Convert a east/north coordinate to the {@link LatLon} coordinate.
     * This method clamps the lat/lon coordinate to the nearest point in the world bounds.
     * @param en east/north
     * @return The lat/lon coordinate.
     */
    LatLon eastNorth2latlonClamped(EastNorth en);

    /**
     * Gets the base projection instance used.
     * @return The projection.
     */
    Projection getBaseProjection();

    /**
     * Returns an map or (subarea, projecting) paris that contains projecting instances to convert the coordinates inside the given area.
     * This can be used by projections to support continuous projections.
     *
     * It is possible that the area covered by the map is bigger than the one given as area. There may be holes.
     * @param area The base area
     * @return a map of non-overlapping {@link ProjectionBounds} instances mapped to the {@link Projecting} object to use for that area.
     */
    Map<ProjectionBounds, Projecting> getProjectingsForArea(ProjectionBounds area);
}
