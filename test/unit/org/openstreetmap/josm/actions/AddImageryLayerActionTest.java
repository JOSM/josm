// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.OsmApi;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link AddImageryLayerAction}.
 */
@BasicPreferences
@BasicWiremock
@OsmApi(OsmApi.APIType.FAKE)
@Projection
final class AddImageryLayerActionTest {
    /**
     * Unit test of {@link AddImageryLayerAction#updateEnabledState}.
     */
    @Test
    void testEnabledState() {
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo")).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_tms", "http://bar", "tms", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_bing", "http://bar", "bing", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_scanex", "http://bar", "scanex", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_wms_endpoint", "http://bar", "wms_endpoint", null, null)).isEnabled());
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - Enabled cases for TMS.
     */
    @Test
    void testActionPerformedEnabledTms() {
        assertTrue(MainApplication.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        new AddImageryLayerAction(new ImageryInfo("foo_tms", "http://bar", "tms", null, null)).actionPerformed(null);
        List<TMSLayer> tmsLayers = MainApplication.getLayerManager().getLayersOfType(TMSLayer.class);
        assertEquals(1, tmsLayers.size());
        MainApplication.getLayerManager().removeLayer(tmsLayers.get(0));
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - Enabled cases for WMS.
     */
    @Test
    void testActionPerformedEnabledWms(WireMockRuntimeInfo wireMockRuntimeInfo) {
        wireMockRuntimeInfo.getWireMock().register(get(urlEqualTo("/wms?apikey=random_key&SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.1.1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBodyFile("imagery/wms-capabilities.xml")));
        wireMockRuntimeInfo.getWireMock().register(get(urlEqualTo("/wms?apikey=random_key&SERVICE=WMS&REQUEST=GetCapabilities"))
                .willReturn(aResponse()
                        .withStatus(404)));
        wireMockRuntimeInfo.getWireMock().register(get(urlEqualTo("/wms?apikey=random_key&SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0"))
                .willReturn(aResponse()
                        .withStatus(404)));

        try {
            FeatureAdapter.registerApiKeyAdapter(id -> "random_key");
            final ImageryInfo imageryInfo = new ImageryInfo("localhost", wireMockRuntimeInfo.getHttpBaseUrl() + "/wms?apikey={apikey}",
                    "wms_endpoint", null, null);
            imageryInfo.setId("testActionPerformedEnabledWms");
            new AddImageryLayerAction(imageryInfo).actionPerformed(null);
            List<WMSLayer> wmsLayers = MainApplication.getLayerManager().getLayersOfType(WMSLayer.class);
            assertEquals(1, wmsLayers.size());

            MainApplication.getLayerManager().removeLayer(wmsLayers.get(0));
        } finally {
            FeatureAdapter.registerApiKeyAdapter(new FeatureAdapter.DefaultApiKeyAdapter());
        }
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - disabled case.
     */
    @Test
    void testActionPerformedDisabled() {
        assertTrue(MainApplication.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        final AddImageryLayerAction action = new AddImageryLayerAction(new ImageryInfo("foo"));
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> action.actionPerformed(null));
        assertEquals("Parameter 'info.url' must not be null", expected.getMessage());
        assertTrue(MainApplication.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
    }


    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/24097">#24097</a>: Zoom to imagery layer
     * This tests two things:
     * <ul>
     *     <li>Imagery layer zoom to action works properly</li>
     *     <li>Imagery layer bounds is not zoomed to on layer add</li>
     * </ul>
     */
    @Disabled("See #24097 comment 3")
    @Main
    @Test
    void testNonRegression24097() {
        // First, add a new data layer
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(),
                "AddImageryLayerActionTest#testNonRegression24097", null));
        // Now zoom to a random area
        MainApplication.getMap().mapView.zoomTo(new Bounds(39.0665807, -108.5212326, 39.0793079, -108.4986591));
        // Initialize the zoom actions
        MainApplication.getMenu().initialize();
        final Bounds startingBounds = MainApplication.getMap().mapView.getRealBounds();
        ImageryInfo testInfo = new ImageryInfo("Test", "https://127.0.0.1/{zoom}/{x}/{y}.png", "tms", null, null, "Test");
        testInfo.setBounds(new ImageryInfo.ImageryBounds("-0.001,-0.001,0.001,0.001", ","));
        new AddImageryLayerAction(testInfo).actionPerformed(null);
        GuiHelper.runInEDTAndWait(() -> { /* Sync GUI thread */ });
        // There is a bit of zooming done during the load of the imagery
        assertTrue(startingBounds.toBBox().bboxIsFunctionallyEqual(MainApplication.getMap().mapView.getRealBounds().toBBox(), 0.001),
                "Adding an imagery layer should not zoom to the imagery layer bounds");
        assertEquals(1, MainApplication.getLayerManager().getLayersOfType(TMSLayer.class).size());
        final TMSLayer tmsLayer = MainApplication.getLayerManager().getLayersOfType(TMSLayer.class).get(0);
        final AutoScaleAction autoScaleAction = Arrays.stream(tmsLayer.getMenuEntries()).filter(AutoScaleAction.class::isInstance)
                .map(AutoScaleAction.class::cast).findFirst().orElseThrow();
        autoScaleAction.actionPerformed(null);
        // We can't check the bbox here, since the mapView doesn't have any actual width/height.
        // So we just check the center.
        assertTrue(new Bounds(-0.001, -0.001, 0.001, 0.001)
                .contains(ProjectionRegistry.getProjection().eastNorth2latlon(
                        MainApplication.getMap().mapView.getCenter())),
                "The action should have zoomed to the bbox for the imagery layer");
    }
}
