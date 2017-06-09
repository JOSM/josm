// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link AudioUtil} class.
 */
public class AudioUtilTest {

    private static final double EPSILON = 1e-11;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test method for {@code AudioUtil#getCalibratedDuration(File)}
     */
    @Test
    public void testGetCalibratedDuration() {
        assertEquals(0.0, AudioUtil.getCalibratedDuration(new File("invalid_file")), EPSILON);
        File wav1 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121226.wav"));
        assertEquals(4.8317006802721085, AudioUtil.getCalibratedDuration(wav1), EPSILON);
        File wav2 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121557.wav"));
        assertEquals(4.924580498866213, AudioUtil.getCalibratedDuration(wav2), EPSILON);
    }
}
