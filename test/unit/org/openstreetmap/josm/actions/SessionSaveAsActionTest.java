// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SessionSaveAsAction}.
 */
public class SessionSaveAsActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link SessionSaveAsAction#actionPerformed}
     */
    @Test
    public void testSessionSaveAsAction() {
        SessionSaveAsAction action = new SessionSaveAsAction();
        assertFalse(action.isEnabled());
        action.actionPerformed(null);
    }
}
