// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.spi.lifecycle.Lifecycle;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import mockit.Mock;
import mockit.MockUp;

/**
 * Test class for {@link ValidatorCLI}
 * @author Taylor Smock
 */
@BasicPreferences
class ValidatorCLITest {
    @TempDir
    static File temporaryDirectory;

    TestHandler handler;

    private static void synchronizeThreads() {
        MainApplication.worker.execute(() -> { /* Sync worker thread */ });
        try {
            SwingUtilities.invokeAndWait(() -> { /* Sync EDT thread */ });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        } catch (InvocationTargetException e) {
            fail(e);
        }
    }

    @BeforeEach
    void setup() {
        TestUtils.assumeWorkingJMockit();
        new LifecycleMock();
        this.handler = new TestHandler();
        Logging.getLogger().addHandler(this.handler);
    }

    @AfterEach
    void tearDown() throws InterruptedException, InvocationTargetException {
        synchronizeThreads();
        Logging.getLogger().removeHandler(this.handler);
        this.handler.close();
        this.handler = null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources/styles/standard/elemstyles.mapcss", "resources/styles/standard/potlatch2.mapcss"})
    void testInternalMapcss(final String resourceLocation) {
        new ValidatorCLI().processArguments(new String[]{"--input", resourceLocation});
        assertEquals(2, this.handler.logRecordList.size());
        assertEquals(resourceLocation + " had no errors", this.handler.logRecordList.get(0).getMessage());
        assertTrue(this.handler.logRecordList.get(1).getMessage().contains("Finishing task"));
    }

    static Stream<Arguments> testInternalValidatorMapcss() {
        return Stream.of(Objects.requireNonNull(new File("resources/data/validator").listFiles()))
                .filter(file -> file.getPath().endsWith(".mapcss"))
                .map(file -> {
                    // External validator mapcss files must have validator.mapcss as the extension.
                    final String renamedValidator = file.getName().endsWith(".validator.mapcss") ?
                            file.getName() : file.getName().replace(".mapcss", ".validator.mapcss");
                    try {
                        return Files.copy(file.toPath(), Paths.get(temporaryDirectory.getPath(), renamedValidator)).getFileName().toString();
                    } catch (IOException e) {
                        fail(e);
                    }
                    return null;
                }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    void testInternalValidatorMapcss(final String resourceLocation) {
        final String path = Paths.get(temporaryDirectory.getPath(), resourceLocation).toString();
        new ValidatorCLI().processArguments(new String[]{"--input", path});
        assertEquals(2, this.handler.logRecordList.size(), this.handler.logRecordList.stream().map(LogRecord::getMessage).collect(
                Collectors.joining(",\n")));
        assertEquals(path + " had no errors", this.handler.logRecordList.get(0).getMessage());
        assertTrue(this.handler.logRecordList.get(1).getMessage().contains("Finishing task"));
    }

    @Test
    void testBadDataTicket13165() {
        // Ticket #13165 was a validator non-regression test.
        final String dataPath = TestUtils.getRegressionDataFile(13165, "13165.osm");
        final String outputPath = Paths.get(temporaryDirectory.getPath(), "testBadDataTicket13165.geojson").toString();
        new ValidatorCLI().processArguments(new String[]{"--input", dataPath, "--output", outputPath});
        final File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
        synchronizeThreads();
        final List<JsonObject> errors = readJsonObjects(outputFile.toPath());
        assertEquals(3, errors.stream().map(ValidatorCLITest::getMessage).filter("Overlapping Identical Landuses"::equals).count());
        assertEquals(3, errors.size(), errors.stream().map(ValidatorCLITest::getMessage).collect(Collectors.joining("\n")));
    }

    @Test
    void testBadDataPlusChangeFile() throws IOException {
        final ValidatorCLI validatorCLI = new ValidatorCLI();
        // Write test data out
        final String osmPath = Paths.get(temporaryDirectory.getPath(), "testBadDataPlusChangeFile.osm").toString();
        final String changePath = Paths.get(temporaryDirectory.getPath(), "testBadDataPlusChangeFile.osc").toString();
        final String errorPath = Paths.get(temporaryDirectory.getPath(), "testBadDataPlusChangeFile.geojson").toString();
        final DataSet dataSet = new DataSet();
        final Node node = new Node(LatLon.ZERO);
        node.setOsmId(1, 1);
        dataSet.addPrimitive(node);
        final PrintWriter printWriter = new PrintWriter(Files.newOutputStream(Paths.get(osmPath)), true);
        final OsmWriter writer = OsmWriterFactory.createOsmWriter(printWriter, true, "0.6");
        writer.write(dataSet);
        printWriter.flush();
        final PrintWriter changeWriter = new PrintWriter(Files.newOutputStream(Paths.get(changePath)), true);
        changeWriter.write("<osmChange version=\"0.6\" generator=\"JOSM testBadDataPlusChangeFile\">");
        changeWriter.write("<delete><node id=\"1\"/></delete>");
        changeWriter.write("</osmChange>");
        changeWriter.flush();

        validatorCLI.processArguments(new String[] {"--input", osmPath, "--output", errorPath});
        final List<JsonObject> errors = readJsonObjects(Paths.get(errorPath));
        // There is already a mapped weather buoy at 0,0 (3000), and the node has no tags (201).
        assertEquals(2, errors.size());
        Files.deleteIfExists(Paths.get(errorPath));

        validatorCLI.processArguments(new String[] {"--input", osmPath, "--change-file", changePath, "--output", errorPath});
        errors.clear();
        errors.addAll(readJsonObjects(Paths.get(errorPath)));
        assertEquals(0, errors.size());
        Files.deleteIfExists(Paths.get(errorPath));
    }

    /**
     * Read json objects from a file
     * @param path The file to read
     * @return The json objects
     */
    private static List<JsonObject> readJsonObjects(final Path path) {
        if (Files.exists(path)) {
            final List<String> lines = assertDoesNotThrow(() -> Files.readAllLines(path));
            lines.replaceAll(line -> Utils.strip(line.replace((char) 0x1e, ' ')));
            return lines.stream().map(str -> Json.createReader(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))))
                    .map(JsonReader::readObject).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get the validation message from a json object
     * @param jsonObject The json object to parse
     * @return The validator message
     */
    private static String getMessage(JsonObject jsonObject) {
        return jsonObject.getJsonArray("features").getValuesAs(JsonObject.class)
                .stream().filter(feature -> feature.containsKey("properties")).map(feature -> feature.getJsonObject("properties"))
                .filter(properties -> properties.containsKey("message")).map(properties -> properties.getJsonString("message").getString())
                .collect(Collectors.joining(","));
    }

    /**
     * This exists to avoid exiting the tests.
     */
    private static final class LifecycleMock extends MockUp<Lifecycle> {
        @Mock
        public static boolean exitJosm(boolean exit, int exitCode) {
            // No-op for now
            return true;
        }
    }

    private static final class TestHandler extends Handler {
        final List<LogRecord> logRecordList = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            this.logRecordList.add(record);
        }

        @Override
        public void flush() {
            this.logRecordList.clear();
        }

        @Override
        public void close() throws SecurityException {
            this.flush();
        }
    }
}
