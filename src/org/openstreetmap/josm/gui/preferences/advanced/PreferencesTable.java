// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Component for editing list of preferences as a table.
 * @since 6021
 */
public class PreferencesTable extends JTable {
    private AllSettingsTableModel model;
    private final List<PrefEntry> displayData;

    /**
     * Constructs a new {@code PreferencesTable}.
     * @param displayData The list of preferences entries to display
     */
    public PreferencesTable(List<PrefEntry> displayData) {
        this.displayData = displayData;
        model = new AllSettingsTableModel();
        setModel(model);
        putClientProperty("terminateEditOnFocusLost", true);
        getColumnModel().getColumn(1).setCellRenderer(new SettingCellRenderer());
        getColumnModel().getColumn(1).setCellEditor(new SettingCellEditor());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editPreference(PreferencesTable.this);
                }
            }
        });
    }

    /**
     * This method should be called when displayed data was changed form external code
     */
    public void fireDataChanged() {
        model.fireTableDataChanged();
    }

    /**
     * The list of currently selected rows
     * @return newly created list of PrefEntry
     */
    public List<PrefEntry> getSelectedItems() {
        List<PrefEntry> entries = new ArrayList<PrefEntry>();
        for (int row : getSelectedRows()) {
            PrefEntry p = (PrefEntry) model.getValueAt(row, -1);
            entries.add(p);
        }
        return entries;
    }

    /**
     * Call this to edit selected row in preferences table
     * @param gui - parent component for messagebox
     * @return true if editing was actually performed during this call
     */
    public boolean editPreference(final JComponent gui) {
        if (getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(
                    gui,
                    tr("Please select the row to edit."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
                    );
            return false;
        }
        final PrefEntry e = (PrefEntry) model.getValueAt(getSelectedRow(), 1);
        Preferences.Setting stg = e.getValue();
        if (stg instanceof Preferences.StringSetting) {
            editCellAt(getSelectedRow(), 1);
            Component editor = getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
            }
        } else if (stg instanceof Preferences.ListSetting) {
            Preferences.ListSetting lSetting = (Preferences.ListSetting) stg;
            ListEditor lEditor = new ListEditor(gui, e, lSetting);
            lEditor.showDialog();
            if (lEditor.getValue() == 1) {
                List<String> data = lEditor.getData();
                if (!lSetting.equalVal(data)) {
                    e.setValue(new Preferences.ListSetting(data));
                    return true;
                }
            }
        } else if (stg instanceof Preferences.ListListSetting) {
            Preferences.ListListSetting llSetting = (Preferences.ListListSetting) stg;
            ListListEditor llEditor = new ListListEditor(gui, e, llSetting);
            llEditor.showDialog();
            if (llEditor.getValue() == 1) {
                List<List<String>> data = llEditor.getData();
                if (!llSetting.equalVal(data)) {
                    e.setValue(new Preferences.ListListSetting(data));
                    return true;
                }
            }
        } else if (stg instanceof Preferences.MapListSetting) {
            Preferences.MapListSetting mlSetting = (Preferences.MapListSetting) stg;
            MapListEditor mlEditor = new MapListEditor(gui, e, mlSetting);
            mlEditor.showDialog();
            if (mlEditor.getValue() == 1) {
                List<Map<String, String>> data = mlEditor.getData();
                if (!mlSetting.equalVal(data)) {
                    e.setValue(new Preferences.MapListSetting(data));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add new preference to the table
     * @param gui - parent component for asking dialogs
     * @return newly created entry or null if adding was cancelled
     */
    public PrefEntry addPreference(final JComponent gui) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key")), GBC.std().insets(0,0,5,0));
        JosmTextField tkey = new JosmTextField("", 50);
        p.add(tkey, GBC.eop().insets(5,0,0,0).fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Select Setting Type:")), GBC.eol().insets(5,15,5,0));

        JRadioButton rbString = new JRadioButton(tr("Simple"));
        JRadioButton rbList = new JRadioButton(tr("List"));
        JRadioButton rbListList = new JRadioButton(tr("List of lists"));
        JRadioButton rbMapList = new JRadioButton(tr("List of maps"));

        ButtonGroup group = new ButtonGroup();
        group.add(rbString);
        group.add(rbList);
        group.add(rbListList);
        group.add(rbMapList);

        p.add(rbString, GBC.eol());
        p.add(rbList, GBC.eol());
        p.add(rbListList, GBC.eol());
        p.add(rbMapList, GBC.eol());

        rbString.setSelected(true);

        ExtendedDialog dlg = new ExtendedDialog(gui, tr("Add setting"), new String[] {tr("OK"), tr("Cancel")});
        dlg.setButtonIcons(new String[] {"ok.png", "cancel.png"});
        dlg.setContent(p);
        dlg.showDialog();

        PrefEntry pe = null;
        boolean ok = false;
        if (dlg.getValue() == 1) {
            if (rbString.isSelected()) {
                Preferences.StringSetting sSetting = new Preferences.StringSetting(null);
                pe = new PrefEntry(tkey.getText(), sSetting, sSetting, false);
                StringEditor sEditor = new StringEditor(gui, pe, sSetting);
                sEditor.showDialog();
                if (sEditor.getValue() == 1) {
                    String data = sEditor.getData();
                    if (!Utils.equal(sSetting.getValue(), data)) {
                        pe.setValue(new Preferences.StringSetting(data));
                        ok = true;
                    }
                }
            } else if (rbList.isSelected()) {
                Preferences.ListSetting lSetting = new Preferences.ListSetting(null);
                pe = new PrefEntry(tkey.getText(), lSetting, lSetting, false);
                ListEditor lEditor = new ListEditor(gui, pe, lSetting);
                lEditor.showDialog();
                if (lEditor.getValue() == 1) {
                    List<String> data = lEditor.getData();
                    if (!lSetting.equalVal(data)) {
                        pe.setValue(new Preferences.ListSetting(data));
                        ok = true;
                    }
                }
            } else if (rbListList.isSelected()) {
                Preferences.ListListSetting llSetting = new Preferences.ListListSetting(null);
                pe = new PrefEntry(tkey.getText(), llSetting, llSetting, false);
                ListListEditor llEditor = new ListListEditor(gui, pe, llSetting);
                llEditor.showDialog();
                if (llEditor.getValue() == 1) {
                    List<List<String>> data = llEditor.getData();
                    if (!llSetting.equalVal(data)) {
                        pe.setValue(new Preferences.ListListSetting(data));
                        ok = true;
                    }
                }
            } else if (rbMapList.isSelected()) {
                Preferences.MapListSetting mlSetting = new Preferences.MapListSetting(null);
                pe = new PrefEntry(tkey.getText(), mlSetting, mlSetting, false);
                MapListEditor mlEditor = new MapListEditor(gui, pe, mlSetting);
                mlEditor.showDialog();
                if (mlEditor.getValue() == 1) {
                    List<Map<String, String>> data = mlEditor.getData();
                    if (!mlSetting.equalVal(data)) {
                        pe.setValue(new Preferences.MapListSetting(data));
                        ok = true;
                    }
                }
            }
        }
        if (ok)
            return pe;
        else
            return null;
    }

    /**
     * Reset selected preferences to their default values
     * @param gui - parent component to display warning messages
     */
    public void resetPreferences(final JComponent gui) {
        if (getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    gui,
                    tr("Please select the row to delete."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
                    );
            return;
        }
        for (int row : getSelectedRows()) {
            PrefEntry e = displayData.get(row);
            e.reset();
        }
        fireDataChanged();
    }

    private class AllSettingsTableModel extends DefaultTableModel {

        public AllSettingsTableModel() {
            setColumnIdentifiers(new String[]{tr("Key"), tr("Value")});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1 && (displayData.get(row).getValue() instanceof Preferences.StringSetting);
        }

        @Override
        public int getRowCount() {
            return displayData.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0)
                return displayData.get(row).getKey();
            else
                return displayData.get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            PrefEntry pe = displayData.get(row);
            String s = (String) o;
            if (!s.equals(pe.getValue().getValue())) {
                pe.setValue(new Preferences.StringSetting(s));
                fireTableCellUpdated(row, column);
            }
        }
    }

    private static class SettingCellRenderer extends DefaultTableCellRenderer {
        private Color backgroundColor = Main.pref.getUIColor("Table.background");
        private Color changedColor = Main.pref.getColor(
                         marktr("Advanced Background: Changed"),
                         new Color(200,255,200));
        private Color foregroundColor = Main.pref.getUIColor("Table.foreground");
        private Color nonDefaultColor = Main.pref.getColor(
                            marktr("Advanced Background: NonDefault"),
                            new Color(255,255,200));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            PrefEntry pe = (PrefEntry) value;
            Preferences.Setting setting = pe.getValue();
            Object val = setting.getValue();
            String display = val != null ? val.toString() : "<html><i>&lt;"+tr("unset")+"&gt;</i></html>";

            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                    display, isSelected, hasFocus, row, column);

            label.setBackground(backgroundColor);
            if (isSelected) {
                label.setForeground(foregroundColor);
            }
            if(pe.isChanged()) {
                label.setBackground(changedColor);
            } else if(!pe.isDefault()) {
                label.setBackground(nonDefaultColor);
            }

            if (!pe.isDefault()) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            val = pe.getDefaultValue().getValue();
            if(val != null)
            {
                if(pe.isDefault()) {
                    label.setToolTipText(tr("Current value is default."));
                } else {
                    label.setToolTipText(tr("Default value is ''{0}''.", val));
                }
            } else {
                label.setToolTipText(tr("Default value currently unknown (setting has not been used yet)."));
            }
            return label;
        }
    }

    private static class SettingCellEditor extends DefaultCellEditor {
        public SettingCellEditor() {
            super(new JosmTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            PrefEntry pe = (PrefEntry) value;
            Preferences.StringSetting stg = (Preferences.StringSetting) pe.getValue();
            String s = stg.getValue() == null ? "" : stg.getValue();
            return super.getTableCellEditorComponent(table, s, isSelected, row, column);
        }
    }
}
