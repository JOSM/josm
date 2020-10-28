// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector.PresetClassification;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TaggingPresetSelector} class.
 */
class TaggingPresetSelectorTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test for {@link PresetClassification#isMatching}.
     */
    @Test
    void testIsMatching() {
        TaggingPreset preset = new TaggingPreset();
        preset.name = "estação de bombeiros"; // fire_station in brazilian portuguese
        PresetClassification pc = new PresetClassification(preset);
        assertEquals(0, pc.isMatchingName("foo"));
        assertTrue(pc.isMatchingName("estação") > 0);
        assertTrue(pc.isMatchingName("estacao") > 0);
    }
}
