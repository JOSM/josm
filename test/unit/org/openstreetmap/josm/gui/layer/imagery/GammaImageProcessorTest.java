// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for the {@link GammaImageProcessor} class.
 * @author Michael Zangl
 */
public class GammaImageProcessorTest {

    /**
     * No special rules
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link GammaImageProcessor#setGamma(double)} and {@link GammaImageProcessor#getGamma()}
     */
    @Test
    public void testSetGet() {
        GammaImageProcessor processor = new GammaImageProcessor();

        assertEquals(1, processor.getGamma(), 0.001);

        processor.setGamma(2);
        assertEquals(2, processor.getGamma(), 0.001);

        processor.setGamma(0);
        assertEquals(0, processor.getGamma(), 0.001);

        processor.setGamma(0.78);
        assertEquals(0.78, processor.getGamma(), 0.001);

        processor.setGamma(1);
        assertEquals(1, processor.getGamma(), 0.001);

        processor.setGamma(-1);
        assertEquals(-1, processor.getGamma(), 0.001);

        processor.setGamma(5);
        assertEquals(5, processor.getGamma(), 0.001);
    }

    /**
     * Test {@link GammaImageProcessor#toString()}
     */
    @Test
    public void testToString() {
        GammaImageProcessor processor = new GammaImageProcessor();
        assertEquals("GammaImageProcessor [gamma=1.0]", processor.toString());
    }
}
