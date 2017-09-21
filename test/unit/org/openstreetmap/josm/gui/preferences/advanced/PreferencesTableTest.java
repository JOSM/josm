// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.gui.preferences.advanced.PreferencesTable.AllSettingsTableModel;

/**
 * Unit tests of {@link PreferencesTable} class.
 */
public class PreferencesTableTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static PrefEntry newPrefEntry(String value) {
        StringSetting val = new StringSetting(value);
        StringSetting def = new StringSetting("defaultValue");
        return new PrefEntry("key", val, def, false);
    }

    private static PreferencesTable newTable() {
        return new PreferencesTable(Arrays.asList(newPrefEntry("value")));
    }

    /**
     * Unit test of {@link PreferencesTable#PreferencesTable}.
     */
    @Test
    public void testPreferencesTable() {
        PreferencesTable t = newTable();
        t.fireDataChanged();
        assertTrue(t.getSelectedItems().isEmpty());
        assertFalse(t.editPreference(null));
        assertNull(t.addPreference(null));
        t.resetPreferences(null);
    }

    /**
     * Unit test of {@link PreferencesTable.AllSettingsTableModel} class.
     */
    @Test
    public void testAllSettingsTableModel() {
        AllSettingsTableModel model = (AllSettingsTableModel) newTable().getModel();
        assertEquals(1, model.getRowCount());
        assertFalse(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(0, 1));
        assertEquals("key", model.getValueAt(0, 0));
        assertEquals(newPrefEntry("value"), model.getValueAt(0, 1));
        String foobar = "foobar";
        model.setValueAt(foobar, 0, 1);
        assertEquals(newPrefEntry(foobar), model.getValueAt(0, 1));
    }
}
