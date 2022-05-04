// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link MemberTableModel} class.
 */
@BasicPreferences
class MemberTableModelTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12443">#12443</a>.
     */
    @Test
    void testTicket12443() {
        final Node n = new Node(1);
        assertNotNull(new MemberTableModel(null, null, new TaggingPresetHandler() {
            @Override
            public void updateTags(List<Tag> tags) {
                // Do nothing
            }

            @Override
            public Collection<OsmPrimitive> getSelection() {
                return Collections.singleton(n);
            }
        }).getRelationMemberForPrimitive(n));
    }

    /**
     * Non-regression test for JOSM #12617, #17906, and #21889. Regrettably, it was not easily possible to test using
     * drag-n-drop methods.
     */
    @Test
    void testTicket12617() {
        final Node[] nodes = new Node[10];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(LatLon.ZERO);
            // The id has to be > 0
            nodes[i].setOsmId(i + 1, 1);
        }
        final Relation relation = TestUtils.newRelation("", Stream.of(nodes).map(node -> new RelationMember("", node))
                .toArray(RelationMember[]::new));
        final OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "testTicket12617", null);
        osmDataLayer.getDataSet().addPrimitiveRecursive(relation);
        final MemberTableModel model = new MemberTableModel(relation, osmDataLayer, new TaggingPresetHandler() {
            @Override
            public Collection<OsmPrimitive> getSelection() {
                return Collections.singleton(relation);
            }

            @Override
            public void updateTags(List<Tag> tags) {
                // Do nothing
            }
        });

        model.populate(relation);
        // Select the members to move
        model.setSelectedMembersIdx(Arrays.asList(2, 3, 5, 9));
        // Move the members (this is similar to what the drag-n-drop code is doing)
        model.addMembersAtIndexKeepingOldSelection(model.getSelectedMembers(), 2);
        model.remove(model.getSelectedIndices());
        // Apply the changes
        model.applyToRelation(relation);

        // Perform the tests
        assertAll(() -> assertEquals(10, relation.getMembersCount(), "There should be no changes to the member count"),
                () -> assertEquals(nodes[0], relation.getMember(0).getMember()),
                () -> assertEquals(nodes[1], relation.getMember(1).getMember()),
                () -> assertEquals(nodes[2], relation.getMember(2).getMember(), "Node 2 should not have moved"),
                () -> assertEquals(nodes[3], relation.getMember(3).getMember(), "Node 3 should not have moved"),
                () -> assertEquals(nodes[4], relation.getMember(6).getMember(), "Node 4 should be in position 5"),
                () -> assertEquals(nodes[5], relation.getMember(4).getMember(), "Node 5 should be in position 4"),
                () -> assertEquals(nodes[6], relation.getMember(7).getMember(), "Node 6 should not have moved"),
                () -> assertEquals(nodes[7], relation.getMember(8).getMember()),
                () -> assertEquals(nodes[8], relation.getMember(9).getMember()),
                () -> assertEquals(nodes[9], relation.getMember(5).getMember(), "Node 9 should have moved")
                );
    }
}
