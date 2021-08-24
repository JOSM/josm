// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;

/**
 * Test class for {@link AutoCompletingComboBox}
 */
@FullPreferences
class AutoCompComboBoxTest {

    @Test
    void testAutoCompletingComboBox() {
        assertNotNull(new AutoCompComboBox<String>());
        assertNotNull(new AutoCompComboBox<AutoCompletionItem>());
    }
}
