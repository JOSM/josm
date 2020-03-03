// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.rtklib;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RtkLibPosReader} class.
 */
public class RtkLibPosReaderTest {
    /**
     * Set the timezone and timeout.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    /**
     * Forces the timezone.
     */
    @Before
    public void setUp() {
        iso8601.setTimeZone(DateUtils.UTC);
    }

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
    public void testReader() throws Exception {
        RtkLibPosReader in = read("nodist/data/rtklib_example.pos");
        assertEquals(137, in.getNumberOfCoordinates());

        List<WayPoint> wayPoints = new ArrayList<>(in.getGpxData().tracks.iterator().next().getSegments().iterator().next().getWayPoints());
        assertEquals(DateUtils.fromString("2019-06-08T08:23:12.000Z"), wayPoints.get(0).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.fromString("2019-06-08T08:23:12.300Z"), wayPoints.get(1).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.fromString("2019-06-08T08:23:12.600Z"), wayPoints.get(2).get(GpxConstants.PT_TIME));
        assertEquals(wayPoints.get(0).getDate(), wayPoints.get(0).get(GpxConstants.PT_TIME));

        assertEquals("2019-06-08T08:23:12.000Z", iso8601.format(wayPoints.get(0).getDate()));
        assertEquals("2019-06-08T08:23:12.300Z", iso8601.format(wayPoints.get(1).getDate()));
        assertEquals("2019-06-08T08:23:12.600Z", iso8601.format(wayPoints.get(2).getDate()));

        assertEquals(new LatLon(46.948881673, -1.484757046), wayPoints.get(0).getCoor());
        assertEquals(5, wayPoints.get(0).get(GpxConstants.RTKLIB_Q));
        assertEquals("92.3955", wayPoints.get(0).get(GpxConstants.PT_ELE));
        assertEquals("2", wayPoints.get(0).get(GpxConstants.PT_SAT));
        assertEquals("2.2090015", wayPoints.get(0).get(GpxConstants.PT_HDOP).toString().trim());
    }

    /**
     * Tests reading another RTKLib pos file with different date format.
     * @throws Exception if any error occurs
     */
    @Test
    public void testReader2() throws Exception {
        RtkLibPosReader in = read("nodist/data/rtklib_example2.pos");
        assertEquals(6, in.getNumberOfCoordinates());
    }
}
