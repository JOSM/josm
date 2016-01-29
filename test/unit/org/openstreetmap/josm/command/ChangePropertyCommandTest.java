// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangePropertyCommand} class.
 */
public class ChangePropertyCommandTest {

    /**
     * Unit test of methods {@link ChangePropertyCommand#equals} and {@link ChangePropertyCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ChangePropertyCommand.class).usingGetClass()
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
