// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link Layers}.
 * @implNote Tests will fail when support is added for new styling information.
 * All current (2021-03-31) properties are checked for in some form or another.
 * @author Taylor Smock
 * @since xxx
 */
class LayersTest {
    @Test
    void testBackground() {
        // Test an empty background layer
        Layers emptyBackgroundLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.BACKGROUND.name())
          .add("id", "Empty Background").build());
        assertEquals("Empty Background", emptyBackgroundLayer.getId());
        assertEquals(Layers.Type.BACKGROUND, emptyBackgroundLayer.getType());
        assertNull(emptyBackgroundLayer.getSource());
        assertSame(Expression.EMPTY_EXPRESSION, emptyBackgroundLayer.getFilter());
        assertEquals("", emptyBackgroundLayer.toString());

        // Test a background layer with some styling information
        JsonObject allProperties = Json.createObjectBuilder()
          .add("background-color", "#fff000") // fill-color:#fff000;
          .add("background-opacity", 0.5) // No good mapping for JOSM yet
          .add("background-pattern", "null") // This should be an image, not implemented
          .build();
        Layers backgroundLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Background layer")
          .add("type", Layers.Type.BACKGROUND.name().toLowerCase(Locale.ROOT))
          .add("paint", allProperties)
        .build());
        assertEquals("canvas{fill-color:#fff000;}", backgroundLayer.toString());

        // Test a background layer with some styling information, but invisible
        Layers invisibleBackgroundLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Background layer")
          .add("type", Layers.Type.BACKGROUND.name().toLowerCase(Locale.ROOT))
          .add("layout", Json.createObjectBuilder().add("visibility", "none").build())
          .add("paint", allProperties).build());
        assertEquals("", invisibleBackgroundLayer.toString());
    }

    @Test
    void testFill() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.FILL.name())
          .add("id", "Empty Fill").build()));

        // Test an empty fill layer
        Layers emptyFillLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.FILL.name())
          .add("id", "Empty Fill")
          .add("source", "Random source").build());
        assertEquals("Empty Fill", emptyFillLayer.getId());
        assertEquals("Random source", emptyFillLayer.getSource());
        assertEquals("", emptyFillLayer.toString());

        // Test a fully implemented fill layer
        JsonObject allLayoutProperties = Json.createObjectBuilder()
          .add("fill-sort-key", 5)
          .add("visibility", "visible")
          .build();
        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("fill-antialias", false)
          .add("fill-color", "#fff000") // fill-color:#fff000
          .add("fill-opacity", 0.5) // fill-opacity:0.5
          .add("fill-outline-color", "#ffff00") // fill-color:#ffff00 (defaults to fill-color)
          .add("fill-pattern", JsonValue.NULL) // disables fill-outline-color and fill-color
          .add("fill-translate", Json.createArrayBuilder().add(5).add(5))
          .add("fill-translate-anchor", "viewport") // requires fill-translate
          .build();

        Layers fullFillLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.FILL.toString())
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullFillLayer.getId());
        assertEquals(Layers.Type.FILL, fullFillLayer.getType());
        assertEquals("area::random-layer-id{fill-color:#fff000;fill-opacity:0.5;color:#ffff00;}", fullFillLayer.toString());

        // Test a fully implemented fill layer (invisible)
        Layers fullFillInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.FILL.toString())
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties)
            .add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullFillInvisibleLayer.getId());
        assertEquals(Layers.Type.FILL, fullFillInvisibleLayer.getType());
        assertEquals("", fullFillInvisibleLayer.toString());
    }

    @Test
    void testLine() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.LINE.name())
          .add("id", "Empty Line").build()));

        JsonObject allLayoutProperties = Json.createObjectBuilder()
          .add("line-cap", "round") // linecap:round;
          .add("line-join", "bevel")
          .add("line-miter-limit", 65)
          .add("line-round-limit", 1.5)
          .add("line-sort-key", 3)
          .add("visibility", "visible")
          .build();
        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("line-blur", 5)
          .add("line-color", "#fff000") // color:#fff000;
          .add("line-dasharray", Json.createArrayBuilder().add(1).add(5).add(1)) // dashes:1,5,1;
          .add("line-gap-width", 6)
          .add("line-gradient", "#ffff00") // disabled by line-dasharray/line-pattern, source must be "geojson"
          .add("line-offset", 12)
          .add("line-opacity", 0.5) // opacity:0.5;
          .add("line-pattern", JsonValue.NULL)
          .add("line-translate", Json.createArrayBuilder().add(-1).add(-2))
          .add("line-translate-anchor", "viewport")
          .add("line-width", 22) // width:22;
          .build();

        // Test fully defined line
        Layers fullLineLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.LINE.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullLineLayer.getId());
        assertEquals(Layers.Type.LINE, fullLineLayer.getType());
        assertEquals("way::random-layer-id{color:#fff000;opacity:0.5;linecap:round;dashes:1,5,1;width:22;}", fullLineLayer.toString());

        // Test invisible line
        Layers fullLineInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.LINE.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties)
            .add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullLineInvisibleLayer.getId());
        assertEquals(Layers.Type.LINE, fullLineInvisibleLayer.getType());
        assertEquals("", fullLineInvisibleLayer.toString());
    }

    @Test
    void testSymbol() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name())
          .add("id", "Empty Symbol").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("icon-color", "#fff000") // also requires sdf icons
          .add("icon-halo-blur", 5)
          .add("icon-halo-color", "#ffff00")
          .add("icon-halo-width", 6)
          .add("icon-opacity", 0.5) // icon-opacity:0.5;
          .add("icon-translate", Json.createArrayBuilder().add(11).add(12))
          .add("icon-translate-anchor", "viewport") // also requires icon-translate
          .add("text-color", "#fffff0") // text-color:#fffff0;
          .add("text-halo-blur", 15)
          .add("text-halo-color", "#ffffff") // text-halo-color:#ffffff;
          .add("text-halo-width", 16) // text-halo-radius:16;
          .add("text-opacity", 0.6) // text-opacity:0.6;
          .add("text-translate", Json.createArrayBuilder().add(26).add(27))
          .add("text-translate-anchor", "viewport")
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder()
          .add("icon-allow-overlap", true)
          .add("icon-anchor", "left")
          .add("icon-ignore-placement", true)
          .add("icon-image", "random-image") // icon-image:concat(\"random-image\");
          .add("icon-keep-upright", true) // also requires icon-rotation-alignment=map and symbol-placement=line|line-center
          .add("icon-offset", Json.createArrayBuilder().add(2).add(3)) // icon-offset-x:2.0;icon-offset-y:3.0;
          .add("icon-optional", true) // also requires text-field
          .add("icon-padding", 4)
          .add("icon-pitch-alignment", "viewport")
          .add("icon-rotate", 30) // icon-rotation:30.0;
          .add("icon-rotation-alignment", "map")
          .add("icon-size", 2)
          .add("icon-text-fit", "width") // also requires text-field
          .add("icon-text-fit-padding", Json.createArrayBuilder().add(7).add(8).add(9).add(10))
          .add("symbol-avoid-edges", true)
          .add("symbol-placement", "line")
          .add("symbol-sort-key", 13)
          .add("symbol-spacing", 14) // requires symbol-placement=line
          .add("symbol-z-order", "source")
          .add("text-allow-overlap", true) // requires text-field
          .add("text-anchor", "left") // requires text-field, disabled by text-variable-anchor
          .add("text-field", "something") // text:something;
          .add("text-font", Json.createArrayBuilder().add("SansSerif")) // DroidSans isn't always available in an IDE
          .add("text-ignore-placement", true)
          .add("text-justify", "left")
          .add("text-keep-upright", false)
          .add("text-letter-spacing", 17)
          .add("text-line-height", 1.3)
          .add("text-max-angle", 18)
          .add("text-max-width", 19)
          .add("text-offset", Json.createArrayBuilder().add(20).add(21))
          .add("text-optional", true)
          .add("text-padding", 22)
          .add("text-pitch-alignment", "viewport")
          .add("text-radial-offset", 23)
          .add("text-rotate", 24)
          .add("text-rotation-alignment", "viewport")
          .add("text-size", 25) // font-size:25;
          .add("text-transform", "uppercase")
          .add("text-variable-anchor", "left")
          .add("text-writing-mode", "vertical")
          .add("visibility", "visible").build();

        // Test fully defined symbol
        Layers fullLineLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullLineLayer.getId());
        assertEquals(Layers.Type.SYMBOL, fullLineLayer.getType());
        assertEquals("node::random-layer-id{icon-image:concat(\"random-image\");icon-offset-x:2.0;icon-offset-y:3.0;"
          + "icon-opacity:0.5;icon-rotation:30.0;text-color:#fffff0;text:something;font-family:\"SansSerif\";font-weight:normal;"
          + "font-style:normal;text-halo-color:#ffffff;text-halo-radius:8;text-opacity:0.6;font-size:25;}", fullLineLayer.toString());

        // Test an invisible symbol
        Layers fullLineInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("random-layer-id", fullLineInvisibleLayer.getId());
        assertEquals(Layers.Type.SYMBOL, fullLineInvisibleLayer.getType());
        assertEquals("", fullLineInvisibleLayer.toString());

        // Test with placeholders in icon-image
        Layers fullOneIconImagePlaceholderLineLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("icon-image", "{value}"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("node::random-layer-id{icon-image:concat(tag(\"value\"));icon-offset-x:2.0;icon-offset-y:3.0;"
          + "icon-opacity:0.5;icon-rotation:30.0;text-color:#fffff0;text:something;font-family:\"SansSerif\";font-weight:normal;"
          + "font-style:normal;text-halo-color:#ffffff;text-halo-radius:8;text-opacity:0.6;font-size:25;}",
          fullOneIconImagePlaceholderLineLayer.toString());

        // Test with placeholders in icon-image
        Layers fullOneIconImagePlaceholderExtraLineLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("icon-image", "something/{value}/random"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("node::random-layer-id{icon-image:concat(\"something/\",tag(\"value\"),\"/random\");icon-offset-x:2.0;"
          + "icon-offset-y:3.0;icon-opacity:0.5;icon-rotation:30.0;text-color:#fffff0;text:something;font-family:\"SansSerif\";"
          + "font-weight:normal;font-style:normal;text-halo-color:#ffffff;text-halo-radius:8;text-opacity:0.6;font-size:25;}",
          fullOneIconImagePlaceholderExtraLineLayer.toString());

        // Test with placeholders in icon-image
        Layers fullTwoIconImagePlaceholderExtraLineLayer = new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SYMBOL.name().toLowerCase(Locale.ROOT))
          .add("id", "random-layer-id")
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("icon-image", "something/{value}/random/{value2}"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("node::random-layer-id{icon-image:concat(\"something/\",tag(\"value\"),\"/random/\",tag(\"value2\"));"
          + "icon-offset-x:2.0;icon-offset-y:3.0;icon-opacity:0.5;icon-rotation:30.0;text-color:#fffff0;text:something;"
          + "font-family:\"SansSerif\";font-weight:normal;font-style:normal;text-halo-color:#ffffff;text-halo-radius:16;"
          + "text-opacity:0.6;font-size:25;}", fullTwoIconImagePlaceholderExtraLineLayer.toString());
    }

    @Test
    void testRaster() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.RASTER.name())
          .add("id", "Empty Raster").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("raster-brightness-max", 0.5)
          .add("raster-brightness-min", 0.6)
          .add("raster-contrast", 0.7)
          .add("raster-fade-duration", 1)
          .add("raster-hue-rotate", 2)
          .add("raster-opacity", 0.7)
          .add("raster-resampling", "nearest")
          .add("raster-saturation", 0.8)
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder().add("visibility", "visible").build();

        // Test fully defined raster
        Layers fullRaster = new Layers(Json.createObjectBuilder()
          .add("id", "test-raster")
          .add("type", Layers.Type.RASTER.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals(Layers.Type.RASTER, fullRaster.getType());
        assertEquals("test-raster", fullRaster.getId());
        assertEquals("Random source", fullRaster.getSource());
        assertEquals("", fullRaster.toString());

        // Test fully defined invisible raster
        Layers fullInvisibleRaster = new Layers(Json.createObjectBuilder()
          .add("id", "test-raster")
          .add("type", Layers.Type.RASTER.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("", fullInvisibleRaster.toString());
    }

    @Test
    void testCircle() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.CIRCLE.name())
          .add("id", "Empty Circle").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("circle-blur", 1)
          .add("circle-color", "#fff000") // symbol-fill-color:#fff000;
          .add("circle-opacity", 0.5) // symbol-fill-opacity:0.5;
          .add("circle-pitch-alignment", "map")
          .add("circle-pitch-scale", "viewport")
          .add("circle-radius", 2) // symbol-size:4.0; (we use width)
          .add("circle-stroke-color", "#ffff00") // symbol-stroke-color:#ffff00;
          .add("circle-stroke-opacity", 0.6) // symbol-stroke-opacity:0.6;
          .add("circle-stroke-width", 5) // symbol-stroke-width:5.0;
          .add("circle-translate", Json.createArrayBuilder().add(3).add(4))
          .add("circle-translate-anchor", "viewport")
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder()
          .add("circle-sort-key", 3)
          .add("visibility", "visible")
          .build();

        Layers fullCircleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Full circle layer")
          .add("type", Layers.Type.CIRCLE.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals(Layers.Type.CIRCLE, fullCircleLayer.getType());
        assertEquals("Full circle layer", fullCircleLayer.getId());
        assertEquals("Random source", fullCircleLayer.getSource());
        assertEquals("node::Full circle layer{symbol-shape:circle;symbol-fill-color:#fff000;symbol-fill-opacity:0.5;"
          + "symbol-size:4.0;symbol-stroke-color:#ffff00;symbol-stroke-opacity:0.6;symbol-stroke-width:5;}", fullCircleLayer.toString());

        Layers fullCircleInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Full circle layer")
          .add("type", Layers.Type.CIRCLE.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals(Layers.Type.CIRCLE, fullCircleInvisibleLayer.getType());
        assertEquals("Full circle layer", fullCircleInvisibleLayer.getId());
        assertEquals("Random source", fullCircleInvisibleLayer.getSource());
        assertEquals("", fullCircleInvisibleLayer.toString());
    }

    @Test
    void testFillExtrusion() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.FILL_EXTRUSION.name())
          .add("id", "Empty Fill Extrusion").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("fill-extrusion-base", 1)
          .add("fill-extrusion-color", "#fff000")
          .add("fill-extrusion-height", 2)
          .add("fill-extrusion-opacity", 0.5)
          .add("fill-extrusion-pattern", "something-random")
          .add("fill-extrusion-translate", Json.createArrayBuilder().add(3).add(4))
          .add("fill-extrusion-translate-anchor", "viewport")
          .add("fill-extrusion-vertical-gradient", false)
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder().add("visibility", "visible").build();

        Layers fullFillLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Fill Extrusion")
          .add("type", Layers.Type.FILL_EXTRUSION.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", allLayoutProperties)
          .add("paint", allPaintProperties)
          .build());
        assertEquals("", fullFillLayer.toString());
        assertEquals(Layers.Type.FILL_EXTRUSION, fullFillLayer.getType());
        Layers fullFillInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Fill Extrusion")
          .add("type", Layers.Type.FILL_EXTRUSION.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .add("paint", allPaintProperties)
          .build());
        assertEquals("", fullFillInvisibleLayer.toString());
        assertEquals(Layers.Type.FILL_EXTRUSION, fullFillInvisibleLayer.getType());
    }

    @Test
    void testHeatmap() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.HEATMAP.name())
          .add("id", "Empty Heatmap").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("heatmap-color", "#fff000") // This will probably be a gradient of some type
          .add("heatmap-intensity", 0.5)
          .add("heatmap-opacity", 0.6)
          .add("heatmap-radius", 1) // This is in pixels
          .add("heatmap-weight", 0.7)
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder().add("visibility", "visible").build();

        Layers fullHeatmapLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Full heatmap")
          .add("type", Layers.Type.HEATMAP.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", allLayoutProperties)
          .build());
        assertEquals(Layers.Type.HEATMAP, fullHeatmapLayer.getType());
        assertEquals("", fullHeatmapLayer.toString());

        Layers fullHeatmapInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Full heatmap")
          .add("type", Layers.Type.HEATMAP.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .build());
        assertEquals(Layers.Type.HEATMAP, fullHeatmapInvisibleLayer.getType());
        assertEquals("", fullHeatmapInvisibleLayer.toString());
    }

    @Test
    void testHillshade() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.HILLSHADE.name())
          .add("id", "Empty Hillshade").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("hillshade-accent-color", "#fff000")
          .add("hillshade-exaggeration", 0.6)
          .add("hillshade-highlight-color", "#ffff00")
          .add("hillshade-illumination-anchor", "map")
          .add("hillshade-illumination-direction", 90)
          .add("hillshade-shadow-color", "#fffff0")
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder()
          .add("visibility", "visible")
          .build();

        Layers fullHillshadeLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Hillshade")
          .add("type", Layers.Type.HILLSHADE.toString().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", allLayoutProperties)
          .build());
        assertEquals(Layers.Type.HILLSHADE, fullHillshadeLayer.getType());
        assertEquals("", fullHillshadeLayer.toString());

        Layers fullHillshadeInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Hillshade")
          .add("type", Layers.Type.HILLSHADE.toString().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .build());
        assertEquals(Layers.Type.HILLSHADE, fullHillshadeInvisibleLayer.getType());
        assertEquals("", fullHillshadeInvisibleLayer.toString());
    }

    @Test
    void testSky() {
        // Test a layer without a source (should fail)
        assertThrows(NullPointerException.class, () -> new Layers(Json.createObjectBuilder()
          .add("type", Layers.Type.SKY.name())
          .add("id", "Empty Sky").build()));

        JsonObject allPaintProperties = Json.createObjectBuilder()
          .add("sky-atmosphere-color", "red")
          .add("sky-atmosphere-halo-color", "yellow")
          // 360180 is apparently included in this? Or it might be a formatting issue in the docs.
          .add("sky-atmosphere-sun", Json.createArrayBuilder().add(0, 360180))
          .add("sky-atmosphere-sun-intensity", 99)
          .add("sky-gradient", "#fff000")
          .add("sky-gradient-center", Json.createArrayBuilder().add(0).add(360180)) // see note on 360180 above
          .add("sky-gradient-radius", 1)
          .add("sky-opacity", 0.5)
          .add("sky-type", "gradient")
          .build();
        JsonObject allLayoutProperties = Json.createObjectBuilder().add("visibility", "visible").build();

        Layers fullSkyLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Sky")
          .add("type", Layers.Type.SKY.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", allLayoutProperties)
          .build());
        assertEquals(Layers.Type.SKY, fullSkyLayer.getType());
        assertEquals("", fullSkyLayer.toString());

        Layers fullSkyInvisibleLayer = new Layers(Json.createObjectBuilder()
          .add("id", "Sky")
          .add("type", Layers.Type.SKY.name().toLowerCase(Locale.ROOT))
          .add("source", "Random source")
          .add("paint", allPaintProperties)
          .add("layout", Json.createObjectBuilder(allLayoutProperties).add("visibility", "none"))
          .build());
        assertEquals(Layers.Type.SKY, fullSkyInvisibleLayer.getType());
        assertEquals("", fullSkyInvisibleLayer.toString());
    }

    @Test
    void testZoomLevels() {
        JsonObject baseInformation = Json.createObjectBuilder()
          .add("id", "dots")
          .add("type", "CiRcLe")
          .add("source", "osm-source")
          .add("source-layer", "osm-images")
          .add("paint", Json.createObjectBuilder()
            .add("circle-color", "#fff000")
            .add("circle-radius", 6)
          ).build();
        Layers noZoomLayer = new Layers(baseInformation);
        String baseString = "node{0}::dots'{symbol-shape:circle;symbol-fill-color:#fff000;symbol-fill-opacity:1;"
          + "symbol-size:12.0;symbol-stroke-color:#000000;symbol-stroke-opacity:1;symbol-stroke-width:0;}'";
        assertEquals("osm-images", noZoomLayer.getSourceLayer());
        assertEquals(MessageFormat.format(baseString, ""), noZoomLayer.toString());

        Layers minZoomLayer = new Layers(Json.createObjectBuilder(baseInformation)
          .add("minzoom", 0)
          .build());
        assertEquals(MessageFormat.format(baseString, "|z0-"), minZoomLayer.toString());

        Layers maxZoomLayer = new Layers(Json.createObjectBuilder(baseInformation)
          .add("maxzoom", 24)
          .build());
        assertEquals(MessageFormat.format(baseString, "|z-24"), maxZoomLayer.toString());

        Layers minMaxZoomLayer = new Layers(Json.createObjectBuilder(baseInformation)
          .add("minzoom", 1)
          .add("maxzoom", 2)
          .build());
        assertEquals(MessageFormat.format(baseString, "|z1-2"), minMaxZoomLayer.toString());

        Layers sameMinMaxZoomLayer = new Layers(Json.createObjectBuilder(baseInformation)
          .add("minzoom", 2)
          .add("maxzoom", 2)
          .build());
        assertEquals(MessageFormat.format(baseString, "|z2"), sameMinMaxZoomLayer.toString());
    }

    @Test
    void testEquals() {
        EqualsVerifier.forClass(Layers.class).usingGetClass().verify();
    }
}
