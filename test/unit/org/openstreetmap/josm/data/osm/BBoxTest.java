// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link BBox}.
 */
public class BBoxTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link BBox#equals} and {@link BBox#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(BBox.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    /**
     * Unit test of method {@link BBox#bboxesAreFunctionallyEqual}
     */
    @Test
    public void testBboxesAreFunctionallyEqual() {
        BBox bbox1 = new BBox(0, 1, 1, 0);
        BBox bbox2 = new BBox(0.1, 0.9, 0.9, 0.1);

        assertFalse(BBox.bboxesAreFunctionallyEqual(bbox1, null, null));
        assertFalse(BBox.bboxesAreFunctionallyEqual(null, bbox2, null));
        assertFalse(BBox.bboxesAreFunctionallyEqual(null, null, null));

        assertFalse(bbox1.bboxIsFunctionallyEqual(bbox2, null));
        assertTrue(bbox1.bboxIsFunctionallyEqual(bbox2, 0.1));
        bbox1.add(0, 1.1);
        assertFalse(bbox1.bboxIsFunctionallyEqual(bbox2, 0.1));
        bbox1.add(2, 0);
        assertFalse(bbox1.bboxIsFunctionallyEqual(bbox2, 0.1));
    }

    /**
     * Test LatLon constructor which might result in invalid bbox
     */
    @Test
    public void testLatLonConstructor() {
        LatLon latLon1 = new LatLon(10, 20);
        LatLon latLon2 = new LatLon(20, 10);
        BBox b1 = new BBox(latLon1, latLon2);
        BBox b2 = new BBox(latLon2, latLon1);
        assertTrue(b1.bounds(latLon1));
        assertTrue(b2.bounds(latLon1));
        assertTrue(b1.bounds(latLon2));
        assertTrue(b2.bounds(latLon2));
        assertTrue(b2.bounds(b1));
        assertTrue(b1.bounds(b2));

        // outside of world latlon values
        LatLon outOfWorld = new LatLon(-190, 340);
        BBox b3 = new BBox(outOfWorld, latLon1);
        BBox b4 = new BBox(latLon1, outOfWorld);
        BBox b5 = new BBox(outOfWorld, outOfWorld);

        assertTrue(b3.isValid());
        assertTrue(b4.isValid());
        assertTrue(b3.bounds(latLon1));
        assertTrue(b4.bounds(latLon1));
        assertTrue(b5.isValid());
        assertFalse(b3.isInWorld());
        assertFalse(b4.isInWorld());
        assertFalse(b5.isInWorld());
    }

    /**
     * Test double constructor which might result in invalid bbox
     */
    @Test
    public void testDoubleConstructor() {
        assertTrue(new BBox(1, 2, 3, 4).isValid());
        assertFalse(new BBox(Double.NaN, 2, 3, 4).isValid());
        assertFalse(new BBox(1, Double.NaN, 3, 4).isValid());
        assertFalse(new BBox(1, 2, Double.NaN, 4).isValid());
        assertFalse(new BBox(1, 2, 3, Double.NaN).isValid());
    }

    /**
     * Test Node constructor which might result in invalid bbox
     */
    @Test
    public void testNodeConstructor() {
        assertTrue(new BBox(new Node(LatLon.NORTH_POLE)).isValid());
        assertFalse(new BBox(new Node()).isValid());
    }

    /**
     * Unit test of {@link BBox#add(LatLon)} method.
     */
    @Test
    public void testAddLatLon() {
        BBox b = new BBox();
        b.add((LatLon) null);
        b.add(new LatLon(Double.NaN, Double.NaN));
        assertFalse(b.isValid());
        b.add(LatLon.NORTH_POLE);
        assertTrue(b.isValid());
        assertEquals(LatLon.NORTH_POLE, b.getCenter());
    }

    /**
     * Unit test of {@link BBox#addLatLon} method.
     */
    @Test
    public void testAddLatLonBuffer() {
        BBox b = new BBox();
        b.addLatLon(LatLon.NORTH_POLE, 0.5);
        assertEquals(LatLon.NORTH_POLE, b.getCenter());
        assertEquals(new LatLon(90.5, -0.5), b.getTopLeft());
        assertEquals(new LatLon(89.5, +0.5), b.getBottomRight());
    }

    /**
     * Unit test of {@link BBox#add(double, double)} method.
     */
    @Test
    public void testAddDouble() {
        BBox b = new BBox();
        b.add(1, Double.NaN);
        assertFalse(b.isValid());
        b.add(Double.NaN, 2);
        assertFalse(b.isValid());
        b.add(1, 2);
        assertTrue(b.isValid());
        assertEquals(new LatLon(2, 1), b.getCenter());
    }

    /**
     * Unit test of {@link BBox#addPrimitive} method.
     */
    @Test
    public void testAddPrimitive() {
        BBox b = new BBox();
        b.addPrimitive(new Node(LatLon.NORTH_POLE), 0.5);
        assertEquals(LatLon.NORTH_POLE, b.getCenter());
        assertEquals(new LatLon(90.5, -0.5), b.getTopLeft());
        assertEquals(new LatLon(89.5, +0.5), b.getBottomRight());
    }

    /**
     * Unit test of {@link BBox#height} and {@link BBox#width} and {@link BBox#area} methods.
     */
    @Test
    public void testHeightWidthArea() {
        BBox b1 = new BBox(1, 2, 3, 5);
        assertEquals(2, b1.width(), 1e-7);
        assertEquals(3, b1.height(), 1e-7);
        assertEquals(6, b1.area(), 1e-7);
        BBox b2 = new BBox();
        assertEquals(0, b2.width(), 1e-7);
        assertEquals(0, b2.height(), 1e-7);
        assertEquals(0, b2.area(), 1e-7);
    }

    /**
     * Unit test of {@link BBox#toString} method.
     */
    @Test
    public void testToString() {
        assertEquals("[ x: Infinity -> -Infinity, y: Infinity -> -Infinity ]", new BBox().toString());
        assertEquals("[ x: 1.0 -> 3.0, y: 2.0 -> 4.0 ]", new BBox(1, 2, 3, 4).toString());
    }
}
