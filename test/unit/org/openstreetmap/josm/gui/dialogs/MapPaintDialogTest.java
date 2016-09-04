// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MapPaintDialog} class.
 */
public class MapPaintDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().commands();

    /**
     * Unit test of {@link MapPaintDialog.InfoAction} class.
     */
    @Test
    public void testInfoAction() {
        Main.map.mapPaintDialog.new InfoAction().actionPerformed(null);
    }
}
