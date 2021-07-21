// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.OverpassDownloadReader.OverpassOutputFormat;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.tools.SearchCompilerQueryWizard;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Unit tests of {@link OverpassDownloadReader} class.
 */
@BasicWiremock
@BasicPreferences
@HTTP
class OverpassDownloadReaderTest {
    /**
     * HTTP mock.
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    private static final String NOMINATIM_URL_PATH = "/search?format=xml&q=";

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        NameFinder.NOMINATIM_URL_PROP.put(wireMockServer.url(NOMINATIM_URL_PATH));
    }

    private String getExpandedQuery(String search) {
        final String query = SearchCompilerQueryWizard.constructQuery(search);
        final String request = new OverpassDownloadReader(new Bounds(1, 2, 3, 4), null, query)
                .getRequestForBbox(1, 2, 3, 4)
                .substring("interpreter?data=".length());
        return Utils.decodeUrl(request);
    }

    /**
     * Tests evaluating the extended query feature {@code bbox}.
     */
    @Test
    void testBbox() {
        final String query = getExpandedQuery("amenity=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:2.0,1.0,4.0,3.0];\n" +
                "(\n" +
                "  nwr[\"amenity\"=\"drinking_water\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    private void stubNominatim(String query) {
        wireMockServer.stubFor(get(urlEqualTo(NOMINATIM_URL_PATH + query))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBodyFile("nominatim/" + query + ".xml")));
    }

    /**
     * Tests evaluating the extended query feature {@code date}.
     */
    @Test
    void testDate() {
        LocalDateTime from = LocalDateTime.of(2017, 7, 14, 2, 40);
        assertEquals("2016-07-14T02:40:00Z", OverpassDownloadReader.date("1 year", from));
        assertEquals("2007-07-14T02:40:00Z", OverpassDownloadReader.date("10years", from));
        assertEquals("2017-06-14T02:40:00Z", OverpassDownloadReader.date("1 month", from));
        assertEquals("2016-09-14T02:40:00Z", OverpassDownloadReader.date("10months", from));
        assertEquals("2017-07-07T02:40:00Z", OverpassDownloadReader.date("1 week", from));
        assertEquals("2017-05-05T02:40:00Z", OverpassDownloadReader.date("10weeks", from));
        assertEquals("2017-07-13T02:40:00Z", OverpassDownloadReader.date("1 day", from));
        assertEquals("2017-07-04T02:40:00Z", OverpassDownloadReader.date("10days", from));
        assertEquals("2017-07-14T01:40:00Z", OverpassDownloadReader.date("1 hour", from));
        assertEquals("2017-07-13T16:40:00Z", OverpassDownloadReader.date("10hours", from));
        assertEquals("2017-07-14T02:39:00Z", OverpassDownloadReader.date("1 minute", from));
        assertEquals("2017-07-14T02:30:00Z", OverpassDownloadReader.date("10minutes", from));
        assertEquals("2017-07-14T02:39:59Z", OverpassDownloadReader.date("1 second", from));
        assertEquals("2017-07-14T02:39:50Z", OverpassDownloadReader.date("10seconds", from));

        assertEquals("2016-07-13T02:40:00Z", OverpassDownloadReader.date("1 year 1 day", from));
        assertEquals("2016-07-14T02:38:20Z", OverpassDownloadReader.date("1 year 100 seconds", from));
        assertEquals("2017-07-13T02:38:20Z", OverpassDownloadReader.date("1 day  100 seconds", from));
    }

    /**
     * Tests evaluating the extended query feature {@code date} through {@code newer:} operator.
     */
    @Test
    void testDateNewer() {
        String query = getExpandedQuery("type:node and newer:3minutes");
        String statement = query.substring(query.indexOf("node(newer:\"") + 12, query.lastIndexOf("\");"));
        assertNotNull(DateUtils.fromString(statement));

        query = getExpandedQuery("type:node and newer:\"2021-05-30T20:00:00Z\"");
        statement = query.substring(query.indexOf("node(newer:\"") + 12, query.lastIndexOf("\");"));
        assertNotNull(DateUtils.fromString(statement));
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    void testGeocodeArea() {
        stubNominatim("London");
        final String query = getExpandedQuery("amenity=drinking_water in London");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "area(3600065606)->.searchArea;\n" +
                "(\n" +
                "  nwr[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    void testGeocodeUnknownArea() {
        stubNominatim("foo-bar-baz-does-not-exist");
        final String query = OverpassDownloadReader.expandExtendedQueries("{{geocodeArea:foo-bar-baz-does-not-exist}}");
        assertEquals("// Failed to evaluate {{geocodeArea:foo-bar-baz-does-not-exist}}\n", query);
    }

    /**
     * Tests evaluating the overpass output format statements.
     */
    @Test
    void testOutputFormatStatement() {
        for (OverpassOutputFormat oof : OverpassOutputFormat.values()) {
            Matcher m = OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher("[out:"+oof.getDirective()+"]");
            assertTrue(m.matches());
            assertEquals(oof.getDirective(), m.group(1));
        }

        assertTrue(OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher(
                "[out:pbf][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  node[\"amenity\"=\"pharmacy\"];\n" +
                "  way[\"amenity\"=\"pharmacy\"];\n" +
                "  relation[\"amenity\"=\"pharmacy\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;").matches());
    }

    /**
     * Test {@link OverpassDownloadReader#fixQuery(String)}.
     */
    @Test
    void testFixQuery() {
        assertNull(OverpassDownloadReader.fixQuery(null));

        assertEquals("out meta;", OverpassDownloadReader.fixQuery("out;"));
        assertEquals("out meta;", OverpassDownloadReader.fixQuery("out body;"));
        assertEquals("out meta;", OverpassDownloadReader.fixQuery("out skel;"));
        assertEquals("out meta;", OverpassDownloadReader.fixQuery("out ids;"));

        assertEquals("out meta id;", OverpassDownloadReader.fixQuery("out id;"));
        assertEquals("out meta id;", OverpassDownloadReader.fixQuery("out body id;"));
        assertEquals("out meta id;", OverpassDownloadReader.fixQuery("out skel id;"));
        assertEquals("out meta id;", OverpassDownloadReader.fixQuery("out ids id;"));

        assertEquals("out meta qt;", OverpassDownloadReader.fixQuery("out qt;"));
        assertEquals("out meta qt;", OverpassDownloadReader.fixQuery("out body qt;"));
        assertEquals("out meta qt;", OverpassDownloadReader.fixQuery("out skel qt;"));
        assertEquals("out meta qt;", OverpassDownloadReader.fixQuery("out ids qt;"));

        assertEquals("[out:json]", OverpassDownloadReader.fixQuery("[out:json]"));
        assertEquals("[out:xml]", OverpassDownloadReader.fixQuery("[out:csv(\n" +
                "    ::\"id\", amenity, name, operator, opening_hours, \"contact:website\", \"contact:phone\", brand, dispensing, lastcheck\n" +
                "  )]"));

        assertEquals("[out:json][timeout:90];\n" +
                "(\n" +
                "  node[\"historic\"=\"ringfort\"];\n" +
                "  way[\"historic\"=\"ringfort\"];\n" +
                ");\n" +
                "out meta;",
            OverpassDownloadReader.fixQuery("[out:json][timeout:90];\n" +
                "(\n" +
                "  node[\"historic\"=\"ringfort\"];\n" +
                "  way[\"historic\"=\"ringfort\"];\n" +
                ");\n" +
                "out body;"));
    }

    /**
     * Unit test of {@link OverpassDownloadReader#searchName(java.util.List)}
     * @throws Exception if an error occurs
     */
    @Test
    void testSearchName() throws Exception {
        try (StringReader reader = new StringReader(NameFinderTest.SAMPLE)) {
            assertEquals(1942586L,
                    OverpassDownloadReader.searchName(NameFinder.parseSearchResults(reader)).getOsmId().getUniqueId());
        }
    }
}
