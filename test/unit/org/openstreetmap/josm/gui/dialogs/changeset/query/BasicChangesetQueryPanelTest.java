// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link BasicChangesetQueryPanel} class.
 */
public class BasicChangesetQueryPanelTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link BasicChangesetQueryPanel#BasicChangesetQueryPanel}.
     */
    @Test
    public void testBasicChangesetQueryPanel() {
        assertNotNull(new BasicChangesetQueryPanel());
    }
}
