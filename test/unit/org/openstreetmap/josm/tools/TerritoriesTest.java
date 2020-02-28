// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link Territories} class.
 */
public class TerritoriesTest {
    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection().territories();

    /**
     * Tests that {@code Territories} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Territories.class);
    }

    /**
     * Test of {@link Territories#isIso3166Code} method.
     */
    @Test
    public void testIsIso3166Code() {
        check("Paris", new LatLon(48.8567, 2.3508), "EU", "FR", "FX");
    }

    private static void check(String name, LatLon ll, String... expectedCodes) {
        for (String e : expectedCodes) {
            assertTrue(name + " " + e, Territories.isIso3166Code(e, ll));
        }
    }

    /**
     * Test of {@link Territories#initializeExternalData} - nominal case
     */
    @Test
    public void testTaginfoGeofabrik_nominal() {
        Map<String, TaginfoRegionalInstance> cache = new TreeMap<>();
        Territories.initializeExternalData(cache, "foo", TestUtils.getTestDataRoot() + "/taginfo/geofabrik-index-v1-nogeom.json");
        assertEquals(5, cache.size());
        checkTaginfoInstance(cache.get("AF"), singleton("AF"), "https://taginfo.geofabrik.de/asia/afghanistan/");
        checkTaginfoInstance(cache.get("AL"), singleton("AL"), "https://taginfo.geofabrik.de/europe/albania/");
        checkTaginfoInstance(cache.get("CA-AB"), singleton("CA-AB"), "https://taginfo.geofabrik.de/north-america/canada/alberta/");
        Set<String> israelAndPalestine = new HashSet<>(Arrays.asList("PS", "IL"));
        checkTaginfoInstance(cache.get("PS"), israelAndPalestine, "https://taginfo.geofabrik.de/asia/israel-and-palestine/");
        checkTaginfoInstance(cache.get("IL"), israelAndPalestine, "https://taginfo.geofabrik.de/asia/israel-and-palestine/");
    }

    private static void checkTaginfoInstance(TaginfoRegionalInstance instance, Set<String> expectedIsoCodes, String expectedUrl) {
        assertEquals(expectedIsoCodes, instance.getIsoCodes());
        assertEquals("foo", instance.getSuffix());
        assertEquals(expectedUrl, instance.getUrl());
    }

    /**
     * Test of {@link Territories#initializeExternalData} - broken contents
     */
    @Test
    public void testTaginfoGeofabrik_broken() {
        Map<String, TaginfoRegionalInstance> cache = new TreeMap<>();
        Logging.clearLastErrorAndWarnings();
        Territories.initializeExternalData(cache, "foo", TestUtils.getTestDataRoot() + "taginfo/geofabrik-index-v1-nogeom-broken.json");
        assertTrue(cache.isEmpty());
        assertEquals("W: Failed to parse external taginfo data at test/data/taginfo/geofabrik-index-v1-nogeom-broken.json: " +
                "Invalid token=EOF at (line no=3, column no=49, offset=97). Expected tokens are: " +
                "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL, SQUARECLOSE]",
                Logging.getLastErrorAndWarnings().get(0));
    }
}
