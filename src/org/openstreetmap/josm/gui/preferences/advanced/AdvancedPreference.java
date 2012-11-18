// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.data.CustomConfigurator;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.ListListSetting;
import org.openstreetmap.josm.data.Preferences.ListSetting;
import org.openstreetmap.josm.data.Preferences.MapListSetting;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Preferences.StringSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.actionsupport.LogShowDialog;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

public class AdvancedPreference extends DefaultTabPreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new AdvancedPreference();
        }
    }

    private AdvancedPreference() {
        super("advanced", tr("Advanced Preferences"), tr("Setting Preference entries directly. Use with caution!"));
    }

    @Override
    public boolean isExpert() {
        return true;
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

        public Setting getDefaultValue() {
            return defaultValue;
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
    
        private void markAsChanged() {
            changed = true;
        }
    
        public void reset() {
            value = defaultValue;
            changed = true;
            isDefault = true;
        }

        @Override
        public int compareTo(PrefEntry other) {
            return key.compareTo(other.key);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private AllSettingsTableModel model;
    protected List<PrefEntry> data;
    protected List<PrefEntry> displayData;
    protected JTextField txtFilter;

    public void addGui(final PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab(this);

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
        readPreferences(Main.pref);
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

        JButton read = new JButton(tr("Read from file"));
        p.add(read, GBC.std().insets(5,5,0,0));
        read.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                File[] files = askUserForCustomSettingsFiles(false, tr("Open JOSM customization file"));
                if (files.length==0) return;
                
                Preferences tmpPrefs = CustomConfigurator.clonePreferences(Main.pref);
                
                StringBuilder log = new StringBuilder();
                log.append("<html>");
                for (File f: files) {
                    CustomConfigurator.readXML(f, tmpPrefs);
                    log.append(CustomConfigurator.getLog());
                }
                //try { Main.pref.save();  } catch (IOException ex) { }
                log.append("</html>");
                String msg = log.toString().replace("\n", "<br/>");
                
                new LogShowDialog(tr("Import log"), tr("<html>Here is file import summary. <br/>"
                        + "You can reject preferences changes by pressing \"Cancel\" in preferences dialog <br/>"
                        + "To activate some changes JOSM restart may be needed.</html>"), msg).showDialog();
                
                //JOptionPane.showMessageDialog(Main.parent,
                //   tr("Installed plugins and some changes in preferences will start to work after JOSM restart"), tr("Warning"), JOptionPane.WARNING_MESSAGE);

                readPreferences(tmpPrefs);
                // sorting after modification - first modified, then non-default, then default entries
                Collections.sort(data, new Comparator<PrefEntry>() {
                    @Override
                    public int compare(PrefEntry o1, PrefEntry o2) {
                        if (o1.changed && !o2.changed) return -1;
                        if (o2.changed && !o1.changed) return 1;
                        if (!(o1.isDefault) && o2.isDefault) return -1;
                        if (!(o2.isDefault) && o1.isDefault) return 1;
                        return o1.key.compareTo(o2.key);
                    }
                  });

                applyFilter();
                ((AllSettingsTableModel) list.getModel()).fireTableDataChanged();
            }

        });
        
        JButton export = new JButton(tr("Export selected items"));
        p.add(export, GBC.std().insets(5,5,0,0));
        export.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> keys = new ArrayList<String>();
                boolean hasLists = false;
                for (int row : list.getSelectedRows()) {
                    PrefEntry p = (PrefEntry) model.getValueAt(row, -1);
                    if (!p.isDefault()) {
                        // preferences with default values are not saved
                        if (!(p.getValue() instanceof StringSetting)) hasLists=true; // => append and replace differs
                        keys.add(p.getKey());
                    }
                }
                if (keys.size()==0) {
                     JOptionPane.showMessageDialog(Main.parent,
                        tr("Please select some preference keys not marked as default"), tr("Warning"), JOptionPane.WARNING_MESSAGE);
                     return;
                }

                File[] files = askUserForCustomSettingsFiles(true, tr("Export preferences keys to JOSM customization file"));
                if (files.length==0) return;
                
                int answer = 0;
                if (hasLists) {
                    answer = JOptionPane.showOptionDialog(
                            Main.parent, tr("What to do with preference lists when this file is to be imported?"), tr("Question"),
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            new String[]{tr("Append preferences from file to existing values"), tr("Replace existing values")}, 0);
                }
                CustomConfigurator.exportPreferencesKeysToFile(files[0].getAbsolutePath(), answer==0, keys);
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

    private void readPreferences(Preferences tmpPrefs) {
        Map<String, Setting> loaded;
        Map<String, Setting> orig = Main.pref.getAllSettings();
        Map<String, Setting> defaults = tmpPrefs.getAllDefaults();
        orig.remove("osm-server.password");
        defaults.remove("osm-server.password");
        if (tmpPrefs != Main.pref) {
            loaded = tmpPrefs.getAllSettings();
            // plugins preference keys may be changed directly later, after plugins are downloaded
            // so we do not want to show it in the table as "changed" now
            Setting pluginSetting = orig.get("plugins");
            if (pluginSetting!=null) {
                loaded.put("plugins", pluginSetting);
            }
        } else {
            loaded = orig;
        }
        prepareData(loaded, orig, defaults);
    }
    
    private File[] askUserForCustomSettingsFiles(boolean saveFileFlag, String title) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
            }
            @Override
            public String getDescription() {
                return tr("JOSM custom settings files (*.xml)");
            }
        };
        JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(!saveFileFlag, !saveFileFlag, title, filter, JFileChooser.FILES_ONLY, "customsettings.lastDirectory");
        if (fc != null) {
            File sel[] = fc.isMultiSelectionEnabled() ? fc.getSelectedFiles() : (new File[]{fc.getSelectedFile()});
            if (sel.length==1 && !sel[0].getName().contains(".")) sel[0]=new File(sel[0].getAbsolutePath()+".xml");
            return sel;
        } 
        return new File[0];
    }
            
    private void prepareData(Map<String, Setting> loaded, Map<String, Setting> orig, Map<String, Setting> defaults) {
        data = new ArrayList<PrefEntry>();
        for (Entry<String, Setting> e : loaded.entrySet()) {
            Setting value = e.getValue();
            Setting old = orig.get(e.getKey());
            Setting def = defaults.get(e.getKey());
            if (def == null) {
                def = value.getNullInstance();
            }
            PrefEntry en = new PrefEntry(e.getKey(), value, def, false);
            // after changes we have nondefault value. Value is changed if is not equal to old value
            if ( !Preferences.isEqual(old, value) ) {
                en.markAsChanged();
            }
            data.add(en);
        }
        for (Entry<String, Setting> e : defaults.entrySet()) {
            if (!loaded.containsKey(e.getKey())) {
                PrefEntry en = new PrefEntry(e.getKey(), e.getValue(), e.getValue(), true);
                // after changes we have default value. So, value is changed if old value is not default
                Setting old = orig.get(e.getKey());
                if ( old!=null ) {
                    en.markAsChanged();
                }
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

    private static class SettingCellRenderer extends DefaultTableCellRenderer {
        private Color backgroundColor = Main.pref.getUIColor("Table.background");
        private Color changedColor = Main.pref.getColor(
                         marktr("Advanced Background: Changed"),
                         new Color(200,255,200));
        private Color foregroundColor = Main.pref.getUIColor("Table.foreground");
        private Color nonDefaultColor = Main.pref.getColor(
                            marktr("Advanced Background: NonDefalut"),
                            new Color(255,255,200));
        
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
            super(new JTextField());
        }

        @Override
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
                    @SuppressWarnings("unchecked")
                    Collection<Collection<String>> llSettingValue = (Collection) llSetting.getValue();
                    if (!Preferences.equalArray(llSettingValue, data)) {
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
                @SuppressWarnings("unchecked")
                Collection<Collection<String>> stgValue = (Collection) stg.getValue();
                if (!Preferences.equalArray(stgValue, data)) {
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
