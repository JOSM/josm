// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link TagEditorModel} class.
 */
public class TagEditorModelTest {

    /**
     * Unit test of {@link TagEditorModel#TagEditorModel}.
     */
    @Test
    public void testTagEditorModel() {
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
