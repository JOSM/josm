// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link DeletedStateConflictResolveCommand} class.
 */
@BasicPreferences
class DeletedStateConflictResolveCommandTest {
    /**
     * Unit test of methods {@link DeletedStateConflictResolveCommand#equals} and {@link DeletedStateConflictResolveCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(DeletedStateConflictResolveCommand.class).usingGetClass()
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Way(), new Way()))
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
