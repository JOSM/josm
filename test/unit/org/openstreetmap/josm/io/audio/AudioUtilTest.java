// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link AudioUtil} class.
 */
@BasicPreferences
class AudioUtilTest {

    private static final double EPSILON = 1e-11;

    /**
     * Test method for {@code AudioUtil#getCalibratedDuration(File)}
     */
    @Test
    void testGetCalibratedDuration() {
        assertEquals(0.0, AudioUtil.getCalibratedDuration(new File("invalid_file")), EPSILON);
        File wav1 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121226.wav"));
        assertEquals(4.8317006802721085, AudioUtil.getCalibratedDuration(wav1), EPSILON);
        File wav2 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121557.wav"));
        assertEquals(4.924580498866213, AudioUtil.getCalibratedDuration(wav2), EPSILON);
    }
}
