// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test of boundaries OSM file.
 */
public class BoundariesTestIT {

    private static final List<String> RETIRED_ISO3166_1_CODES = Arrays.asList(
            "AN", "BU", "CS", "NT", "TP", "YU", "ZR");

    private static final List<String> EXCEPTIONNALY_RESERVED_ISO3166_1_CODES = Arrays.asList(
            "AC", "CP", "DG", "EA", "EU", "EZ", "FX", "IC", "SU", "TA", "UK", "UN");

    private static final List<String> ISO3166_2_CODES = Arrays.asList(
            "AU-ACT", "AU-NSW", "AU-NT", "AU-QLD", "AU-SA", "AU-TAS", "AU-VIC", "AU-WA",
            "CA-AB", "CA-BC", "CA-MB", "CA-NB", "CA-NL", "CA-NS", "CA-NT", "CA-NU", "CA-ON", "CA-PE", "CA-QC", "CA-SK", "CA-YT",
            "CN-AH", "CN-BJ", "CN-CQ", "CN-FJ", "CN-GD", "CN-GS", "CN-GX", "CN-GZ", "CN-HA", "CN-HB", "CN-HE", "CN-HI", "CN-HK", "CN-HL",
            "CN-HN", "CN-JL", "CN-JS", "CN-JX", "CN-LN", "CN-MO", "CN-NM", "CN-NX", "CN-QH", "CN-SC", "CN-SD", "CN-SH", "CN-SN", "CN-SX",
            "CN-TJ", "CN-TW", "CN-XJ", "CN-XZ", "CN-YN", "CN-ZJ",
            "IN-AP", "IN-AR", "IN-AS", "IN-BR", "IN-CT", "IN-GA", "IN-GJ", "IN-HR", "IN-HP", "IN-JK", "IN-JH", "IN-KA", "IN-KL", "IN-MP",
            "IN-MH", "IN-MN", "IN-ML", "IN-MZ", "IN-NL", "IN-OR", "IN-PB", "IN-RJ", "IN-SK", "IN-TN", "IN-TG", "IN-TR", "IN-UT", "IN-UP",
            "IN-WB", "IN-AN", "IN-CH", "IN-DN", "IN-DD", "IN-DL", "IN-LD", "IN-PY",
            "US-AL", "US-AK", "US-AS", "US-AZ", "US-AR", "US-CA", "US-CO", "US-CT", "US-DE", "US-DC", "US-FL", "US-GA", "US-GU", "US-HI",
            "US-ID", "US-IL", "US-IN", "US-IA", "US-KS", "US-KY", "US-LA", "US-ME", "US-MD", "US-MA", "US-MI", "US-MN", "US-MS", "US-MO",
            "US-MT", "US-NE", "US-NV", "US-NH", "US-NJ", "US-NM", "US-NY", "US-NC", "US-ND", "US-MP", "US-OH", "US-OK", "US-OR", "US-PA",
            "US-PR", "US-RI", "US-SC", "US-SD", "US-TN", "US-TX", "US-UM", "US-UT", "US-VT", "US-VA", "US-VI", "US-WA", "US-WV", "US-WI",
            "US-WY");

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of boundaries OSM file.
     * @throws Exception if an error occurs
     */
    @Test
    public void testBoundariesFile() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/data/boundaries.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            List<OsmPrimitive> tagged = ds.allPrimitives().stream().filter(OsmPrimitive::isTagged).collect(Collectors.toList());
            List<String> iso31661a2 = Arrays.asList(Locale.getISOCountries());
            // Check presence of all ISO-3166-1 alpha 2 codes
            for (String code : iso31661a2) {
                if (!RETIRED_ISO3166_1_CODES.contains(code)) {
                    assertEquals(code, 1, tagged.stream().filter(SearchCompiler.compile("ISO3166-1\\:alpha2="+code)).count());
                }
            }
            // Check for unknown ISO-3166-1 alpha 2 codes
            for (OsmPrimitive p : tagged.stream().filter(SearchCompiler.compile("ISO3166-1\\:alpha2")).collect(Collectors.toList())) {
                String code = p.get("ISO3166-1:alpha2");
                assertTrue(code, iso31661a2.contains(code) || EXCEPTIONNALY_RESERVED_ISO3166_1_CODES.contains(code));
            }
            // Check presence of all ISO-3166-2 codes for USA, Canada, Australia (for speed limits)
            for (String code : ISO3166_2_CODES) {
                assertEquals(code, 1, tagged.stream().filter(SearchCompiler.compile("ISO3166-2="+code)).count());
            }
        }
    }
}
