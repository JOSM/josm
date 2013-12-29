// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.TestUtils;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.CustomMatchers.hasSize;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests of {@link TaggingPresetReader} class.
 */
public class TaggingPresetReaderTest {

    @BeforeClass
    public static void setUpClass() {
        Main.initApplicationPreferences();
    }

    /**
     * Gets path to test data directory for given ticketid.
     * @param ticketid 
     * @return 
     */
    protected static String getRegressionDataDir(int ticketid) {
        return TestUtils.getTestDataRoot() + "/regress/" + ticketid;
    }

    /**
     * Gets path to given file in test data directory for given ticketid.
     * @param ticketid
     * @param filename
     * @return 
     */
    protected static String getRegressionDataFile(int ticketid, String filename) {
        return getRegressionDataDir(ticketid) + '/' + filename;
    }

    /**
     * #8954 - last checkbox in the preset is not added
     */
    @Test
    public void test8954() throws SAXException, IOException {
        String presetfile = getRegressionDataFile(8954, "preset.xml");
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
        assertThat(keys.toString(), is("[A1, A2, A3, B1, B2, B3, C1, C2, C3]"));
    }

    /**
     * Validate internal presets
     * See #9027
     */
    @Test
    public void readDefaulPresets() throws SAXException, IOException {
        String presetfile = "resource://data/defaultpresets.xml";
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, true);
        Assert.assertTrue("Default presets are empty", presets.size()>0);
    }
    
}
