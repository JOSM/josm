// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link ConflictResolveCommand} class.
 */
public class ConflictResolveCommandTest {

    /**
     * Unit test of methods {@link ConflictResolveCommand#equals} and {@link ConflictResolveCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ConflictResolveCommand.class).usingGetClass()
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
