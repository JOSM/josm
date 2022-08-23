// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmReader.Options;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link OsmReader} class.
 */
@BasicPreferences
class OsmReaderTest {
    private static Stream<Arguments> options() {
        return Stream.of(Arguments.of((Object) null),
                Arguments.of((Object) new Options[0]),
                Arguments.of((Object) new Options[] {Options.CONVERT_UNKNOWN_TO_TAGS}),
                Arguments.of((Object) new Options[] {Options.SAVE_ORIGINAL_ID}),
                Arguments.of((Object) new Options[] {Options.CONVERT_UNKNOWN_TO_TAGS, Options.SAVE_ORIGINAL_ID}));
    }

    private static final class PostProcessorStub implements OsmServerReadPostprocessor {
        boolean called;

        @Override
        public void postprocessDataSet(DataSet ds, ProgressMonitor progress) {
            called = true;
        }
    }

    /**
     * Unit test of {@link OsmReader#registerPostprocessor} / {@link OsmReader#deregisterPostprocessor}.
     * @throws Exception if any error occurs
     */
    @Test
    void testPostProcessors() throws Exception {
        PostProcessorStub registered = new PostProcessorStub();
        PostProcessorStub unregistered = new PostProcessorStub();

        OsmReader.registerPostprocessor(registered);

        OsmReader.registerPostprocessor(unregistered);
        OsmReader.deregisterPostprocessor(unregistered);

        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "empty.osm"))) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            assertTrue(registered.called);
            assertFalse(unregistered.called);
        } finally {
            OsmReader.deregisterPostprocessor(registered);
        }
    }

    private static void testUnknown(String osm, Options[] options) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            assertTrue(OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE, options).allPrimitives().isEmpty());
        }
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - root case.
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testUnknownRoot(Options... options) throws Exception {
        testUnknown("<nonosm/>", options);
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - meta case from Overpass API.
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testUnknownMeta(Options... options) throws Exception {
        testUnknown("<osm version='0.6'><meta osm_base='2017-03-29T19:04:03Z'/></osm>", options);
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - note case from Overpass API.
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testUnknownNote(Options... options) throws Exception {
        testUnknown("<osm version='0.6'><note>The data included in this document is from www.openstreetmap.org.</note></osm>", options);
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - other cases.
     * @param options The options to test
     */
    @ParameterizedTest
    @MethodSource("options")
    void testUnknownTag(Options... options) {
        assertAll(() -> testUnknown("<osm version='0.6'><foo>bar</foo></osm>", options),
                () -> testUnknown("<osm version='0.6'><foo><bar/></foo></osm>", options));
    }

    /**
     * Test valid data.
     * @param osm OSM data without XML prefix
     * @param options The options to test
     * @return parsed data set
     * @throws Exception if any error occurs
     */
    private static DataSet testValidData(String osm, Options[] options) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            return assertDoesNotThrow(() -> OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE, options));
        }
    }

    /**
     * Test invalid data.
     * @param osm OSM data without XML prefix
     * @return The exception
     */
    private static IllegalDataException testInvalidData(String osm) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            return assertThrows(IllegalDataException.class, () -> OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE));
        }
    }

    /**
     * Test invalid UID.
     * @throws Exception if any error occurs
     */
    @Test
    void testInvalidUid() throws Exception {
        assertEquals("Illegal value for attribute 'uid'. Got 'nan'. (at line 1, column 82). 82 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' uid='nan'/></osm>").getMessage());
    }

    /**
     * Test missing ID.
     * @throws Exception if any error occurs
     */
    @Test
    void testMissingId() throws Exception {
        assertEquals("Missing required attribute 'id'. (at line 1, column 65). 64 bytes have been read",
                testInvalidData("<osm version='0.6'><node/></osm>").getMessage());
    }

    /**
     * Test missing ref.
     * @throws Exception if any error occurs
     */
    @Test
    void testMissingRef() throws Exception {
        assertEquals("Missing mandatory attribute 'ref' on <nd> of way 1. (at line 1, column 87). 88 bytes have been read",
                testInvalidData("<osm version='0.6'><way id='1' version='1'><nd/></way></osm>").getMessage());
        assertEquals("Missing attribute 'ref' on member in relation 1. (at line 1, column 96). 101 bytes have been read",
                testInvalidData("<osm version='0.6'><relation id='1' version='1'><member/></relation></osm>").getMessage());
    }

    /**
     * Test illegal ref.
     * @throws Exception if any error occurs
     */
    @Test
    void testIllegalRef() throws Exception {
        assertEquals("Illegal value of attribute 'ref' of element <nd>. Got 0. (at line 1, column 95). 96 bytes have been read",
                testInvalidData("<osm version='0.6'><way id='1' version='1'><nd ref='0'/></way></osm>").getMessage());
        assertEquals("Illegal long value for attribute 'ref'. Got 'nan'. (at line 1, column 97). 98 bytes have been read",
                testInvalidData("<osm version='0.6'><way id='1' version='1'><nd ref='nan'/></way></osm>").getMessage());

        assertEquals("Incomplete <member> specification with ref=0 (at line 1, column 116). 121 bytes have been read",
                testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='node' ref='0'/></relation></osm>").getMessage());
        assertEquals("Illegal value for attribute 'ref' on member in relation 1. Got nan (at line 1, column 118). 123 bytes have been read",
                testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='node' ref='nan'/></relation></osm>")
                        .getMessage());
    }

    /**
     * Test missing member type.
     * @throws Exception if any error occurs
     */
    @Test
    void testMissingType() throws Exception {
        assertEquals("Missing attribute 'type' on member 1 in relation 1. (at line 1, column 104). 109 bytes have been read",
                testInvalidData("<osm version='0.6'><relation id='1' version='1'><member ref='1'/></relation></osm>").getMessage());
    }

    /**
     * Test illegal member type.
     * @throws Exception if any error occurs
     */
    @Test
    void testIllegalType() throws Exception {
        assertEquals("Illegal value for attribute 'type' on member 1 in relation 1. Got foo. (at line 1, column 115). 120 bytes have been read",
                testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='foo' ref='1'/></relation></osm>").getMessage());
    }

    /**
     * Test missing key/value.
     * @throws Exception if any error occurs
     */
    @Test
    void testMissingKeyValue() throws Exception {
        assertEquals("Missing key or value attribute in tag. (at line 1, column 89). 89 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='1'><tag/></node></osm>").getMessage());
        assertEquals("Missing key or value attribute in tag. (at line 1, column 97). 97 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='1'><tag k='foo'/></node></osm>").getMessage());
        assertEquals("Missing key or value attribute in tag. (at line 1, column 97). 97 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='1'><tag v='bar'/></node></osm>").getMessage());
    }

    /**
     * Test missing version.
     * @throws Exception if any error occurs
     */
    @Test
    void testMissingVersion() throws Exception {
        assertEquals("Missing mandatory attribute 'version'. (at line 1, column 45). 44 bytes have been read",
                testInvalidData("<osm/>").getMessage());
        assertEquals("Missing attribute 'version' on OSM primitive with ID 1. (at line 1, column 72). 72 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1'/></osm>").getMessage());
    }

    /**
     * Test unsupported version.
     * @throws Exception if any error occurs
     */
    @Test
    void testUnsupportedVersion() throws Exception {
        assertEquals("Unsupported version: 0.1 (at line 1, column 59). 58 bytes have been read",
                testInvalidData("<osm version='0.1'/>").getMessage());
    }

    /**
     * Test illegal version.
     * @throws Exception if any error occurs
     */
    @Test
    void testIllegalVersion() throws Exception {
        assertEquals("Illegal value for attribute 'version' on OSM primitive with ID 1. " +
                        "Got nan. (at line 1, column 86). 86 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='nan'/></osm>").getMessage());
    }

    /**
     * Test illegal changeset.
     * @throws Exception if any error occurs
     */
    @Test
    void testIllegalChangeset() throws Exception {
        assertEquals("Illegal value for attribute 'changeset'. Got nan. (at line 1, column 100). 100 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='1' changeset='nan'/></osm>").getMessage());
        assertEquals("Illegal value for attribute 'changeset'. Got -1. (at line 1, column 99). 99 bytes have been read",
                testInvalidData("<osm version='0.6'><node id='1' version='1' changeset='-1'/></osm>").getMessage());
    }

    /**
     * Test GDPR-compliant changeset.
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testGdprChangeset(Options... options) throws Exception {
        String gdprChangeset = "<osm version='0.6'><node id='1' version='1' changeset='0'/></osm>";
        testValidData(gdprChangeset, options);
    }

    /**
     * Test invalid bounds.
     * @throws Exception if any error occurs
     */
    @Test
    void testInvalidBounds() throws Exception {
        assertEquals("Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='null',minlat='null',maxlon='null',maxlat='null', origin='null'. (at line 1, column 67). 72 bytes have been read",
                testInvalidData("<osm version='0.6'><bounds/></osm>").getMessage());
        assertEquals("Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='null',maxlon='null',maxlat='null', origin='null'. (at line 1, column 78). 83 bytes have been read",
                testInvalidData("<osm version='0.6'><bounds minlon='0'/></osm>").getMessage());
        assertEquals("Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='0',maxlon='null',maxlat='null', origin='null'. (at line 1, column 89). 94 bytes have been read",
                testInvalidData("<osm version='0.6'><bounds minlon='0' minlat='0'/></osm>").getMessage());
        assertEquals("Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='0',maxlon='1',maxlat='null', origin='null'. (at line 1, column 100). 105 bytes have been read",
                testInvalidData("<osm version='0.6'><bounds minlon='0' minlat='0' maxlon='1'/></osm>").getMessage());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14199">Bug #14199</a>.
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket14199() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14199, "emptytag.osm")) {
            Way w = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE).getWays().iterator().next();
            assertEquals(1, w.getKeys().size());
            assertNull(w.get("  "));
            assertTrue(w.isModified());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14754">Bug #14754</a>.
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket14754() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14754, "malformed_for_14754.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            String prefix = "Illegal value for attributes 'lat', 'lon' on node with ID 1425146006. Got '550.3311950157', '10.49428298298'.";
            assertThat(e.getMessage(), anyOf(
                    is(prefix + " (at line 5, column 179). 578 bytes have been read"),
                    is(prefix + " (at line 5, column 179). 581 bytes have been read") // GitHub Actions
            ));
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14788">Bug #14788</a>.
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket14788() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14788, "remove_sign_test_4.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            String prefix = "Illegal value for attributes 'lat', 'lon' on node with ID 978. Got 'nan', 'nan'.";
            assertThat(e.getMessage(), anyOf(
                    is(prefix + " (at line 4, column 151). 336 bytes have been read"),
                    is(prefix + " (at line 4, column 151). 338 bytes have been read") // GitHub Actions
            ));
        }
    }

    /**
     * Test reading remark from Overpass API.
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testRemark(Options... options) throws Exception {
        String query = "<osm version=\"0.6\" generator=\"Overpass API 0.7.55.4 3079d8ea\">\r\n" +
                "<note>The data included in this document is from www.openstreetmap.org. The data is made available under ODbL.</note>\r\n" +
                "<meta osm_base=\"2018-08-30T12:46:02Z\" areas=\"2018-08-30T12:40:02Z\"/>\r\n" +
                "<remark>runtime error: Query ran out of memory in \"query\" at line 5.</remark>\r\n" +
                "</osm>";
        DataSet ds = testValidData(query, options);
        assertEquals("runtime error: Query ran out of memory in \"query\" at line 5.", ds.getRemark());
    }

    /**
     * Test reading a file with unknown attributes in osm primitives
     * @param options The options to test
     * @throws Exception if any error occurs
     */
    @ParameterizedTest
    @MethodSource("options")
    void testUnknownAttributeTags(Options... options) throws Exception {
        String testData = "<osm version=\"0.6\" generator=\"fake generator\">"
                + "<node id='1' version='1' visible='true' changeset='82' randomkey='randomvalue'></node>" + "</osm>";
        DataSet ds = testValidData(testData, options);
        Node firstNode = ds.getNodes().iterator().next();
        if (options != null && Arrays.asList(options).contains(Options.CONVERT_UNKNOWN_TO_TAGS)) {
            assertEquals("randomvalue", firstNode.get("randomkey"));
        } else {
            assertNull(firstNode.get("randomkey"));
        }
        if (options != null && Arrays.asList(options).contains(Options.SAVE_ORIGINAL_ID)) {
            assertEquals("1", firstNode.get("current_id"));
        } else {
            assertNull(firstNode.get("current_id"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<osm version=\"0.6\"><error>Mismatch in tags key and value size</error></osm>",
            "<osm version=\"0.6\"><node id=\"1\" visible=\"true\" version=\"1\"><error>Mismatch in tags key and value size</error></node></osm>",
            "<osm version=\"0.6\"><way id=\"1\" visible=\"true\" version=\"1\"><error>Mismatch in tags key and value size</error></way></osm>",
            "<osm version=\"0.6\"><way id=\"1\" visible=\"true\" version=\"1\"><error>Mismatch in tags key and value size</error></way></osm>",
            "<osm version=\"0.6\"><relation id=\"1\" visible=\"true\" version=\"1\"><error>Mismatch in tags key and value size</error>" +
                    "</relation></osm>"
    })
    void testErrorMessage(String testData) throws Exception {
        IllegalDataException illegalDataException = testInvalidData(testData);
        assertTrue(illegalDataException.getMessage().contains("Mismatch in tags key and value size"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<osm version=\"0.6\"><error></error></osm>",
            "<osm version=\"0.6\"><node id=\"1\" visible=\"true\" version=\"1\"><error/></node></osm>",
            "<osm version=\"0.6\"><way id=\"1\" visible=\"true\" version=\"1\"><error></error></way></osm>",
            "<osm version=\"0.6\"><way id=\"1\" visible=\"true\" version=\"1\"><error></error></way></osm>",
            "<osm version=\"0.6\"><relation id=\"1\" visible=\"true\" version=\"1\"><error/></relation></osm>"
    })
    void testErrorNoMessage(String testData) throws Exception {
        IllegalDataException illegalDataException = testInvalidData(testData);
        assertTrue(illegalDataException.getMessage().contains("Unknown error element type"));
    }
}
