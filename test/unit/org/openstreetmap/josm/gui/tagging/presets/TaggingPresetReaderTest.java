// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.CustomMatchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link TaggingPresetReader} class.
 */
class TaggingPresetReaderTest {
    /**
     * #8954 - last checkbox in the preset is not added
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTicket8954() throws SAXException, IOException {
        String presetfile = TestUtils.getRegressionDataFile(8954, "preset.xml");
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, false);
        assertEquals(1, presets.size(), "Number of preset items");
        final TaggingPreset preset = presets.iterator().next();
        assertEquals(1, preset.data.size(), "Number of entries");
        final TaggingPresetItem item = preset.data.get(0);
        assertInstanceOf(Check.class, item, "Entry is not checkbox");
    }

    /**
     * Test nested chunks
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testNestedChunks() throws SAXException, IOException {
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
     * @throws IOException in case of I/O error
     */
    @Test
    void testExternalEntityResolving() throws IOException {
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
    @Territories
    @Test
    void testReadDefaultPresets() throws SAXException, IOException {
        String presetfile = "resource://data/defaultpresets.xml";
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll(presetfile, true);
        assertTrue(presets.size() > 0, "Default presets are empty");
    }
}
