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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class AdvancedPreference implements PreferenceSetting {

    private Map<String,String> orig;
    private Map<String,String> defaults;
    private DefaultTableModel model;

    public void addGui(final PreferenceDialog gui) {
        JPanel p = gui.createPreferenceTab("advanced", tr("Advanced Preferences"),
                tr("Setting Preference entries directly. Use with caution!"), false);

        model = new DefaultTableModel(new String[]{tr("Key"), tr("Value")},0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column)
            {
                JLabel label=new JLabel();
                String s = defaults.get(value);
                if(s != null)
                {
                    if(s.equals(model.getValueAt(row, 1)))
                        label.setToolTipText(tr("Current value is default."));
                    else
                        label.setToolTipText(tr("Default value is ''{0}''.", s));
                }
                else
                    label.setToolTipText(tr("Default value currently unknown (setting has not been used yet)."));
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
        TreeSet<String> ts = new TreeSet<String>(orig.keySet());
        for (String s : defaults.keySet())
        {
            if(!ts.contains(s))
                ts.add(s);
        }

        for (String s : ts)
        {
            String val = Main.pref.get(s);
            if(val == null) val = "";
            model.addRow(new String[]{s, val});
        }

        JButton add = new JButton(tr("Add"));
        p.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
        p.add(add, GBC.std().insets(0,5,0,0));
        add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JLabel(tr("Key")), GBC.std().insets(0,0,5,0));
                JTextField key = new JTextField(10);
                JTextField value = new JTextField(10);
                p.add(key, GBC.eop().insets(5,0,0,0).fill(GBC.HORIZONTAL));
                p.add(new JLabel(tr("Value")), GBC.std().insets(0,0,5,0));
                p.add(value, GBC.eol().insets(5,0,0,0).fill(GBC.HORIZONTAL));
                int answer = JOptionPane.showConfirmDialog(gui, p, tr("Enter a new key/value pair"), JOptionPane.OK_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION)
                    model.addRow(new String[]{key.getText(), value.getText()});
            }
        });

        JButton edit = new JButton(tr("Edit"));
        p.add(edit, GBC.std().insets(5,5,5,0));
        edit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                edit(gui, list);
            }
        });

        JButton delete = new JButton(tr("Delete"));
        p.add(delete, GBC.std().insets(0,5,0,0));
        delete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedRowCount() == 0) {
                    JOptionPane.showMessageDialog(gui, tr("Please select the row to delete."));
                    return;
                }
                for(int row: list.getSelectedRows())
                    model.setValueAt("", row, 1);
            }
        });

        list.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit(gui, list);
            }
        });
    }

    public void ok() {
        for (int i = 0; i < model.getRowCount(); ++i) {
            String value = model.getValueAt(i,1).toString();
            if(value.length() != 0)
            {
                String key = model.getValueAt(i,0).toString();
                String origValue = orig.get(key);
                if (origValue == null || !origValue.equals(value))
                    Main.pref.put(key, value);
                orig.remove(key); // processed.
            }
        }
        for (Entry<String, String> e : orig.entrySet())
            Main.pref.put(e.getKey(), null);
    }


    private void edit(final PreferenceDialog gui, final JTable list) {
        if (list.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(gui, tr("Please select the row to edit."));
            return;
        }
        String v = JOptionPane.showInputDialog(tr("New value for {0}", model.getValueAt(list.getSelectedRow(), 0)), model.getValueAt(list.getSelectedRow(), 1));
        if (v != null)
            model.setValueAt(v, list.getSelectedRow(), 1);
    }
}
