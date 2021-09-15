// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SaveLayerInfo} class.
 */
@BasicPreferences
class SaveLayerInfoTest {
    /**
     * Test of {@link SaveLayerInfo} class - null case.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
    void testSaveLayerInfoNull() {
        assertThrows(IllegalArgumentException.class, () -> new SaveLayerInfo(null));
    }

    /**
     * Test of {@link SaveLayerInfo} class - nominal case.
     */
    @Test
    void testSaveLayerInfoNominal() {
        File file = new File("test");
        String name = "layername";
        AbstractModifiableLayer layer = new OsmDataLayer(new DataSet(), name, file);
        SaveLayerInfo sli = new SaveLayerInfo(layer);
        assertEquals(file, sli.getFile());
        assertEquals(layer, sli.getLayer());
        assertEquals(name, sli.getName());
        assertNull(sli.getSaveState());
        assertNull(sli.getUploadState());
    }
}
