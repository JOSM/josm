// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link ManualAuthorizationUI} class.
 */
public class ManualAuthorizationUITest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ManualAuthorizationUI#ManualAuthorizationUI}.
     */
    @Test
    public void testManualAuthorizationUI() {
        assertNotNull(new ManualAuthorizationUI("", Main.worker));
    }
}
