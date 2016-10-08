// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ExitAction}.
 */
public final class ExitActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * System.exit rule
     */
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    /**
     * Unit test of {@link ExitAction#actionPerformed}
     */
    @Test
    public void testActionPerformed() {
        exit.expectSystemExitWithStatus(0);
        // No layer
        new ExitAction().actionPerformed(null);
    }
}
