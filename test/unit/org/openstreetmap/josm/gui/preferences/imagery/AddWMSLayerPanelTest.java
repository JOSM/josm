// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddWMSLayerPanel} class.
 */
public class AddWMSLayerPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().preferences();

    /**
     * Unit test of {@link AddWMSLayerPanel}.
     */
    @Test
    public void testAddWMSLayerPanel() {
        AddWMSLayerPanel panel = new AddWMSLayerPanel();
        assertFalse(panel.isImageryValid());
    }
}
