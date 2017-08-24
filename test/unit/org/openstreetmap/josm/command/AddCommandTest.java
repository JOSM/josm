// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link AddCommand} class.
 */
public class AddCommandTest {

    /**
     * We need prefs for nodes.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();

    /**
     * Test if the add command is executed correctly and sets the modified flag.
     */
    @Test
    public void testAdd() {
        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "l1", null);
        MainApplication.getLayerManager().addLayer(layer1);
        assertArrayEquals(new Object[0], layer1.data.allPrimitives().toArray());

        Node osm = new Node(LatLon.ZERO);
        assertTrue(new AddCommand(osm).executeCommand());

        assertArrayEquals(new Object[] {osm}, layer1.data.allPrimitives().toArray());
        assertArrayEquals(new Object[] {osm}, layer1.data.allModifiedPrimitives().toArray());
        assertTrue(osm.isModified());
    }

    /**
     * Tests if the add command respects the layer.
     */
    @Test
    public void testAddToLayer() {
        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "l1", null);
        OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), "l1", null);

        MainApplication.getLayerManager().addLayer(layer1);
        MainApplication.getLayerManager().addLayer(layer2);

        Node osm = new Node(LatLon.ZERO);
        assertTrue(new AddCommand(layer2, osm).executeCommand());

        assertArrayEquals(new Object[0], layer1.data.allPrimitives().toArray());
        assertArrayEquals(new Object[] {osm}, layer2.data.allPrimitives().toArray());
    }

    /**
     * Test {@link AddCommand#undoCommand()}
     */
    @Test
    public void testUndo() {
        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "l1", null);
        MainApplication.getLayerManager().addLayer(layer1);
        Node osm = new Node(LatLon.ZERO);
        layer1.data.addPrimitive(osm);

        AddCommand command = new AddCommand(new Node(LatLon.ZERO));
        command.executeCommand();

        command.undoCommand();
        assertArrayEquals(new Object[] {osm}, layer1.data.allPrimitives().toArray());
    }

    /**
     * Test {@link AddCommand#getParticipatingPrimitives()}
     */
    @Test
    public void testParticipatingPrimitives() {
        Node osm = new Node(LatLon.ZERO);

        assertArrayEquals(new Object[] {osm}, new AddCommand(osm).getParticipatingPrimitives().toArray());
    }

    /**
     * Tests {@link AddCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    public void testFillModifiedData() {
        Node osm = new Node(LatLon.ZERO);

        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new AddCommand(osm).fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {osm}, added.toArray());
   }

    /**
     * Test {@link AddCommand#getDescriptionText()}
     */
    @Test
    public void testDescription() {
        Node node = new Node(LatLon.ZERO);
        node.put("name", "xy");
        Way way = new Way();
        way.addNode(node);
        way.put("name", "xy");
        Relation relation = new Relation();
        relation.put("name", "xy");

        assertTrue(new AddCommand(node).getDescriptionText().matches("Add node.*xy.*"));
        assertTrue(new AddCommand(way).getDescriptionText().matches("Add way.*xy.*"));
        assertTrue(new AddCommand(relation).getDescriptionText().matches("Add relation.*xy.*"));
    }

    /**
     * Unit test of methods {@link AddCommand#equals} and {@link AddCommand#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(AddCommand.class).usingGetClass()
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
            .withPrefabValues(DataSet.class,
                    new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
