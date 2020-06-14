// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Objects;

import javax.swing.JPanel;

import org.CustomMatchers;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Some tests for the {@link NavigatableComponent} class.
 * @author Michael Zangl
 *
 */
public class NavigatableComponentTest {

    private static final class NavigatableComponentMock extends NavigatableComponent {
        @Override
        public Point getLocationOnScreen() {
            return new Point(30, 40);
        }

        @Override
        protected boolean isVisibleOnScreen() {
            return true;
        }
    }

    private static final int HEIGHT = 200;
    private static final int WIDTH = 300;
    private NavigatableComponent component;

    /**
     * We need the projection for coordinate conversions.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Create a new, fresh {@link NavigatableComponent}
     */
    @Before
    public void setUp() {
        component = new NavigatableComponentMock();
        component.setBounds(new Rectangle(WIDTH, HEIGHT));
        // wait for the event to be propagated.
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
        component.setVisible(true);
        JPanel parent = new JPanel();
        parent.add(component);
        component.updateLocationState();
    }

    /**
     * Test if the default scale was set correctly.
     */
    @Test
    public void testDefaultScale() {
        assertEquals(ProjectionRegistry.getProjection().getDefaultZoomInPPD(), component.getScale(), 0.00001);
    }

    /**
     * Tests {@link NavigatableComponent#getPoint2D(EastNorth)}
     */
    @Test
    public void testPoint2DEastNorth() {
        assertThat(component.getPoint2D((EastNorth) null), CustomMatchers.is(new Point2D.Double()));
        Point2D shouldBeCenter = component.getPoint2D(component.getCenter());
        assertThat(shouldBeCenter, CustomMatchers.is(new Point2D.Double(WIDTH / 2, HEIGHT / 2)));

        EastNorth testPoint = component.getCenter().add(300 * component.getScale(), 200 * component.getScale());
        Point2D testPointConverted = component.getPoint2D(testPoint);
        assertThat(testPointConverted, CustomMatchers.is(new Point2D.Double(WIDTH / 2 + 300, HEIGHT / 2 - 200)));
    }

    /**
     * TODO: Implement this test.
     */
    @Test
    public void testPoint2DLatLon() {
        assertThat(component.getPoint2D((LatLon) null), CustomMatchers.is(new Point2D.Double()));
        // TODO: Really test this.
    }

    /**
     * Tests {@link NavigatableComponent#zoomTo(LatLon)}
     */
    @Test
    public void testZoomToLatLon() {
        component.zoomTo(new LatLon(10, 10));
        Point2D shouldBeCenter = component.getPoint2D(new LatLon(10, 10));
        // 0.5 pixel tolerance, see isAfterZoom
        assertEquals(shouldBeCenter.getX(), WIDTH / 2., 0.5);
        assertEquals(shouldBeCenter.getY(), HEIGHT / 2., 0.5);
    }

    /**
     * Tests {@link NavigatableComponent#zoomToFactor(double)} and {@link NavigatableComponent#zoomToFactor(EastNorth, double)}
     */
    @Test
    public void testZoomToFactor() {
        EastNorth center = component.getCenter();
        double initialScale = component.getScale();

        // zoomToFactor(double)
        component.zoomToFactor(0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertThat(component.getCenter(), isAfterZoom(center, component.getScale()));
        component.zoomToFactor(2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getCenter(), isAfterZoom(center, component.getScale()));

        // zoomToFactor(EastNorth, double)
        EastNorth newCenter = new EastNorth(10, 20);
        component.zoomToFactor(newCenter, 0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertThat(component.getCenter(), isAfterZoom(newCenter, component.getScale()));
        component.zoomToFactor(newCenter, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getCenter(), isAfterZoom(newCenter, component.getScale()));
    }

    /**
     * Tests {@link NavigatableComponent#getEastNorth(int, int)}
     */
    @Test
    public void testGetEastNorth() {
        EastNorth center = component.getCenter();
        assertThat(component.getEastNorth(WIDTH / 2, HEIGHT / 2), CustomMatchers.is(center));

        EastNorth testPoint = component.getCenter().add(WIDTH * component.getScale(), HEIGHT * component.getScale());
        assertThat(component.getEastNorth(3 * WIDTH / 2, -HEIGHT / 2), CustomMatchers.is(testPoint));
    }

    /**
     * Tests {@link NavigatableComponent#zoomToFactor(double, double, double)}
     */
    @Test
    public void testZoomToFactorCenter() {
        // zoomToFactor(double, double, double)
        // assumes getEastNorth works as expected
        EastNorth testPoint1 = component.getEastNorth(0, 0);
        EastNorth testPoint2 = component.getEastNorth(200, 150);
        double initialScale = component.getScale();

        component.zoomToFactor(0, 0, 0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(0, 0), isAfterZoom(testPoint1, component.getScale()));
        component.zoomToFactor(0, 0, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(0, 0), isAfterZoom(testPoint1, component.getScale()));

        component.zoomToFactor(200, 150, 0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(200, 150), isAfterZoom(testPoint2, component.getScale()));
        component.zoomToFactor(200, 150, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(200, 150), isAfterZoom(testPoint2, component.getScale()));

    }

    /**
     * Tests {@link NavigatableComponent#getProjectionBounds()}
     */
    @Test
    public void testGetProjectionBounds() {
        ProjectionBounds bounds = component.getProjectionBounds();
        assertThat(bounds.getCenter(), CustomMatchers.is(component.getCenter()));

        assertThat(bounds.getMin(), CustomMatchers.is(component.getEastNorth(0, HEIGHT)));
        assertThat(bounds.getMax(), CustomMatchers.is(component.getEastNorth(WIDTH, 0)));
    }

    /**
     * Tests {@link NavigatableComponent#getRealBounds()}
     */
    @Test
    public void testGetRealBounds() {
        Bounds bounds = component.getRealBounds();
        assertThat(bounds.getCenter(), CustomMatchers.is(component.getLatLon(WIDTH / 2, HEIGHT / 2)));

        assertThat(bounds.getMin(), CustomMatchers.is(component.getLatLon(0, HEIGHT)));
        assertThat(bounds.getMax(), CustomMatchers.is(component.getLatLon(WIDTH, 0)));
    }

    /**
     * Check that EastNorth is the same as expected after zooming the NavigatableComponent.
     *
     * Adds tolerance of 0.5 pixel for pixel grid alignment, see
     * {@link NavigatableComponent#zoomTo(EastNorth, double, boolean)}
     * @param expected expected
     * @param scale current scale
     * @return Matcher object
     */
    private Matcher<EastNorth> isAfterZoom(EastNorth expected, double scale) {
        return new CustomTypeSafeMatcher<EastNorth>(Objects.toString(expected)) {
            @Override
            protected boolean matchesSafely(EastNorth actual) {
                // compare pixels (east/north divided by scale)
                return Math.abs((expected.getX() - actual.getX()) / scale) <= 0.5
                        && Math.abs((expected.getY() - actual.getY()) / scale) <= 0.5;
            }
        };
    }

}
