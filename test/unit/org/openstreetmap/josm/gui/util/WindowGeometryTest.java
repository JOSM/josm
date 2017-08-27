// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.util.WindowGeometry.WindowGeometryException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link WindowGeometry} class.
 */
public class WindowGeometryTest {
    /**
     * Some of this depends on preferences.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link WindowGeometry#centerInWindow} method.
     */
    @Test
    public void testCenterInWindow() {
        assertNotNull(WindowGeometry.centerInWindow(null, null));
        assertNotNull(WindowGeometry.centerInWindow(new JPanel(), null));
    }

    /**
     * Test of {@link WindowGeometry#centerOnScreen} method.
     */
    @Test
    public void testCenterOnScreen() {
        Dimension dim = new Dimension(200, 100);
        assertEquals(new WindowGeometry(new Point(0, 0), dim), WindowGeometry.centerOnScreen(dim));
        assertEquals(new WindowGeometry(new Point(300, 250), dim), WindowGeometry.centerOnScreen(dim, null));

        Main.pref.put("gui.geometry", "x=0,y=0,width=800,height=600");
        assertEquals(new WindowGeometry(new Point(300, 250), dim), WindowGeometry.centerOnScreen(dim));
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException always
     */
    @Test(expected = WindowGeometryException.class)
    public void testWindowGeometryException1() throws WindowGeometryException {
        Main.pref.put("test", null);
        new WindowGeometry("test");
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException always
     */
    @Test(expected = WindowGeometryException.class)
    public void testWindowGeometryException2() throws WindowGeometryException {
        Main.pref.put("test", "");
        new WindowGeometry("test");
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException always
     */
    @Test(expected = WindowGeometryException.class)
    public void testWindowGeometryException3() throws WindowGeometryException {
        Main.pref.put("test", "x=not_a_number");
        new WindowGeometry("test");
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException always
     */
    @Test(expected = WindowGeometryException.class)
    public void testWindowGeometryException4() throws WindowGeometryException {
        Main.pref.put("test", "wrong_pattern");
        new WindowGeometry("test");
    }

    /**
     * Test of {@link WindowGeometry.WindowGeometryException} class.
     * @throws WindowGeometryException never
     */
    @Test
    public void testWindowGeometryException5() throws WindowGeometryException {
        Main.pref.put("test", "x=15,y=55,width=200,height=100");
        assertNotNull(new WindowGeometry("test"));
    }

    /**
     * Test of {@link WindowGeometry#isBugInMaximumWindowBounds} method.
     */
    @Test
    public void testIsBugInMaximumWindowBounds() {
        assertFalse(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(10, 10)));
        assertTrue(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(10, 0)));
        assertTrue(WindowGeometry.isBugInMaximumWindowBounds(new Rectangle(0, 10)));
    }

    /**
     * Test of {@link WindowGeometry#getVirtualScreenBounds} method.
     */
    @Test
    public void testGetVirtualScreenBounds() {
        assertNotNull(WindowGeometry.getVirtualScreenBounds());
    }

    /**
     * Test of {@link WindowGeometry#getMaxDimensionOnScreen} method.
     */
    @Test
    public void testGetMaxDimensionOnScreen() {
        assertNotNull(WindowGeometry.getMaxDimensionOnScreen(new JLabel()));
    }

    /**
     * Test of {@link WindowGeometry#toString} method.
     */
    @Test
    public void testToString() {
        assertEquals("WindowGeometry{topLeft=java.awt.Point[x=0,y=0],extent=java.awt.Dimension[width=0,height=0]}",
                new WindowGeometry(new Rectangle()).toString());
    }

    /**
     * Unit test of methods {@link WindowGeometry#equals} and {@link WindowGeometry#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(WindowGeometry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
