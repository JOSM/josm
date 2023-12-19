// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;

/**
 * Unit tests of {@link Optional} class.
 */
class OptionalTest implements TaggingPresetItemTest {
    @Override
    public Optional getInstance() {
        return new Optional();
    }
}
