// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AddImageryLayerAction}.
 */
public final class AddImageryLayerActionTest {
    /**
     * We need prefs for this. We need platform for actions and the OSM API for checking blacklist.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform().fakeAPI();

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()));

    /**
     * Unit test of {@link AddImageryLayerAction#updateEnabledState}.
     */
    @Test
    public void testEnabledState() {
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
    public void testActionPerformedEnabledTms() {
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        new AddImageryLayerAction(new ImageryInfo("foo_tms", "http://bar", "tms", null, null)).actionPerformed(null);
        List<TMSLayer> tmsLayers = Main.getLayerManager().getLayersOfType(TMSLayer.class);
        assertEquals(1, tmsLayers.size());
        Main.getLayerManager().removeLayer(tmsLayers.get(0));
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - Enabled cases for WMS.
     */
    @Test
    public void testActionPerformedEnabledWms() {
        wireMockRule.stubFor(get(urlEqualTo("/wms?VERSION=1.1.1&SERVICE=WMS&REQUEST=GetCapabilities"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBodyFile("imagery/wms-capabilities.xml")));
        new AddImageryLayerAction(new ImageryInfo("localhost", "http://localhost:" + wireMockRule.port() + "/wms?",
                "wms_endpoint", null, null)).actionPerformed(null);
        List<WMSLayer> wmsLayers = Main.getLayerManager().getLayersOfType(WMSLayer.class);
        assertEquals(1, wmsLayers.size());

        Main.getLayerManager().removeLayer(wmsLayers.get(0));
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - disabled case.
     */
    @Test
    public void testActionPerformedDisabled() {
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        try {
            new AddImageryLayerAction(new ImageryInfo("foo")).actionPerformed(null);
        } catch (IllegalArgumentException expected) {
            assertEquals("Parameter 'info.url' must not be null", expected.getMessage());
        }
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
    }
}
