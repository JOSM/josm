// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link FullyAutomaticAuthorizationUI} class.
 */
public class FullyAutomaticAuthorizationUITest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link FullyAutomaticAuthorizationUI#FullyAutomaticAuthorizationUI}.
     */
    @Test
    public void testFullyAutomaticAuthorizationUI() {
        assertNotNull(new FullyAutomaticAuthorizationUI("", MainApplication.worker));
    }
}
