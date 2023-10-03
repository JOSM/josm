// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.StringSetting;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link PrefEntry} class.
 */
class PrefEntryTest {
    /**
     * Unit test of {@link PrefEntry#PrefEntry}.
     */
    @Test
    void testPrefEntry() {
        String key = "key";
        StringSetting val = new StringSetting("value");
        StringSetting def = new StringSetting("defaultValue");
        PrefEntry pe = new PrefEntry(key, val, def, false);
        assertFalse(pe.isDefault());
        assertEquals(key, pe.getKey());
        assertEquals(val, pe.getValue());
        assertEquals(def, pe.getDefaultValue());
        StringSetting val2 = new StringSetting("value2");
        assertFalse(pe.isChanged());
        pe.setValue(val2);
        assertEquals(val2, pe.getValue());
        assertEquals(val2.toString(), pe.toString());
        assertTrue(pe.isChanged());
        pe.reset();
        pe.markAsChanged();
        assertTrue(pe.isChanged());
    }

    /**
     * Unit test of methods {@link PrefEntry#equals} and {@link PrefEntry#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(PrefEntry.class).usingGetClass()
            .withIgnoredFields("value", "defaultValue", "isDefault", "changed")
            .verify();
    }
}
