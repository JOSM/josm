// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ValidatorLayer} class.
 */
class ValidatorLayerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().main();

    /**
     * Unit test of {@link ValidatorLayer#ValidatorLayer}.
     */
    @Test
    void testValidatorLayer() {
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
        ValidatorLayer layer = new ValidatorLayer();
        MainApplication.getLayerManager().addLayer(layer);
        assertFalse(layer.isMergable(null));
        assertNotNull(layer.getIcon());
        assertEquals("<html>No validation errors</html>", layer.getToolTipText());
        assertEquals("<html>No validation errors</html>", layer.getInfoComponent());
        assertTrue(layer.getMenuEntries().length > 0);
    }
}
