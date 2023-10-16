// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.advanced.PreferencesTable.AllSettingsTableModel;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

/**
 * Unit tests of {@link PreferencesTable} class.
 */
@AssertionsInEDT
@BasicPreferences
class PreferencesTableTest {
    private static PrefEntry newPrefEntry(String value) {
        StringSetting val = new StringSetting(value);
        StringSetting def = new StringSetting("defaultValue");
        return new PrefEntry("key", val, def, false);
    }

    private static PreferencesTable newTable() {
        return new PreferencesTable(Collections.singletonList(newPrefEntry("value")));
    }

    /**
     * Unit test of {@link PreferencesTable#PreferencesTable}.
     */
    @Test
    void testPreferencesTable() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker mocker = new JOptionPaneSimpleMocker();
        mocker.getMockResultMap().put("Please select the row to edit.", JOptionPane.OK_OPTION);
        mocker.getMockResultMap().put("Please select the row to delete.", JOptionPane.OK_OPTION);
        new ExtendedDialogMocker() {
            @Override
            protected int getMockResult(final ExtendedDialog instance) {
                if (instance.getTitle().equals("Add setting")) {
                    return 1 + this.getButtonPositionFromLabel(instance, "Cancel");
                } else {
                    return super.getMockResult(instance);
                }
            }
        };
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
    void testAllSettingsTableModel() {
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
