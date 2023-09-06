// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TaggingPresetSelector} class.
 */
class TaggingPresetSelectorTest {
    /**
     * Unit test for {@link TaggingPresetSelector.PresetClassifications#getMatchingPresets}.
     */
    @Test
    void testGetMatching() {
        TaggingPreset preset = new TaggingPreset();
        preset.name = "estação de bombeiros"; // fire_station in brazilian portuguese
        TaggingPresetSelector.PresetClassifications presetClassifications = new TaggingPresetSelector.PresetClassifications();
        presetClassifications.loadPresets(Collections.singleton(preset));
        assertTrue(presetClassifications.getMatchingPresets(null, new String[] {"foo"}, false,
                false, null, null).isEmpty());
        assertSame(preset, presetClassifications.getMatchingPresets(null, new String[] {"estação"}, false,
                false, null, null).get(0).preset);
        assertSame(preset, presetClassifications.getMatchingPresets(null, new String[] {"estacao"}, false,
                false, null, null).get(0).preset);
    }
}
