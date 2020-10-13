// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;

import javax.swing.JLabel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests of {@link ApiUrlTestTask} class.
 */
public class ApiUrlTestTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(30000);

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()));

    private static final Component PARENT = new JLabel();

    /**
     * Unit test of {@link ApiUrlTestTask#ApiUrlTestTask} - null url.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullApiUrl() {
        new ApiUrlTestTask(PARENT, null);
    }

    /**
     * Unit test of {@link ApiUrlTestTask} - nominal url.
     */
    @Test
    public void testNominalUrl() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockRule.url("/__files/osm_api"));
        task.run();
        assertTrue(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidUrl} - malformed url.
     */
    @Test
    public void testAlertInvalidUrl() {
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
    public void testUnknownHost() {
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
    public void testAlertInvalidServerResult() {
        Logging.clearLastErrorAndWarnings();
        wireMockRule.stubFor(get(urlEqualTo("/does-not-exist/0.6/capabilities"))
                .willReturn(aResponse().withStatus(404)));

        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockRule.url("/does-not-exist"));
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "The server responded with the return code 404 instead of 200."));
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidCapabilities} - invalid contents.
     */
    @Test
    public void testAlertInvalidCapabilities() {
        Logging.clearLastErrorAndWarnings();
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, wireMockRule.url("/__files/invalid_api"));
        task.run();
        assertFalse(task.isSuccess());
        assertThat(Logging.getLastErrorAndWarnings().toString(), containsString(
                "The OSM API server at 'XXX' did not return a valid response.<br>It is likely that 'XXX' is not an OSM API server."
                        .replace("XXX", wireMockRule.url("/__files/invalid_api"))));
    }
}
