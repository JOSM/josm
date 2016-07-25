// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.CustomMatchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link TaggingPresetReader} class.
 */
public class TaggingPresetReaderTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * #8954 - last checkbox in the preset is not added
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void test8954() throws SAXException, IOException {
        String presetfile = TestUtils.getRegressionDataFile(8954, "preset.xml");
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, false);
        Assert.assertEquals("Number of preset items", 1, presets.size());
        final TaggingPreset preset = presets.iterator().next();
        Assert.assertEquals("Number of entries", 1, preset.data.size());
        final TaggingPresetItem item = preset.data.get(0);
        Assert.assertTrue("Entry is not checkbox", item instanceof Check);
    }

    /**
     * Test nested chunks
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testNestedChunks() throws SAXException, IOException {
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(TestUtils.getTestDataRoot() + "preset_chunk.xml", true);
        assertThat(presets, hasSize(1));
        final TaggingPreset abc = presets.iterator().next();
        assertTrue(abc.data.stream().allMatch(Key.class::isInstance));
        final List<String> keys = abc.data.stream().map(x -> ((Key) x).key).collect(Collectors.toList());
        assertEquals("[A1, A2, A3, B1, B2, B3, C1, C2, C3]", keys.toString());
    }

    /**
     * Validate internal presets
     * See #9027
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void readDefaulPresets() throws SAXException, IOException {
        String presetfile = "resource://data/defaultpresets.xml";
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, true);
        Assert.assertTrue("Default presets are empty", presets.size() > 0);
    }
}
