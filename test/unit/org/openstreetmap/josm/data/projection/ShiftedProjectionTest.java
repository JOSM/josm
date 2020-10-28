// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Tests for {@link ShiftedProjecting}
 * @author Michael Zangl
 */
class ShiftedProjectionTest {
    private static final class ProjectingBase implements Projecting {
        @Override
        public EastNorth latlon2eastNorth(ILatLon ll) {
            return new EastNorth(ll.lat() * 2, ll.lon() * 3);
        }

        @Override
        public Map<ProjectionBounds, Projecting> getProjectingsForArea(ProjectionBounds area) {
            HashMap<ProjectionBounds, Projecting> map = new HashMap<>();
            // split at east = 0
            if (area.minEast < 0) {
                map.put(new ProjectionBounds(area.minEast, area.minNorth, Math.min(area.maxEast, 0), area.maxNorth), this);
            }
            if (area.maxEast > 0) {
                map.put(new ProjectionBounds(Math.max(area.minEast, 0), area.minNorth, area.maxEast, area.maxNorth), this);
            }

            return map;
        }

        @Override
        public Projection getBaseProjection() {
            throw new AssertionError();
        }

        @Override
        public LatLon eastNorth2latlonClamped(EastNorth en) {
            return new LatLon(en.east() / 2, en.north() / 3);
        }
    }

    /**
     * Test {@link ShiftedProjecting#latlon2eastNorth(ILatLon)}
     */
    @Test
    void testLatlon2eastNorth() {
        Projecting base = new ProjectingBase();

        ShiftedProjecting unshifted = new ShiftedProjecting(base, new EastNorth(0, 0));
        EastNorth unshift_00 = unshifted.latlon2eastNorth(new LatLon(0, 0));
        assertEquals(0, unshift_00.east(), 1e-10);
        assertEquals(0, unshift_00.north(), 1e-10);
        EastNorth unshift_12 = unshifted.latlon2eastNorth(new LatLon(1, 2));
        assertEquals(2, unshift_12.east(), 1e-10);
        assertEquals(6, unshift_12.north(), 1e-10);

        ShiftedProjecting shifted = new ShiftedProjecting(base, new EastNorth(5, 7));
        EastNorth shift_00 = shifted.latlon2eastNorth(new LatLon(0, 0));
        assertEquals(5, shift_00.east(), 1e-10);
        assertEquals(7, shift_00.north(), 1e-10);
        EastNorth shift_12 = shifted.latlon2eastNorth(new LatLon(1, 2));
        assertEquals(2 + 5, shift_12.east(), 1e-10);
        assertEquals(6 + 7, shift_12.north(), 1e-10);
    }

    /**
     * Test {@link ShiftedProjecting#eastNorth2latlonClamped(EastNorth)}
     */
    @Test
    void testEastNorth2latlonClamped() {
        Projecting base = new ProjectingBase();

        ShiftedProjecting unshifted = new ShiftedProjecting(base, new EastNorth(0, 0));
        LatLon unshift_00 = unshifted.eastNorth2latlonClamped(new EastNorth(0, 0));
        assertEquals(0, unshift_00.lat(), 1e-10);
        assertEquals(0, unshift_00.lon(), 1e-10);
        LatLon unshift_12 = unshifted.eastNorth2latlonClamped(new EastNorth(2, 6));
        assertEquals(1, unshift_12.lat(), 1e-10);
        assertEquals(2, unshift_12.lon(), 1e-10);

        ShiftedProjecting shifted = new ShiftedProjecting(base, new EastNorth(5, 7));
        LatLon shift_00 = shifted.eastNorth2latlonClamped(new EastNorth(5, 7));
        assertEquals(0, shift_00.lat(), 1e-10);
        assertEquals(0, shift_00.lon(), 1e-10);
        LatLon shift_12 = shifted.eastNorth2latlonClamped(new EastNorth(2 + 5, 6 + 7));
        assertEquals(1, shift_12.lat(), 1e-10);
        assertEquals(2, shift_12.lon(), 1e-10);
    }

    /**
     * Test {@link ShiftedProjecting#getProjectingsForArea(ProjectionBounds)}, single area case
     */
    @Test
    void testGetProjectingsForArea() {
        Projecting base = new ProjectingBase();
        ShiftedProjecting shifted = new ShiftedProjecting(base, new EastNorth(5, 7));

        ProjectionBounds area = new ProjectionBounds(10, 0, 20, 20);

        Map<ProjectionBounds, Projecting> areas = shifted.getProjectingsForArea(area);
        assertEquals(1, areas.size());
        ProjectionBounds pb = areas.keySet().iterator().next();
        assertEquals(area.minEast, pb.minEast, 1e-7);
        assertEquals(area.maxEast, pb.maxEast, 1e-7);
        assertEquals(area.minNorth, pb.minNorth, 1e-7);
        assertEquals(area.maxNorth, pb.maxNorth, 1e-7);
    }

    /**
     * Test {@link ShiftedProjecting#getProjectingsForArea(ProjectionBounds)}, multiple area case
     */
    @Test
    void testGetProjectingsForAreaMultiple() {
        Projecting base = new ProjectingBase();
        ShiftedProjecting shifted = new ShiftedProjecting(base, new EastNorth(5, 7));

        ProjectionBounds area = new ProjectionBounds(-10, 0, 20, 20);

        // breach is at:
        EastNorth breachAt = shifted.latlon2eastNorth(base.eastNorth2latlonClamped(new EastNorth(0, 0)));
        assertEquals(5, breachAt.east(), 1e-7);

        Map<ProjectionBounds, Projecting> areas = shifted.getProjectingsForArea(area);
        assertEquals(2, areas.size());
        List<Entry<ProjectionBounds, Projecting>> entries = areas.entrySet().stream()
                .sorted(Comparator.comparingDouble(b -> b.getKey().minEast)).collect(Collectors.toList());
        assertEquals(area.minEast, entries.get(0).getKey().minEast, 1e-7);
        assertEquals(5, entries.get(0).getKey().maxEast, 1e-7);
        assertEquals(area.minNorth, entries.get(0).getKey().minNorth, 1e-7);
        assertEquals(area.maxNorth, entries.get(0).getKey().maxNorth, 1e-7);
        assertEquals(5, entries.get(1).getKey().minEast, 1e-7);
        assertEquals(area.maxEast, entries.get(1).getKey().maxEast, 1e-7);
        assertEquals(area.minNorth, entries.get(1).getKey().minNorth, 1e-7);
        assertEquals(area.maxNorth, entries.get(1).getKey().maxNorth, 1e-7);
    }
}
