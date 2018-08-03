// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmReader} class.
 */
public class OsmReaderTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
    public void testPostProcessors() throws Exception {
        PostProcessorStub registered = new PostProcessorStub();
        PostProcessorStub unregistered = new PostProcessorStub();

        OsmReader.registerPostprocessor(registered);

        OsmReader.registerPostprocessor(unregistered);
        OsmReader.deregisterPostprocessor(unregistered);

        try (InputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "empty.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            assertTrue(registered.called);
            assertFalse(unregistered.called);
        } finally {
            OsmReader.deregisterPostprocessor(registered);
        }
    }

    private static void testUnknown(String osm) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            assertTrue(OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE).allPrimitives().isEmpty());
        }
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - root case.
     * @throws Exception if any error occurs
     */
    @Test
    public void testUnknownRoot() throws Exception {
        testUnknown("<nonosm/>");
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - meta case from Overpass API.
     * @throws Exception if any error occurs
     */
    @Test
    public void testUnknownMeta() throws Exception {
        testUnknown("<osm version='0.6'><meta osm_base='2017-03-29T19:04:03Z'/></osm>");
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - note case from Overpass API.
     * @throws Exception if any error occurs
     */
    @Test
    public void testUnknownNote() throws Exception {
        testUnknown("<osm version='0.6'><note>The data included in this document is from www.openstreetmap.org.</note></osm>");
    }

    /**
     * Unit test of {@link OsmReader#parseUnknown} - other cases.
     * @throws Exception if any error occurs
     */
    @Test
    public void testUnknownTag() throws Exception {
        testUnknown("<osm version='0.6'><foo>bar</foo></osm>");
        testUnknown("<osm version='0.6'><foo><bar/></foo></osm>");
    }

    /**
     * Test valid data.
     * @param osm OSM data without XML prefix
     * @throws Exception if any error occurs
     */
    private static void testValidData(String osm) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
        }
    }

    /**
     * Test invalid data.
     * @param osm OSM data without XML prefix
     * @param expectedError expected error message
     * @throws Exception if any error occurs
     */
    private static void testInvalidData(String osm, String expectedError) throws Exception {
        try (InputStream in = new ByteArrayInputStream(
                ("<?xml version='1.0' encoding='UTF-8'?>" + osm).getBytes(StandardCharsets.UTF_8))) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            assertEquals(expectedError, e.getMessage());
        }
    }

    /**
     * Test invalid UID.
     * @throws Exception if any error occurs
     */
    @Test
    public void testInvalidUid() throws Exception {
        testInvalidData("<osm version='0.6'><node id='1' uid='nan'/></osm>",
                "Illegal value for attribute 'uid'. Got 'nan'. (at line 1, column 82). 82 bytes have been read");
    }

    /**
     * Test missing ID.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMissingId() throws Exception {
        testInvalidData("<osm version='0.6'><node/></osm>",
                "Missing required attribute 'id'. (at line 1, column 65). 64 bytes have been read");
    }

    /**
     * Test missing ref.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMissingRef() throws Exception {
        testInvalidData("<osm version='0.6'><way id='1' version='1'><nd/></way></osm>",
                "Missing mandatory attribute 'ref' on <nd> of way 1. (at line 1, column 87). 88 bytes have been read");
        testInvalidData("<osm version='0.6'><relation id='1' version='1'><member/></relation></osm>",
                "Missing attribute 'ref' on member in relation 1. (at line 1, column 96). 101 bytes have been read");
    }

    /**
     * Test illegal ref.
     * @throws Exception if any error occurs
     */
    @Test
    public void testIllegalRef() throws Exception {
        testInvalidData("<osm version='0.6'><way id='1' version='1'><nd ref='0'/></way></osm>",
                "Illegal value of attribute 'ref' of element <nd>. Got 0. (at line 1, column 95). 96 bytes have been read");
        testInvalidData("<osm version='0.6'><way id='1' version='1'><nd ref='nan'/></way></osm>",
                "Illegal long value for attribute 'ref'. Got 'nan'. (at line 1, column 97). 98 bytes have been read");

        testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='node' ref='0'/></relation></osm>",
                "Incomplete <member> specification with ref=0 (at line 1, column 116). 121 bytes have been read");
        testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='node' ref='nan'/></relation></osm>",
                "Illegal value for attribute 'ref' on member in relation 1. Got nan (at line 1, column 118). 123 bytes have been read");
    }

    /**
     * Test missing member type.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMissingType() throws Exception {
        testInvalidData("<osm version='0.6'><relation id='1' version='1'><member ref='1'/></relation></osm>",
                "Missing attribute 'type' on member 1 in relation 1. (at line 1, column 104). 109 bytes have been read");
    }

    /**
     * Test illegal member type.
     * @throws Exception if any error occurs
     */
    @Test
    public void testIllegalType() throws Exception {
        testInvalidData("<osm version='0.6'><relation id='1' version='1'><member type='foo' ref='1'/></relation></osm>",
                "Illegal value for attribute 'type' on member 1 in relation 1. Got foo. (at line 1, column 115). 120 bytes have been read");
    }

    /**
     * Test missing key/value.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMissingKeyValue() throws Exception {
        testInvalidData("<osm version='0.6'><node id='1' version='1'><tag/></node></osm>",
                "Missing key or value attribute in tag. (at line 1, column 89). 89 bytes have been read");
        testInvalidData("<osm version='0.6'><node id='1' version='1'><tag k='foo'/></node></osm>",
                "Missing key or value attribute in tag. (at line 1, column 97). 97 bytes have been read");
        testInvalidData("<osm version='0.6'><node id='1' version='1'><tag v='bar'/></node></osm>",
                "Missing key or value attribute in tag. (at line 1, column 97). 97 bytes have been read");
    }

    /**
     * Test missing version.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMissingVersion() throws Exception {
        testInvalidData("<osm/>",
                "Missing mandatory attribute 'version'. (at line 1, column 45). 44 bytes have been read");
        testInvalidData("<osm version='0.6'><node id='1'/></osm>",
                "Missing attribute 'version' on OSM primitive with ID 1. (at line 1, column 72). 72 bytes have been read");
    }

    /**
     * Test unsupported version.
     * @throws Exception if any error occurs
     */
    @Test
    public void testUnsupportedVersion() throws Exception {
        testInvalidData("<osm version='0.1'/>",
                "Unsupported version: 0.1 (at line 1, column 59). 58 bytes have been read");
    }

    /**
     * Test illegal version.
     * @throws Exception if any error occurs
     */
    @Test
    public void testIllegalVersion() throws Exception {
        testInvalidData("<osm version='0.6'><node id='1' version='nan'/></osm>",
                "Illegal value for attribute 'version' on OSM primitive with ID 1. Got nan. (at line 1, column 86). 86 bytes have been read");
    }

    /**
     * Test illegal changeset.
     * @throws Exception if any error occurs
     */
    @Test
    public void testIllegalChangeset() throws Exception {
        testInvalidData("<osm version='0.6'><node id='1' version='1' changeset='nan'/></osm>",
                "Illegal value for attribute 'changeset'. Got nan. (at line 1, column 100). 100 bytes have been read");
        testInvalidData("<osm version='0.6'><node id='1' version='1' changeset='-1'/></osm>",
                "Illegal value for attribute 'changeset'. Got -1. (at line 1, column 99). 99 bytes have been read");
    }

    /**
     * Test GDPR-compliant changeset.
     * @throws Exception if any error occurs
     */
    @Test
    public void testGdprChangeset() throws Exception {
        testValidData("<osm version='0.6'><node id='1' version='1' changeset='0'/></osm>");
    }

    /**
     * Test invalid bounds.
     * @throws Exception if any error occurs
     */
    @Test
    public void testInvalidBounds() throws Exception {
        testInvalidData("<osm version='0.6'><bounds/></osm>",
                "Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='null',minlat='null',maxlon='null',maxlat='null', origin='null'. (at line 1, column 67). 72 bytes have been read");
        testInvalidData("<osm version='0.6'><bounds minlon='0'/></osm>",
                "Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='null',maxlon='null',maxlat='null', origin='null'. (at line 1, column 78). 83 bytes have been read");
        testInvalidData("<osm version='0.6'><bounds minlon='0' minlat='0'/></osm>",
                "Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='0',maxlon='null',maxlat='null', origin='null'. (at line 1, column 89). 94 bytes have been read");
        testInvalidData("<osm version='0.6'><bounds minlon='0' minlat='0' maxlon='1'/></osm>",
                "Missing mandatory attributes on element 'bounds'. " +
                "Got minlon='0',minlat='0',maxlon='1',maxlat='null', origin='null'. (at line 1, column 100). 105 bytes have been read");
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14199">Bug #14199</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket14199() throws Exception {
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
    public void testTicket14754() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14754, "malformed_for_14754.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            assertEquals("Illegal value for attributes 'lat', 'lon' on node with ID 1425146006." +
                    " Got '550.3311950157', '10.49428298298'." +
                    " (at line 5, column 179). 578 bytes have been read", e.getMessage());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14788">Bug #14788</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket14788() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14788, "remove_sign_test_4.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            assertEquals("Illegal value for attributes 'lat', 'lon' on node with ID 978." +
                    " Got 'nan', 'nan'." +
                    " (at line 4, column 151). 336 bytes have been read", e.getMessage());
        }
    }
}
