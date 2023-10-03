// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link NmeaReader} class.
 */
class NmeaReaderTest {
    /**
     * Tests reading a nmea file.
     * @throws Exception if any error occurs
     */
    @Test
    void testReader() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        final NmeaReader in = new NmeaReader(Files.newInputStream(Paths.get("nodist/data/btnmeatrack_2016-01-25.nmea")));
        in.parse(true);
        assertEquals(30, in.getNumberOfCoordinates());
        assertEquals(0, in.getParserMalformed());

        final List<WayPoint> wayPoints = new ArrayList<>(in.data.tracks.iterator().next().getSegments().iterator().next().getWayPoints());
        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.200Z"), wayPoints.get(0).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.400Z"), wayPoints.get(1).get(GpxConstants.PT_TIME));
        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.600Z"), wayPoints.get(2).get(GpxConstants.PT_TIME));
        assertEquals(wayPoints.get(0).getInstant(), wayPoints.get(0).get(GpxConstants.PT_TIME));

        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.200Z"), wayPoints.get(0).getInstant());
        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.400Z"), wayPoints.get(1).getInstant());
        assertEquals(DateUtils.parseInstant("2016-01-25T05:05:09.600Z"), wayPoints.get(2).getInstant());

        assertEquals(new LatLon(46.98807, -1.400525), wayPoints.get(0).getCoor());
        assertEquals("38.9", wayPoints.get(0).get(GpxConstants.PT_ELE));
        assertEquals("16", wayPoints.get(0).get(GpxConstants.PT_SAT));
        assertEquals("3d", wayPoints.get(0).get(GpxConstants.PT_FIX));
        assertEquals("0.7", wayPoints.get(0).get(GpxConstants.PT_HDOP).toString().trim());
        assertNull(wayPoints.get(0).get(GpxConstants.PT_VDOP));
        assertNull(wayPoints.get(0).get(GpxConstants.PT_PDOP));
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
        IGpxTrack gpxTrack = gpx.tracks.iterator().next();
        IGpxTrack nmeaTrack = in.data.tracks.iterator().next();
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
    void testIsSentence() {
        assertTrue(NmeaParser.isSentence("$GPVTG", Sentence.VTG));
        assertTrue(NmeaParser.isSentence("$GAVTG", Sentence.VTG));
        assertTrue(NmeaParser.isSentence("$GNVTG", Sentence.VTG));
        assertFalse(NmeaParser.isSentence("XGAVTG", Sentence.VTG));
        assertFalse(NmeaParser.isSentence("$GPXXX", Sentence.VTG));
        assertFalse(NmeaParser.isSentence("$XXVTG", Sentence.VTG));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/1433">Bug #1433</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket1433() throws Exception {
        compareWithReference(1433, "2008-08-14-16-04-58", 1241);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/1853">Bug #1853</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket1853() throws Exception {
        compareWithReference(1853, "PosData-20081216-115434", 1285);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/2147">Bug #2147</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket2147() throws Exception {
        compareWithReference(2147, "WG20080203171807.log", 487);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14924">Bug #14924</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket14924() throws Exception {
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

    private static Instant readDate(String nmeaLine) throws IOException, SAXException {
        return readWayPoint(nmeaLine).getInstant();
    }

    private static double readSpeed(String nmeaLine) throws IOException, SAXException {
        return Double.parseDouble(readWayPoint(nmeaLine).getString("speed"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16496">Bug #16496</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket16496() throws Exception {
        assertEquals(DateUtils.parseInstant("2018-05-30T16:28:59.400Z"),
                readDate("$GNRMC,162859.400,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*13"));
        assertEquals(DateUtils.parseInstant("2018-05-30T16:28:59.400Z"),
                readDate("$GNRMC,162859.40,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*23"));
        assertEquals(DateUtils.parseInstant("2018-05-30T16:28:59.400Z"),
                readDate("$GNRMC,162859.4,A,4543.03388,N,00058.19870,W,45.252,209.07,300518,,,D,V*13"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16554">Bug #16554</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket16554() throws Exception {
        assertEquals(63.2420959, readSpeed(
                  "$GNRMC,141448.80,A,4659.05514,N,00130.44695,W,34.148,289.80,300718,,,D,V*26"), 1e-7);
        assertEquals(63.2430000, readSpeed(
                  "$GNRMC,141448.80,A,4659.05514,N,00130.44695,W,34.148,289.80,300718,,,D,V*26"
                + "$GNVTG,289.80,T,,M,34.148,N,63.243,K,D*27"), 1e-7);
    }
}
