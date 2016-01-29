// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangeCommand} class.
 */
public class ChangeCommandTest {

    /**
     * Unit test of methods {@link ChangeCommand#equals} and {@link ChangeCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ChangeCommand.class).usingGetClass()
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
