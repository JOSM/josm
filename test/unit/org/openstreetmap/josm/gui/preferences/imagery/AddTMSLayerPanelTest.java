// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddTMSLayerPanel} class.
 */
public class AddTMSLayerPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link AddTMSLayerPanel}.
     */
    @Test
    public void testAddTMSLayerPanel() {
        AddTMSLayerPanel panel = new AddTMSLayerPanel();
        assertEquals("", panel.getImageryInfo().getUrl());
        assertFalse(panel.isImageryValid());
    }
}
