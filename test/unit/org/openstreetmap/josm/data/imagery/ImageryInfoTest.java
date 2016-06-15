// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.tools.MultiMap;

/**
 *
 * Unit tests for class {@link ImageryInfo}.
 *
 */
public class ImageryInfoTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test if extended URL is returned properly
     */
    @Test
    public void testGetExtendedUrl() {
        ImageryInfo testImageryTMS = new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
        testImageryTMS.setDefaultMinZoom(16);
        testImageryTMS.setDefaultMaxZoom(23);
        assertEquals("tms[16,23]:http://localhost", testImageryTMS.getExtendedUrl());
    }

    /**
     * Tests the {@linkplain Preferences#serializeStruct(Object, Class) serialization} of {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    public void testSerializeStruct() {
        final ImageryInfo.ImageryPreferenceEntry info = new ImageryInfo.ImageryPreferenceEntry();
        info.noTileHeaders = new MultiMap<>();
        info.noTileHeaders.put("ETag", "foo");
        info.noTileHeaders.put("ETag", "bar");
        final Map<String, String> map = Preferences.serializeStruct(info, ImageryInfo.ImageryPreferenceEntry.class);
        assertEquals("{noTileHeaders={\"ETag\":[\"foo\",\"bar\"]}}", map.toString());
    }

    /**
     * Tests the {@linkplain Preferences#deserializeStruct(Map, Class)} deserialization} of {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    public void testDeserializeStruct() {
        final ImageryInfo.ImageryPreferenceEntry info = Preferences.deserializeStruct(
                Collections.singletonMap("noTileHeaders", "{\"ETag\":[\"foo\",\"bar\"]}"), ImageryInfo.ImageryPreferenceEntry.class);
        MultiMap<String, String> expect = new MultiMap<>();
        expect.put("ETag", "foo");
        expect.put("ETag", "bar");
        assertEquals(info.noTileHeaders, expect);
        final Set<String> eTag = info.noTileHeaders.get("ETag");
        assertEquals(eTag, new HashSet<>(Arrays.asList("foo", "bar")));
    }

    /**
     * Tests the {@linkplain Preferences#deserializeStruct(Map, Class)} deserialization} of legacy {@link ImageryInfo.ImageryPreferenceEntry}
     */
    @Test
    public void testDeserializeStructTicket12474() {
        final ImageryInfo.ImageryPreferenceEntry info = Preferences.deserializeStruct(
                Collections.singletonMap("noTileHeaders", "{\"ETag\":\"foo-and-bar\"}"), ImageryInfo.ImageryPreferenceEntry.class);
        final Set<String> eTag = info.noTileHeaders.get("ETag");
        assertEquals(eTag, Collections.singleton("foo-and-bar"));
    }
}
