// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.io.UploadStrategy;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApi;

/**
 * Unit tests of {@link UploadStrategySelectionPanel} class.
 */
@BasicPreferences
@OsmApi(OsmApi.APIType.DEV)
class UploadStrategySelectionPanelTest {
    /**
     * Test of {@link UploadStrategySelectionPanel#UploadStrategySelectionPanel}.
     */
    @Test
    void testUploadStrategySelectionPanel() {
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
    void testUploadStrategySpecification() {
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
