// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


import org.openstreetmap.josm.TestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test class for {@link GeometryTypes}
 * @author Taylor Smock
 * @since xxx
 */
class GeometryTypesTest {
    @Test
    void testNaiveEnumTest() {
        TestUtils.superficialEnumCodeCoverage(GeometryTypes.class);
        TestUtils.superficialEnumCodeCoverage(GeometryTypes.Ring.class);
    }

    @ParameterizedTest
    @EnumSource(GeometryTypes.class)
    void testExpectedIds(GeometryTypes type) {
        // Ensure that users can get the type from the ordinal
        // See https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto#L8
        // for the expected values
        final int expectedId;
        if (type == GeometryTypes.UNKNOWN) {
            expectedId = 0;
        } else if (type == GeometryTypes.POINT) {
            expectedId = 1;
        } else if (type == GeometryTypes.LINESTRING) {
            expectedId = 2;
        } else if (type == GeometryTypes.POLYGON) {
            expectedId = 3;
        } else {
            fail("Unknown geometry type, see vector tile spec");
            expectedId = Integer.MIN_VALUE;
        }
        assertEquals(expectedId, type.ordinal());
    }
}
