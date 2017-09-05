// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.data.coor.conversion.DMSCoordinateFormat;

/**
 * EXIF metadata extraction test
 * @since 6209
 */
public class ExifReaderTest {
    /**
     * Set the timezone and timeout.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private File orientationSampleFile, directionSampleFile;

    /**
     * Setup test
     */
    @Before
    public void setUp() {
        directionSampleFile = new File("data_nodist/exif-example_direction.jpg");
        orientationSampleFile = new File("data_nodist/exif-example_orientation=6.jpg");
    }

    /**
     * Test time extraction
     * @throws ParseException if {@link ExifReader#readTime} fails to parse date/time of sample file
     */
    @Test
    public void testReadTime() throws ParseException {
        Date date = ExifReader.readTime(directionSampleFile);
        assertEquals(ZonedDateTime.of(2010, 5, 15, 17, 12, 5, 0, ZoneId.systemDefault()).toInstant(), date.toInstant());

        final TimeZone zone = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault(zone);
        date = ExifReader.readTime(directionSampleFile);
        TimeZone.setDefault(DateUtils.UTC);
        assertEquals(ZonedDateTime.of(2010, 5, 15, 17, 12, 5, 0, zone.toZoneId()).toInstant(), date.toInstant());
    }

    /**
     * Tests reading sub-seconds from the EXIF header
     * @throws ParseException if {@link ExifReader#readTime} fails to parse date/time of sample file
     */
    @Test
    public void testReadTimeSubSecond1() throws ParseException {
        Date date = ExifReader.readTime(new File("data_nodist/IMG_20150711_193419.jpg"));
        doTest("2015-07-11T19:34:19.100", date);

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        date = ExifReader.readTime(new File("data_nodist/IMG_20150711_193419.jpg"));
        TimeZone.setDefault(DateUtils.UTC);
        doTest("2015-07-11T17:34:19.100", date);
    }

    private static void doTest(String expectedDate, Date parsedDate) {
        assertEquals(expectedDate, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(parsedDate));
    }

    private static void doTestFile(String expectedDate, int ticket, String filename) {
        doTest(expectedDate, ExifReader.readTime(new File(TestUtils.getRegressionDataFile(ticket, filename))));
    }

    /**
     * Test orientation extraction
     */
    @Test
    public void testReadOrientation() {
        Integer orientation = ExifReader.readOrientation(orientationSampleFile);
        assertEquals(Integer.valueOf(6), orientation);
    }

    /**
     * Test coordinates extraction
     */
    @Test
    public void testReadLatLon() {
        LatLon latlon = ExifReader.readLatLon(directionSampleFile);
        assertNotNull(latlon);
        DecimalFormat f = new DecimalFormat("00.0");
        assertEquals("51°46'"+f.format(43.0)+"\"", DMSCoordinateFormat.degreesMinutesSeconds(latlon.lat()));
        assertEquals("8°21'"+f.format(56.3)+"\"", DMSCoordinateFormat.degreesMinutesSeconds(latlon.lon()));
    }

    /**
     * Test direction extraction
     */
    @Test
    public void testReadDirection() {
        assertEquals(Double.valueOf(46.5), ExifReader.readDirection(directionSampleFile));
    }

    /**
     * Test speed extraction
     */
    @Test
    public void testReadSpeed() {
        assertEquals(Double.valueOf(12.3), ExifReader.readSpeed(new File("data_nodist/exif-example_speed_ele.jpg")));
    }

    /**
     * Test elevation extraction
     */
    @Test
    public void testReadElevation() {
        assertEquals(Double.valueOf(23.4), ExifReader.readElevation(new File("data_nodist/exif-example_speed_ele.jpg")));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11685">#11685</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket11685() throws IOException {
        doTestFile("2015-11-08T15:33:27.500", 11685, "2015-11-08_15-33-27-Xiaomi_YI-Y0030832.jpg");
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14209">#14209</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket14209() throws IOException {
        doTestFile("2017-01-16T18:27:00.000", 14209, "0MbEfj1S--.1.jpg");
        doTestFile("2016-08-13T19:51:13.000", 14209, "7VWFOryj--.1.jpg");
    }
}
