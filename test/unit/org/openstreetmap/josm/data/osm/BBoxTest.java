// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
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
        EqualsVerifier.forClass(BBox.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
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

}
