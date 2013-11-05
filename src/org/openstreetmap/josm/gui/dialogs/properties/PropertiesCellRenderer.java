// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIDefaults;
import javax.swing.table.DefaultTableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Cell renderer of tags table.
 * @since 6314
 */
public class PropertiesCellRenderer extends DefaultTableCellRenderer {

    private void setColors(Component c, String key, boolean isSelected) {
        UIDefaults defaults = javax.swing.UIManager.getDefaults();
        if (OsmPrimitive.getDiscardableKeys().contains(key)) {
            if (isSelected) {
                c.setForeground(Main.pref.getColor(marktr("Discardable key: selection Foreground"), Color.GRAY));
                c.setBackground(Main.pref.getColor(marktr("Discardable key: selection Background"), defaults.getColor("Table.selectionBackground")));
            } else {
                c.setForeground(Main.pref.getColor(marktr("Discardable key: foreground"), Color.GRAY));
                c.setBackground(Main.pref.getColor(marktr("Discardable key: background"), defaults.getColor("Table.background")));
            }
        } else {
            c.setForeground(defaults.getColor("Table."+(isSelected ? "selectionF" : "f")+"oreground"));
            c.setBackground(defaults.getColor("Table."+(isSelected ? "selectionB" : "b")+"ackground"));
        }
    }
    
    @Override 
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        if (value == null)
            return this;
        if (c instanceof JLabel) {
            String str = null;
            if (value instanceof String) {
                str = (String) value;
            } else if (value instanceof Map<?, ?>) {
                Map<?, ?> v = (Map<?, ?>) value;
                if (v.size() != 1) {
                    str=tr("<different>");
                    c.setFont(c.getFont().deriveFont(Font.ITALIC));
                } else {
                    final Map.Entry<?, ?> entry = v.entrySet().iterator().next();
                    str = (String) entry.getKey();
                }
            }
            ((JLabel)c).putClientProperty("html.disable", Boolean.TRUE); // Fix #8730
            ((JLabel)c).setText(str);
            if (Main.pref.getBoolean("display.discardable-keys", false)) {
                String key = null;
                if (column == 0) {
                    key = str;
                } else if (column == 1) {
                    Object value0 = table.getModel().getValueAt(row, 0);
                    if (value0 instanceof String) {
                        key = (String) value0;
                    }
                }
                setColors(c, key, isSelected);
            }
        }
        return c;
    }
}
