// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable.DoubleClickAdapter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SelectionTable} class.
 */
public class SelectionTableTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link SelectionTable#SelectionTable}.
     */
    @Test
    public void testSelectionTable() {
        // Constructs a relation with a member
        DataSet ds = new DataSet();
        Node n = new Node(LatLon.ZERO);
        Relation r = new Relation();
        r.addMember(new RelationMember(null, n));
        // Add it to dataset
        ds.addPrimitive(n);
        ds.addPrimitive(r);
        // Add a new layer as active one
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            // Constructs models and table
            MemberTableModel memberTableModel = new MemberTableModel(r, layer, null);
            memberTableModel.populate(r);
            memberTableModel.register();
            SelectionTableModel selectionTableModel = new SelectionTableModel(layer);
            selectionTableModel.register();
            try {
                SelectionTable t = new SelectionTable(selectionTableModel, memberTableModel);
                DoubleClickAdapter adapter = null;
                for (MouseListener listener : t.getMouseListeners()) {
                    if (listener instanceof DoubleClickAdapter) {
                        adapter = (DoubleClickAdapter) listener;
                    }
                }
                assertNotNull(adapter);
                // Select member, trigger selection change events
                ds.setSelected(n);
                // Simple left click, do nothing
                adapter.mouseClicked(new MouseEvent(t, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0,
                        1, false, MouseEvent.BUTTON1));
                assertEquals(-1, memberTableModel.getSelectionModel().getMinSelectionIndex());
                // Double right click, do nothing
                adapter.mouseClicked(new MouseEvent(t, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0,
                        2, false, MouseEvent.BUTTON2));
                assertEquals(-1, memberTableModel.getSelectionModel().getMinSelectionIndex());
                // Double left click, select member
                adapter.mouseClicked(new MouseEvent(t, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0,
                        2, false, MouseEvent.BUTTON1));
                assertEquals(0, memberTableModel.getSelectionModel().getMinSelectionIndex());
            } finally {
                selectionTableModel.unregister();
                memberTableModel.unregister();
            }
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
