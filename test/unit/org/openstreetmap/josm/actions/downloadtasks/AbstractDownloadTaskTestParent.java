// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.Rule;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Superclass of {@link DownloadGpsTaskTest}, {@link DownloadOsmTaskTest} and {@link DownloadNotesTaskTest}.
 */
public abstract class AbstractDownloadTaskTestParent {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https();

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()));

    /**
     * Returns the path to remote test file to download via http.
     * @return the path to remote test file, relative to JOSM root directory
     */
    protected abstract String getRemoteFile();

    /**
     * Returns the http URL to remote test file to download.
     * @return the http URL to remote test file to download
     */
    protected final String getRemoteFileUrl() {
        return "http://localhost:" + wireMockRule.port() + "/" + getRemoteFile();
    }

    /**
     * Mock the HTTP server.
     */
    protected final void mockHttp() {
        wireMockRule.stubFor(get(urlEqualTo("/" + getRemoteFile()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBodyFile(getRemoteFile())));
    }
}
