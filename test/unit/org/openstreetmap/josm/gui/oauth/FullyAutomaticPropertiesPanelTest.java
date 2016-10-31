// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link FullyAutomaticPropertiesPanel} class.
 */
public class FullyAutomaticPropertiesPanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link FullyAutomaticPropertiesPanel#FullyAutomaticPropertiesPanel}.
     */
    @Test
    public void testFullyAutomaticPropertiesPanel() {
        assertTrue(new FullyAutomaticPropertiesPanel().getComponentCount() > 0);
    }
}
