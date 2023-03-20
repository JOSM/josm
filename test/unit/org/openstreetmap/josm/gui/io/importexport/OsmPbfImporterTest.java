// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.protobuf.ProtobufTest;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link OsmPbfImporter}
 */
@BasicPreferences
class OsmPbfImporterTest {
    /**
     * BlobHeader, type=OSMHeader, datasize=146, blob compressed by zlib, compressed size 132
     */
    private static final byte[] HEADER_DATA = ProtobufTest.toByteArray(new int[]{
            // BlobHeader, type=OSMHeader, datasize=146
            0, 0, 0, 14, 10, 9, 79, 83, 77, 72, 101, 97, 100, 101, 114, 24, -110, 1,
            // size=132, type=zlib
            16, -124, 1, 26, -116, 1, 120, -100, -29, -110, -30, 56, -66, 125, -49, 3, 70, -127, -9, 55, -65, -35, 100, -108, 120, 113, -17, -50,
            -115, 70, 102, -123, 31, -117, -97, 93, 107, 100, 86, -30, -13, 47, -50, 13, 78, -50, 72, -51, 77, -44, 13, 51, -48, 51, 83, -30,
            114, 73, -51, 43, 78, -11, -53, 79, 73, 45, -42, 18, 12, -50, 47, 42, -47, 11, -87, 44, 72, -115, 47, -55, 72, -51, -117, -9, 116,
            105, 98, -28, -49, 47, -50, 77, -50, -49, 43, 75, 45, 42, 81, 48, -48, 51, 119, -17, 98, 84, -55, 40, 41, 41, -80, -46, -41, 47, 47,
            47, -41, -53, 47, 0, -22, 46, 41, 74, 77, 45, -55, 77, 44, -48, -53, 47, 74, -41, 79, 44, -56, -44, 7, -102, 11, 0, -14, -78, 50, 42
    });

    private static OsmPbfImporter importer;

    @BeforeAll
    static void setup() {
        importer = new OsmPbfImporter();
    }

    @Test
    void testGoodHeader() {
        final ByteArrayInputStream goodHeader = new ByteArrayInputStream(HEADER_DATA);
        // Test good data header
        final DataSet ds = assertDoesNotThrow(() -> importer.parseDataSet(goodHeader, NullProgressMonitor.INSTANCE));
        assertTrue(ds.isEmpty());
    }

    @Test
    void testTooBigHeader() {
        // Test a bad data header
        byte[] badData = HEADER_DATA.clone();
        badData[1] = -128;
        badData[2] = -128;
        final ByteArrayInputStream badHeader = new ByteArrayInputStream(badData);
        IllegalDataException ide = assertThrows(IllegalDataException.class,
                () -> importer.parseDataSet(badHeader, NullProgressMonitor.INSTANCE));
        assertTrue(ide.getMessage().contains("OSM PBF BlobHeader is too large. PBF is probably corrupted"));
    }

    @Test
    void testMissingRequiredFeature() {
        // Test a bad data blob
        byte[] badData = HEADER_DATA.clone();
        // OsmSchema-V0.6 -> OtmSchema-V0.6
        badData[60] = -55;
        // Correct zip information
        badData[160] = -13;
        badData[161] = 23;
        badData[163] = 43;
        final ByteArrayInputStream badBlob = new ByteArrayInputStream(badData);
        final IllegalDataException ide = assertThrows(IllegalDataException.class,
                () -> importer.parseDataSet(badBlob, NullProgressMonitor.INSTANCE));
        assertEquals("PBF Parser: Unknown required feature OtmSchema-V0.6", ide.getMessage());
    }

    @Test
    void testMultipleHeaders() {
        byte[] badData = Arrays.copyOf(HEADER_DATA, HEADER_DATA.length * 2);
        System.arraycopy(HEADER_DATA, 0, badData, HEADER_DATA.length, HEADER_DATA.length);
        final ByteArrayInputStream badBlob = new ByteArrayInputStream(badData);
        final IllegalDataException ide = assertThrows(IllegalDataException.class,
                () -> importer.parseDataSet(badBlob, NullProgressMonitor.INSTANCE));
        assertEquals("Too many header blocks in protobuf", ide.getMessage());
    }

    @Test
    void testSimpleCase() throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "pbf", "osm", "simple.osm.pbf"))) {
            DataSet ds = assertDoesNotThrow(() -> importer.parseDataSet(is, NullProgressMonitor.INSTANCE));
            assertEquals(1, ds.getRelations().size());
            assertAll(() -> assertEquals(4, ds.getNodes().size()),
                    () -> assertEquals(1, ds.getWays().size()),
                    () -> assertEquals(1, ds.getRelations().size()),
                    () -> assertTrue(ds.getNodes().stream()
                            .filter(node -> node.getCoor().equalsEpsilon((ILatLon) new LatLon(39.1998868, -108.6907137)))
                            .allMatch(node -> "house".equals(node.get("building")))),
                    () -> assertTrue(ds.getNodes().stream()
                            .filter(node -> !node.getCoor().equalsEpsilon((ILatLon) new LatLon(39.1998868, -108.6907137)))
                            .noneMatch(AbstractPrimitive::hasKeys))
            );
            Way way = ds.getWays().iterator().next();
            Relation rel = ds.getRelations().iterator().next();
            assertAll(() -> assertEquals(5, way.getNodes().size()),
                    () -> assertTrue(way.isClosed()),
                    () -> assertTrue(way.firstNode().equalsEpsilon(new LatLon(39.1998868, -108.6907137))),
                    () -> assertEquals("house", way.get("building")),
                    () -> assertEquals("house", rel.get("building")),
                    () -> assertEquals(1, rel.getMembersCount()),
                    () -> assertEquals("outer", rel.getRole(0)),
                    () -> assertSame(way, rel.getMember(0).getMember())
            );
        }
    }
}
