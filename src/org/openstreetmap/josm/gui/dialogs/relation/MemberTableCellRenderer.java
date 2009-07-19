// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the tables of {@see RelationMemberMerger}.
 * 
 */
public  class MemberTableCellRenderer extends JLabel implements TableCellRenderer {
    static private final PrimitiveNameFormatter NAME_FORMATTER = new PrimitiveNameFormatter();

    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_DOUBLE_ENTRY = new Color(255,234,213);


    private HashMap<OsmPrimitiveType, ImageIcon>  icons;

    /**
     * Load the image icon for an OSM primitive of type node
     * 
     * @return the icon; null, if not found
     */
    protected void loadIcons() {
        icons = new HashMap<OsmPrimitiveType, ImageIcon>();
        icons.put(OsmPrimitiveType.NODE,ImageProvider.get("data", "node"));
        icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
        icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
    }

    /**
     * constructor
     */
    public MemberTableCellRenderer() {
        setIcon(null);
        setOpaque(true);
        loadIcons();
    }

    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=")
        .append(primitive.id)
        .append("<br>");
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

    protected void renderBackground( MemberTableModel model, OsmPrimitive primitive, boolean isSelected) {
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

    protected void renderPrimitive(OsmPrimitive primitive) {
        setIcon(icons.get(OsmPrimitiveType.from(primitive)));
        setText(NAME_FORMATTER.getName(primitive));
        setToolTipText(buildToolTipText(primitive));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();

        renderForeground(isSelected);
        switch(column) {
        case 0:
            String role = (String)value;
            renderBackground(getModel(table), null, isSelected);
            setText(role);
            break;
        case 1:
            OsmPrimitive primitive = (OsmPrimitive)value;
            renderBackground(getModel(table), primitive, isSelected);
            renderPrimitive(primitive);
            break;
        case 2:
            setText("");
            renderBackground(getModel(table), null, isSelected);
            break;
        default:
            // should not happen
        }
        return this;
    }

    /**
     * replies the model
     * @param table  the table
     * @return the table model
     */
    protected MemberTableModel getModel(JTable table) {
        return (MemberTableModel)table.getModel();
    }
}
