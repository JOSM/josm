// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link RelationMemberConflictResolverCommand} class.
 */
public class RelationMemberConflictResolverCommandTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    /**
     * Unit test of methods {@link RelationMemberConflictResolverCommand#equals} and {@link RelationMemberConflictResolverCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(RelationMemberConflictResolverCommand.class).usingGetClass()
            .withPrefabValues(Relation.class,
                    new Relation(1), new Relation(2))
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Relation(), new Relation()))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
