// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangeRelationMemberRoleCommand} class.
 */
public class ChangeRelationMemberRoleCommandTest {

    /**
     * Unit test of methods {@link ChangeRelationMemberRoleCommand#equals} and {@link ChangeRelationMemberRoleCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ChangeRelationMemberRoleCommand.class).usingGetClass()
            .withPrefabValues(Relation.class,
                new Relation(1), new Relation(2))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
