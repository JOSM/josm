// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import java.util.Collections;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ExportProfileAction} class.
 */
@BasicPreferences
class ExportProfileActionTest {
    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().assertionsInEDT();

    /**
     * Unit test of {@link ExportProfileAction#actionPerformed}.
     */
    @Test
    void testAction() {
        TestUtils.assumeWorkingJMockit();
        new JOptionPaneSimpleMocker(Collections.singletonMap(
            "All the preferences of this group are default, nothing to save", JOptionPane.OK_OPTION
        ));
        new ExportProfileAction(Preferences.main(), "foo", "bar").actionPerformed(null);
        new ExportProfileAction(Preferences.main(), "expert", "expert").actionPerformed(null);
    }
}
