// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Presets;
import org.openstreetmap.josm.testutils.annotations.Users;
import org.xml.sax.SAXException;

import com.github.tomakehurst.wiremock.WireMockServer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DefaultNameFormatter} class.
 */
// Preferences are needed for OSM primitives
@BasicPreferences
@BasicWiremock
@HTTP
@Main
@Presets
@Users
class DefaultNameFormatterTest {
    /**
     * HTTP mock.
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9632">#9632</a>.
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any XML error occurs
     */
    @Test
    @SuppressFBWarnings(value = "ITA_INEFFICIENT_TO_ARRAY")
    void testTicket9632() throws IllegalDataException, IOException, SAXException {
        String source = "presets/Presets_BicycleJunction-preset.xml";
        wireMockServer.stubFor(get(urlEqualTo("/" + source))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBodyFile(source)));
        TaggingPresets.addTaggingPresets(TaggingPresetReader.readAll(wireMockServer.url(source), true));

        Comparator<IRelation<?>> comparator = DefaultNameFormatter.getInstance().getRelationComparator();

        try (InputStream is = TestUtils.getRegressionDataStream(9632, "data.osm.zip")) {
            DataSet ds = OsmReader.parseDataSet(is, null);

            // Test with 3 known primitives causing the problem. Correct order is p1, p3, p2 with this preset
            Relation p1 = (Relation) ds.getPrimitiveById(2983382, OsmPrimitiveType.RELATION);
            Relation p2 = (Relation) ds.getPrimitiveById(550315, OsmPrimitiveType.RELATION);
            Relation p3 = (Relation) ds.getPrimitiveById(167042, OsmPrimitiveType.RELATION);

            // route_master ("Bus 453", 6 members)
            System.out.println("p1: "+DefaultNameFormatter.getInstance().format(p1)+" - "+p1);
            // TMC ("A 6 Kaiserslautern - Mannheim [negative]", 123 members)
            System.out.println("p2: "+DefaultNameFormatter.getInstance().format(p2)+" - "+p2);
            // route(lcn Sal  Salier-Radweg(412 members)
            System.out.println("p3: "+DefaultNameFormatter.getInstance().format(p3)+" - "+p3);

            // CHECKSTYLE.OFF: SingleSpaceSeparator
            assertEquals(comparator.compare(p1, p2), -1); // p1 < p2
            assertEquals(comparator.compare(p2, p1),  1); // p2 > p1

            assertEquals(comparator.compare(p1, p3), -1); // p1 < p3
            assertEquals(comparator.compare(p3, p1),  1); // p3 > p1
            assertEquals(comparator.compare(p2, p3),  1); // p2 > p3
            assertEquals(comparator.compare(p3, p2), -1); // p3 < p2
            // CHECKSTYLE.ON: SingleSpaceSeparator

            Relation[] relations = new ArrayList<>(ds.getRelations()).toArray(new Relation[0]);

            TestUtils.checkComparableContract(comparator, relations);
        }
    }

    /**
     * Tests formatting of relation names.
     */
    @Test
    void testRelationName() {
        assertEquals("relation (0, 0 members)",
                getFormattedRelationName("X=Y"));
        assertEquals("relation (\"Foo\", 0 members)",
                getFormattedRelationName("name=Foo"));
        assertEquals("route (\"123\", 0 members)",
                getFormattedRelationName("type=route route=tram ref=123"));
        assertEquals("multipolygon (\"building\", 0 members)",
                getFormattedRelationName("type=multipolygon building=yes"));
        assertEquals("multipolygon (\"123\", 0 members)",
                getFormattedRelationName("type=multipolygon building=yes ref=123"));
        assertEquals("multipolygon (\"building\", 0 members)",
                getFormattedRelationName("type=multipolygon building=yes addr:housenumber=123"));
        assertEquals("multipolygon (\"residential\", 0 members)",
                getFormattedRelationName("type=multipolygon building=residential addr:housenumber=123"));
    }

    /**
     * Tests formatting of way names.
     */
    @Test
    void testWayName() {
        assertEquals("\u200Ebuilding\u200E (0 nodes)\u200C", getFormattedWayName("building=yes"));
        assertEquals("\u200EHouse number 123\u200E (0 nodes)\u200C",
                getFormattedWayName("building=yes addr:housenumber=123"));
        assertEquals("\u200EHouse number 123 at FooStreet\u200E (0 nodes)\u200C",
                getFormattedWayName("building=yes addr:housenumber=123 addr:street=FooStreet"));
        assertEquals("\u200EHouse FooName\u200E (0 nodes)\u200C",
                getFormattedWayName("building=yes addr:housenumber=123 addr:housename=FooName"));
    }

    static String getFormattedRelationName(String tagsString) {
        return DefaultNameFormatter.getInstance().format(OsmUtils.createPrimitive("relation " + tagsString));
    }

    static String getFormattedWayName(String tagsString) {
        return DefaultNameFormatter.getInstance().format(OsmUtils.createPrimitive("way " + tagsString));
    }

    /**
     * Test of {@link DefaultNameFormatter#formatAsHtmlUnorderedList} methods.
     */
    @Test
    void testFormatAsHtmlUnorderedList() {
        assertEquals("<ul><li>incomplete</li></ul>",
                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(new Node(1)));

        List<Node> nodes = IntStream.rangeClosed(1, 10).mapToObj(i -> new Node(i, 1))
                .collect(Collectors.toList());
        assertEquals("<ul><li>1</li><li>2</li><li>3</li><li>4</li><li>...</li></ul>",
                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(nodes, 5));
    }

    /**
     * Test of {@link DefaultNameFormatter#buildDefaultToolTip(IPrimitive)}.
     */
    @Test
    void testBuildDefaultToolTip() {
        assertEquals("<html><strong>id</strong>=0<br>"+
                           "<strong>name:en</strong>=foo<br>"+
                           "<strong>tourism</strong>=hotel<br>"+
                           "<strong>url</strong>=http://foo.bar<br>"+
                           "<strong>xml</strong>=&lt;tag/&gt;</html>",
                DefaultNameFormatter.getInstance().buildDefaultToolTip(
                        TestUtils.newNode("tourism=hotel name:en=foo url=http://foo.bar xml=<tag/>")));
    }

    /**
     * Test of {@link DefaultNameFormatter#removeBiDiCharacters(String)}.
     */
    @Test
    void testRemoveBiDiCharacters() {
        assertEquals("building (0 nodes)", DefaultNameFormatter.removeBiDiCharacters("\u200Ebuilding\u200E (0 nodes)\u200C"));
    }
}
