// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link MapPaintDialog} class.
 */
public class MapPaintDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link MapPaintDialog.InfoAction} class.
     */
    @Test
    public void testInfoAction() {
        Main.map.mapPaintDialog.new InfoAction().actionPerformed(null);
    }
}
