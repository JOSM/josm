// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link JosmTextField}
 */
@BasicPreferences
class JosmTextFieldTest {
    @Test
    void testSetHint() {
        JosmTextField josmTextField = new JosmTextField();
        josmTextField.setHint("some hint");
        assertEquals("some hint", josmTextField.getHint());
        assertEquals("some hint", josmTextField.setHint("new hint"));
        assertEquals("new hint", josmTextField.getHint());
    }
}
