// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        assertEquals(new GregorianCalendar(2010, Calendar.MAY, 15, 17, 12, 05).getTime(), date);

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        date = ExifReader.readTime(directionSampleFile);
        TimeZone.setDefault(DateUtils.UTC);
        assertEquals(new GregorianCalendar(2010, Calendar.MAY, 15, 15, 12, 05).getTime(), date);
    }

    /**
     * Tests reading sub-seconds from the EXIF header
     * @throws ParseException if {@link ExifReader#readTime} fails to parse date/time of sample file
     */
    @Test
    public void testReadTimeSubSecond1() throws ParseException {
        Date date = ExifReader.readTime(new File("data_nodist/IMG_20150711_193419.jpg"));
        String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(date);
        assertEquals("2015-07-11T19:34:19.100", dateStr);

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        date = ExifReader.readTime(new File("data_nodist/IMG_20150711_193419.jpg"));
        TimeZone.setDefault(DateUtils.UTC);
        dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(date);
        assertEquals("2015-07-11T17:34:19.100", dateStr);
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
        assertEquals("51°46'"+f.format(43.0)+"\"", LatLon.dms(latlon.lat()));
        assertEquals("8°21'"+f.format(56.3)+"\"", LatLon.dms(latlon.lon()));
    }

    /**
     * Test direction extraction
     */
    @Test
    public void testReadDirection() {
        assertEquals(Double.valueOf(46.5), ExifReader.readDirection(directionSampleFile));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11685">#11685</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket11685() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(11685, "2015-11-08_15-33-27-Xiaomi_YI-Y0030832.jpg"));
        String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(ExifReader.readTime(file));
        assertEquals("2015-11-08T15:33:27.500", dateStr);
    }
}
