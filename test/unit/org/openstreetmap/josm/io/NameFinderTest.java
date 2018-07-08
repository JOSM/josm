// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Unit tests of {@link NameFinder} class.
 */
public class NameFinderTest {

    // CHECKSTYLE.OFF: LineLength

    /**
     * Sample Nominatim results.
     */
    public static final String SAMPLE =
        "<searchresults timestamp=\"Sat, 07 Jul 18 23:43:34 +0000\" attribution=\"Data © OpenStreetMap contributors, ODbL 1.0. http://www.openstreetmap.org/copyright\" querystring=\"Delhi\" polygon=\"false\" exclude_place_ids=\"28527497,179568493,138693057,1277318,78359235,223543668,421009,177545659,389477,384720\" more_url=\"https://nominatim.openstreetmap.org/search.php?q=Delhi&amp;exclude_place_ids=28527497%2C179568493%2C138693057%2C1277318%2C78359235%2C223543668%2C421009%2C177545659%2C389477%2C384720&amp;format=xml&amp;accept-language=fr%2Cfr-FR%3Bq%3D0.8%2Cen-US%3Bq%3D0.5%2Cen%3Bq%3D0.3\">\n" +
        "    <place place_id=\"28527497\" osm_type=\"node\" osm_id=\"2702400314\" place_rank=\"16\" boundingbox=\"28.4917178,28.8117178,77.0619388,77.3819388\" lat=\"28.6517178\" lon=\"77.2219388\" display_name=\"Delhi, Central Delhi, Delhi, 110006, Inde\" class=\"place\" type=\"city\" importance=\"0.28221035601801\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_city.p.20.png\"/>\n" +
        "    <place place_id=\"179568493\" osm_type=\"relation\" osm_id=\"1942586\" place_rank=\"8\" boundingbox=\"28.404625,28.8834464,76.8388351,77.3463006\" lat=\"28.6273928\" lon=\"77.1716954\" display_name=\"Delhi, Inde\" class=\"boundary\" type=\"administrative\" importance=\"0.28221035601801\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_boundary_administrative.p.20.png\"/>\n" +
        "    <place place_id=\"138693057\" osm_type=\"way\" osm_id=\"301750823\" place_rank=\"17\" boundingbox=\"21.4455864,21.6565955,73.9133997,73.9519009\" lat=\"21.575792\" lon=\"73.9353788\" display_name=\"Delhi, Nandubar, Maharashtra, Inde\" class=\"waterway\" type=\"river\" importance=\"0.19125\"/>\n" +
        "    <place place_id=\"1277318\" osm_type=\"node\" osm_id=\"300479055\" place_rank=\"18\" boundingbox=\"42.809918,42.889918,-80.533736,-80.453736\" lat=\"42.849918\" lon=\"-80.493736\" display_name=\"Delhi, Norfolk County, Ontario, N4B 2B8, Canada\" class=\"place\" type=\"town\" importance=\"0.18841616785547\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_town.p.20.png\"/>\n" +
        "    <place place_id=\"78359235\" osm_type=\"way\" osm_id=\"33197279\" place_rank=\"30\" boundingbox=\"37.419316,37.448629,-120.795433,-120.740257\" lat=\"37.434026\" lon=\"-120.776838290828\" display_name=\"Delhi, El Capitan Way, Delhi, Merced County, Californie, 95315, États-Unis d'Amérique\" class=\"boundary\" type=\"administrative\" importance=\"0.18777680269932\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_boundary_administrative.p.20.png\"/>\n" +
        "    <place place_id=\"223543668\" osm_type=\"node\" osm_id=\"150960458\" place_rank=\"19\" boundingbox=\"37.4121589,37.4521589,-120.7985354,-120.7585354\" lat=\"37.4321589\" lon=\"-120.7785354\" display_name=\"Delhi, Merced County, Californie, 95315, États-Unis d'Amérique\" class=\"place\" type=\"village\" importance=\"0.18777680269932\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_village.p.20.png\"/>\n" +
        "    <place place_id=\"421009\" osm_type=\"node\" osm_id=\"153844606\" place_rank=\"18\" boundingbox=\"39.0550595,39.1350595,-84.6452225,-84.5652225\" lat=\"39.0950595\" lon=\"-84.6052225\" display_name=\"Delhi, Delhi Township, Comté de Hamilton, Ohio, 45238, États-Unis d'Amérique\" class=\"place\" type=\"town\" importance=\"0.185\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_town.p.20.png\"/>\n" +
        "    <place place_id=\"177545659\" osm_type=\"relation\" osm_id=\"132164\" place_rank=\"16\" boundingbox=\"32.434949,32.471221,-91.506983,-91.471765\" lat=\"32.4576421\" lon=\"-91.4931736\" display_name=\"Delhi, Richland Parish, Louisiane, États-Unis d'Amérique\" class=\"boundary\" type=\"administrative\" importance=\"0.18311126145507\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_boundary_administrative.p.20.png\"/>\n" +
        "    <place place_id=\"389477\" osm_type=\"node\" osm_id=\"151839268\" place_rank=\"19\" boundingbox=\"29.808095,29.848095,-97.4155563,-97.3755563\" lat=\"29.828095\" lon=\"-97.3955563\" display_name=\"Delhi, Caldwell County, Texas, 78953, États-Unis d'Amérique\" class=\"place\" type=\"hamlet\" importance=\"0.17875\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_village.p.20.png\"/>\n" +
        "    <place place_id=\"384720\" osm_type=\"node\" osm_id=\"151753473\" place_rank=\"19\" boundingbox=\"39.0253249,39.0653249,-90.2759455,-90.2359455\" lat=\"39.0453249\" lon=\"-90.2559455\" display_name=\"Delhi, Jersey County, Illinois, États-Unis d'Amérique\" class=\"place\" type=\"hamlet\" importance=\"0.17875\" icon=\"https://nominatim.openstreetmap.org/images/mapicons/poi_place_village.p.20.png\"/>\n" +
        "</searchresults>";

    // CHECKSTYLE.ON: LineLength

    /**
     * Unit test of {@link NameFinder#parseSearchResults}.
     * @throws Exception if any error occurs
     */
    @Test
    public void testParseSearchResults() throws Exception {
        try (StringReader reader = new StringReader(SAMPLE)) {
            assertEquals(Arrays.asList(
                    2702400314L, 1942586L, 301750823L, 300479055L, 33197279L, 150960458L, 153844606L, 132164L, 151839268L, 151753473L),
                    NameFinder.parseSearchResults(reader).stream().map(r -> r.getOsmId().getUniqueId()).collect(Collectors.toList()));
        }
    }
}
