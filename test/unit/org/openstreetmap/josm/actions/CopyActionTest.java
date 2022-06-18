// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link CopyAction}.
 */
class CopyActionTest {
    private static final class CapturingCopyAction extends CopyAction {
        private boolean warningShown;

        @Override
        protected void showEmptySelectionWarning() {
            warningShown = true;
        }
    }

    /**
     * We need prefs for this.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().preferences().fakeAPI().main().projection();

    /**
     * Test that copy action copies the selected primitive
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedFlavorException if the requested data flavor is not supported
     */
    @Test
    void testWarnOnEmpty() throws UnsupportedFlavorException, IOException {
        Clipboard clipboard = ClipboardUtils.getClipboard();
        clipboard.setContents(new StringSelection("test"), null);

        CapturingCopyAction action = new CapturingCopyAction();

        action.updateEnabledState();
        assertFalse(action.isEnabled());
        action.actionPerformed(null);
        assertTrue(action.warningShown);

        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "test", null));
        action.warningShown = false;

        action.updateEnabledState();
        assertFalse(action.isEnabled());
        action.actionPerformed(null);
        assertTrue(action.warningShown);

        assertEquals("test", clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor));
    }

    /**
     * Test that copy action copies the selected primitive
     * @throws Exception if an error occurs
     */
    @Test
    void testCopySinglePrimitive() throws Exception {
        DataSet data = new DataSet();

        Node node1 = new Node();
        node1.setCoor(LatLon.ZERO);
        data.addPrimitive(node1);

        Node node2 = new Node();
        node2.setCoor(LatLon.ZERO);
        data.addPrimitive(node2);
        Way way = new Way();
        way.setNodes(Arrays.asList(node1, node2));
        data.addPrimitive(way);
        data.setSelected(way);

        MainApplication.getLayerManager().addLayer(new OsmDataLayer(data, "test", null));

        CopyAction action = new CopyAction() {
            @Override
            protected void showEmptySelectionWarning() {
                fail("Selection is not empty.");
            }
        };
        action.updateEnabledState();
        assertTrue(action.isEnabled());
        action.actionPerformed(null);

        Object copied = ClipboardUtils.getClipboard().getContents(null).getTransferData(PrimitiveTransferData.DATA_FLAVOR);
        assertNotNull(copied);
        assertTrue(copied instanceof PrimitiveTransferData);
        PrimitiveTransferData ptd = (PrimitiveTransferData) copied;
        Object[] direct = ptd.getDirectlyAdded().toArray();
        assertEquals(1, direct.length);
        Object[] referenced = ptd.getReferenced().toArray();
        assertEquals(2, referenced.length);
    }
}
