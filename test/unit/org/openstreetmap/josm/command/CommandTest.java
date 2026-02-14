// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link Command} class.
 */
@I18n
// We need prefs for nodes / data sets.
@BasicPreferences
public class CommandTest {
    /**
     * Unit test of methods {@link Command#equals} and {@link Command#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(Command.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .withPrefabValues(ElemStyles.class,
                new ElemStyles(), new ElemStyles())
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    /**
     * A change test data consisting of two nodes and a way.
     * @author Michael Zangl
     */
    public static class CommandTestData {
        /**
         * A test layer
         */
        public final OsmDataLayer layer;
        /**
         * A test node
         */
        public final Node existingNode;
        /**
         * A second test node
         */
        public final Node existingNode2;
        /**
         * A test way
         */
        public final Way existingWay;

        /**
         * Creates the new test data and adds {@link #layer} to the layer manager.
         */
        public CommandTestData() {
            layer = new OsmDataLayer(new DataSet(), "layer", null);
            MainApplication.getLayerManager().addLayer(layer);

            existingNode = createNode(5, layer);
            existingNode2 = createNode(6, layer);

            existingWay = createWay(10, layer, existingNode, existingNode2);
        }

        /**
         * Create and add a new test node.
         * @param id the id
         * @return The node.
         */
        public Node createNode(long id) {
            return createNode(id, layer);
        }

        public static Node createNode(long id, OsmDataLayer layer) {
            Node node = new Node();
            node.setOsmId(id, 1);
            node.setCoor(LatLon.ZERO);
            node.put("existing", "existing");
            node.setReferrersDownloaded(true);
            layer.data.addPrimitive(node);
            return node;
        }

        /**
         * Create and add a new test way.
         * @param id the id
         * @param nodes The nodes
         * @return The way.
         */
        public Way createWay(int id, Node... nodes) {
            return createWay(id, layer, nodes);
        }

        public static Way createWay(int id, OsmDataLayer layer, Node... nodes) {
            Way way = new Way();
            way.setOsmId(id, 1);
            way.setNodes(Arrays.asList(nodes));
            way.put("existing", "existing");
            way.setReferrersDownloaded(true);
            layer.data.addPrimitive(way);
            return way;
        }

        /**
         * Create and add a new test relation.
         * @param id the id
         * @param members The members
         * @return The relation.
         */
        public Relation createRelation(int id, RelationMember... members) {
            return createRelation(id, layer, members);
        }
        
        public static Relation createRelation(int id, OsmDataLayer layer, RelationMember... members) {
            Relation relation = new Relation(id, 1);
            for (RelationMember member : members) {
                relation.addMember(member);
            }
            relation.put("existing", "existing");
            relation.setReferrersDownloaded(true);
            layer.data.addPrimitive(relation);
            return relation;
        }
    }

    /**
     * A change test data consisting of two nodes, a way and a relation.
     * @author Michael Zangl
     */
    public static class CommandTestDataWithRelation extends CommandTestData {
        /**
         * A test relation
         */
        public final Relation existingRelation;

        /**
         * Creates the new test data and adds {@link #layer} to the layer manager.
         */
        public CommandTestDataWithRelation() {
            existingRelation = createRelation(20, layer, new RelationMember("node", existingNode), new RelationMember("way", existingWay));
        }
    }
}
