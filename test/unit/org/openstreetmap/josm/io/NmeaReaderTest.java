// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.NmeaReader.NMEA_TYPE;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link NmeaReader} class.
 */
public class NmeaReaderTest {
    /**
     * Set the timezone and timeout.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link NMEA_TYPE#equals} and {@link NMEA_TYPE#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(NMEA_TYPE.class).verify();
    }

    /**
     * Tests reading a nmea file.
     * @throws Exception if any error occurs
     */
    @Test
    public void testReader() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        final NmeaReader in = new NmeaReader(new FileInputStream("data_nodist/btnmeatrack_2016-01-25.nmea"));
        assertEquals(30, in.getNumberOfCoordinates());
        assertEquals(0, in.getParserMalformed());

        final List<WayPoint> wayPoints = new ArrayList<>(in.data.tracks.iterator().next().getSegments().iterator().next().getWayPoints());
        assertEquals("2016-01-25T05:05:09.200Z", wayPoints.get(0).get(GpxConstants.PT_TIME));
        assertEquals("2016-01-25T05:05:09.400Z", wayPoints.get(1).get(GpxConstants.PT_TIME));
        assertEquals("2016-01-25T05:05:09.600Z", wayPoints.get(2).get(GpxConstants.PT_TIME));

        final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        assertEquals("2016-01-25T06:05:09.200+01", iso8601.format(wayPoints.get(0).getTime()));
        assertEquals("2016-01-25T06:05:09.400+01", iso8601.format(wayPoints.get(1).getTime()));
        assertEquals("2016-01-25T06:05:09.600+01", iso8601.format(wayPoints.get(2).getTime()));

        assertEquals(new LatLon(46.98807, -1.400525), wayPoints.get(0).getCoor());
        assertEquals("38.9", wayPoints.get(0).get(GpxConstants.PT_ELE));
        assertEquals("16", wayPoints.get(0).get(GpxConstants.PT_SAT));
        assertEquals("3d", wayPoints.get(0).get(GpxConstants.PT_FIX));
        assertEquals("0.7", wayPoints.get(0).get(GpxConstants.PT_HDOP).toString().trim());
        assertEquals(null, wayPoints.get(0).get(GpxConstants.PT_VDOP));
        assertEquals(null, wayPoints.get(0).get(GpxConstants.PT_PDOP));
    }
}
