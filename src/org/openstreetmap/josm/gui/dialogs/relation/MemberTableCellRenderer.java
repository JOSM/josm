// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is the {@see TableCellRenderer} used in the tables of {@see RelationMemberMerger}.
 *
 */
public abstract class MemberTableCellRenderer extends JLabel implements TableCellRenderer {
    public final static Color BGCOLOR_SELECTED = new Color(143, 170, 255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234, 234, 234);

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255, 197, 197);
    public final static Color BGCOLOR_DOUBLE_ENTRY = new Color(255, 234, 213);

    /**
     * constructor
     */
    public MemberTableCellRenderer() {
        setIcon(null);
        setOpaque(true);
    }

    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=").append(primitive.getId()).append("<br>");
        ArrayList<String> keyList = new ArrayList<String>(primitive.keySet());
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            if (i > 0) {
                sb.append("<br>");
            }
            String key = keyList.get(i);
            sb.append("<strong>").append(key).append("</strong>").append("=");
            String value = primitive.get(key);
            while (value.length() != 0) {
                sb.append(value.substring(0, Math.min(50, value.length())));
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

    /**
     * reset the renderer
     */
    protected void reset() {
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
        setBorder(null);
        setIcon(null);
        setToolTipText(null);
    }

    protected void renderBackground(MemberTableModel model, OsmPrimitive primitive, boolean isSelected) {
        Color bgc = Color.WHITE;
        if (isSelected) {
            bgc = BGCOLOR_SELECTED;
        } else if (primitive != null && model.getNumMembersWithPrimitive(primitive) > 1) {
            bgc = BGCOLOR_DOUBLE_ENTRY;
        }
        setBackground(bgc);
    }

    protected void renderForeground(boolean isSelected) {
        Color fgc = Color.BLACK;
        setForeground(fgc);
    }

    abstract public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column);

    /**
     * replies the model
     * @param table the table
     * @return the table model
     */
    protected MemberTableModel getModel(JTable table) {
        return (MemberTableModel) table.getModel();
    }
}
