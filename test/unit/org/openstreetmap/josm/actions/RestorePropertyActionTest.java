// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RestorePropertyAction}
 */
class RestorePropertyActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    @Test
    void testTicket20965() {
        doTest20965(null, null);
        doTest20965("key", null);
        doTest20965(null, "val");
        doTest20965("key", "val");
    }

    private static void doTest20965(String key, String val) {
        ListSelectionModel selModel = new DefaultListSelectionModel();
        selModel.setSelectionInterval(1, 2);
        Node n = new Node(LatLon.NORTH_POLE);
        new DataSet(n);
        UndoRedoHandler.getInstance().clean();
        new RestorePropertyAction(k -> key, v -> val, () -> n, selModel).actionPerformed(null);
        assertInstanceOf(ChangePropertyCommand.class, UndoRedoHandler.getInstance().getLastCommand());
    }
}
