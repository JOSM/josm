// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.mockers.OpenBrowserMocker;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Unit tests of {@link BugReportSender} class.
 */
@BasicPreferences
@BasicWiremock
@HTTP
class BugReportSenderTest {
    /**
     * HTTP mock
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    /**
     * Unit test for {@link BugReportSender#BugReportSender}.
     * @throws InterruptedException if the thread is interrupted
     */
    @Test
    void testBugReportSender() throws InterruptedException {
        Config.getPref().put("josm.url", wireMockServer.baseUrl());
        wireMockServer.stubFor(post(urlEqualTo("/josmticket"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<josmticket status=\"ok\">\n" +
                                "  <preparedid>6bccff5c0417217bfbbe5fff</preparedid>\n" +
                                "</josmticket>\n")));
        new OpenBrowserMocker();

        BugReportSender sender = BugReportSender.reportBug(ShowStatusReportAction.getReportHeader());
        assertNotNull(sender);
        synchronized (sender) {
            while (sender.isAlive()) {
                sender.wait();
            }
        }

        assertFalse(sender.isAlive());
        assertNull(sender.getErrorMessage(), sender.getErrorMessage());
        wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/josmticket")).withRequestBody(containing("pdata=")));

        List<URI> calledURIs = OpenBrowserMocker.getCalledURIs();
        assertEquals(1, calledURIs.size());
        assertEquals(wireMockServer.url("/josmticket?pdata_stored=6bccff5c0417217bfbbe5fff"), calledURIs.get(0).toString());
    }
}
