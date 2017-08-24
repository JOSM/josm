// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DeleteLayerAction}.
 */
public final class DeleteLayerActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Unit test of {@link DeleteLayerAction#actionPerformed}
     */
    @Test
    public void testActionPerformed() {
        DeleteLayerAction action = new DeleteLayerAction();
        // No layer
        action.actionPerformed(null);
        // OsmDataLayer
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());
        action.actionPerformed(null);
        assertNull(MainApplication.getLayerManager().getActiveLayer());
    }
}
