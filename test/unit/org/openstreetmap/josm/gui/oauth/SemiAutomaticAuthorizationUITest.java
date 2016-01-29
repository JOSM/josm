// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link SemiAutomaticAuthorizationUI} class.
 */
public class SemiAutomaticAuthorizationUITest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link SemiAutomaticAuthorizationUI#SemiAutomaticAuthorizationUI}.
     */
    @Test
    public void testSemiAutomaticAuthorizationUI() {
        assertNotNull(new SemiAutomaticAuthorizationUI("", Main.worker));
    }
}
