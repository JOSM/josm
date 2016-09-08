// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddWMTSLayerPanel} class.
 */
public class AddWMTSLayerPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link AddWMTSLayerPanel}.
     */
    @Test
    public void testAddWMTSLayerPanel() {
        AddWMTSLayerPanel panel = new AddWMTSLayerPanel();
        assertFalse(panel.isImageryValid());
    }

    /**
     * Unit test of {@link AddWMTSLayerPanel#getImageryInfo}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetImageryInfo() {
        new AddWMTSLayerPanel().getImageryInfo();
    }
}
