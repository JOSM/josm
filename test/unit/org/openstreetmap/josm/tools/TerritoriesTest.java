// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.annotations.Projection;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link Territories} class.
 */
@Projection
@org.openstreetmap.josm.testutils.annotations.Territories
class TerritoriesTest {
    /**
     * Tests that {@code Territories} satisfies utility class criteria.
     *
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Territories.class);
    }

    /**
     * Test of {@link Territories#isIso3166Code} method.
     */
    @Test
    void testIsIso3166Code() {
        check("Paris", new LatLon(48.8567, 2.3508), "EU", "FR", "FX");
    }

    private static void check(String name, LatLon ll, String... expectedCodes) {
        for (String e : expectedCodes) {
            assertTrue(Territories.isIso3166Code(e, ll), name + " " + e);
        }
    }

    /**
     * Test of {@link Territories#initializeExternalData} - nominal case
     */
    @Test
    void testTaginfoGeofabrik_nominal() {
        Territories.initializeExternalData("foo", TestUtils.getTestDataRoot() + "/taginfo/geofabrik-index-v1-nogeom.json");
        Map<String, TaginfoRegionalInstance> cache = Territories.taginfoGeofabrikCache;
        assertEquals(5, cache.size());
        checkTaginfoInstance(cache.get("AF"), singleton("AF"), "https://taginfo.geofabrik.de/asia/afghanistan/");
        checkTaginfoInstance(cache.get("AL"), singleton("AL"), "https://taginfo.geofabrik.de/europe/albania/");
        checkTaginfoInstance(cache.get("CA-AB"), singleton("CA-AB"), "https://taginfo.geofabrik.de/north-america/canada/alberta/");
        Set<String> israelAndPalestine = new HashSet<>(Arrays.asList("PS", "IL"));
        checkTaginfoInstance(cache.get("PS"), israelAndPalestine, "https://taginfo.geofabrik.de/asia/israel-and-palestine/");
        checkTaginfoInstance(cache.get("IL"), israelAndPalestine, "https://taginfo.geofabrik.de/asia/israel-and-palestine/");
        List<TaginfoRegionalInstance> regionalTaginfo = Territories.getRegionalTaginfoUrls(new LatLon(41.3268733, 19.8187913));
        assertEquals(1, regionalTaginfo.size());
        checkTaginfoInstance(regionalTaginfo.iterator().next(), singleton("AL"), "https://taginfo.geofabrik.de/europe/albania/");
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
    void testTaginfoGeofabrik_broken() {
        Logging.clearLastErrorAndWarnings();
        Territories.initializeExternalData("foo", TestUtils.getTestDataRoot() + "taginfo/geofabrik-index-v1-nogeom-broken.json");
        Map<String, TaginfoRegionalInstance> cache = Territories.taginfoGeofabrikCache;
        assertTrue(cache.isEmpty());
        String error = Logging.getLastErrorAndWarnings().get(0);
        assertTrue(error.contains("W: Failed to parse external taginfo data at "), error);
        assertTrue(error.contains(": Invalid token=EOF at (line no=3,"), error);
    }

    /**
     * Unit test of {@link Territories#getCustomTags}
     */
    @Test
    void testGetCustomTags() {
        assertNull(Territories.getCustomTags(null));
        assertNull(Territories.getCustomTags("foo"));
        assertEquals("arab", Territories.getCustomTags("BH").get("ldml:nu:ar"));
    }
}
