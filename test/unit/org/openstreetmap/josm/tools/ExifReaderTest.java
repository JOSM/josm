// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * EXIF metadata extraction test
 * @since 6209
 */
public class ExifReaderTest {

    private File sampleFile;
    
    /**
     * Setup test
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        sampleFile = new File("data_nodist/exif-direction-example.jpg");
    }

    /** 
     * Test time extraction
     * @throws ParseException 
     */
    @Test
    public void testReadTime() throws ParseException {
        Date date = ExifReader.readTime(sampleFile);
        assertEquals(new GregorianCalendar(2010, Calendar.MAY, 15, 17, 12, 05).getTime(), date);
    }

    /**
     * Test orientation extraction
     */
    @Test
    public void testReadOrientation() {
        Integer orientation = ExifReader.readOrientation(sampleFile);
        assertEquals(Integer.valueOf(1), orientation);
    }
    
    /**
     * Test coordinates extraction
     */
    @Test
    public void testReadLatLon() {
        LatLon latlon = ExifReader.readLatLon(sampleFile);
        assertNotNull(latlon);
        DecimalFormat f = new DecimalFormat("00.0");
        assertEquals("51°46'"+f.format(43.0)+"\"", LatLon.dms(latlon.lat()));
        assertEquals("8°21'"+f.format(56.3)+"\"", LatLon.dms(latlon.lon()));
    }

    /**
     * Test coordinates extraction
     */
    @Test
    public void testReadDirection() {
        Double direction = ExifReader.readDirection(sampleFile);
        assertEquals(new Double(46.5), direction);
    }
}
