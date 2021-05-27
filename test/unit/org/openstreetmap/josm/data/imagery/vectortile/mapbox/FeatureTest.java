// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


import static org.openstreetmap.josm.data.imagery.vectortile.mapbox.LayerTest.getSimpleFeatureLayerBytes;
import static org.openstreetmap.josm.data.imagery.vectortile.mapbox.LayerTest.getLayer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.I18n;

/**
 * Test class for {@link Feature}
 */
class FeatureTest {
    /**
     * This can be used to replace bytes 11-14 (inclusive) in {@link LayerTest#simpleFeatureLayerBytes}.
     */
    private final byte[] nonPackedTags = new byte[] {0x10, 0x00, 0x10, 0x00};

    @Test
    void testCreation() {
        testCreation(getSimpleFeatureLayerBytes());
    }

    @Test
    void testCreationUnpacked() {
        byte[] copyBytes = getSimpleFeatureLayerBytes();
        System.arraycopy(nonPackedTags, 0, copyBytes, 13, nonPackedTags.length);
        testCreation(copyBytes);
    }

    @Test
    void testCreationTrueToFalse() {
        byte[] copyBytes = getSimpleFeatureLayerBytes();
        copyBytes[copyBytes.length - 1] = 0x00; // set value=false
        Layer layer = assertDoesNotThrow(() -> getLayer(copyBytes));
        assertSame(Boolean.FALSE, layer.getValue(0));
    }

    @Test
    void testNumberGrouping() {
        // This is the float we are adding
        // 49 74 24 00 == 1_000_000f
        // 3f 80 00 00 == 1f
        byte[] newBytes = new byte[] {0x22, 0x05, 0x15, 0x00, 0x24, 0x74, 0x49};
        byte[] copyBytes = Arrays.copyOf(getSimpleFeatureLayerBytes(), getSimpleFeatureLayerBytes().length + newBytes.length - 4);
        // Change last few bytes
        System.arraycopy(newBytes, 0, copyBytes, 25, newBytes.length);
        // Update the length of the record
        copyBytes[1] = (byte) (copyBytes[1] + newBytes.length - 4);
        final NumberFormat numberFormat = NumberFormat.getNumberInstance();
        final boolean numberFormatGroupingUsed = numberFormat.isGroupingUsed();
        // Sanity check
        Layer layer;
        try {
            numberFormat.setGroupingUsed(true);
            layer = assertDoesNotThrow(() -> getLayer(copyBytes));
            assertTrue(numberFormat.isGroupingUsed());
        } finally {
            numberFormat.setGroupingUsed(numberFormatGroupingUsed);
        }
        assertEquals(1, layer.getFeatures().size());
        assertEquals("t", layer.getName());
        assertEquals(2, layer.getVersion());
        assertEquals("a", layer.getKey(0));
        assertEquals(1_000_000f, ((Number) layer.getValue(0)).floatValue(), 0.00001);
        
        // Feature check
        Feature feature = layer.getFeatures().iterator().next();
        checkDefaultGeometry(feature);
        assertEquals("1000000", feature.getTags().get("a"));
    }

    /**
     * Non-regression test for #20933 (Russian)
     * @see #testNumberGroupingDecimalEn()
     */
    @I18n("ru")
    @Test
    void testNumberGroupingDecimalRu() {
        testNumberGroupingDecimal();
    }

    /**
     * Non-regression test for #20933 (English)
     * @see #testNumberGroupingDecimalRu()
     */
    @I18n("en")
    @Test
    void testNumberGroupingDecimalEn() {
        testNumberGroupingDecimal();
    }

    /**
     * Common parts for non-regression tests for #20933
     * @see #testNumberGroupingDecimalEn()
     * @see #testNumberGroupingDecimalRu()
     */
    private void testNumberGroupingDecimal() {
        byte[] newBytes = new byte[] {0x22, 0x09, 0x19, -45, 0x4D, 0x62, 0x10, 0x58, -71, 0x67, 0x40};
        byte[] copyBytes = Arrays.copyOf(getSimpleFeatureLayerBytes(), getSimpleFeatureLayerBytes().length + newBytes.length - 4);
        // Change last few bytes
        System.arraycopy(newBytes, 0, copyBytes, 25, newBytes.length);
        // Update the length of the record
        copyBytes[1] = (byte) (copyBytes[1] + newBytes.length - 4);
        Layer layer = assertDoesNotThrow(() -> getLayer(copyBytes));
        layer.getKey(0);
        List<Feature> features = new ArrayList<>(layer.getFeatures());
        assertEquals(1, features.size());
        assertEquals("189.792", features.get(0).getTags().get("a"));
    }

    private void testCreation(byte[] bytes) {
        Layer layer = assertDoesNotThrow(() -> getLayer(bytes));
        // Sanity check the layer
        assertEquals(1, layer.getFeatures().size());
        assertEquals("t", layer.getName());
        assertEquals(2, layer.getVersion());
        assertEquals("a", layer.getKey(0));
        assertSame(Boolean.TRUE, layer.getValue(0));

        // OK. Get the feature.
        Feature feature = layer.getFeatures().iterator().next();

        checkDefaultTags(feature);

        // Check id (should be the default of 0)
        assertEquals(1, feature.getId());

        checkDefaultGeometry(feature);
    }

    private void checkDefaultTags(Feature feature) {
        // Check tags
        assertEquals(1, feature.getTags().size());
        assertTrue(feature.getTags().containsKey("a"));
        // We are converting to a tag map (Map<String, String>), so "true"
        assertEquals("true", feature.getTags().get("a"));
    }

    private void checkDefaultGeometry(Feature feature) {
        // Check the geometry
        assertEquals(GeometryTypes.POINT, feature.getGeometryType());
        assertEquals(1, feature.getGeometry().size());
        CommandInteger geometry = feature.getGeometry().get(0);
        assertEquals(Command.MoveTo, geometry.getType());
        assertEquals(2, geometry.getOperations().length);
        assertEquals(25, geometry.getOperations()[0]);
        assertEquals(17, geometry.getOperations()[1]);
        assertNotNull(feature.getGeometryObject());
        assertEquals(feature.getGeometryObject(), feature.getGeometryObject());
    }
}
