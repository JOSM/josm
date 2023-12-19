// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * Unit tests of {@link PresetLink} class.
 */
@TaggingPresets
class PresetLinkTest implements TaggingPresetItemTest {
    @Override
    public PresetLink getInstance() {
        PresetLink presetLink = new PresetLink();
        presetLink.preset_name = "River";
        return presetLink;
    }
}
