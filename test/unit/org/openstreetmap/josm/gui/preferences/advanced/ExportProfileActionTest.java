// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link ExportProfileAction} class.
 */
public class ExportProfileActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ExportProfileAction#actionPerformed}.
     */
    @Test
    public void testAction() {
        new ExportProfileAction(Main.pref, "foo", "bar").actionPerformed(null);
        new ExportProfileAction(Main.pref, "expert", "expert").actionPerformed(null);
    }
}
