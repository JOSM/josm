// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests for class {@link LatLonParser}.
 */
class LatLonParserTest {
    /**
     * Unit test of {@link LatLonParser#parse} method.
     */
    @Test
    void testParse() {
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("49.29918° 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("N 49.29918 E 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("49.29918° 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("N 49.29918 E 19.24788"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("n 49.29918 e 19.24788"));
        assertEquals(new LatLon(-19 - 24.788 / 60, -49 - 29.918 / 60), LatLonParser.parse("W 49°29.918' S 19°24.788'"));
        assertEquals(new LatLon(-19 - 24.788 / 60, -49 - 29.918 / 60), LatLonParser.parse("w 49°29.918' s 19°24.788'"));
        assertEquals(new LatLon(49 + 29. / 60 + 04. / 3600, 19 + 24. / 60 + 43. / 3600), LatLonParser.parse("N 49°29'04\" E 19°24'43\""));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("49.29918 N, 19.24788 E"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonParser.parse("49.29918 n, 19.24788 e"));
        assertEquals(new LatLon(49 + 29. / 60 + 21. / 3600, 19 + 24. / 60 + 38. / 3600), LatLonParser.parse("49°29'21\" N 19°24'38\" E"));
        assertEquals(new LatLon(49 + 29. / 60 + 51. / 3600, 19 + 24. / 60 + 18. / 3600), LatLonParser.parse("49 29 51, 19 24 18"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonParser.parse("49 29, 19 24"));
        assertEquals(new LatLon(19 + 24. / 60, 49 + 29. / 60), LatLonParser.parse("E 49 29, N 19 24"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonParser.parse("49° 29; 19° 24"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonParser.parse("49° 29; 19° 24"));
        assertEquals(new LatLon(49 + 29. / 60, -19 - 24. / 60), LatLonParser.parse("N 49° 29, W 19° 24"));
        assertEquals(new LatLon(-49 - 29.5 / 60, 19 + 24.6 / 60), LatLonParser.parse("49° 29.5 S, 19° 24.6 E"));
        assertEquals(new LatLon(49 + 29.918 / 60, 19 + 15.88 / 60), LatLonParser.parse("N 49 29.918 E 19 15.88"));
        assertEquals(new LatLon(49 + 29.4 / 60, 19 + 24.5 / 60), LatLonParser.parse("49 29.4 19 24.5"));
        assertEquals(new LatLon(-49 - 29.4 / 60, 19 + 24.5 / 60), LatLonParser.parse("-49 29.4 N -19 24.5 W"));
    }

    /**
     * Unit test of {@link LatLonParser#parse} method - invalid case 1.
     */
    @Test
    void testParseInvalid1() {
        assertThrows(IllegalArgumentException.class, () -> LatLonParser.parse("48°45'S 23°30'S"));
    }

    /**
     * Unit test of {@link LatLonParser#parse} method - invalid case 2.
     */
    @Test
    void testParseInvalid2() {
        assertThrows(IllegalArgumentException.class, () -> LatLonParser.parse("47°45'N 24°00'S"));
    }

}
