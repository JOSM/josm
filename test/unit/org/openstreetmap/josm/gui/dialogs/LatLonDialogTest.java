// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests of {@link LatLonDialog} class.
 */
public class LatLonDialogTest {

    /**
     * Unit test of {@link LatLonDialog#parseLatLon} method.
     */
    @Test
    public void testparseLatLon() {
        assertEquals(new LatLon(49.29918, 19.24788), LatLonDialog.parseLatLon("49.29918° 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonDialog.parseLatLon("N 49.29918 E 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonDialog.parseLatLon("49.29918° 19.24788°"));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonDialog.parseLatLon("N 49.29918 E 19.24788"));
        assertEquals(new LatLon(-19 - 24.788 / 60, -49 - 29.918 / 60), LatLonDialog.parseLatLon("W 49°29.918' S 19°24.788'"));
        assertEquals(new LatLon(49 + 29. / 60 + 04. / 3600, 19 + 24. / 60 + 43. / 3600), LatLonDialog.parseLatLon("N 49°29'04\" E 19°24'43\""));
        assertEquals(new LatLon(49.29918, 19.24788), LatLonDialog.parseLatLon("49.29918 N, 19.24788 E"));
        assertEquals(new LatLon(49 + 29. / 60 + 21. / 3600, 19 + 24. / 60 + 38. / 3600), LatLonDialog.parseLatLon("49°29'21\" N 19°24'38\" E"));
        assertEquals(new LatLon(49 + 29. / 60 + 51. / 3600, 19 + 24. / 60 + 18. / 3600), LatLonDialog.parseLatLon("49 29 51, 19 24 18"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonDialog.parseLatLon("49 29, 19 24"));
        assertEquals(new LatLon(19 + 24. / 60, 49 + 29. / 60), LatLonDialog.parseLatLon("E 49 29, N 19 24"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonDialog.parseLatLon("49° 29; 19° 24"));
        assertEquals(new LatLon(49 + 29. / 60, 19 + 24. / 60), LatLonDialog.parseLatLon("49° 29; 19° 24"));
        assertEquals(new LatLon(49 + 29. / 60, -19 - 24. / 60), LatLonDialog.parseLatLon("N 49° 29, W 19° 24"));
        assertEquals(new LatLon(-49 - 29.5 / 60, 19 + 24.6 / 60), LatLonDialog.parseLatLon("49° 29.5 S, 19° 24.6 E"));
        assertEquals(new LatLon(49 + 29.918 / 60, 19 + 15.88 / 60), LatLonDialog.parseLatLon("N 49 29.918 E 19 15.88"));
        assertEquals(new LatLon(49 + 29.4 / 60, 19 + 24.5 / 60), LatLonDialog.parseLatLon("49 29.4 19 24.5"));
        assertEquals(new LatLon(-49 - 29.4 / 60, 19 + 24.5 / 60), LatLonDialog.parseLatLon("-49 29.4 N -19 24.5 W"));
    }
}
