// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTimeOffset;
import org.openstreetmap.josm.data.gpx.GpxTimezone;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link CorrelateGpxWithImages} class.
 */
public class CorrelateGpxWithImagesTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        DateUtilsTest.setTimeZone(DateUtils.UTC);
    }

    /**
     * Tests automatic guessing of timezone/offset
     * @throws Exception if an error occurs
     */
    @Test
    public void testAutoGuess() throws Exception {
        final GpxData gpx = GpxReaderTest.parseGpxData("nodist/data/2094047.gpx");
        final ImageEntry i0 = new ImageEntry();
        i0.setExifTime(DateUtils.fromString("2016:01:03 11:59:54")); // 4 sec before start of GPX
        i0.createTmp();
        assertEquals(Pair.create(GpxTimezone.ZERO, GpxTimeOffset.seconds(-4)),
                CorrelateGpxWithImages.autoGuess(Collections.singletonList(i0), gpx));
    }
}
