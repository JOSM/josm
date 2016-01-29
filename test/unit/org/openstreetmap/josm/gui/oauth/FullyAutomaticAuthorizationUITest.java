// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link FullyAutomaticAuthorizationUI} class.
 */
public class FullyAutomaticAuthorizationUITest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link FullyAutomaticAuthorizationUI#FullyAutomaticAuthorizationUI}.
     */
    @Test
    public void testFullyAutomaticAuthorizationUI() {
        assertNotNull(new FullyAutomaticAuthorizationUI("", Main.worker));
    }
}
