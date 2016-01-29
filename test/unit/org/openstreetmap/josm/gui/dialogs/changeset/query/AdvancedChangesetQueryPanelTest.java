// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link AdvancedChangesetQueryPanel} class.
 */
public class AdvancedChangesetQueryPanelTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AdvancedChangesetQueryPanel#AdvancedChangesetQueryPanel}.
     */
    @Test
    public void testAdvancedChangesetQueryPanel() {
        assertNotNull(new AdvancedChangesetQueryPanel());
    }
}
