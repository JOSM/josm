// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.junit.Assert.assertEquals;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Test for {@link ICoordinateFormat} implementations.
 */
public class ICoordinateFormatTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Tests {@link ICoordinateFormat#latToString(org.openstreetmap.josm.data.coor.ILatLon)}
     * and {@link ICoordinateFormat#lonToString(org.openstreetmap.josm.data.coor.ILatLon)}
     * and {@link ICoordinateFormat#toString(ILatLon, String)}
     * for various implementations.
     */
    @Test
    public void testFormatting() {
        LatLon c = new LatLon(47.000000, 19.000000);
        assertEquals("47.0", DecimalDegreesCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("19.0", DecimalDegreesCoordinateFormat.INSTANCE.lonToString(c));
        assertEquals("47.0 19.0", DecimalDegreesCoordinateFormat.INSTANCE.toString(c, " "));
        assertEquals("47°00'00.0\"N", DMSCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("19°00'00.0\"E", DMSCoordinateFormat.INSTANCE.lonToString(c));
        assertEquals("47°00'00.0\"N  19°00'00.0\"E", DMSCoordinateFormat.INSTANCE.toString(c, "  "));
        assertEquals("47°00.000'N", NauticalCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("19°00.000'E", NauticalCoordinateFormat.INSTANCE.lonToString(c));
        assertEquals("5942074.0724311", ProjectedCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("2115070.3250722", ProjectedCoordinateFormat.INSTANCE.lonToString(c));
        c = new LatLon(-47.000000, -19.000000);
        assertEquals("47°00'00.0\"S", DMSCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("19°00'00.0\"W", DMSCoordinateFormat.INSTANCE.lonToString(c));
        assertEquals("47°00.000'S", NauticalCoordinateFormat.INSTANCE.latToString(c));
        assertEquals("19°00.000'W", NauticalCoordinateFormat.INSTANCE.lonToString(c));
    }

}
