// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Partition tests for {@link DeleteAction#checkAndConfirmOutlyingDelete}.
 *
 * <p>
 * Input domain: a collection of {@link OsmPrimitive} objects to be deleted.
 * The partitions are defined based on the combination of primitive types
 * contained in the collection.
 * </p>
 */
public class DeleteActionPartitionTest {

    /**
     * Partition P1:
     * Collection contains only Node primitives.
     */
    @Test
    void testPartitionOnlyNodes() {
        Node n = new Node(new LatLon(0, 0));
        Collection<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(n);

        Collection<OsmPrimitive> ignore = Collections.emptyList();

        assertDoesNotThrow(() ->
            DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore)
        );
    }

    /**
     * Partition P2:
     * Collection contains only Way primitives.
     */
    @Test
    void testPartitionOnlyWays() {
        Way w = new Way();
        Collection<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(w);

        Collection<OsmPrimitive> ignore = Collections.emptyList();

        assertDoesNotThrow(() ->
            DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore)
        );
    }

    /**
     * Partition P3:
     * Collection contains only Relation primitives.
     */
    @Test
    void testPartitionOnlyRelations() {
        Relation r = new Relation();
        Collection<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(r);

        Collection<OsmPrimitive> ignore = Collections.emptyList();

        assertDoesNotThrow(() ->
            DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore)
        );
    }

    /**
     * Partition P4:
     * Mixed collection containing Node and Way primitives.
     */
    @Test
    void testPartitionNodeAndWay() {
        Node n = new Node(new LatLon(0, 0));
        Way w = new Way();
        Collection<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(n);
        primitives.add(w);

        Collection<OsmPrimitive> ignore = Collections.emptyList();

        assertDoesNotThrow(() ->
            DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore)
        );
    }

    /**
     * Partition P5:
     * Mixed collection containing Node, Way, and Relation primitives.
     */
    @Test
    void testPartitionNodeWayAndRelation() {
        Node n = new Node(new LatLon(0, 0));
        Way w = new Way();
        Relation r = new Relation();
        Collection<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(n);
        primitives.add(w);
        primitives.add(r);

        Collection<OsmPrimitive> ignore = Collections.emptyList();

        assertDoesNotThrow(() ->
            DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore)
        );
    }
}
