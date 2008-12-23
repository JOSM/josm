// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.BorderFactory;
import javax.swing.Box;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

public class ColorPreference implements PreferenceSetting {

    private DefaultTableModel tableModel;
    private JTable colors;

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
            tableModel.addColumn(tr("Color"));
            tableModel.addColumn(tr("Name"));
        }

        // clear old model:
        while(tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        // fill model with colors:
        List<String> colorKeyList = new ArrayList<String>();
        for(String key : colorMap.keySet()) {
            colorKeyList.add(key);
        }
        Collections.sort(colorKeyList);
        for (String key : colorKeyList) {
            Vector<Object> row = new Vector<Object>(2);
            row.add(key);
            row.add(ColorHelper.html2color(colorMap.get(key)));
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

    public void addGui(final PreferenceDialog gui) {
        // initial fill with colors from preferences:
        Map<String,String> prefColorMap = new TreeMap<String, String>(Main.pref.getAllPrefix("color."));
        fixColorPrefixes(prefColorMap);
        Map<String,String> colorMap = new TreeMap<String, String>();
        for(String key : prefColorMap.keySet()) {
            colorMap.put(key.substring("color.".length()), prefColorMap.get(key));
        }
        setColorModel(colorMap);

        colors = new JTable(tableModel) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
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
                return oldColorsRenderer.getTableCellRendererComponent(t,tr(o.toString()),selected,focus,row,column);
            }
        });
        colors.getColumnModel().getColumn(1).setWidth(100);

        JButton colorEdit = new JButton(tr("Choose"));
        colorEdit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (colors.getSelectedRowCount() == 0) {
                    JOptionPane.showMessageDialog(gui, tr("Please select a color."));
                    return;
                }
                int sel = colors.getSelectedRow();
                JColorChooser chooser = new JColorChooser((Color)colors.getValueAt(sel, 1));
                int answer = JOptionPane.showConfirmDialog(gui, chooser, tr("Choose a color for {0}", colors.getValueAt(sel, 0)), JOptionPane.OK_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION)
                    colors.setValueAt(chooser.getColor(), sel, 1);
            }
        });
        colors.setToolTipText(tr("Colors used by different objects in JOSM."));
        colors.setPreferredScrollableViewportSize(new Dimension(100,112));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JScrollPane scrollpane = new JScrollPane(colors);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        panel.add(scrollpane, GBC.eol().fill(GBC.BOTH));
        panel.add(colorEdit, GBC.eol().anchor(GBC.EAST));
        gui.displaycontent.addTab(tr("Colors"), panel);
    }

    /**
     * Add all missing color entries.
     */
    private void fixColorPrefixes(Map<String, String> prefColorMap) {
        String[] cp = {
            marktr("background"), ColorHelper.color2html(Color.black),
            marktr("node"), ColorHelper.color2html(Color.red),
            marktr("way"), ColorHelper.color2html(SimplePaintVisitor.darkblue),
            marktr("incomplete way"), ColorHelper.color2html(SimplePaintVisitor.darkerblue),
            marktr("relation"), ColorHelper.color2html(SimplePaintVisitor.teal),
            marktr("selected"), ColorHelper.color2html(Color.white),
            marktr("gps marker"), ColorHelper.color2html(Color.gray),
            marktr("gps point"), ColorHelper.color2html(Color.gray),
            marktr("conflict"), ColorHelper.color2html(Color.gray),
            marktr("scale"), ColorHelper.color2html(Color.white),
            marktr("inactive"), ColorHelper.color2html(Color.darkGray),
        };
        for (int i = 0; i < cp.length/2; ++i)
        {
            if (!Main.pref.hasKey("color."+cp[i*2]))
                Main.pref.put("color."+cp[i*2], cp[i*2+1]);
            Main.pref.putDefault("color."+cp[i*2], cp[i*2+1]);
        }
    }

    public void ok() {
        for (int i = 0; i < colors.getRowCount(); ++i) {
            String name = (String)colors.getValueAt(i, 0);
            Color col = (Color)colors.getValueAt(i, 1);
            Main.pref.put("color." + name, ColorHelper.color2html(col));
        }
        org.openstreetmap.josm.gui.layer.OsmDataLayer.createHatchTexture();
    }
}
