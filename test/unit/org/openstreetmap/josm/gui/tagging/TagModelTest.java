// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagModel} class.
 */
class TagModelTest {

    /**
     * Unit test of {@link TagModel#TagModel} - single value.
     */
    @Test
    void testTagModelSingleValue() {
        TagModel tm = new TagModel();
        assertEquals("", tm.getName());
        assertEquals("", tm.getValue());
        assertEquals(1, tm.getValueCount());
        assertTrue(tm.hasValue(null));
        assertTrue(tm.hasValue(""));

        tm.clearValues();
        assertEquals(0, tm.getValueCount());
        assertEquals("", tm.getValue());

        tm.setValue(null);
        assertEquals(1, tm.getValueCount());
        assertEquals("", tm.getValue());

        tm = new TagModel("key");
        assertEquals("key", tm.getName());
        assertEquals("", tm.getValue());
        assertEquals(1, tm.getValueCount());
        assertTrue(tm.hasValue(""));
    }

    /**
     * Unit test of {@link TagModel#TagModel} - multiple values.
     */
    @Test
    void testTagModelMultipleValues() {
        TagModel tm = new TagModel("key2", "val2");
        assertEquals("key2", tm.getName());
        assertEquals("val2", tm.getValue());
        assertEquals(1, tm.getValueCount());
        assertTrue(tm.hasValue("val2"));

        tm.setName("key3");
        tm.setValue("val3");
        assertEquals("key3", tm.getName());
        assertEquals("val3", tm.getValue());
        assertEquals(1, tm.getValueCount());
        assertTrue(tm.hasValue("val3"));

        tm.addValue("val4");
        tm.addValue("val4");
        assertEquals(2, tm.getValueCount());
        assertEquals("val3;val4", tm.getValue());

        tm.removeValue("something");
        tm.removeValue(null);
        assertEquals(2, tm.getValueCount());
        assertEquals("val3;val4", tm.getValue());

        tm.removeValue("val3");
        assertEquals(1, tm.getValueCount());
        assertEquals("val4", tm.getValue());
    }
}
