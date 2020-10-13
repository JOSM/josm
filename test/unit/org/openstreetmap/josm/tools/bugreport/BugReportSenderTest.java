// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.mockers.OpenBrowserMocker;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests of {@link BugReportSender} class.
 */
public class BugReportSenderTest {

    /**
     * Setup tests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    /**
     * Unit test for {@link BugReportSender#BugReportSender}.
     * @throws InterruptedException if the thread is interrupted
     */
    @Test
    public void testBugReportSender() throws InterruptedException {
        Config.getPref().put("josm.url", wireMockRule.baseUrl());
        wireMockRule.stubFor(post(urlEqualTo("/josmticket"))
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
        verify(exactly(1), postRequestedFor(urlEqualTo("/josmticket")).withRequestBody(containing("pdata=")));

        List<URI> calledURIs = OpenBrowserMocker.getCalledURIs();
        assertEquals(1, calledURIs.size());
        assertEquals(wireMockRule.baseUrl() + "/josmticket?pdata_stored=6bccff5c0417217bfbbe5fff", calledURIs.get(0).toString());
    }
}
