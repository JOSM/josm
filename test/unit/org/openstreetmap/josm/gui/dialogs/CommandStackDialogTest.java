// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link CommandStackDialog} class.
 */
public class CommandStackDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().commands();

    /**
     * Unit test of {@link CommandStackDialog} class.
     */
    @Test
    public void testCommandStackDialog() {
        CommandStackDialog dlg = new CommandStackDialog();
        dlg.showDialog();
        assertTrue(dlg.isVisible());
        dlg.hideDialog();
        assertFalse(dlg.isVisible());
    }
}
