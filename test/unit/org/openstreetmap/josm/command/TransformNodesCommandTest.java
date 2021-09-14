// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link TransformNodesCommand} class.
 */
@BasicPreferences
class TransformNodesCommandTest {
    /**
     * Unit test of methods {@link TransformNodesCommand#equals} and {@link TransformNodesCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(TransformNodesCommand.class).usingGetClass()
            .withPrefabValues(LatLon.class,
                LatLon.ZERO, new LatLon(45, 45))
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
