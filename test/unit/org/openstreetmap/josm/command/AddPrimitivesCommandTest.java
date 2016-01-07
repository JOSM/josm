// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link AddPrimitivesCommand} class.
 */
public class AddPrimitivesCommandTest {

    /**
     * Unit test of methods {@link AddPrimitivesCommand#equals} and {@link AddPrimitivesCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(AddPrimitivesCommand.class).usingGetClass()
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
