// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.tools.MultiMap;

/**
 *
 * Unit tests for class {@link ImageryInfo}.
 *
 */
class ImageryInfoTest {
    /**
     * Test if extended URL is returned properly
     */
    @Test
    void testGetExtendedUrl() {
        ImageryInfo testImageryTMS = new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
        testImageryTMS.setDefaultMinZoom(16);
        testImageryTMS.setDefaultMaxZoom(23);
        assertEquals("tms[16,23]:http://localhost", testImageryTMS.getExtendedUrl());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13264">Bug #13264</a>.
     */
    @Test
    void testConstruct13264() {
        final ImageryInfo info = new ImageryInfo("test imagery", "tms[16-23]:http://localhost");
        assertEquals(ImageryInfo.ImageryType.TMS, info.getImageryType());
        assertEquals(16, info.getMinZoom());
        assertEquals(23, info.getMaxZoom());
        assertEquals("http://localhost", info.getUrl());
    }

    /**
     * Tests the {@linkplain StructUtils#serializeStruct serialization} of {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    void testSerializeStruct() {
        final ImageryInfo.ImageryPreferenceEntry info = new ImageryInfo.ImageryPreferenceEntry();
        info.noTileHeaders = new MultiMap<>();
        info.noTileHeaders.put("ETag", "foo");
        info.noTileHeaders.put("ETag", "bar");
        final Map<String, String> map = StructUtils.serializeStruct(info, ImageryInfo.ImageryPreferenceEntry.class);
        assertEquals("{noTileHeaders={\"ETag\":[\"foo\",\"bar\"]}}", map.toString());
    }

    /**
     * Tests the {@linkplain StructUtils#deserializeStruct(Map, Class)} deserialization} of {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    void testDeserializeStruct() {
        final ImageryInfo.ImageryPreferenceEntry info = StructUtils.deserializeStruct(
                Collections.singletonMap("noTileHeaders", "{\"ETag\":[\"foo\",\"bar\"]}"), ImageryInfo.ImageryPreferenceEntry.class);
        MultiMap<String, String> expect = new MultiMap<>();
        expect.put("ETag", "foo");
        expect.put("ETag", "bar");
        assertEquals(info.noTileHeaders, expect);
        final Set<String> eTag = info.noTileHeaders.get("ETag");
        assertEquals(eTag, new HashSet<>(Arrays.asList("foo", "bar")));
    }

    /**
     * Tests the {@linkplain StructUtils#deserializeStruct(Map, Class)} deserialization} of legacy {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    void testDeserializeStructTicket12474() {
        final ImageryInfo.ImageryPreferenceEntry info = StructUtils.deserializeStruct(
                Collections.singletonMap("noTileHeaders", "{\"ETag\":\"foo-and-bar\"}"), ImageryInfo.ImageryPreferenceEntry.class);
        final Set<String> eTag = info.noTileHeaders.get("ETag");
        assertEquals(eTag, Collections.singleton("foo-and-bar"));
    }
}
