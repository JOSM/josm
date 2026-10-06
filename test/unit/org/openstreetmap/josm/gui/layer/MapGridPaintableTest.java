// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.draw.BlendComposite;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.GridType;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.LatLonGridLine;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.ProjectedGridLine;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link MapGridPaintable}.
 */
@Main
@Projection
class MapGridPaintableTest {

    private static long count(List<ProjectedGridLine> lines, boolean vertical) {
        return lines.stream().filter(l -> vertical == (Math.abs(l.start.east() - l.end.east()) < 1e-9))
                .peek(l -> assertEquals(vertical, l.constantEast, "wrong direction flag: " + l)).count();
    }

    /**
     * An axis aligned grid covers the area with lines at multiples of the spacing.
     */
    @Test
    void testProjectedGridAxisAligned() {
        ProjectionBounds area = new ProjectionBounds(new EastNorth(-250, -120), new EastNorth(1010, 380));
        List<ProjectedGridLine> lines = MapGridPaintable.getProjectedGridLines(area, 100, 50, 0, 0, 0);
        // vertical lines at -200 .. 1000 (13), horizontal at -100 .. 350 (10)
        assertEquals(13, count(lines, true));
        assertEquals(10, count(lines, false));
        assertEquals(23, lines.size());
        for (ProjectedGridLine line : lines) {
            if (line.constantEast) {
                assertEquals(0, line.start.east() % 100, 1e-9, "vertical line not on the grid: " + line);
                assertEquals(area.minNorth, Math.min(line.start.north(), line.end.north()), 1e-9);
                assertEquals(area.maxNorth, Math.max(line.start.north(), line.end.north()), 1e-9);
            } else {
                assertEquals(0, line.start.north() % 50, 1e-9, "horizontal line not on the grid: " + line);
                assertEquals(area.minEast, Math.min(line.start.east(), line.end.east()), 1e-9);
                assertEquals(area.maxEast, Math.max(line.start.east(), line.end.east()), 1e-9);
            }
        }
    }

    /**
     * The origin shifts the grid, the rotation turns it around the origin.
     */
    @Test
    void testProjectedGridOriginAndRotation() {
        ProjectionBounds area = new ProjectionBounds(new EastNorth(0, 0), new EastNorth(100, 100));
        List<ProjectedGridLine> lines = MapGridPaintable.getProjectedGridLines(area, 30, 30, 0, 5, 10);
        // vertical lines at 5, 35, 65, 95; horizontal at 10, 40, 70, 100
        assertEquals(4, count(lines, true));
        assertEquals(4, count(lines, false));
        assertTrue(lines.stream().anyMatch(l -> Math.abs(l.start.east() - 95) < 1e-9 && Math.abs(l.end.east() - 95) < 1e-9));
        assertTrue(lines.stream().anyMatch(l -> Math.abs(l.start.north() - 100) < 1e-9 && Math.abs(l.end.north() - 100) < 1e-9));

        lines = MapGridPaintable.getProjectedGridLines(area, 30, 30, 45, 0, 0);
        assertTrue(lines.size() > 4, lines.toString());
        for (ProjectedGridLine line : lines) {
            double dx = line.end.east() - line.start.east();
            double dy = line.end.north() - line.start.north();
            // every line runs at +45° or -45°
            assertEquals(Math.abs(dx), Math.abs(dy), 1e-6, "line not rotated by 45°: " + line);
            // the lines pass through the grid points (u, v) = (k*30, m*30) rotated by 45°: check the distance of the
            // origin to the line is a multiple of 30
            double len = Math.hypot(dx, dy);
            double dist = Math.abs(dx * (0 - line.start.north()) - dy * (0 - line.start.east())) / len;
            assertEquals(0, dist % 30 < 1e-6 ? 0 : Math.abs(dist % 30 - 30), 1e-6, "line not on the rotated grid: " + dist);
        }
    }

    /**
     * Invalid spacings and huge line counts yield nothing.
     */
    @Test
    void testProjectedGridLimits() {
        ProjectionBounds area = new ProjectionBounds(new EastNorth(0, 0), new EastNorth(100, 100));
        assertTrue(MapGridPaintable.getProjectedGridLines(area, 0, 10, 0, 0, 0).isEmpty());
        assertTrue(MapGridPaintable.getProjectedGridLines(area, 10, -1, 0, 0, 0).isEmpty());
        assertTrue(MapGridPaintable.getProjectedGridLines(area, 0.01, 0.01, 0, 0, 0).isEmpty());
    }

    /**
     * Latitude/longitude lines: multiples of the spacing (shifted by the origin), clamped to the world, curved
     * lines with the requested number of segments.
     */
    @Test
    void testLatLonGrid() {
        Bounds world = new Bounds(-85, -180, 85, 180);
        Bounds area = new Bounds(50.2, 7.9, 52.1, 10.3);
        List<LatLonGridLine> gridLines = MapGridPaintable.getLatLonGridLines(area, world, 1, 0.5, 0, 0, 4);
        // meridians 8, 9, 10; parallels 50.5, 51, 51.5, 52
        assertEquals(7, gridLines.size());
        assertEquals(3, gridLines.stream().filter(l -> l.meridian).count());
        for (LatLonGridLine gridLine : gridLines) {
            List<LatLon> line = gridLine.points;
            assertEquals(5, line.size());
            assertEquals(gridLine.meridian, line.get(0).lon() == line.get(4).lon());
            if (gridLine.meridian) {
                assertEquals(0, line.get(0).lon() % 1, 1e-9);
                assertEquals(50.2, line.get(0).lat(), 1e-9);
                assertEquals(52.1, line.get(4).lat(), 1e-9);
            } else {
                assertEquals(0, line.get(0).lat() % 0.5, 1e-9);
                assertEquals(7.9, line.get(0).lon(), 1e-9);
                assertEquals(10.3, line.get(4).lon(), 1e-9);
            }
        }
        // origin offset
        List<List<LatLon>> lines = points(MapGridPaintable.getLatLonGridLines(area, world, 1, 1, 0.5, 0.25, 1));
        assertTrue(lines.stream().anyMatch(l -> l.get(0).lon() == 8.5 && l.get(1).lon() == 8.5), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.get(0).lat() == 51.25 && l.get(1).lat() == 51.25), lines.toString());

        // clamped to the world bounds: no parallel at 90
        lines = points(MapGridPaintable.getLatLonGridLines(new Bounds(80, 0, 89.9, 10), world, 10, 10, 0, 0, 1));
        assertTrue(lines.stream().noneMatch(l -> l.get(0).lat() > 85), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.get(0).lat() == 80 && l.get(1).lat() == 80), lines.toString());

        // across the antimeridian: meridians 170 .. 180, -170; longitudes stay in [-180, 180]
        gridLines = MapGridPaintable.getLatLonGridLines(new Bounds(0, 165, 10, -165), world, 10, 90, 0, 0, 1);
        assertEquals(3, gridLines.stream().filter(l -> l.meridian).count(), gridLines.toString());
        assertTrue(gridLines.stream().allMatch(l -> l.points.stream().allMatch(ll -> ll.lon() >= -180 && ll.lon() <= 180)));
    }

    /**
     * A projection which does not span the whole globe clamps the meridians to its longitude range, the same way
     * the parallels are clamped to its latitude range.
     */
    @Test
    void testLatLonGridLimitedWorld() {
        // a projection valid for 6 degrees of longitude only, e.g. a UTM zone
        Bounds world = new Bounds(0, 6, 84, 12);
        Bounds area = new Bounds(45, 2, 50, 16);
        List<LatLonGridLine> lines = MapGridPaintable.getLatLonGridLines(area, world, 2, 2, 0, 0, 1);
        assertTrue(lines.stream().filter(l -> l.meridian).findAny().isPresent(), lines.toString());
        assertTrue(lines.stream().filter(l -> l.meridian)
                .allMatch(l -> l.points.get(0).lon() >= 6 && l.points.get(0).lon() <= 12), lines.toString());
        // meridians 6, 8, 10, 12 are inside the projection, 2, 4, 14, 16 are not
        assertEquals(4, lines.stream().filter(l -> l.meridian).count(), lines.toString());

        // a world spanning all longitudes keeps every meridian, including across the antimeridian
        Bounds whole = new Bounds(-85, -180, 85, 180);
        assertEquals(8, MapGridPaintable.getLatLonGridLines(area, whole, 2, 2, 0, 0, 1)
                .stream().filter(l -> l.meridian).count());
    }

    /**
     * A parallel which crosses the antimeridian is split in two, one line on each side of it. Non-regression
     * test: wrapping the longitudes of a single polyline instead made it jump right across the view.
     */
    @Test
    void testLatLonGridAcrossAntimeridian() {
        Bounds world = new Bounds(-85, -180, 85, 180);
        List<LatLonGridLine> lines = MapGridPaintable.getLatLonGridLines(new Bounds(0, 165, 10, -165), world, 10, 90, 0, 0, 4);
        List<List<LatLon>> parallels = points(lines.stream().filter(l -> !l.meridian).collect(Collectors.toList()));
        // the parallel at latitude 0 runs from 165 to 180 and from -180 to -165
        assertEquals(2, parallels.size(), parallels.toString());
        assertTrue(parallels.stream().anyMatch(l -> l.get(0).lon() == 165 && l.get(4).lon() == 180), parallels.toString());
        assertTrue(parallels.stream().anyMatch(l -> l.get(0).lon() == -180 && l.get(4).lon() == -165), parallels.toString());
        for (List<LatLon> line : parallels) {
            assertEquals(0, line.get(0).lat(), 1e-9);
            for (int i = 1; i < line.size(); i++) {
                // no jump: every step is a quarter of the 15 degrees the line spans
                assertEquals(3.75, line.get(i).lon() - line.get(i - 1).lon(), 1e-9, line.toString());
            }
        }
    }

    /**
     * The visible bounds cover the whole world once the view is larger than it. Non-regression test: the corners
     * of such a view lie outside the world, so their longitude wraps around and simply converting them - as
     * {@link MapView#getRealBounds()} does - yields a far too narrow range which jumps around while zooming.
     */
    @Test
    void testVisibleLatLonBoundsAtWorldZoom() {
        SizedMapView mv = new SizedMapView();
        mv.setBounds(new Rectangle(713, 570));
        GuiHelper.runInEDTAndWait(() -> { /* let the component listener update the view state */ });
        mv.updateState();
        mv.zoomTo(new LatLon(0, 0));
        Bounds world = mv.getProjection().getWorldBoundsLatLon();
        try {
            // zoomed in: the whole view shows the world, the bounds are those of the view
            mv.zoomTo(mv.getCenter(), 1000);
            Bounds bounds = MapGridPaintable.getVisibleLatLonBounds(mv);
            assertEquals(mv.getRealBounds().getMinLon(), bounds.getMinLon(), 1e-6);
            assertEquals(mv.getRealBounds().getMaxLon(), bounds.getMaxLon(), 1e-6);

            // zoomed out until the world is narrower than the view: the bounds are those of the world
            for (double scale : new double[] {45000, 60000, 77000, 200000}) {
                mv.zoomTo(mv.getCenter(), scale);
                bounds = MapGridPaintable.getVisibleLatLonBounds(mv);
                double worldPixels = mv.getPoint2D(new LatLon(0, 180)).getX() - mv.getPoint2D(new LatLon(0, -180)).getX();
                if (worldPixels < mv.getWidth()) {
                    assertEquals(-180, bounds.getMinLon(), 1e-6, "scale " + scale);
                    assertEquals(180, bounds.getMaxLon(), 1e-6, "scale " + scale);
                    assertTrue(bounds.getMaxLon() - bounds.getMinLon() > mv.getRealBounds().getMaxLon() - mv.getRealBounds().getMinLon(),
                            "scale " + scale + ": " + bounds + " not wider than " + mv.getRealBounds());
                }
                // Bounds.extend rounds to the OSM precision, so allow for that
                assertTrue(bounds.getMinLat() >= world.getMinLat() - 1e-6 && bounds.getMaxLat() <= world.getMaxLat() + 1e-6,
                        "scale " + scale + ": " + bounds + " outside " + world);
            }
        } finally {
            mv.destroy();
        }
    }

    /**
     * Panning past the antimeridian, so that there is blank space beside the world, keeps the grid on screen.
     * Non-regression test: the clipped edge falls exactly on the antimeridian, whose longitude is ambiguous and
     * is normalized to -180, which turned a visible range of e.g. 6 W .. 180 into 180 W .. 6 W - the other half
     * of the world, drawn completely off screen, so that the grid seemed to disappear.
     */
    @Test
    void testVisibleLatLonBoundsPastTheAntimeridian() {
        SizedMapView mv = new SizedMapView();
        mv.setBounds(new Rectangle(713, 570));
        GuiHelper.runInEDTAndWait(() -> { /* let the component listener update the view state */ });
        mv.updateState();
        Bounds world = mv.getProjection().getWorldBoundsLatLon();
        double halfWorld = mv.getProjection().getWorldBoundsBoxEastNorth().maxEast;
        try {
            for (double fraction : new double[] {-0.8, -0.5, 0.5, 0.8}) {
                mv.zoomTo(new EastNorth(fraction * halfWorld, 0), 30000);
                boolean eastwards = fraction > 0;
                assertTrue(eastwards ? mv.getProjectionBounds().maxEast > halfWorld : mv.getProjectionBounds().minEast < -halfWorld,
                        "no blank space beside the world at " + fraction);
                Bounds bounds = MapGridPaintable.getVisibleLatLonBounds(mv);
                String at = "at " + fraction + ": " + bounds;
                assertTrue(bounds.getMinLon() < bounds.getMaxLon(), at);
                // the range reaches the antimeridian on the blank side and the view edge on the other one
                assertEquals(eastwards ? 180 : -180, eastwards ? bounds.getMaxLon() : bounds.getMinLon(), 1e-5, at);
                assertEquals(mv.getProjection().eastNorth2latlon(
                            new EastNorth(eastwards ? mv.getProjectionBounds().minEast : mv.getProjectionBounds().maxEast, 0)).lon(),
                        eastwards ? bounds.getMinLon() : bounds.getMaxLon(), 1e-5, at);
                // and the meridians really are drawn inside the view
                List<LatLonGridLine> lines = MapGridPaintable.getLatLonGridLines(bounds, world, 10, 10, 0, 0, 2);
                assertTrue(lines.stream().filter(l -> l.meridian)
                        .anyMatch(l -> mv.getPoint2D(l.points.get(0)).getX() >= 0 && mv.getPoint2D(l.points.get(0)).getX() <= mv.getWidth()),
                        at + ", meridians off screen: " + lines);
            }
        } finally {
            mv.destroy();
        }
    }

    private static List<List<LatLon>> points(List<LatLonGridLine> lines) {
        return lines.stream().map(l -> l.points).collect(Collectors.toList());
    }

    /**
     * In Mercator one metre is 1/cos(latitude) projection units.
     */
    @Test
    void testProjectionUnitsPerMetre() {
        org.openstreetmap.josm.data.projection.Projection proj = ProjectionRegistry.getProjection();
        assertEquals(1, MapGridPaintable.projectionUnitsPerMetre(proj, proj.latlon2eastNorth(new LatLon(0, 10))), 1e-3);
        assertEquals(1 / Math.cos(Math.toRadians(50)),
                MapGridPaintable.projectionUnitsPerMetre(proj, proj.latlon2eastNorth(new LatLon(50, 10))), 1e-3);
        assertEquals(1 / Math.cos(Math.toRadians(20.6)),
                MapGridPaintable.projectionUnitsPerMetre(proj, proj.latlon2eastNorth(new LatLon(20.6, 87.8))), 1e-3);
        // outside the world: no scaling
        assertEquals(1, MapGridPaintable.projectionUnitsPerMetre(proj, new EastNorth(0, 1e12)), 1e-9);
    }

    /**
     * The thinning factor is a power of ten bringing the spacing above the minimum.
     */
    @Test
    void testThinningFactor() {
        MapGridPaintable.MIN_PIXEL_SPACING.put(25.0);
        try {
            assertEquals(1, MapGridPaintable.thinningFactor(30));
            assertEquals(1, MapGridPaintable.thinningFactor(25));
            assertEquals(10, MapGridPaintable.thinningFactor(24.9));
            assertEquals(100, MapGridPaintable.thinningFactor(0.3));
            assertEquals(1, MapGridPaintable.thinningFactor(0));
            assertEquals(1, MapGridPaintable.thinningFactor(Double.NaN));
        } finally {
            MapGridPaintable.MIN_PIXEL_SPACING.remove();
        }
    }

    /**
     * Painting on a map view draws something when enabled and nothing when disabled, for both grid types and
     * with a blend mode.
     */
    @Test
    void testPaint() {
        SizedMapView mv = new SizedMapView();
        mv.setBounds(new Rectangle(400, 300));
        GuiHelper.runInEDTAndWait(() -> { /* let the component listener update the view state */ });
        mv.updateState();
        mv.zoomTo(new LatLon(50, 10));
        assertTrue(mv.getRealBounds().getMaxLat() > mv.getRealBounds().getMinLat(), "map view has no size: " + mv.getRealBounds());
        MapGridPaintable grid = new MapGridPaintable();
        try {
            MapGridPaintable.ENABLED.put(false);
            assertEquals(0, paintedPixels(grid, mv), "grid drawn although disabled");
            MapGridPaintable.ENABLED.put(true);
            MapGridPaintable.TYPE.put(GridType.LATLON);
            MapGridPaintable.SPACING_X.put(0.001);
            MapGridPaintable.SPACING_Y.put(0.001);
            assertTrue(paintedPixels(grid, mv) > 100, "lat/lon grid not drawn");
            MapGridPaintable.TYPE.put(GridType.PROJECTED);
            MapGridPaintable.SPACING_X.put(100.0);
            MapGridPaintable.SPACING_Y.put(100.0);
            MapGridPaintable.ROTATION.put(30.0);
            MapGridPaintable.BLEND_MODE.put(BlendComposite.Mode.MULTIPLY);
            assertTrue(paintedPixels(grid, mv) > 100, "projected grid not drawn");
        } finally {
            grid.destroy();
            mv.destroy();
            for (String key : new String[] {"enabled", "type", "spacing-x", "spacing-y", "rotation", "blend-mode"}) {
                org.openstreetmap.josm.spi.preferences.Config.getPref().put("draw.grid." + key, null);
            }
        }
    }

    /** A map view which believes it is shown on screen, so that its view state gets a size */
    private static final class SizedMapView extends MapView {
        SizedMapView() {
            super(MainApplication.getLayerManager(), null);
        }

        @Override
        public Point getLocationOnScreen() {
            return new Point(0, 0);
        }

        @Override
        protected boolean isVisibleOnScreen() {
            return true;
        }

        void updateState() {
            updateLocationState();
        }
    }

    private static int paintedPixels(MapGridPaintable grid, MapView mv) {
        BufferedImage img = new BufferedImage(mv.getWidth(), mv.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        grid.paint(g, mv, mv.getRealBounds());
        g.dispose();
        int count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) & 0xffffff) != 0xffffff) {
                    count++;
                }
            }
        }
        return count;
    }
}
