// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ConflictResolver} class.
 */
public class ConflictResolverTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ConflictResolver#buildResolveCommand} - empty case.
     */
    @Test(expected = NoSuchElementException.class)
    public void testBuildResolveCommandEmpty() {
        assertNotNull(new ConflictResolver().buildResolveCommand());
    }

    /**
     * Unit test of {@link ConflictResolver#buildResolveCommand} - node case.
     */
    @Test
    public void testBuildResolveCommandNode() {
        ConflictResolver resolver = new ConflictResolver();
        Node n1 = new Node(LatLon.SOUTH_POLE);
        n1.put("source", "my");
        Node n2 = new Node(LatLon.NORTH_POLE);
        n2.put("source", "theirs");
        new DataSet(n1, n2);
        resolver.populate(new Conflict<>(n1, n2));
        resolver.decideRemaining(MergeDecisionType.KEEP_MINE);
        assertFalse(((SequenceCommand) resolver.buildResolveCommand()).getChildren().isEmpty());
    }

    /**
     * Unit test of {@link ConflictResolver#buildResolveCommand} - way case.
     */
    @Test
    public void testBuildResolveCommandWay() {
        ConflictResolver resolver = new ConflictResolver();
        Way w1 = new Way();
        w1.put("source", "my");
        Way w2 = new Way();
        w2.put("source", "theirs");
        new DataSet(w1, w2);
        resolver.populate(new Conflict<>(w1, w2));
        resolver.decideRemaining(MergeDecisionType.KEEP_MINE);
        assertFalse(((SequenceCommand) resolver.buildResolveCommand()).getChildren().isEmpty());
    }

    /**
     * Unit test of {@link ConflictResolver#buildResolveCommand} - relation case.
     */
    @Test
    public void testBuildResolveCommandRelation() {
        ConflictResolver resolver = new ConflictResolver();
        Relation r1 = new Relation();
        r1.put("source", "my");
        Relation r2 = new Relation();
        r2.put("source", "theirs");
        new DataSet(r1, r2);
        resolver.populate(new Conflict<>(r1, r2));
        resolver.decideRemaining(MergeDecisionType.KEEP_MINE);
        assertFalse(((SequenceCommand) resolver.buildResolveCommand()).getChildren().isEmpty());
    }
}
