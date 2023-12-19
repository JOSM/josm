// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Projection;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Unit tests of {@link WMSImagery} class.
 */
@BasicPreferences(true)
@BasicWiremock
@Projection
class WMSImageryTest {
    @BasicWiremock
    WireMockServer tileServer;

    /**
     * Unit test of {@code WMSImagery.WMSGetCapabilitiesException} class
     */
    @Test
    void testWMSGetCapabilitiesException() {
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
    void testTicket15730() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withBody(
                Files.readAllBytes(Paths.get(TestUtils.getRegressionDataDir(15730), "capabilities.xml"))
                )));

        WMSImagery wms = new WMSImagery(tileServer.url("capabilities.xml"));
        assertEquals(1, wms.getLayers().size());
        assertTrue(wms.getLayers().get(0).getAbstract().startsWith("South Carolina  NAIP Imagery 2017    Resolution: 100CM "));
    }

    @Test
    void testNestedLayers() throws Exception {
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
    void testTicket16248() throws IOException, WMSGetCapabilitiesException {
        byte[] capabilities = Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(16248, "capabilities.xml")));
        tileServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withBody(capabilities)));
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("http://wms.hgis.cartomatic.pl/topo/3857/m25k?", wms.buildRootUrl());
        assertEquals("wms.hgis.cartomatic.pl", wms.getLayers().get(0).getName());
        assertEquals("http://wms.hgis.cartomatic.pl/topo/3857/m25k?FORMAT=image/png&TRANSPARENT=TRUE&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap&"
                + "LAYERS=wms.hgis.cartomatic.pl&STYLES=&CRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                wms.buildGetMapUrl(wms.getLayers(), (List<String>) null, true));
    }

    /**
     * Non-regression test for bug #19193.
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException never
     */
    @Test
    void testTicket19193() throws IOException, WMSGetCapabilitiesException {
        byte[] capabilities = Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(19193, "capabilities.xml")));
        tileServer.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withBody(capabilities)));
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("https://inspire.brandenburg.de/services/gn_alkis_wms?", wms.buildRootUrl());
        assertEquals(1, wms.getLayers().size());
        assertNull(wms.getLayers().get(0).getName());
        assertEquals("INSPIRE GN ALKIS BB", wms.getLayers().get(0).getTitle());
        assertEquals("https://inspire.brandenburg.de/services/gn_alkis_wms?FORMAT=image/png&TRANSPARENT=TRUE&VERSION=1.3.0&"
                + "SERVICE=WMS&REQUEST=GetMap&LAYERS=null&STYLES=&CRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                wms.buildGetMapUrl(wms.getLayers(), (List<String>) null, true));
    }

    /**
     * Regression test for bug #16333 (null style name)
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException  never
     */
    @Test
    void testTicket16333() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withBody(
                        Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(16333, "capabilities.xml")))
                ))
        );
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("https://duinoord.xs4all.nl/geoserver/ows?SERVICE=WMS&", wms.buildRootUrl());
        assertNull(wms.getLayers().get(0).getName());
        assertEquals("", wms.getLayers().get(0).getTitle());

        assertEquals("bag:Matching Street", wms.getLayers().get(0).getChildren().get(0).getName());
        assertEquals("Dichtstbijzijnde straat", wms.getLayers().get(0).getChildren().get(0).getTitle());
    }

    @Test
    void testForTitleWithinAttribution_ticket16940() throws IOException, WMSGetCapabilitiesException {
        tileServer.stubFor(
                WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withBody(
                        Files.readAllBytes(Paths.get(TestUtils.getRegressionDataFile(16940, "capabilities.xml")))
                ))
        );
        WMSImagery wms = new WMSImagery(tileServer.url("any"));
        assertEquals("Hipsográfico", wms.getLayers().stream().findFirst().get().getTitle());
    }
}
