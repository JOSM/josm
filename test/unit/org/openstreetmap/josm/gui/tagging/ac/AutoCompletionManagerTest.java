// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager.UserInputTag;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link AutoCompletionManager} class.
 */
@BasicPreferences
@LayerEnvironment
class AutoCompletionManagerTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/17064">#17064</a>.
     */
    @Test
    void testTicket17064() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "testTicket17064", null);
        MainApplication.getLayerManager().addLayer(layer);
        AutoCompletionManager.of(ds);
        MainApplication.getLayerManager().removeLayer(layer); // NPE in #17064
    }

    /**
     * Unit test of methods {@link UserInputTag#equals} and {@link UserInputTag#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(UserInputTag.class).usingGetClass()
            .verify();
    }
}
