// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.Bounds;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.InvalidMapboxVectorTileException;

/**
 * Test class for {@link Source}
 * @author Taylor Smock
 * @since xxx
 */
public class SourceTest {
    @Test
    void testEquals() {
        EqualsVerifier.forClass(Source.class).usingGetClass().verify();
    }

    @Test
    void testSimpleSources() {
        final JsonObject emptyObject = Json.createObjectBuilder().build();
        assertThrows(NullPointerException.class, () -> new Source("Test source", emptyObject));

        final JsonObject badTypeValue = Json.createObjectBuilder().add("type", "bad type value").build();
        assertThrows(IllegalArgumentException.class, () -> new Source("Test source", badTypeValue));

        // Only SourceType.{VECTOR,RASTER} are supported
        final SourceType[] supported = new SourceType[] {SourceType.VECTOR, SourceType.RASTER};
        for (SourceType type : supported) {
            final JsonObject goodSourceType = Json.createObjectBuilder().add("type", type.toString().toLowerCase(Locale.ROOT)).build();
            Source source = assertDoesNotThrow(() -> new Source(type.name(), goodSourceType));
            // Check defaults
            assertEquals(0, source.getMinZoom());
            assertEquals(22, source.getMaxZoom());
            assertEquals(type.name(), source.getName());
            assertNull(source.getAttributionText());
            assertTrue(source.getUrls().isEmpty());
            assertEquals(new Bounds(-85.051129, -180, 85.051129, 180), source.getBounds());
        }

        // Check that unsupported types throw
        for (SourceType type : Stream.of(SourceType.values()).filter(t -> Stream.of(supported).noneMatch(t::equals)).collect(
          Collectors.toList())) {
            final JsonObject goodSourceType = Json.createObjectBuilder().add("type", type.toString().toLowerCase(Locale.ROOT)).build();
            assertThrows(UnsupportedOperationException.class, () -> new Source(type.name(), goodSourceType));
        }
    }

    @Test
    void testTileJsonSpec() {
        // This isn't currently implemented, so it should throw. Mostly here to remind implementor to add tests...
        final JsonObject tileJsonSpec = Json.createObjectBuilder()
          .add("type", SourceType.VECTOR.name()).add("url", "some-random-url.com")
          .build();
        assertThrows(InvalidMapboxVectorTileException.class, () -> new Source("Test TileJson", tileJsonSpec));
    }

    @Test
    void testBounds() {
        // Check a "good" bounds
        final JsonObject tileJsonSpec = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("bounds",
          Json.createArrayBuilder().add(-1).add(-2).add(3).add(4)).build();
        Source source = new Source("Test Bounds[-1, -2, 3, 4]", tileJsonSpec);
        assertEquals(new Bounds(-2, -1, 4, 3), source.getBounds());

        // Check "bad" bounds
        final JsonObject tileJsonSpecShort = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("bounds",
          Json.createArrayBuilder().add(-1).add(-2).add(3)).build();
        IllegalArgumentException badLengthException = assertThrows(IllegalArgumentException.class,
          () -> new Source("Test Bounds[-1, -2, 3]", tileJsonSpecShort));
        assertEquals("bounds must have four values, but has 3", badLengthException.getMessage());

        final JsonObject tileJsonSpecLong = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("bounds",
          Json.createArrayBuilder().add(-1).add(-2).add(3).add(4).add(5)).build();
        badLengthException = assertThrows(IllegalArgumentException.class, () -> new Source("Test Bounds[-1, -2, 3, 4, 5]", tileJsonSpecLong));
        assertEquals("bounds must have four values, but has 5", badLengthException.getMessage());
    }

    @Test
    void testTiles() {
        // No url
        final JsonObject tileJsonSpecEmpty = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("tiles",
          JsonValue.NULL).build();
        Source source = new Source("Test Tile[]", tileJsonSpecEmpty);
        assertTrue(source.getUrls().isEmpty());

        // Create a tile URL
        final JsonObject tileJsonSpec = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("tiles",
          Json.createArrayBuilder().add("https://example.org/{bbox-epsg-3857}")).build();
        source = new Source("Test Tile[https://example.org/{bbox-epsg-3857}]", tileJsonSpec);
        assertEquals(1, source.getUrls().size());
        // Make certain that {bbox-epsg-3857} is replaced with the JOSM equivalent
        assertEquals("https://example.org/{bbox}", source.getUrls().get(0));

        // Check with invalid data
        final JsonObject tileJsonSpecBad = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("tiles",
          Json.createArrayBuilder().add(1).add("https://example.org/{bbox-epsg-3857}").add(false).add(Json.createArrayBuilder().add("hello"))
            .add(Json.createObjectBuilder().add("bad", "array"))).build();
        source = new Source("Test Tile[1, https://example.org/{bbox-epsg-3857}, false, [\"hello\"], {\"bad\": \"array\"}]", tileJsonSpecBad);
        assertEquals(1, source.getUrls().size());
        // Make certain that {bbox-epsg-3857} is replaced with the JOSM equivalent
        assertEquals("https://example.org/{bbox}", source.getUrls().get(0));
    }

    @Test
    void testZoom() {
        // Min zoom
        final JsonObject minZoom5 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("minzoom",
          5).build();
        Source source = new Source("Test Zoom[minzoom=5]", minZoom5);
        assertEquals(5, source.getMinZoom());
        assertEquals(22, source.getMaxZoom());

        // Negative min zoom
        final JsonObject minZoomNeg1 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("minzoom",
          -1).build();
        source = new Source("Test Zoom[minzoom=-1]", minZoomNeg1);
        assertEquals(0, source.getMinZoom());
        assertEquals(22, source.getMaxZoom());

        // Max zoom
        final JsonObject maxZoom5 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("maxzoom",
          5).build();
        source = new Source("Test Zoom[maxzoom=5]", maxZoom5);
        assertEquals(0, source.getMinZoom());
        assertEquals(5, source.getMaxZoom());

        // Big Max zoom
        final JsonObject maxZoom31 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("maxzoom",
          31).build();
        source = new Source("Test Zoom[maxzoom=31]", maxZoom31);
        assertEquals(0, source.getMinZoom());
        assertEquals(30, source.getMaxZoom());

        // Negative max zoom
        final JsonObject maxZoomNeg5 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("maxzoom",
          -5).build();
        source = new Source("Test Zoom[maxzoom=-5]", maxZoomNeg5);
        assertEquals(0, source.getMinZoom());
        assertEquals(0, source.getMaxZoom());

        // Min max zoom
        final JsonObject minZoom1MaxZoom5 = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("maxzoom",
          5).add("minzoom", 1).build();
        source = new Source("Test Zoom[minzoom=1,maxzoom=5]", minZoom1MaxZoom5);
        assertEquals(1, source.getMinZoom());
        assertEquals(5, source.getMaxZoom());
    }

    @Test
    void testToString() {
        // Simple (no urls)
        final JsonObject noTileJsonSpec = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).build();
        Source source = new Source("Test String[]", noTileJsonSpec);
        assertEquals("Test String[]", source.toString());

        // With one url
        final JsonObject tileJsonSpec = Json.createObjectBuilder().add("type", SourceType.VECTOR.name()).add("tiles",
          Json.createArrayBuilder().add("https://example.org/{bbox-epsg-3857}")).build();
        source = new Source("Test String[https://example.org/{bbox-epsg-3857}]", tileJsonSpec);
        assertEquals("Test String[https://example.org/{bbox-epsg-3857}] https://example.org/{bbox}", source.toString());

        // With two URLs
        final JsonObject tileJsonSpecMultiple = Json.createObjectBuilder().add("type", SourceType.VECTOR.name())
          .add("tiles", Json.createArrayBuilder()
            .add("https://example.org/{bbox-epsg-3857}")
            .add("https://example.com/{bbox-epsg-3857}")).build();
        source = new Source("Test String[https://example.org/{bbox-epsg-3857},https://example.com/{bbox-epsg-3857}]", tileJsonSpecMultiple);
        assertEquals("Test String[https://example.org/{bbox-epsg-3857},https://example.com/{bbox-epsg-3857}] https://example.org/{bbox} "
          + "https://example.com/{bbox}", source.toString());
    }
}
