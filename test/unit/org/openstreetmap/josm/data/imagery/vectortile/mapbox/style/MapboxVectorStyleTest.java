// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test class for {@link MapboxVectorStyle}
 * @author Taylor Smock
 */
public class MapboxVectorStyleTest {
    /** Used to store sprite files (specifically, sprite{,@2x}.{png,json}) */
    @TempDir
    File spritesDirectory;

    // Needed for osm primitives (we really just need to initialize the config)
    // OSM primitives are called when we load style sources
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules();

    /** The base information */
    private static final String BASE_STYLE = "'{'\"version\":8,\"name\":\"test_style\",\"owner\":\"josm test\",\"id\":\"{0}\",{1}'}'";
    /** Source 1 */
    private static final String SOURCE1 = "\"source1\":{\"type\":\"vector\",\"tiles\":[\"https://example.org/{z}/{x}/{y}.mvt\"]}";
    /** Layer 1 */
    private static final String LAYER1 = "{\"id\":\"layer1\",\"type\":\"circle\",\"source\":\"source1\",\"source-layer\":\"nodes\"}";
    /** Source 2 */
    private static final String SOURCE2 = "\"source2\":{\"type\":\"vector\",\"tiles\":[\"https://example.org/{z}2/{x}/{y}.mvt\"]}";
    /** Layer 2 */
    private static final String LAYER2 = "{\"id\":\"layer2\",\"type\":\"circle\",\"source\":\"source2\",\"source-layer\":\"nodes\"}";

    /**
     * Check that the version matches the supported style version(s). Currently, only version 8 exists and is (partially)
     * supported.
     */
    @Test
    void testVersionChecks() {
        assertThrows(NullPointerException.class, () -> new MapboxVectorStyle(JsonValue.EMPTY_JSON_OBJECT));
        IllegalArgumentException badVersion = assertThrows(IllegalArgumentException.class,
          () -> new MapboxVectorStyle(Json.createObjectBuilder().add("version", 7).build()));
        assertEquals("Vector Tile Style Version not understood: version 7 (json: {\"version\":7})", badVersion.getMessage());
        badVersion = assertThrows(IllegalArgumentException.class,
          () -> new MapboxVectorStyle(Json.createObjectBuilder().add("version", 9).build()));
        assertEquals("Vector Tile Style Version not understood: version 9 (json: {\"version\":9})", badVersion.getMessage());
        assertDoesNotThrow(() -> new MapboxVectorStyle(Json.createObjectBuilder().add("version", 8).build()));
    }

    @Test
    void testSources() {
        // Check with an invalid sources list
        assertTrue(new MapboxVectorStyle(getJson(JsonObject.class, "{\"version\":8,\"sources\":[\"s1\",\"s2\"]}")).getSources().isEmpty());
        Map<Source, ElemStyles> sources = new MapboxVectorStyle(getJson(JsonObject.class, MessageFormat.format(BASE_STYLE, "test",
          MessageFormat.format("\"sources\":'{'{0},{1},\"source3\":[\"bad source\"]'}',\"layers\":[{2},{3},{4}]",
            SOURCE1, SOURCE2, LAYER1, LAYER2, LAYER2.replace('2', '3'))))).getSources();
        assertEquals(3, sources.size());
        assertTrue(sources.containsKey(null)); // This is due to there being no source3 layer
        sources.remove(null); // Avoid null checks later
        assertTrue(sources.keySet().stream().map(Source::getName).anyMatch("source1"::equals));
        assertTrue(sources.keySet().stream().map(Source::getName).anyMatch("source2"::equals));
        assertTrue(sources.keySet().stream().map(Source::getName).noneMatch("source3"::equals));
    }

    @Test
    void testSavedFiles() {
        assertTrue(new MapboxVectorStyle(getJson(JsonObject.class, "{\"version\":8,\"sources\":[\"s1\",\"s2\"]}")).getSources().isEmpty());
        Map<Source, ElemStyles> sources = new MapboxVectorStyle(getJson(JsonObject.class, MessageFormat.format(BASE_STYLE, "test",
          MessageFormat.format("\"sources\":'{'{0},{1}'}',\"layers\":[{2},{3}]", SOURCE1, SOURCE2, LAYER1, LAYER2)))).getSources();
        assertEquals(2, sources.size());
        // For various reasons, the map _must_ be reliably ordered in the order of encounter
        Source source1 = sources.keySet().iterator().next();
        Source source2 = sources.keySet().stream().skip(1).findFirst().orElseGet(() -> fail("No second source"));
        assertEquals("source1", source1.getName());
        assertEquals("source2", source2.getName());

        // Check that the files have been saved. Ideally, we would check that they haven't been
        // saved earlier, since this is in a different thread. Unfortunately, that is a _race condition_.
        MapCSSStyleSource styleSource1 = (MapCSSStyleSource) sources.get(source1).getStyleSources().get(0);
        MapCSSStyleSource styleSource2 = (MapCSSStyleSource) sources.get(source2).getStyleSources().get(0);

        AtomicBoolean saveFinished = new AtomicBoolean();
        MainApplication.worker.execute(() -> saveFinished.set(true));
        Awaitility.await().atMost(Durations.ONE_SECOND).until(saveFinished::get);

        assertTrue(styleSource1.url.endsWith("source1.mapcss"));
        assertTrue(styleSource2.url.endsWith("source2.mapcss"));

        MapCSSStyleSource mapCSSStyleSource1 = new MapCSSStyleSource(styleSource1.url, styleSource1.name, styleSource1.title);
        MapCSSStyleSource mapCSSStyleSource2 = new MapCSSStyleSource(styleSource2.url, styleSource2.name, styleSource2.title);

        assertEquals(styleSource1, mapCSSStyleSource1);
        assertEquals(styleSource2, mapCSSStyleSource2);
    }

    @Test
    void testSprites() throws IOException {
        generateSprites(false);
        // Ensure that we fall back to 1x sprites
        assertTrue(new File(this.spritesDirectory, "sprite.png").exists());
        assertFalse(new File(this.spritesDirectory, "sprite@2x.png").exists());
        assertTrue(new File(this.spritesDirectory, "sprite.json").exists());
        assertFalse(new File(this.spritesDirectory, "sprite@2x.json").exists());

        checkImages(false);

        generateSprites(true);
        checkImages(true);
    }

    private void checkImages(boolean hiDpi) {
        // Ensure that we don't have images saved in the ImageProvider cache
        ImageProvider.clearCache();
        int hiDpiScalar = hiDpi ? 2 : 1;
        String spritePath = this.spritesDirectory.toURI() + "sprite";
        MapboxVectorStyle style = new MapboxVectorStyle(getJson(JsonObject.class,
          MessageFormat.format(BASE_STYLE, "sprite_test", "\"sprite\":\"" + spritePath + "\"")));
        assertEquals(spritePath, style.getSpriteUrl());

        AtomicBoolean saveFinished = new AtomicBoolean();
        MainApplication.worker.execute(() -> saveFinished.set(true));
        Awaitility.await().atMost(Durations.ONE_SECOND).until(saveFinished::get);

        int scalar = 28; // 255 / 9 (could be 4, but this was a nicer number)
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                // Expected color
                Color color = new Color(scalar * x, scalar * y, scalar * x * y);
                int finalX = x;
                int finalY = y;
                BufferedImage image = (BufferedImage) assertDoesNotThrow(
                  () -> ImageProvider.get(new File("test_style", MessageFormat.format("({0},{1})", finalX, finalY)).getPath()))
                  .getImage();
                assertEquals(3 * hiDpiScalar, image.getWidth(null));
                assertEquals(3 * hiDpiScalar, image.getHeight(null));
                for (int x2 = 0; x2 < image.getWidth(null); x2++) {
                    for (int y2 = 0; y2 < image.getHeight(null); y2++) {
                        assertEquals(color.getRGB(), image.getRGB(x2, y2));
                    }
                }
            }
        }
    }

    private void generateSprites(boolean hiDpi) throws IOException {
        // Create a 3x3 grid of 3x3 or 6x6 pixel squares (depends upon the dpi setting)
        int hiDpiScale = hiDpi ? 2 : 1;
        BufferedImage nineByNine = new BufferedImage(hiDpiScale * 9, hiDpiScale * 9, BufferedImage.TYPE_4BYTE_ABGR);
        int scalar = 28; // 255 / 9 (could be 4, but this was a nicer number)
        Graphics2D g = nineByNine.createGraphics();
        JsonObjectBuilder json = Json.createObjectBuilder();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Color color = new Color(scalar * x, scalar * y, scalar * x * y);
                g.setColor(color);
                g.drawRect(3 * hiDpiScale * x, 3 * hiDpiScale * y, 3 * hiDpiScale, 3 * hiDpiScale);
                g.fillRect(3 * hiDpiScale * x, 3 * hiDpiScale * y, 3 * hiDpiScale, 3 * hiDpiScale);

                JsonObjectBuilder sprite = Json.createObjectBuilder();
                sprite.add("height", hiDpiScale * 3);
                sprite.add("pixelRatio", hiDpiScale);
                sprite.add("width", hiDpiScale * 3);
                sprite.add("x", 3 * hiDpiScale * x);
                sprite.add("y", 3 * hiDpiScale * y);

                json.add(MessageFormat.format("({0},{1})", x, y), sprite);
            }
        }
        String imageName = hiDpi ? "sprite@2x.png" : "sprite.png";
        ImageIO.write(nineByNine, "png", new File(this.spritesDirectory, imageName));
        String jsonName = hiDpi ? "sprite@2x.json" : "sprite.json";
        File jsonFile = new File(this.spritesDirectory, jsonName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile)) {
            fileOutputStream.write(json.build().toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static <T extends JsonStructure> T getJson(Class<T> clazz, String json) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            JsonStructure structure = reader.read();
            if (clazz.isAssignableFrom(structure.getClass())) {
                return clazz.cast(structure);
            }
        }
        fail("Could not cast to expected class");
        throw new IllegalArgumentException();
    }

    @Test
    void testMapillaryStyle() {
        final String file = "file:/" + TestUtils.getTestDataRoot() + "/mapillary.json";
        final MapboxVectorStyle style = MapboxVectorStyle.getMapboxVectorStyle(file);
        assertNotNull(style);
        // There are three "sources" in the mapillary.json file
        assertEquals(3, style.getSources().size());
        final ElemStyles mapillarySource = style.getSources().entrySet().stream()
          .filter(source -> "mapillary-source".equals(source.getKey().getName())).map(
            Map.Entry::getValue).findAny().orElse(null);
        assertNotNull(mapillarySource);
        mapillarySource.getStyleSources().forEach(StyleSource::loadStyleSource);
        assertEquals(1, mapillarySource.getStyleSources().size());
        final MapCSSStyleSource mapillaryCssSource = (MapCSSStyleSource) mapillarySource.getStyleSources().get(0);
        assertTrue(mapillaryCssSource.getErrors().isEmpty());
        final MapCSSRule mapillaryOverview = getRule(mapillaryCssSource, "node", "mapillary-overview");
        assertNotNull(mapillaryOverview);
        assertInInstructions(mapillaryOverview.declaration.instructions, "symbol-shape", new Keyword("circle"));
        assertInInstructions(mapillaryOverview.declaration.instructions, "symbol-fill-color", ColorHelper.html2color("#05CB63"));
        assertInInstructions(mapillaryOverview.declaration.instructions, "symbol-fill-opacity", 0.6f);
        // Docs indicate that symbol-size is total width, while we are translating from a radius. So 2 * 4 = 8.
        assertInInstructions(mapillaryOverview.declaration.instructions, "symbol-size", 8.0f);
    }

    @Test
    void testEqualsContract() {
        // We need to "load" the style sources to avoid the verifier from thinking they are equal
        StyleSource canvas = new MapCSSStyleSource("meta{title:\"canvas\";}canvas{default-points:false;}");
        StyleSource node = new MapCSSStyleSource("meta{title:\"node\";}node{text:ref;}");
        node.loadStyleSource();
        canvas.loadStyleSource();
        EqualsVerifier.forClass(MapboxVectorStyle.class)
          .withPrefabValues(ImageProvider.class, new ImageProvider("cancel"), new ImageProvider("ok"))
          .withPrefabValues(StyleSource.class, canvas, node)
          .usingGetClass().verify();
    }

    /**
     * Check that an instruction is in a collection of instructions, and return it
     * @param instructions The instructions to search
     * @param key The key to look for
     * @param value The expected value for the key
     */
    private void assertInInstructions(Collection<Instruction> instructions, String key, Object value) {
        // In JOSM, all Instruction objects are AssignmentInstruction objects
        Collection<Instruction.AssignmentInstruction> instructionKeys = instructions.stream()
          .filter(Instruction.AssignmentInstruction.class::isInstance)
          .map(Instruction.AssignmentInstruction.class::cast).filter(instruction -> Objects.equals(key, instruction.key))
          .collect(Collectors.toList());
        Optional<Instruction.AssignmentInstruction> instructionOptional = instructionKeys.stream()
          .filter(instruction -> Objects.equals(value, instruction.val)).findAny();
        assertTrue(instructionOptional.isPresent(), MessageFormat
          .format("Expected {0}, but got {1}", value, instructionOptional.orElse(instructionKeys.stream().findAny()
            .orElseThrow(() -> new AssertionError("No instruction with "+key+" found"))).val));
    }

    private static MapCSSRule getRule(MapCSSStyleSource source, String base, String subpart) {
        // We need to do a new arraylist just to avoid the occasional ConcurrentModificationException
        return new ArrayList<>(source.rules).stream().filter(rule -> rule.selectors.stream()
          .anyMatch(selector -> base.equals(selector.getBase()) && subpart.equals(selector.getSubpart().getId(null))))
          .findAny().orElse(null);
    }
}
