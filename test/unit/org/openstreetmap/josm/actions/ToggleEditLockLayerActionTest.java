// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ToggleEditLockLayerAction}.
 */
final class ToggleEditLockLayerActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Test null parameter
     */
    @Test
    void testNullLayer() {
        assertThrows(IllegalArgumentException.class, () -> new ToggleEditLockLayerAction<OsmDataLayer>(null));
    }

    /**
     * Test edit lock toggle functionality
     * @param locked Initial layer lock status
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testLayerLockToggle(boolean locked) {
        OsmDataLayer testLayer = new OsmDataLayer(new DataSet(), "testLayerLock", null);
        MainApplication.getLayerManager().addLayer(testLayer);
        if (locked) {
            testLayer.lock();
        }
        // Sanity check
        assertEquals(locked, testLayer.isLocked());
        ToggleEditLockLayerAction<OsmDataLayer> action = new ToggleEditLockLayerAction<>(testLayer);
        action.actionPerformed(null);
        assertNotEquals(locked, testLayer.isLocked());
        action.actionPerformed(null);
        assertEquals(locked, testLayer.isLocked());
    }
}
