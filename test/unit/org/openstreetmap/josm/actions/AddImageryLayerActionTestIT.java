// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AddImageryLayerAction}.
 */
public final class AddImageryLayerActionTestIT {
    /**
     * We need prefs for this. We need platform for actions and the OSM API for checking blacklist.
     * The timeout is set to default httpclient read timeout + connect timeout + a small delay to ignore
     * common but harmless network issues.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform().fakeAPI().timeout(45500);

    /**
     * Integration test of {@link AddImageryLayerAction#actionPerformed} - Enabled cases for WMS.
     */
    @Test
    public void testActionPerformedEnabledWms() {
        new AddImageryLayerAction(new ImageryInfo("wms.openstreetmap.fr", "http://wms.openstreetmap.fr/wms?",
                "wms_endpoint", null, null)).actionPerformed(null);
        List<WMSLayer> wmsLayers = Main.getLayerManager().getLayersOfType(WMSLayer.class);
        assertEquals(1, wmsLayers.size());

        Main.getLayerManager().removeLayer(wmsLayers.get(0));
    }
}
