// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import javax.swing.JFrame;

import org.CustomMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Some tests for the {@link NavigatableComponent} class.
 * @author Michael Zangl
 *
 */
public class NavigatableComponentTest {

    private static final int HEIGHT = 200;
    private static final int WIDTH = 300;
    private NavigatableComponent component;

    /**
     * We need the projection for coordinate conversions.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform().projection();

    /**
     * Create a new, fresh {@link NavigatableComponent}
     */
    @Before
    public void setUp() {
        component = new NavigatableComponent() {
            @Override
            public Point getLocationOnScreen() {
                return new Point(30, 40);
            }
        };
        component.setBounds(new Rectangle(WIDTH, HEIGHT));
        // wait for the event to be propagated.
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
        component.setVisible(true);
        JFrame window = new JFrame();
        window.add(component);
        component.updateLocationState();
    }

    /**
     * Test if the default scale was set correctly.
     */
    @Test
    public void testDefaultScale() {
        assertEquals(Main.getProjection().getDefaultZoomInPPD(), component.getScale(), 0.00001);
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
        assertThat(shouldBeCenter, CustomMatchers.is(new Point2D.Double(WIDTH / 2, HEIGHT / 2)));
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
        assertEquals(center, component.getCenter());
        component.zoomToFactor(2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertEquals(center, component.getCenter());

        // zoomToFactor(EastNorth, double)
        EastNorth newCenter = new EastNorth(10, 20);
        component.zoomToFactor(newCenter, 0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertEquals(newCenter, component.getCenter());
        component.zoomToFactor(newCenter, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertEquals(newCenter, component.getCenter());
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
        assertThat(component.getEastNorth(0, 0), CustomMatchers.is(testPoint1));
        component.zoomToFactor(0, 0, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(0, 0), CustomMatchers.is(testPoint1));

        component.zoomToFactor(200, 150, 0.5);
        assertEquals(initialScale / 2, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(200, 150), CustomMatchers.is(testPoint2));
        component.zoomToFactor(200, 150, 2);
        assertEquals(initialScale, component.getScale(), 0.00000001);
        assertThat(component.getEastNorth(200, 150), CustomMatchers.is(testPoint2));

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

}
