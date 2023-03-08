// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Unit test of {@link WikimediaCommonsLoader}
 */
@BasicPreferences
class WikimediaCommonsLoaderTest {

    @BeforeAll
    static void beforeAll() {
        HttpClient.setFactory(Http1Client::new);
    }

    @AfterEach
    void tearDown() {
        if (ImageViewerDialog.hasInstance()) {
            ImageViewerDialog.getInstance().destroy();
        }
    }

    /**
     * Unit test of {@link WikimediaCommonsLoader}
     *
     * @throws Exception if an error occurs
     */
    @Test
    void test() throws Exception {
        WireMockServer wireMock = mockHttp();
        wireMock.start();

        WikimediaCommonsLoader loader = new WikimediaCommonsLoader(new Bounds(47., 11., 48., 12.));
        loader.apiUrl = wireMock.url("/w/api.php");
        loader.realRun();
        wireMock.stop();

        List<ImageEntry> images = loader.layer.getImages();
        assertEquals(1, images.size());
        ImageEntry image = images.get(0);
        assertEquals("File:ISS053-E-105875_-_View_of_Earth.jpg", image.getDisplayName());
        assertEquals(new URL("https://upload.wikimedia.org/wikipedia/commons/e/e8/ISS053-E-105875_-_View_of_Earth.jpg"),
                image.getImageUrl());
    }

    private static WireMockServer mockHttp() {
        String xml =
                "<api batchcomplete=\"\">\n" +
                "<query>\n" +
                "<geosearch>\n" +
                "<gs pageid=\"102860206\" ns=\"6\" title=\"File:ISS053-E-105875 - View of Earth.jpg\" lat=\"47.2\" lon=\"11.3\" " +
                "dist=\"0\" primary=\"\" type=\"camera\"/>\n" +
                "</geosearch>\n" +
                "</query>\n" +
                "</api>";
        final WireMockServer wireMock = new WireMockServer(options().dynamicPort());
        wireMock.stubFor(get(urlEqualTo("/w/api.php?format=xml&action=query&list=geosearch" +
                "&gsnamespace=6&gslimit=500&gsprop=type%7Cname&gsbbox=48.0%7C11.0%7C47.0%7C12.0"))
                .willReturn(aResponse().withBody(xml)));
        return wireMock;
    }
}
