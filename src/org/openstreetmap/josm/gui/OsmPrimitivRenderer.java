// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 *
 * Can be used in lists and tables.
 *
 * @author imi
 * @author Frederik Ramm <frederik@remote.org>
 */
public class OsmPrimitivRenderer implements ListCellRenderer, TableCellRenderer {
    /**
     * Default list cell renderer - delegate for ListCellRenderer operation
     */
    private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();

    /**
     * Default table cell renderer - delegate for TableCellRenderer operation
     */
    private DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

    /**
     * Adapter method supporting the ListCellRenderer interface.
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component def = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        return renderer(def, (OsmPrimitive) value);
    }

    /**
     * Adapter method supporting the TableCellRenderer interface.
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component def = defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        return renderer(def, (OsmPrimitive) value);
    }

    /**
     * Internal method that stuffs information into the rendering component
     * provided that it's a kind of JLabel.
     * @param def the rendering component
     * @param value the OsmPrimtive to render
     * @return the modified rendering component
     */
    private Component renderer(Component def, OsmPrimitive value) {
        if (def != null && value != null && def instanceof JLabel) {
            ((JLabel)def).setText(value.getDisplayName(DefaultNameFormatter.getInstance()));
            ((JLabel)def).setIcon(ImageProvider.get(OsmPrimitiveType.from(value)));
            ((JLabel)def).setToolTipText(buildToolTipText(value));
        }
        return def;
    }

    /**
     * build the tool tip text for an {@see OsmPrimitive}. It consist of the formatted
     * key/value pairs for this primitive.
     * 
     * @param primitive
     * @return the tool tip text
     */
    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        // show the id
        //
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
        .append("<br>");

        // show the key/value-pairs, sorted by key
        //
        ArrayList<String> keyList = new ArrayList<String>(primitive.keySet());
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            if (i > 0) {
                sb.append("<br>");
            }
            String key = keyList.get(i);
            sb.append("<strong>")
            .append(key)
            .append("</strong>")
            .append("=");
            // make sure long values are split into several rows. Otherwise
            // the tool tip window can become to wide
            //
            String value = primitive.get(key);
            while(value.length() != 0) {
                sb.append(value.substring(0,Math.min(50, value.length())));
                if (value.length() > 50) {
                    sb.append("<br>");
                    value = value.substring(50);
                } else {
                    value = "";
                }
            }
        }
        sb.append("</html>");
        return sb.toString();
    }
}
