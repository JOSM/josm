// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.CustomMatchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.tools.Utils;
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
     */
    @Test
    public void test8954() throws SAXException, IOException {
        String presetfile = TestUtils.getRegressionDataFile(8954, "preset.xml");
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, false);
        Assert.assertEquals("Number of preset items", 1, presets.size());
        final TaggingPreset preset = presets.iterator().next();
        Assert.assertEquals("Number of entries", 1, preset.data.size());
        final TaggingPresetItem item = preset.data.get(0);
        Assert.assertTrue("Entry is not checkbox", item instanceof TaggingPresetItems.Check);
    }

    @Test
    public void testNestedChunks() throws Exception {
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(TestUtils.getTestDataRoot() + "preset_chunk.xml", true);
        assertThat(presets, hasSize(1));
        final TaggingPreset abc =  presets.iterator().next();
        final List<String> keys = Utils.transform(abc.data, new Utils.Function<TaggingPresetItem, String>() {
            @Override
            public String apply(TaggingPresetItem x) {
                return ((TaggingPresetItems.Key) x).key;
            }
        });
        assertEquals("[A1, A2, A3, B1, B2, B3, C1, C2, C3]", keys.toString());
    }

    /**
     * Validate internal presets
     * See #9027
     */
    @Test
    public void readDefaulPresets() throws SAXException, IOException {
        String presetfile = "resource://data/defaultpresets.xml";
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, true);
        Assert.assertTrue("Default presets are empty", presets.size() > 0);
    }
}
