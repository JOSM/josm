// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link CommandStackDialog} class.
 */
@BasicPreferences
class CommandStackDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Unit test of {@link CommandStackDialog} class - empty case.
     */
    @Test
    void testCommandStackDialogEmpty() {
        CommandStackDialog dlg = new CommandStackDialog();
        dlg.showDialog();
        assertTrue(dlg.isVisible());
        dlg.hideDialog();
        assertFalse(dlg.isVisible());
    }

    /**
     * Unit test of {@link CommandStackDialog} class - not empty case.
     */
    @Test
    void testCommandStackDialogNotEmpty() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Command cmd1 = TestUtils.newCommand(ds);
            Command cmd2 = TestUtils.newCommand(ds);
            UndoRedoHandler.getInstance().add(cmd1);
            UndoRedoHandler.getInstance().add(cmd2);
            UndoRedoHandler.getInstance().undo(1);

            assertTrue(UndoRedoHandler.getInstance().hasUndoCommands());
            assertTrue(UndoRedoHandler.getInstance().hasRedoCommands());

            MapFrame map = MainApplication.getMap();
            CommandStackDialog dlg = new CommandStackDialog();
            map.addToggleDialog(dlg);
            dlg.unfurlDialog();
            assertTrue(dlg.isVisible());
            map.removeToggleDialog(dlg);
            dlg.hideDialog();
            assertFalse(dlg.isVisible());
        } finally {
            UndoRedoHandler.getInstance().clean();
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link CommandStackDialog} class - undo followed by addCommand should empty redo tree.
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16911">Bug #16911</a>.
     */
    @Test
    void testCommandStackDialogUndoAddCommand() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Command cmd1 = TestUtils.newCommand(ds);
            Command cmd2 = TestUtils.newCommand(ds);
            Command cmd3 = TestUtils.newCommand(ds);
            MapFrame map = MainApplication.getMap();
            CommandStackDialog dlg = new CommandStackDialog();
            map.addToggleDialog(dlg);
            dlg.unfurlDialog();
            assertTrue(dlg.isVisible());
            assertTrue(dlg.redoTreeIsEmpty());
            UndoRedoHandler.getInstance().add(cmd1);
            assertTrue(dlg.redoTreeIsEmpty());
            UndoRedoHandler.getInstance().add(cmd2);
            assertTrue(dlg.redoTreeIsEmpty());
            UndoRedoHandler.getInstance().undo(1);
            assertFalse(dlg.redoTreeIsEmpty());
            UndoRedoHandler.getInstance().add(cmd3);
            assertTrue(dlg.redoTreeIsEmpty());

            assertTrue(UndoRedoHandler.getInstance().hasUndoCommands());
            assertFalse(UndoRedoHandler.getInstance().hasRedoCommands());

            map.removeToggleDialog(dlg);
            dlg.hideDialog();
            assertFalse(dlg.isVisible());
        } finally {
            UndoRedoHandler.getInstance().clean();
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}

