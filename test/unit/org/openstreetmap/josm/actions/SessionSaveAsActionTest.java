// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SessionSaveAsAction}.
 */
class SessionSaveAsActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Unit test of {@link SessionSaveAsAction#actionPerformed}
     */
    @Test
    void testSessionSaveAsAction() {
        SessionSaveAsAction action = new SessionSaveAsAction();
        assertFalse(action.isEnabled());
        action.actionPerformed(null);
    }
}
