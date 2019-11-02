// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.nmea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    /**
     * Forces the timezone.
     */
    @Before
    public void setUp() {
        iso8601.setTimeZone(DateUtils.UTC);
    }

    /**
     * Tests reading a nmea file.
     * @throws Exception if any error occurs
     */
    @Test
    public void testReader() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        final NmeaReader in = new NmeaReader(Files.newInputStream(Paths.get("data_nodist/btnmeatrack_2016-01-25.nmea")));
        in.parse(true);
        assertEquals(30, in.getNumberOfCoordinates());
        assertEquals(0, in.getParserMalformed());

        final List<WayPoint> wayPoints = new ArrayList<>(in.data.tracks.iterator().next().getSegments().iterator().next().getWayPoints());
        assertEquals(DateUtils.fromString("2016-01-25T05:05:09.200Z"), wayPoints.get(0).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.fromString("2016-01-25T05:05:09.400Z"), wayPoints.get(1).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.fromString("2016-01-25T05:05:09.600Z"), wayPoints.get(2).get(GpxConstants.PT_TIME));
        assertEquals(wayPoints.get(0).getDate(), wayPoints.get(0).get(GpxConstants.PT_TIME));

        assertEquals("2016-01-25T05:05:09.200Z", iso8601.format(wayPoints.get(0).getDate()));
        assertEquals("2016-01-25T05:05:09.400Z", iso8601.format(wayPoints.get(1).getDate()));
        assertEquals("2016-01-25T05:05:09.600Z", iso8601.format(wayPoints.get(2).getDate()));

        assertEquals(new LatLon(46.98807, -1.400525), wayPoints.get(0).getCoor());
        assertEquals("38.9", wayPoints.get(0).get(GpxConstants.PT_ELE));
        assertEquals("16", wayPoints.get(0).get(GpxConstants.PT_SAT));
        assertEquals("3d", wayPoints.get(0).get(GpxConstants.PT_FIX));
        assertEquals("0.7", wayPoints.get(0).get(GpxConstants.PT_HDOP).toString().trim());
        assertEquals(null, wayPoints.get(0).get(GpxConstants.PT_VDOP));
        assertEquals(null, wayPoints.get(0).get(GpxConstants.PT_PDOP));
    }

    private static void compareWithReference(int ticket, String filename, int numCoor) throws IOException, SAXException {
        GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getRegressionDataFile(ticket, filename+".gpx"));
        NmeaReader in = new NmeaReader(Files.newInputStream(Paths.get(TestUtils.getRegressionDataFile(ticket, filename+".nmea"))));
        in.parse(true);
        assertEquals(numCoor, in.getNumberOfCoordinates());
        assertEquals(0, in.getParserMalformed());
        assertEquals(gpx.dataSources, in.data.dataSources);
        assertEquals(1, gpx.tracks.size());
        assertEquals(1, in.data.tracks.size());
        GpxTrack gpxTrack = gpx.tracks.iterator().next();
        GpxTrack nmeaTrack = in.data.tracks.iterator().next();
        assertEquals(gpxTrack.getBounds(), nmeaTrack.getBounds());
        int nTracks = gpxTrack.getSegments().size();
        assertEquals(nTracks, nmeaTrack.getSegments().size());
        if (nTracks > 0) {
            IGpxTrackSegment gpxSeg = gpxTrack.getSegments().iterator().next();
            IGpxTrackSegment nmeaSeg = nmeaTrack.getSegments().iterator().next();
            assertEquals(gpxSeg.getBounds(), nmeaSeg.getBounds());
            assertEquals(numCoor, gpxSeg.getWayPoints().size());
            assertEquals(numCoor, nmeaSeg.getWayPoints().size());
            WayPoint gpxWpt = gpxSeg.getWayPoints().iterator().next();
            WayPoint nmeaWpt = nmeaSeg.getWayPoints().iterator().next();
            assertEquals(gpxWpt.getCoor().getRoundedToOsmPrecision(), nmeaWpt.getCoor().getRoundedToOsmPrecision());
        }
    }

    /**
     * Unit test of {@link NmeaReader#isSentence}.
     */
    @Test
    public void testIsSentence() {
        assertTrue(NmeaReader.isSentence("$GPVTG", Sentence.VTG));
        assertTrue(NmeaReader.isSentence("$GAVTG", Sentence.VTG));
        assertTrue(NmeaReader.isSentence("$GNVTG", Sentence.VTG));
        assertFalse(NmeaReader.isSentence("XGAVTG", Sentence.VTG));
        assertFalse(NmeaReader.isSentence("$GPXXX", Sentence.VTG));
        assertFalse(NmeaReader.isSentence("$XXVTG", Sentence.VTG));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/1433">Bug #1433</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket1433() throws Exception {
        compareWithReference(1433, "2008-08-14-16-04-58", 1241);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/1853">Bug #1853</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket1853() throws Exception {
        compareWithReference(1853, "PosData-20081216-115434", 1285);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/2147">Bug #2147</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket2147() throws Exception {
        compareWithReference(2147, "WG20080203171807.log", 487);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14924">Bug #14924</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket14924() throws Exception {
        compareWithReference(14924, "input", 0);
    }

    private static GpxData read(String nmeaLine) throws IOException, SAXException {
        NmeaReader in = new NmeaReader(new ByteArrayInputStream(nmeaLine.getBytes(StandardCharsets.UTF_8)));
        in.parse(true);
        return in.data;
    }

    private static WayPoint readWayPoint(String nmeaLine) throws IOException, SAXException {
        return read(nmeaLine).tracks.iterator().next().getSegments().iterator().next().getWayPoints().iterator().next();
    }

    private static Date readDate(String nmeaLine) throws IOException, SAXException {
        return readWayPoint(nmeaLine).getDate();
    }

    private static double readSpeed(String nmeaLine) throws IOException, SAXException {
        return Double.parseDouble(readWayPoint(nmeaLine).getString("speed"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16496">Bug #16496</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket16496() throws Exception {
        assertEquals("2018-05-30T16:28:59.400Z", iso8601.format(
                readDate("$GNRMC,162859.400,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*13")));
        assertEquals("2018-05-30T16:28:59.400Z", iso8601.format(
                readDate("$GNRMC,162859.40,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*23")));
        assertEquals("2018-05-30T16:28:59.400Z", iso8601.format(
                readDate("$GNRMC,162859.4,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*13")));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16554">Bug #16554</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket16554() throws Exception {
        assertEquals(63.2420959, readSpeed(
                  "$GNRMC,141448.80,A,4659.05514,N,00130.44695,W,34.148,289.80,300718,,,D,V*26"), 1e-7);
        assertEquals(63.2430000, readSpeed(
                  "$GNRMC,141448.80,A,4659.05514,N,00130.44695,W,34.148,289.80,300718,,,D,V*26"
                + "$GNVTG,289.80,T,,M,34.148,N,63.243,K,D*27"), 1e-7);
    }
}
