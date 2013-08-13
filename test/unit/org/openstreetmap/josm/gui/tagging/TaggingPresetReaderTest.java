// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.io.IOException;
import java.util.Collection;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link TaggingPresetReader} class.
 */
public class TaggingPresetReaderTest {
    /**
     * path to test data root directory
     */
    private static String testdataroot;
    
    @BeforeClass
    public static void setUpClass() {
        Main.pref = new Preferences();
        testdataroot = System.getProperty("josm.test.data");
        if (testdataroot == null || testdataroot.isEmpty()) {
            testdataroot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testdataroot + "'");
        }
    }

    /**
     * Gets path to test data directory for given ticketid.
     * @param ticketid 
     * @return 
     */
    protected static String getRegressionDataDir(int ticketid) {
        return testdataroot + "/regress/" + ticketid;
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

}
