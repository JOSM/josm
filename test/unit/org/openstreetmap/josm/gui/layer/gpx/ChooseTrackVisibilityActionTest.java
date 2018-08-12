// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ChooseTrackVisibilityAction} class.
 */
public class ChooseTrackVisibilityActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test action.
     * @throws Exception if an error occurs
     */
    @Test
    public void testAction() throws Exception {
        new ChooseTrackVisibilityAction(GpxLayerTest.getMinimalGpxLayer()).actionPerformed(null);
    }
}
