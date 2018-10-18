// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertEquals;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

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
        TestUtils.assumeWorkingJMockit();
        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker() {
            @Override
            protected String getString(final ExtendedDialog instance) {
                return ((JLabel) ((JPanel) this.getContent(instance)).getComponent(2)).getText();
            }
        };
        edMocker.getMockResultMap().put(
            "<html>Select all tracks that you want to be displayed. You can drag select a range of " +
            "tracks or use CTRL+Click to select specific ones. The map is updated live in the " +
            "background. Open the URLs by double clicking them.</html>",
            "Show all"
        );

        new ChooseTrackVisibilityAction(GpxLayerTest.getMinimalGpxLayer()).actionPerformed(null);

        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Set track visibility for Bananas", invocationLogEntry[2]);
    }
}
