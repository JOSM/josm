// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link TransformNodesCommand} class.
 */
public class TransformNodesCommandTest {

    /**
     * Unit test of methods {@link TransformNodesCommand#equals} and {@link TransformNodesCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(TransformNodesCommand.class).usingGetClass()
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
