// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.AbstractCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.DMSCoordinateFormat;
import org.openstreetmap.josm.testutils.annotations.TimeZoneAnnotation;

/**
 * EXIF metadata extraction test
 * @since 6209
 */
@TimeZoneAnnotation
class ExifReaderTest {
    private File orientationSampleFile, directionSampleFile;

    /**
     * Setup test
     */
    @BeforeEach
    public void setUp() {
        directionSampleFile = new File("nodist/data/exif-example_direction.jpg");
        orientationSampleFile = new File("nodist/data/exif-example_orientation=6.jpg");
    }

    /**
     * Test time extraction
     */
    @Test
    void testReadTime() {
        Instant date = ExifReader.readInstant(directionSampleFile);
        assertEquals(Instant.parse("2010-05-15T17:12:05.000Z"), date);
    }

    /**
     * Tests reading sub-seconds from the EXIF header
     */
    @Test
    void testReadTimeSubSecond1() {
        Instant date = ExifReader.readInstant(new File("nodist/data/IMG_20150711_193419.jpg"));
        assertEquals(Instant.parse("2015-07-11T19:34:19.100Z"), date);
    }

    private static void doTestFile(String expectedDate, int ticket, String filename) {
        Instant date = ExifReader.readInstant(new File(TestUtils.getRegressionDataFile(ticket, filename)));
        assertEquals(Instant.parse(expectedDate), date);
    }

    /**
     * Test orientation extraction
     */
    @Test
    void testReadOrientation() {
        Integer orientation = ExifReader.readOrientation(orientationSampleFile);
        assertEquals(Integer.valueOf(6), orientation);
    }

    /**
     * Test coordinates extraction
     */
    @Test
    void testReadLatLon() {
        LatLon latlon = ExifReader.readLatLon(directionSampleFile);
        assertNotNull(latlon);
        DecimalFormat f = AbstractCoordinateFormat.newUnlocalizedDecimalFormat("00.0");
        assertEquals("51°46'"+f.format(43.0)+"\"", DMSCoordinateFormat.degreesMinutesSeconds(latlon.lat()));
        assertEquals("8°21'"+f.format(56.3)+"\"", DMSCoordinateFormat.degreesMinutesSeconds(latlon.lon()));
    }

    /**
     * Test direction extraction
     */
    @Test
    void testReadDirection() {
        assertEquals(Double.valueOf(46.5), ExifReader.readDirection(directionSampleFile));
    }

    /**
     * Test speed extraction
     */
    @Test
    void testReadSpeed() {
        assertEquals(Double.valueOf(12.3), ExifReader.readSpeed(new File("nodist/data/exif-example_speed_ele.jpg")));
    }

    /**
     * Test elevation extraction
     */
    @Test
    void testReadElevation() {
        assertEquals(Double.valueOf(23.4), ExifReader.readElevation(new File("nodist/data/exif-example_speed_ele.jpg")));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11685">#11685</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    void testTicket11685() throws IOException {
        doTestFile("2015-11-08T15:33:27.500Z", 11685, "2015-11-08_15-33-27-Xiaomi_YI-Y0030832.jpg");
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14209">#14209</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    void testTicket14209() throws IOException {
        doTestFile("2017-01-16T18:27:00.000Z", 14209, "0MbEfj1S--.1.jpg");
        doTestFile("2016-08-13T19:51:13.000Z", 14209, "7VWFOryj--.1.jpg");
    }
}
