// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

/**
 * Unit tests of {@link GeoUrlToBoundsTest} class.
 */
public class GeoUrlToBoundsTest {

    /**
     * Tests parsing Geo URLs with the zoom specified.
     */
    @Test
    public void testParse() {
        assertThat(
                GeoUrlToBounds.parse("geo:12.34,56.78?z=9"),
                is(OsmUrlToBounds.positionToBounds(12.34, 56.78, 9))
        );
    }

    /**
     * Tests parsing Geo URLs without the zoom parameter.
     */
    @Test
    public void testParseWithoutZoom() {
        assertThat(
                GeoUrlToBounds.parse("geo:12.34,56.78"),
                is(OsmUrlToBounds.positionToBounds(12.34, 56.78, 18))
        );
        assertThat(
                GeoUrlToBounds.parse("geo:-37.786971,-122.399677"),
                is(OsmUrlToBounds.positionToBounds(-37.786971, -122.399677, 18))
        );
    }

    /**
     * Tests parsing Geo URLs with a CRS and/or uncertainty.
     */
    @Test
    public void testParseCrsUncertainty() {
        assertThat(
                GeoUrlToBounds.parse("geo:60.00000,17.000000;crs=wgs84"),
                is(OsmUrlToBounds.positionToBounds(60.0, 17.0, 18))
        );
        assertThat(
                GeoUrlToBounds.parse("geo:60.00000,17.000000;crs=wgs84;u=0"),
                is(OsmUrlToBounds.positionToBounds(60.0, 17.0, 18))
        );
        assertThat(
                GeoUrlToBounds.parse("geo:60.00000,17.000000;u=20"),
                is(OsmUrlToBounds.positionToBounds(60.0, 17.0, 18))
        );
    }

    /**
     * Tests parsing invalid Geo URL.
     */
    @Test
    public void testInvalid() {
        assertThat(GeoUrlToBounds.parse("geo:foo"), nullValue());
        assertThat(GeoUrlToBounds.parse("geo:foo,bar"), nullValue());
    }

    /**
     * Tests parsing null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        GeoUrlToBounds.parse(null);
    }
}
