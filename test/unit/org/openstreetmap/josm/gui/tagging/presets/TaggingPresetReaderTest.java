// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.CustomMatchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TaggingPresetReader} class.
 */
public class TaggingPresetReaderTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * #8954 - last checkbox in the preset is not added
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testTicket8954() throws SAXException, IOException {
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
     * Test external entity resolving.
     * See #19286
     */
    @Test
    public void testExternalEntityResolving() throws IOException {
        try {
            TaggingPresetReader.readAll(TestUtils.getTestDataRoot() + "preset_external_entity.xml", true);
            fail("Reading a file with external entities should throw an SAXParseException!");
        } catch (SAXException e) {
            String expected = "DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true.";
            assertEquals(expected, e.getMessage());
        }
    }

    /**
     * Validate internal presets
     * See #9027
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testReadDefaulPresets() throws SAXException, IOException {
        String presetfile = "resource://data/defaultpresets.xml";
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, true);
        Assert.assertTrue("Default presets are empty", presets.size() > 0);
    }
}
