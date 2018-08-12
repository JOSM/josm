// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link WMSImagery} class.
 */
public class WMSImageryTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    @Rule
    public WireMockRule tileServer = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort());
    /**
     * Unit test of {@code WMSImagery.WMSGetCapabilitiesException} class
     */
    @Test
    public void testWMSGetCapabilitiesException() {
        Exception cause = new Exception("test");
        WMSGetCapabilitiesException exc = new WMSGetCapabilitiesException(cause, "bar");
        assertEquals(cause, exc.getCause());
        assertEquals("bar", exc.getIncomingData());
        exc = new WMSGetCapabilitiesException("foo", "bar");
        assertEquals("foo", exc.getMessage());
        assertEquals("bar", exc.getIncomingData());
    }

    /**
     * Non-regression test for bug #15730.
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException never
     */
    @Test
    public void testTicket15730() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withBody(
                Files.readAllBytes(Paths.get(TestUtils.getRegressionDataDir(15730), "capabilities.xml"))
                )));

        WMSImagery wms = new WMSImagery(tileServer.url("capabilities.xml"));
        assertEquals(1, wms.getLayers().size());
        assertTrue(wms.getLayers().get(0).getAbstract().startsWith("South Carolina  NAIP Imagery 2017    Resolution: 100CM "));
    }

    @Test
    public void testNestedLayers() throws Exception {
        tileServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withBody(
                Files.readAllBytes(Paths.get(TestUtils.getTestDataRoot() + "wms/mapa-um-warszawa-pl.xml")))));
        WMSImagery wmsi = new WMSImagery(tileServer.url("/serwis"));
        assertEquals(1, wmsi.getLayers().size());
        assertEquals("Server WMS m.st. Warszawy", wmsi.getLayers().get(0).toString());
        assertEquals(202, wmsi.getLayers().get(0).getChildren().size());
    }

    /**
     * Non-regression test for bug #16248.
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException never
     */
    @Test
    public void testTicket16248() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withBody(
                        Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(16248, "capabilities.xml")))
                        ))
                );
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("http://wms.hgis.cartomatic.pl/topo/3857/m25k?", wms.buildRootUrl());
        assertEquals("wms.hgis.cartomatic.pl", wms.getLayers().get(0).getName());
        assertEquals("http://wms.hgis.cartomatic.pl/topo/3857/m25k?FORMAT=image/png&TRANSPARENT=TRUE&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&"
                + "LAYERS=wms.hgis.cartomatic.pl&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                wms.buildGetMapUrl(wms.getLayers(), (List<String>) null, true));
    }

    /**
     * Regression test for bug #16333 (null style name)
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException  never
     */
    @Test
    public void testTicket16333() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withBody(
                        Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(16333, "capabilities.xml")))
                ))
        );
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("https://duinoord.xs4all.nl/geoserver/ows?SERVICE=WMS&", wms.buildRootUrl());
        assertEquals(null, wms.getLayers().get(0).getName());
        assertEquals("", wms.getLayers().get(0).getTitle());

        assertEquals("bag:Matching Street", wms.getLayers().get(0).getChildren().get(0).getName());
        assertEquals("Dichtstbijzijnde straat", wms.getLayers().get(0).getChildren().get(0).getTitle());

    }
}

