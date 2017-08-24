// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ManualAuthorizationUI} class.
 */
public class ManualAuthorizationUITest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ManualAuthorizationUI#ManualAuthorizationUI}.
     */
    @Test
    public void testManualAuthorizationUI() {
        assertNotNull(new ManualAuthorizationUI("", MainApplication.worker));
    }
}
