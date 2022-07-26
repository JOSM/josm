// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.rtklib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.annotations.TimeZoneAnnotation;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link RtkLibPosReader} class.
 */
@TimeZoneAnnotation
class RtkLibPosReaderTest {
    private static RtkLibPosReader read(String path) throws IOException, SAXException {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        RtkLibPosReader in = new RtkLibPosReader(Files.newInputStream(Paths.get(path)));
        in.parse(true);
        return in;
    }

    /**
     * Tests reading a RTKLib pos file.
     * @throws Exception if any error occurs
     */
    @Test
    void testReader() throws Exception {
        RtkLibPosReader in = read("nodist/data/rtklib_example.pos");
        assertEquals(137, in.getNumberOfCoordinates());

        List<WayPoint> wayPoints = new ArrayList<>(in.getGpxData().tracks.iterator().next().getSegments().iterator().next().getWayPoints());
        WayPoint wp0 = wayPoints.get(0);
        assertEquals(DateUtils.parseInstant("2019-06-08T08:23:12.000Z"), wp0.get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.parseInstant("2019-06-08T08:23:12.300Z"), wayPoints.get(1).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.parseInstant("2019-06-08T08:23:12.600Z"), wayPoints.get(2).get(GpxConstants.PT_TIME));
        assertEquals(wp0.getInstant(), wp0.get(GpxConstants.PT_TIME));

        assertEquals(Instant.parse("2019-06-08T08:23:12.000Z"), wp0.getInstant());
        assertEquals(Instant.parse("2019-06-08T08:23:12.300Z"), wayPoints.get(1).getInstant());
        assertEquals(Instant.parse("2019-06-08T08:23:12.600Z"), wayPoints.get(2).getInstant());

        assertEquals(new LatLon(46.948881673, -1.484757046), wp0.getCoor());
        assertEquals(5, wp0.get(GpxConstants.RTKLIB_Q));
        assertEquals("92.3955", wp0.get(GpxConstants.PT_ELE));
        assertEquals("2", wp0.get(GpxConstants.PT_SAT));
        assertEquals("1.8191757", wp0.get(GpxConstants.PT_HDOP).toString().trim());

        assertEquals("1.5620", wp0.get(GpxConstants.RTKLIB_SDN));
        assertEquals("0.9325", wp0.get(GpxConstants.RTKLIB_SDE));
        assertEquals("0.8167", wp0.get(GpxConstants.RTKLIB_SDU));
        assertEquals("-0.7246", wp0.get(GpxConstants.RTKLIB_SDNE));
        assertEquals("0.7583", wp0.get(GpxConstants.RTKLIB_SDEU));
        assertEquals("0.6573", wp0.get(GpxConstants.RTKLIB_SDUN));
    }

    /**
     * Tests reading another RTKLib pos file with different date format.
     * @throws Exception if any error occurs
     */
    @Test
    void testReader2() throws Exception {
        RtkLibPosReader in = read("nodist/data/rtklib_example2.pos");
        assertEquals(6, in.getNumberOfCoordinates());
    }

    /**
     * Tests reading another RTKLib pos file with yet another different date format.
     * @throws Exception if any error occurs
     */
    @Test
    void testReader3() throws Exception {
        RtkLibPosReader in = read("nodist/data/rtklib_example3.pos");
        assertEquals(1, in.getNumberOfCoordinates());
    }
}
