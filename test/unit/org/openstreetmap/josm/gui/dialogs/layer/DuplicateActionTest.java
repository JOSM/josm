// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DuplicateAction} class.
 */
public class DuplicateActionTest {
    /**
     * TMS layer needs prefs. Platform for LayerListDialog shortcuts.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/4539">#4539</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket4539() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(4539, "josm_error_#4539.osm.zip")) {
            OsmDataLayer layer = new OsmDataLayer(OsmReader.parseDataSet(is, null), null, null);
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            assertNull(editLayer);
            try {
                new DuplicateAction(layer, null).actionPerformed(null);
                editLayer = MainApplication.getLayerManager().getEditLayer();
                assertNotNull(editLayer);
                assertFalse(layer.equals(editLayer));
                assertEquals(layer.data.getNodes().size(), editLayer.data.getNodes().size());
                assertEquals(layer.data.getWays().size(), editLayer.data.getWays().size());
                assertEquals(layer.data.getRelations().size(), editLayer.data.getRelations().size());
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                if (editLayer != null) {
                    MainApplication.getLayerManager().removeLayer(editLayer);
                }
            }
        }
    }
}
