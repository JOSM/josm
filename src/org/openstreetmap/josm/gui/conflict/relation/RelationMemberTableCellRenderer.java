// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the tables of {@see RelationMemberMerger}.
 * 
 *
 */
public  class RelationMemberTableCellRenderer extends JLabel implements TableCellRenderer {
    private static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000");
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    private ImageIcon nodeIcon;
    private ImageIcon wayIcon;
    private ImageIcon relationIcon;

    /**
     * Load the image icon for an OSM primitive of type node
     * 
     * @return the icon; null, if not found
     */
    protected void loadIcons() {
        nodeIcon = ImageProvider.get("data", "node");
        wayIcon = ImageProvider.get("data", "way");
        relationIcon = ImageProvider.get("data", "relation");
    }

    /**
     * constructor
     */
    public RelationMemberTableCellRenderer() {
        setIcon(null);
        setOpaque(true);
        loadIcons();
    }

    /**
     * creates the display name for a node. The name is derived from the nodes id,
     * its name (i.e. the value of the tag with key name) and its coordinates.
     * 
     * @param node  the node
     * @return the display name
     */
    protected String getDisplayName(RelationMember member) {
        StringBuilder sb = new StringBuilder();
        OsmPrimitive primitive = member.member;
        if (primitive instanceof Node) {
            sb.append(tr("Node"));
        } else if (primitive instanceof Way) {
            sb.append(tr("Way"));
        } else if (primitive instanceof Relation) {
            sb.append(tr("Relation"));
        }
        sb.append(" ");
        if (primitive.get("name") != null) {
            sb.append(primitive.get("name"));
            sb.append("/");
            sb.append(primitive.id);
        } else {
            sb.append(primitive.id);
        }

        if (primitive instanceof Node) {
            Node n = (Node)primitive;
            sb.append(" (");
            if (n.coor != null) {
                sb.append(COORD_FORMATTER.format(n.coor.lat()));
                sb.append(",");
                sb.append(COORD_FORMATTER.format(n.coor.lon()));
            } else {
                sb.append("?,?");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * reset the renderer
     */
    protected void reset() {
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }


    protected void setBackground(boolean isSelected) {
        Color bgc = isSelected ?  BGCOLOR_SELECTED : Color.WHITE;
        setBackground(bgc);
    }

    protected void renderRole(RelationMember member) {
        setText(member.role == null ? "" : member.role);
        setIcon(null);
    }

    protected void renderPrimitive(RelationMember member) {
        String displayName = getDisplayName(member);
        setText(displayName);
        setToolTipText(displayName);
        if (member.member instanceof Node) {
            setIcon(nodeIcon);
        } else if (member.member instanceof Way) {
            setIcon(wayIcon);
        } else if (member.member instanceof Relation) {
            setIcon(relationIcon);
        } else {
            // should not happen
            setIcon(null);
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        RelationMember member = (RelationMember)value;
        reset();
        setBackground(isSelected);
        switch(column) {
        case 0:
            renderRole(member);
            break;
        case 1:
            renderPrimitive(member);
            break;
        default:
            // should not happen
        }
        return this;
    }

}
