// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * Unit tests of {@link NoteImporter} class.
 */
public class NoteImporterTest {

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12531">Bug #12531</a>.
     */
    @Test
    public void testTicket12531() {
        if (Main.map != null) {
            for (Layer l: Main.getLayerManager().getLayers()) {
                Main.map.mapView.removeLayer(l);
            }
            Main.main.setMapFrame(null);
        }
        assertNull(Main.map);
        assertTrue(new NoteImporter().importDataHandleExceptions(
                new File(TestUtils.getRegressionDataFile(12531, "notes.osn")), null));
    }
}
