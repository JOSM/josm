// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapScaler.AccessibleMapScaler;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MapScaler} class.
 */
class MapScalerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Unit test of {@link MapScaler#MapScaler}.
     */
    @Test
    void testMapScaler() {
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
        assertEquals(Color.WHITE, MapScaler.getColor());
        MapScaler ms = new MapScaler(MainApplication.getMap().mapView);
        assertEquals("/MapView/Scaler", ms.helpTopic());
        ms.paint(TestUtils.newGraphics());
        AccessibleMapScaler ams = (AccessibleMapScaler) ms.getAccessibleContext();
        assertEquals(1000.0, ams.getCurrentAccessibleValue().doubleValue(), 1e-3);
        assertFalse(ams.setCurrentAccessibleValue(500));
        assertNull(ams.getMinimumAccessibleValue());
        assertNull(ams.getMaximumAccessibleValue());
    }
}
