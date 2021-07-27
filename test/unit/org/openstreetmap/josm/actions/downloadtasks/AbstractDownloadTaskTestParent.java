// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;

import com.github.tomakehurst.wiremock.WireMockServer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Superclass of {@link DownloadGpsTaskTest}, {@link DownloadOsmTaskTest} and {@link DownloadNotesTaskTest}.
 */
public abstract class AbstractDownloadTaskTestParent {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    JOSMTestRules test = new JOSMTestRules().https();

    /**
     * HTTP mock.
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    /**
     * Returns the path to remote test file to download via http.
     * @return the path to remote test file, relative to JOSM root directory
     */
    protected abstract String getRemoteFile();

    /**
     * Returns the {@code Content-Type} with which to serve the file referenced
     * by {@link #getRemoteFile()}
     * @return the {@code Content-Type} string for file {@link #getRemoteFile()}
     */
    protected String getRemoteContentType() {
        return "text/xml";
    }

    /**
     * Returns the http URL to remote test file to download.
     * @return the http URL to remote test file to download
     */
    protected final String getRemoteFileUrl() {
        return wireMockServer.url(getRemoteFile());
    }

    /**
     * Mock the HTTP server.
     */
    protected final void mockHttp() {
        wireMockServer.stubFor(get(urlEqualTo("/" + getRemoteFile()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", getRemoteContentType())
                    .withBodyFile(getRemoteFile())));
    }
}
