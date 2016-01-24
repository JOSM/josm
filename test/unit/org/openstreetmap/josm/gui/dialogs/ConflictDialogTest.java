// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Color;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ConflictDialog} class.
 */
public class ConflictDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ConflictDialog#ConflictDialog}.
     */
    @Test
    public void testConflictDialog() {
        assertNotNull(new ConflictDialog());
    }

    /**
     * Unit test of {@link ConflictDialog#getColor} method.
     */
    @Test
    public void testGetColor() {
        assertEquals(Color.gray, ConflictDialog.getColor());
    }
}
