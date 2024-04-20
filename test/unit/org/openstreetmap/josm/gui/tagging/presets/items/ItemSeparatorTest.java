// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;

/**
 * Unit tests of {@link ItemSeparator} class.
 */
class ItemSeparatorTest implements TaggingPresetItemTest {
    @Override
    public ItemSeparator getInstance() {
        return new ItemSeparator();
    }
}
