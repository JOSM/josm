// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class AdvancedPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new AdvancedPreference();
        }
    }

    private Map<String,String> orig;
    private Map<String,String> defaults;
    private DefaultTableModel model;
    protected Map<String, String> data;
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
            public void changedUpdate(DocumentEvent e) {
                action();
            }

            public void insertUpdate(DocumentEvent e) {
                action();
            }

            public void removeUpdate(DocumentEvent e) {
                action();
            }

            private void action() {
                dataToModel();
            }
        });

        model = new DefaultTableModel(new String[]{tr("Key"), tr("Value")},0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
            @Override public void fireTableCellUpdated(int row, int column)
            {
                super.fireTableCellUpdated(row, column);
                if(column == 1)
                {
                    data.put((String) model.getValueAt(row, 0),
                            (String) model.getValueAt(row, 1));
                }
            }

        };
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column)
            {
                JLabel label=new JLabel();
                String s = defaults.get(value);
                if(s != null)
                {
                    if(s.equals(model.getValueAt(row, 1))) {
                        label.setToolTipText(tr("Current value is default."));
                    } else {
                        label.setToolTipText(tr("Default value is ''{0}''.", s));
                    }
                } else {
                    label.setToolTipText(tr("Default value currently unknown (setting has not been used yet)."));
                }
                label.setText((String)value);
                return label;
            }
        };
        final JTable list = new JTable(model);
        list.getColumn(tr("Key")).setCellRenderer(renderer);
        JScrollPane scroll = new JScrollPane(list);
        p.add(scroll, GBC.eol().fill(GBC.BOTH));
        scroll.setPreferredSize(new Dimension(400,200));

        orig = Main.pref.getAllPrefix("");
        defaults = Main.pref.getDefaults();
        orig.remove("osm-server.password");
        defaults.remove("osm-server.password");
        prepareData();
        dataToModel();

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

        JButton delete = new JButton(tr("Delete"));
        p.add(delete, GBC.std().insets(0,5,0,0));
        delete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                removePreference(gui, list);
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

    private void prepareData() {
        TreeSet<String> ts = new TreeSet<String>(orig.keySet());
        for (String s : defaults.keySet())
        {
            if(!ts.contains(s)) {
                ts.add(s);
            }
        }
        data = new TreeMap<String, String>();
        for (String s : ts)
        {
            String val = Main.pref.get(s);
            if(val == null) {
                val = "";
            }
            data.put(s, val);
        }
    }

    private void dataToModel() {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        for (String prefKey : data.keySet()) {
            String prefValue = data.get(prefKey);
            String input[] = txtFilter.getText().split("\\s+");
            boolean canHas = true;

            // Make 'wmsplugin cache' search for e.g. 'cache.wmsplugin'
            for (String bit : input) {
                if (!prefKey.contains(bit) && !prefValue.contains(bit)) {
                    canHas = false;
                }
            }

            if (canHas) {
                model.addRow(new String[] {prefKey, prefValue});
            }
        }
    }

    public boolean ok() {
        for (String key : data.keySet()) {
            String value = data.get(key);
            if(value.length() != 0)
            {
                String origValue = orig.get(key);
                if (origValue == null || !origValue.equals(value)) {
                    Main.pref.put(key, value);
                }
                orig.remove(key); // processed.
            }
        }
        for (Entry<String, String> e : orig.entrySet()) {
            Main.pref.put(e.getKey(), null);
        }
        return false;
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
        String v = (String) JOptionPane.showInputDialog(
                Main.parent,
                tr("New value for {0}", model.getValueAt(list.getSelectedRow(), 0)),
                tr("New value"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                model.getValueAt(list.getSelectedRow(), 1)
        );
        if (v != null) {
            data.put((String) model.getValueAt(list.getSelectedRow(), 0), v);
            model.setValueAt(v, list.getSelectedRow(), 1);
        }
    }

    private void removePreference(final PreferenceTabbedPane gui, final JTable list) {
        if (list.getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    gui,
                    tr("Please select the row to delete."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        for(int row: list.getSelectedRows()) {
            data.put((String) model.getValueAt(row, 0), "");
            model.setValueAt("", row, 1);
        }
    }

    private void addPreference(final PreferenceTabbedPane gui) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key")), GBC.std().insets(0,0,5,0));
        JTextField key = new JTextField(10);
        JTextField value = new JTextField(10);
        p.add(key, GBC.eop().insets(5,0,0,0).fill(GBC.HORIZONTAL));
        p.add(new JLabel(tr("Value")), GBC.std().insets(0,0,5,0));
        p.add(value, GBC.eol().insets(5,0,0,0).fill(GBC.HORIZONTAL));
        int answer = JOptionPane.showConfirmDialog(
                gui, p,
                tr("Enter a new key/value pair"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (answer == JOptionPane.OK_OPTION) {
            data.put(key.getText(), value.getText());
            model.addRow(new String[]{key.getText(), value.getText()});
        }
    }
}
