// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.swing.JOptionPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.advanced.PreferencesTable.AllSettingsTableModel;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PreferencesTable} class.
 */
public class PreferencesTableTest {
    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().assertionsInEDT();

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
        new JOptionPaneSimpleMocker(ImmutableMap.of(
            "Please select the row to edit.", JOptionPane.OK_OPTION,
            "Please select the row to delete.", JOptionPane.OK_OPTION
        ));
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
