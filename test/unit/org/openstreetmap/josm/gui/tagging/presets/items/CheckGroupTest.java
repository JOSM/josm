// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;

/**
 * Unit tests of {@link CheckGroup} class.
 */
class CheckGroupTest implements TaggingPresetItemTest {
    @Override
    public TaggingPresetItem getInstance() {
        return new CheckGroup();
    }
}
