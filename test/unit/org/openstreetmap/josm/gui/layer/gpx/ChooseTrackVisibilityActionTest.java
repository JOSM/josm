// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

/**
 * Unit tests of {@link ChooseTrackVisibilityAction} class.
 */
@BasicPreferences
class ChooseTrackVisibilityActionTest {
    /**
     * Test action.
     * @throws Exception if an error occurs
     */
    @Test
    @Disabled("broken, see #16796")
    void testAction() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker() {
            @Override
            protected String getString(final ExtendedDialog instance) {
                return ((JLabel) ((JPanel) instance.getContentPane().getComponent(0)).getComponent(2)).getText();
            }
        };
        edMocker.getMockResultMap().put(
                "<html>Select all tracks that you want to be displayed. " +
                        "You can drag select a range of tracks or use CTRL+Click to select specific ones. " +
                        "The map is updated live in the background. "+
                        "Open the URLs by double clicking them, edit name and description by double clicking the cell.</html>",
                        "Show all"
                );

        new ChooseTrackVisibilityAction(GpxLayerTest.getMinimalGpxLayer()).actionPerformed(null);

        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(2, (int) invocationLogEntry[0]);
        assertEquals("Set track visibility for Bananas", invocationLogEntry[2]);
    }
}
