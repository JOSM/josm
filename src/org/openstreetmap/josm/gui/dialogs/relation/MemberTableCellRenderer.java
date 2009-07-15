// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the tables of {@see RelationMemberMerger}.
 * 
 */
public  class MemberTableCellRenderer extends JLabel implements TableCellRenderer {
    private final static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000");
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SAME_POSITION_IN_OPPOSITE = new Color(217,255,217);

    public final static Color BGCOLOR_PARTICIPAING_IN_COMPARISON = Color.BLACK;
    public final static Color FGCOLOR_PARTICIPAING_IN_COMPARISON = Color.WHITE;

    public final static Color BGCOLOR_FROZEN = new Color(234,234,234);

    private HashMap<OsmPrimitiveType, ImageIcon>  icons;
    private  Border rowNumberBorder = null;

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


    protected void renderBackground( boolean isSelected) {
        Color bgc = Color.WHITE;
        if (isSelected) {
            bgc = BGCOLOR_SELECTED;
        }
        setBackground(bgc);
    }

    protected void renderForeground(boolean isSelected) {
        Color fgc = Color.BLACK;
        setForeground(fgc);
    }

    protected void renderPrimitive(OsmPrimitive primitive) {
        NameVisitor visitor = new NameVisitor();
        primitive.visit(visitor);
        setIcon(icons.get(OsmPrimitiveType.from(primitive)));
        setText(visitor.name);
        setToolTipText(buildToolTipText(primitive));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        renderBackground(isSelected);
        renderForeground(isSelected);
        switch(column) {
        case 0:
            String role = (String)value;
            setText(role);
            break;
        case 1:
            OsmPrimitive primitive = (OsmPrimitive)value;
            renderPrimitive(primitive);
            break;
        case 2:
            setText("");
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
