// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.Bounds;

class TileZXYTest {
    static Stream<Arguments> testBBoxCalculation() {
        return Stream.of(
                Arguments.of(new Bounds(-85.0511288, -180, 85.0511288, 180), new TileZXY(0, 0, 0)),
                Arguments.of(new Bounds(0, -180, 85.0511288, 0), new TileZXY(1, 0, 0)),
                Arguments.of(new Bounds(0, 0, 85.0511288, 180), new TileZXY(1, 1, 0)),
                Arguments.of(new Bounds(-85.0511288, -180, 0, 0), new TileZXY(1, 0, 1)),
                Arguments.of(new Bounds(-85.0511288, 0, 0, 180), new TileZXY(1, 1, 1)),
                Arguments.of(new Bounds(0, 0, 0.0006866, 0.0006866), new TileZXY(19, 262144, 262143)),
                Arguments.of(new Bounds(0, -0.0006866, 0.0006866, 0), new TileZXY(19, 262143, 262143)),
                Arguments.of(new Bounds(-0.0006866, -0.0006866, 0, 0), new TileZXY(19, 262143, 262144)),
                Arguments.of(new Bounds(-0.0006866, 0, 0, 0.0006866), new TileZXY(19, 262144, 262144))
                );
    }

    @ParameterizedTest
    @MethodSource
    void testBBoxCalculation(Bounds expected, TileZXY tile) {
        assertEquals(expected, TileZXY.tileToBounds(tile));
    }

    static Stream<Arguments> testTileFromLatLon() {
        final double delta = 0.00001; // Purely to get off of tile boundaries
        return testBBoxCalculation().flatMap(arg -> {
            final Bounds expected = (Bounds) arg.get()[0];
            final TileZXY tile = (TileZXY) arg.get()[1];
            return Stream.of(Arguments.of("UL", expected.getMaxLat() - delta, expected.getMinLon() + delta, tile),
                    Arguments.of("LL", expected.getMinLat() + delta, expected.getMinLon() + delta, tile),
                    Arguments.of("UR", expected.getMaxLat() - delta, expected.getMaxLon() - delta, tile),
                    Arguments.of("LR", expected.getMinLat() + delta, expected.getMaxLon() - delta, tile),
                    Arguments.of("Center", expected.getCenter().lat(), expected.getCenter().lon(), tile));
        });
    }

    @ParameterizedTest(name = "{3} - {0}")
    @MethodSource
    void testTileFromLatLon(String description, double lat, double lon, TileZXY tile) {
        assertEquals(tile, TileZXY.latLonToTile(lat, lon, tile.zoom()));
    }

    @Test
    void testEqualsContract() {
        EqualsVerifier.forClass(TileZXY.class).verify();
    }
}
