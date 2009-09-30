// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.MapPaintVisitor;
import org.openstreetmap.josm.gui.MapScaler;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

public class ColorPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ColorPreference();
        }
    }


    private DefaultTableModel tableModel;
    private JTable colors;
    private ArrayList<String> del = new ArrayList<String>();

    JButton colorEdit;
    JButton defaultSet;
    JButton remove;

    /**
     * Set the colors to be shown in the preference table. This method creates a table model if
     * none exists and overwrites all existing values.
     * @param colorMap the map holding the colors
     * (key = color id (without prefixes, so only <code>background</code>; not <code>color.background</code>),
     * value = html representation of the color.
     */
    public void setColorModel(Map<String, String> colorMap) {
        if(tableModel == null) {
            tableModel = new DefaultTableModel();
            tableModel.addColumn(tr("Name"));
            tableModel.addColumn(tr("Color"));
        }

        // clear old model:
        while(tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        // fill model with colors:
        Map<String, String> colorKeyList = new TreeMap<String, String>();
        Map<String, String> colorKeyList_mappaint = new TreeMap<String, String>();
        Map<String, String> colorKeyList_layer = new TreeMap<String, String>();
        for(String key : colorMap.keySet()) {
            if(key.startsWith("layer ")) {
                colorKeyList_layer.put(getName(key), key);
            } else if(key.startsWith("mappaint.")) {
                colorKeyList_mappaint.put(getName(key), key);
            } else {
                colorKeyList.put(getName(key), key);
            }
        }
        for (Entry<String, String> k : colorKeyList.entrySet()) {
            Vector<Object> row = new Vector<Object>(2);
            row.add(k.getValue());
            row.add(ColorHelper.html2color(colorMap.get(k.getValue())));
            tableModel.addRow(row);
        }
        for (Entry<String, String> k : colorKeyList_mappaint.entrySet()) {
            Vector<Object> row = new Vector<Object>(2);
            row.add(k.getValue());
            row.add(ColorHelper.html2color(colorMap.get(k.getValue())));
            tableModel.addRow(row);
        }
        for (Entry<String, String> k : colorKeyList_layer.entrySet()) {
            Vector<Object> row = new Vector<Object>(2);
            row.add(k.getValue());
            row.add(ColorHelper.html2color(colorMap.get(k.getValue())));
            tableModel.addRow(row);
        }
        if(this.colors != null) {
            this.colors.repaint();
        }
    }

    /**
     * Returns a map with the colors in the table (key = color name without prefix, value = html color code).
     * @return a map holding the colors.
     */
    public Map<String, String> getColorModel() {
        String key;
        String value;
        Map<String, String> colorMap = new HashMap<String, String>();
        for(int row = 0; row < tableModel.getRowCount(); ++row) {
            key = (String)tableModel.getValueAt(row, 0);
            value = ColorHelper.color2html((Color)tableModel.getValueAt(row, 1));
            colorMap.put(key, value);
        }
        return colorMap;
    }

    private String getName(String o)
    {
        try
        {
            Matcher m = Pattern.compile("mappaint\\.(.+?)\\.(.+)").matcher(o);
            m.matches();
            return tr("Paint style {0}: {1}", tr(m.group(1)), tr(m.group(2)));
        }
        catch (Exception e) {}
        try
        {
            Matcher m = Pattern.compile("layer (.+)").matcher(o);
            m.matches();
            return tr("Layer: {0}", tr(m.group(1)));
        }
        catch (Exception e) {}
        return tr(o);
    }

    public void addGui(final PreferenceDialog gui) {
        fixColorPrefixes();
        setColorModel(Main.pref.getAllColors());

        colorEdit = new JButton(tr("Choose"));
        colorEdit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int sel = colors.getSelectedRow();
                JColorChooser chooser = new JColorChooser((Color)colors.getValueAt(sel, 1));
                int answer = JOptionPane.showConfirmDialog(
                        gui, chooser,
                        tr("Choose a color for {0}", getName((String)colors.getValueAt(sel, 0))),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    colors.setValueAt(chooser.getColor(), sel, 1);
                }
            }
        });
        defaultSet = new JButton(tr("Set to default"));
        defaultSet.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int sel = colors.getSelectedRow();
                String name = (String)colors.getValueAt(sel, 0);
                Color c = Main.pref.getDefaultColor(name);
                if (c != null) {
                    colors.setValueAt(c, sel, 1);
                }
            }
        });
        JButton defaultAll = new JButton(tr("Set all to default"));
        defaultAll.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                for(int i = 0; i < colors.getRowCount(); ++i)
                {
                    String name = (String)colors.getValueAt(i, 0);
                    Color c = Main.pref.getDefaultColor(name);
                    if (c != null) {
                        colors.setValueAt(c, i, 1);
                    }
                }
            }
        });
        remove = new JButton(tr("Remove"));
        remove.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int sel = colors.getSelectedRow();
                del.add((String)colors.getValueAt(sel, 0));
                tableModel.removeRow(sel);
            }
        });
        remove.setEnabled(false);
        colorEdit.setEnabled(false);
        defaultSet.setEnabled(false);

        colors = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override public void valueChanged(ListSelectionEvent e) {
                super.valueChanged(e);
                int sel = getSelectedRow();
                remove.setEnabled(sel >= 0 && isRemoveColor(sel));
                colorEdit.setEnabled(sel >= 0);
                defaultSet.setEnabled(sel >= 0);
            }
        };
        colors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final TableCellRenderer oldColorsRenderer = colors.getDefaultRenderer(Object.class);
        colors.setDefaultRenderer(Object.class, new TableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t, Object o, boolean selected, boolean focus, int row, int column) {
                if (column == 1) {
                    JLabel l = new JLabel(ColorHelper.color2html((Color)o));
                    l.setBackground((Color)o);
                    l.setOpaque(true);
                    return l;
                }
                return oldColorsRenderer.getTableCellRendererComponent(t,getName(o.toString()),selected,focus,row,column);
            }
        });
        colors.getColumnModel().getColumn(1).setWidth(100);
        colors.setToolTipText(tr("Colors used by different objects in JOSM."));
        colors.setPreferredScrollableViewportSize(new Dimension(100,112));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JScrollPane scrollpane = new JScrollPane(colors);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        panel.add(scrollpane, GBC.eol().fill(GBC.BOTH));
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        panel.add(buttonPanel, GBC.eol().insets(5,0,5,5).fill(GBC.HORIZONTAL));
        buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
        buttonPanel.add(colorEdit, GBC.std().insets(0,5,0,0));
        buttonPanel.add(defaultSet, GBC.std().insets(5,5,5,0));
        buttonPanel.add(defaultAll, GBC.std().insets(0,5,0,0));
        buttonPanel.add(remove, GBC.std().insets(0,5,0,0));
        gui.displaycontent.addTab(tr("Colors"), panel);
    }

    Boolean isRemoveColor(int row)
    {
        return ((String)colors.getValueAt(row, 0)).startsWith("layer ");
    }

    /**
     * Add all missing color entries.
     */
    private void fixColorPrefixes() {
        (new MapPaintVisitor()).getColors();
        MarkerLayer.getColor(null);
        MapScaler.getColor();
        ConflictDialog.getColor();
    }

    public boolean ok() {
        Boolean ret = false;
        for(String d : del) {
            Main.pref.put("color."+d, null);
        }
        for (int i = 0; i < colors.getRowCount(); ++i) {
            String key = (String)colors.getValueAt(i, 0);
            if(Main.pref.putColor(key, (Color)colors.getValueAt(i, 1)))
            {
                if(key.startsWith("mappaint.")) {
                    ret = true;
                }
            }
        }
        org.openstreetmap.josm.gui.layer.OsmDataLayer.createHatchTexture();
        return ret;
    }
}
