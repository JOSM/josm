// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.DefaultCellEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.ListListSetting;
import org.openstreetmap.josm.data.Preferences.ListSetting;
import org.openstreetmap.josm.data.Preferences.MapListSetting;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Preferences.StringSetting;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

public class AdvancedPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new AdvancedPreference();
        }
    }

    public static class PrefEntry implements Comparable<PrefEntry> {
        private String key;
        private Setting value;
        private Setting defaultValue;
        private boolean isDefault;
        private boolean changed;

        public PrefEntry(String key, Setting value, Setting defaultValue, boolean isDefault) {
            CheckParameterUtil.ensureParameterNotNull(key);
            CheckParameterUtil.ensureParameterNotNull(value);
            CheckParameterUtil.ensureParameterNotNull(defaultValue);
            this.key = key;
            this.value = value;
            this.defaultValue = defaultValue;
            this.isDefault = isDefault;
        }

        public String getKey() {
            return key;
        }

        public Setting getValue() {
            return value;
        }

        public void setValue(Setting value) {
            this.value = value;
            changed = true;
            isDefault = false;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public boolean isChanged() {
            return changed;
        }

        public void reset() {
            value = defaultValue;
            changed = true;
            isDefault = true;
        }

        public int compareTo(PrefEntry other) {
            return key.compareTo(other.key);
        }
    }

    private AllSettingsTableModel model;
    protected List<PrefEntry> data;
    protected List<PrefEntry> displayData;
    protected JTextField txtFilter;

    public void addGui(final PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab("advanced", tr("Advanced Preferences"),
                tr("Setting Preference entries directly. Use with caution!"), false);

        txtFilter = new JTextField();
        JLabel lbFilter = new JLabel(tr("Search: "));
        lbFilter.setLabelFor(txtFilter);
        p.add(lbFilter);
        p.add(txtFilter, GBC.eol().fill(GBC.HORIZONTAL));
        txtFilter.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void changedUpdate(DocumentEvent e) {
                action();
            }
            @Override public void insertUpdate(DocumentEvent e) {
                action();
            }
            @Override public void removeUpdate(DocumentEvent e) {
                action();
            }
            private void action() {
                applyFilter();
            }
        });

        Map<String, Setting> orig = Main.pref.getAllSettings();
        Map<String, Setting> defaults = Main.pref.getAllDefaults();
        orig.remove("osm-server.password");
        defaults.remove("osm-server.password");
        prepareData(orig, defaults);
        model = new AllSettingsTableModel();
        applyFilter();

        final JTable list = new JTable(model);
        list.putClientProperty("terminateEditOnFocusLost", true);
        list.getColumnModel().getColumn(1).setCellRenderer(new SettingCellRenderer());
        list.getColumnModel().getColumn(1).setCellEditor(new SettingCellEditor());

        JScrollPane scroll = new JScrollPane(list);
        p.add(scroll, GBC.eol().fill(GBC.BOTH));
        scroll.setPreferredSize(new Dimension(400,200));

        JButton add = new JButton(tr("Add"));
        p.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
        p.add(add, GBC.std().insets(0,5,0,0));
        add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                addPreference(gui);
            }
        });

        JButton edit = new JButton(tr("Edit"));
        p.add(edit, GBC.std().insets(5,5,5,0));
        edit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                editPreference(gui, list);
            }
        });

        JButton reset = new JButton(tr("Reset"));
        p.add(reset, GBC.std().insets(0,5,0,0));
        reset.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                resetPreference(gui, list);
            }
        });

        list.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editPreference(gui, list);
                }
            }
        });
    }

    private void prepareData(Map<String, Setting> orig, Map<String, Setting> defaults) {
        data = new ArrayList<PrefEntry>();
        for (Entry<String, Setting> e : orig.entrySet()) {
            Setting value = e.getValue();
            Setting def = defaults.get(e.getKey());
            if (def == null) {
                def = value.getNullInstance();
            }
            PrefEntry en = new PrefEntry(e.getKey(), value, def, false);
            data.add(en);
        }
        for (Entry<String, Setting> e : defaults.entrySet()) {
            if (!orig.containsKey(e.getKey())) {
                PrefEntry en = new PrefEntry(e.getKey(), e.getValue(), e.getValue(), true);
                data.add(en);
            }
        }
        Collections.sort(data);
        displayData = new ArrayList<PrefEntry>(data);
    }

    class AllSettingsTableModel extends DefaultTableModel {

        public AllSettingsTableModel() {
            setColumnIdentifiers(new String[]{tr("Key"), tr("Value")});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1 && (displayData.get(row).getValue() instanceof StringSetting);
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
                pe.setValue(new StringSetting(s));
                fireTableCellUpdated(row, column);
            }
        }
    }

    private class SettingCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            PrefEntry pe = (PrefEntry) value;
            Setting setting = pe.getValue();
            Object val = setting.getValue();
            String display = val != null ? val.toString() : "<html><i>&lt;"+tr("unset")+"&gt;</i></html>";

            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                    display, isSelected, hasFocus, row, column);
            if (!pe.isDefault()) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            //label.setToolTipText("..."); TODO
            return label;
        }
    }

    private class SettingCellEditor extends DefaultCellEditor {
        public SettingCellEditor() {
            super(new JTextField());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            PrefEntry pe = (PrefEntry) value;
            StringSetting stg = (StringSetting) pe.getValue();
            String s = stg.getValue() == null ? "" : stg.getValue();
            return super.getTableCellEditorComponent(table, s, isSelected, row, column);
        }
    }

    private void applyFilter() {
        displayData.clear();
        for (PrefEntry e : data) {

            String prefKey = e.getKey();
            Setting valueSetting = e.getValue();
            String prefValue = valueSetting.getValue() == null ? "" : valueSetting.getValue().toString();

            String input[] = txtFilter.getText().split("\\s+");
            boolean canHas = true;

            // Make 'wmsplugin cache' search for e.g. 'cache.wmsplugin'
            final String prefKeyLower = prefKey.toLowerCase();
            final String prefValueLower = prefValue.toLowerCase();
            for (String bit : input) {
                bit = bit.toLowerCase();
                if (!prefKeyLower.contains(bit) && !prefValueLower.contains(bit)) {
                    canHas = false;
                    break;
                }
            }
            if (canHas) {
                displayData.add(e);
            }
        }
        model.fireTableDataChanged();
    }

    @Override
    public boolean ok() {
        for (PrefEntry e : data) {
            if (e.isChanged()) {
                Main.pref.putSetting(e.getKey(), e.getValue());
            }
        }
        return false;
    }

    private void resetPreference(final PreferenceTabbedPane gui, final JTable list) {
        if (list.getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    gui,
                    tr("Please select the row to delete."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        for (int row : list.getSelectedRows()) {
            PrefEntry e = displayData.get(row);
            e.reset();
        }
        model.fireTableDataChanged();
    }

    private void addPreference(final PreferenceTabbedPane gui) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key")), GBC.std().insets(0,0,5,0));
        JTextField tkey = new JTextField("", 50);
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
                StringSetting sSetting = new StringSetting(null);
                pe = new PrefEntry(tkey.getText(), sSetting, sSetting, false);
                StringEditor sEditor = new StringEditor(gui, pe, sSetting);
                sEditor.showDialog();
                if (sEditor.getValue() == 1) {
                    String data = sEditor.getData();
                    if (!Utils.equal(sSetting.getValue(), data)) {
                        pe.setValue(new StringSetting(data));
                        ok = true;
                    }
                }
            } else if (rbList.isSelected()) {
                ListSetting lSetting = new ListSetting(null);
                pe = new PrefEntry(tkey.getText(), lSetting, lSetting, false);
                ListEditor lEditor = new ListEditor(gui, pe, lSetting);
                lEditor.showDialog();
                if (lEditor.getValue() == 1) {
                    List<String> data = lEditor.getData();
                    if (!Preferences.equalCollection(lSetting.getValue(), data)) {
                        pe.setValue(new ListSetting(data));
                        ok = true;
                    }
                }
            } else if (rbListList.isSelected()) {
                ListListSetting llSetting = new ListListSetting(null);
                pe = new PrefEntry(tkey.getText(), llSetting, llSetting, false);
                ListListEditor llEditor = new ListListEditor(gui, pe, llSetting);
                llEditor.showDialog();
                if (llEditor.getValue() == 1) {
                    List<List<String>> data = llEditor.getData();
                    if (!Preferences.equalArray((Collection) llSetting.getValue(), data)) {
                        pe.setValue(new ListListSetting(data));
                        ok = true;
                    }
                }
            } else if (rbMapList.isSelected()) {
                MapListSetting mlSetting = new MapListSetting(null);
                pe = new PrefEntry(tkey.getText(), mlSetting, mlSetting, false);
                MapListEditor mlEditor = new MapListEditor(gui, pe, mlSetting);
                mlEditor.showDialog();
                if (mlEditor.getValue() == 1) {
                    List<Map<String, String>> data = mlEditor.getData();
                    if (!Preferences.equalListOfStructs(mlSetting.getValue(), data)) {
                        pe.setValue(new MapListSetting(data));
                        ok = true;
                    }
                }
            }
            if (ok) {
                data.add(pe);
                Collections.sort(data);
                applyFilter();
            }
        }
    }

    private void editPreference(final PreferenceTabbedPane gui, final JTable list) {
        if (list.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(
                    gui,
                    tr("Please select the row to edit."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        final PrefEntry e = (PrefEntry) model.getValueAt(list.getSelectedRow(), 1);
        Setting stg = e.getValue();
        if (stg instanceof StringSetting) {
            list.editCellAt(list.getSelectedRow(), 1);
            Component editor = list.getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
            }
        } else if (stg instanceof ListSetting) {
            ListSetting lSetting = (ListSetting) stg;
            ListEditor lEditor = new ListEditor(gui, e, lSetting);
            lEditor.showDialog();
            if (lEditor.getValue() == 1) {
                List<String> data = lEditor.getData();
                if (!Preferences.equalCollection(lSetting.getValue(), data)) {
                    e.setValue(new ListSetting(data));
                    applyFilter();
                }
            }
        } else if (stg instanceof ListListSetting) {
            ListListEditor llEditor = new ListListEditor(gui, e, (ListListSetting) stg);
            llEditor.showDialog();
            if (llEditor.getValue() == 1) {
                List<List<String>> data = llEditor.getData();
                if (!Preferences.equalArray((Collection) stg.getValue(), data)) {
                    e.setValue(new ListListSetting(data));
                    applyFilter();
                }
            }
        } else if (stg instanceof MapListSetting) {
            MapListSetting mlSetting = (MapListSetting) stg;
            MapListEditor mlEditor = new MapListEditor(gui, e, mlSetting);
            mlEditor.showDialog();
            if (mlEditor.getValue() == 1) {
                List<Map<String, String>> data = mlEditor.getData();
                if (!Preferences.equalListOfStructs(mlSetting.getValue(), data)) {
                    e.setValue(new MapListSetting(data));
                    applyFilter();
                }
            }
        }
    }
}
