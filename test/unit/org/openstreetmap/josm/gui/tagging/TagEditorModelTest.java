// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagEditorModel} class.
 */
class TagEditorModelTest {

    /**
     * Unit test of {@link TagEditorModel#TagEditorModel}.
     */
    @Test
    void testTagEditorModel() {
        TagEditorModel tem = new TagEditorModel();
        tem.add(null, null);
        assertEquals(1, tem.getRowCount());
        assertEquals(2, tem.getColumnCount());
        tem.add("key", "val");
        assertEquals(2, tem.getRowCount());
        assertEquals(2, tem.getColumnCount());
        tem.delete(null);
        tem.delete("");
        assertEquals(1, tem.getRowCount());
        tem.delete("key");
        assertEquals(0, tem.getRowCount());
    }
}
