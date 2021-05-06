// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.protobuf.ProtobufParser;
import org.openstreetmap.josm.data.protobuf.ProtobufRecord;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link Layer}
 */
public class LayerTest {
    /**
     * This looks something like this (if it were json). Note that some keys could be repeated,
     * and so could be better represented as an array. Specifically, "features", "key", and "value".
     * "layer": {
     *     "name": "t",
     *     "version": 2,
     *     "features": {
     *         "type": "POINT",
     *         "tags": [0, 0],
     *         "geometry": [9, 50, 34]
     *     },
     *     "key": "a",
     *     "value": true
     * }
     *
     * WARNING: DO NOT MODIFY THIS ARRAY DIRECTLY -- it could contaminate other tests
     */
    private static final byte[] simpleFeatureLayerBytes = new byte[] {
      0x1a, 0x1b, // layer, 27 bytes for the rest
      0x0a, 0x01, 0x74, // name=t
      0x78, 0x02, // version=2
      0x12, 0x0d, // features, 11 bytes
      0x08, 0x01, // id=1
      0x18, 0x01, // type=POINT
      0x12, 0x02, 0x00, 0x00, // tags=[0, 0] (packed). Non-packed would be [0x10, 0x00, 0x10, 0x00]
      0x22, 0x03, 0x09, 0x32, 0x22, // geometry=[9, 50, 34]
      0x1a, 0x01, 0x61, // key=a
      0x22, 0x02, 0x38, 0x01, // value=true (boolean)
    };

    /**
     * Gets a copy of {@link #simpleFeatureLayerBytes} so that a test doesn't accidentally change the bytes
     * @return An array that can be modified.
     */
    static byte[] getSimpleFeatureLayerBytes() {
        return Arrays.copyOf(simpleFeatureLayerBytes, simpleFeatureLayerBytes.length);
    }

    /**
     * Create a layer from bytes
     * @param bytes The bytes that make up the layer
     * @return The generated layer
     * @throws IOException If something happened (should never trigger)
     */
    static Layer getLayer(byte[] bytes) throws IOException {
        List<ProtobufRecord> records = (List<ProtobufRecord>) new ProtobufParser(bytes).allRecords();
        assertEquals(1, records.size());
        return new Layer(new ProtobufParser(records.get(0).getBytes()).allRecords());
    }

    @Test
    void testLayerCreation() throws IOException {
        List<ProtobufRecord> layers = (List<ProtobufRecord>) new ProtobufParser(new FileInputStream(TestUtils.getTestDataRoot()
          + "pbf/mapillary/14/3249/6258.mvt")).allRecords();
        Layer sequenceLayer = new Layer(layers.get(0).getBytes());
        assertEquals("mapillary-sequences", sequenceLayer.getName());
        assertEquals(1, sequenceLayer.getFeatures().size());
        assertEquals(1, sequenceLayer.getGeometry().size());
        assertEquals(4096, sequenceLayer.getExtent());
        assertEquals(1, sequenceLayer.getVersion());

        Layer imageLayer = new Layer(layers.get(1).getBytes());
        assertEquals("mapillary-images", imageLayer.getName());
        assertEquals(116, imageLayer.getFeatures().size());
        assertEquals(116, imageLayer.getGeometry().size());
        assertEquals(4096, imageLayer.getExtent());
        assertEquals(1, imageLayer.getVersion());
    }

    @Test
    void testLayerEqualsHashCode() throws IOException {
        List<ProtobufRecord> layers = (List<ProtobufRecord>) new ProtobufParser(new FileInputStream(TestUtils.getTestDataRoot()
          + "pbf/mapillary/14/3249/6258.mvt")).allRecords();
        EqualsVerifier.forClass(Layer.class).withPrefabValues(byte[].class, layers.get(0).getBytes(), layers.get(1).getBytes())
          .verify();
    }

    @Test
    void testVersionsNumbers() {
        byte[] copyByte = getSimpleFeatureLayerBytes();
        assertEquals(2, assertDoesNotThrow(() -> getLayer(copyByte)).getVersion());
        copyByte[6] = 1;
        assertEquals(1, assertDoesNotThrow(() -> getLayer(copyByte)).getVersion());
        copyByte[6] = 0;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getLayer(copyByte));
        assertEquals("We do not understand version 0 of the vector tile specification", exception.getMessage());
        copyByte[6] = 3;
        exception = assertThrows(IllegalArgumentException.class, () -> getLayer(copyByte));
        assertEquals("We do not understand version 3 of the vector tile specification", exception.getMessage());
        // Remove version number (AKA change it to some unknown field). Default is version=1.
        copyByte[5] = 0x18;
        assertEquals(1, assertDoesNotThrow(() -> getLayer(copyByte)).getVersion());
    }

    @Test
    void testLayerName() throws IOException {
        byte[] copyByte = getSimpleFeatureLayerBytes();
        Layer layer = getLayer(copyByte);
        assertEquals("t", layer.getName());
        copyByte[2] = 0x1a; // name=t -> ?
        Exception noNameException = assertThrows(IllegalArgumentException.class, () -> getLayer(copyByte));
        assertEquals("Vector tile layers must have a layer name", noNameException.getMessage());
    }

    @Test
    void testUnknownField() {
        byte[] copyByte = getSimpleFeatureLayerBytes();
        copyByte[27] = 0x78;
        Exception unknownField = assertThrows(IllegalArgumentException.class, () -> getLayer(copyByte));
        assertEquals("Unknown field in vector tile layer value (15)", unknownField.getMessage());
    }
}
