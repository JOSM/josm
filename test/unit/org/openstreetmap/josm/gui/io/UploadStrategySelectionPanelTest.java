// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;

/**
 * Unit tests of {@link UploadStrategySelectionPanel} class.
 */
public class UploadStrategySelectionPanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
        try {
            OsmApi.getOsmApi().initialize(null);
        } catch (OsmTransferCanceledException | OsmApiInitializationException e) {
            Main.error(e);
        }
    }

    /**
     * Test of {@link UploadStrategySelectionPanel#UploadStrategySelectionPanel}.
     */
    @Test
    public void testUploadStrategySelectionPanel() {
        UploadStrategySelectionPanel p = new UploadStrategySelectionPanel();
        p.setNumUploadedObjects(Integer.MAX_VALUE);
        p.rememberUserInput();
        p.initFromPreferences();
        p.initEditingOfChunkSize();
    }

    /**
     * Test of {@link UploadStrategySelectionPanel#setUploadStrategySpecification}
     *       / {@link UploadStrategySelectionPanel#getUploadStrategySpecification}.
     */
    @Test
    public void testUploadStrategySpecification() {
        UploadStrategySelectionPanel p = new UploadStrategySelectionPanel();

        UploadStrategySpecification def = new UploadStrategySpecification();
        assertEquals(def, p.getUploadStrategySpecification());
        p.setUploadStrategySpecification(null);
        assertEquals(def, p.getUploadStrategySpecification());

        UploadStrategySpecification strat = new UploadStrategySpecification().setStrategy(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY);
        p.setUploadStrategySpecification(strat);
        assertEquals(strat, p.getUploadStrategySpecification());
    }
}
