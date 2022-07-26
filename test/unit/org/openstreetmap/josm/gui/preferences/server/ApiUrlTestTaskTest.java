// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Unit tests of {@link ApiUrlTestTask} class.
 */
@Timeout(30)
@BasicPreferences
@BasicWiremock
@HTTP
@ExtendWith(BasicWiremock.OsmApiExtension.class)
class ApiUrlTestTaskTest {
    /**
     * HTTP mock.
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    private static final Component PARENT = new JLabel();

    /**
     * Unit test of {@link ApiUrlTestTask#ApiUrlTestTask} - null url.
     */
    @Test
    void testNullApiUrl() {
        assertThrows(IllegalArgumentException.class, () -> new ApiUrlTestTask(PARENT, null));
    }

    /**
     * Unit test of {@link ApiUrlTestTask} - nominal url.
     */
    @Test
    void testNominalUrl() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockServer.url("/__files/api"));
        task.run();
        assertTrue(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidUrl} - malformed url.
     */
    @Test
    void testAlertInvalidUrl() {
        Logging.clearLastErrorAndWarnings();
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "malformed url");
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "<html>'malformed url' is not a valid OSM API URL.<br>Please check the spelling and validate again.</html>"));
    }

    /**
     * Unit test of {@link ApiUrlTestTask} - unknown host.
     */
    @Test
    void testUnknownHost() {
        Logging.clearLastErrorAndWarnings();
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "http://unknown");
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "java.net.UnknownHostException: unknown"));
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidServerResult} - http 404.
     */
    @Test
    void testAlertInvalidServerResult() {
        Logging.clearLastErrorAndWarnings();
        wireMockServer.stubFor(get(urlEqualTo("/does-not-exist/0.6/capabilities"))
                .willReturn(aResponse().withStatus(404)));

        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockServer.url("/does-not-exist"));
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "The server responded with the return code 404 instead of 200."));
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidCapabilities} - invalid contents.
     */
    @Test
    void testAlertInvalidCapabilities() {
        Logging.clearLastErrorAndWarnings();
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockServer.url("/__files/invalid_api"));
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "The OSM API server at 'XXX' did not return a valid response.<br>It is likely that 'XXX' is not an OSM API server."
                        .replace("XXX", wireMockServer.url("/__files/invalid_api"))));
    }
}
