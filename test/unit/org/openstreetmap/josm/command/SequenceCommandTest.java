// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link SequenceCommand} class.
 */
public class SequenceCommandTest {

    /**
     * Unit test of methods {@link SequenceCommand#equals} and {@link SequenceCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(SequenceCommand.class).usingGetClass()
            .withPrefabValues(Command.class,
                new AddCommand(new Node(1)), new AddCommand(new Node(2)))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
