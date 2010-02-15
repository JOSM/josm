// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the tables of {@see RelationMemberMerger}.
 *
 */
public  class RelationMemberTableCellRenderer extends JLabel implements TableCellRenderer {
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SAME_POSITION_IN_OPPOSITE = new Color(217,255,217);

    public final static Color BGCOLOR_PARTICIPAING_IN_COMPARISON = Color.BLACK;
    public final static Color FGCOLOR_PARTICIPAING_IN_COMPARISON = Color.WHITE;

    public final static Color BGCOLOR_FROZEN = new Color(234,234,234);

    private ImageIcon nodeIcon;
    private ImageIcon wayIcon;
    private ImageIcon relationIcon;
    private  Border rowNumberBorder = null;

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
        rowNumberBorder = BorderFactory.createEmptyBorder(0,4,0,0);
    }

    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
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

    protected void renderBackground(ListMergeModel<Node>.EntriesTableModel model, RelationMember member, int row, int col, boolean isSelected) {
        Color bgc = Color.WHITE;
        if (col == 0) {
            if (model.getListMergeModel().isFrozen()) {
                bgc = BGCOLOR_FROZEN;
            } else if (model.isParticipatingInCurrentComparePair()) {
                bgc = BGCOLOR_PARTICIPAING_IN_COMPARISON;
            } else if (isSelected) {
                bgc = BGCOLOR_SELECTED;
            }
        } else {
            if (model.getListMergeModel().isFrozen()) {
                bgc = BGCOLOR_FROZEN;
            } else if (member == null) {
                bgc = BGCOLOR_EMPTY_ROW;
            } else if (isSelected) {
                bgc = BGCOLOR_SELECTED;
            } else {
                if (model.isParticipatingInCurrentComparePair()) {
                    if (model.isSamePositionInOppositeList(row)) {
                        bgc = BGCOLOR_SAME_POSITION_IN_OPPOSITE;
                    } else if (model.isIncludedInOppositeList(row)) {
                        bgc = BGCOLOR_IN_OPPOSITE;
                    } else {
                        bgc = BGCOLOR_NOT_IN_OPPOSITE;
                    }
                }
            }
        }
        setBackground(bgc);
    }

    protected void renderForeground(ListMergeModel<Node>.EntriesTableModel model, RelationMember member, int row, int col, boolean isSelected) {
        Color fgc = Color.BLACK;
        if (col == 0 && model.isParticipatingInCurrentComparePair() && ! model.getListMergeModel().isFrozen()) {
            fgc = Color.WHITE;
        }
        setForeground(fgc);
    }

    protected void renderRole(RelationMember member) {
        setText(member.getRole());
        setToolTipText(member.getRole());
    }

    protected void renderPrimitive(RelationMember member) {
        String displayName = member.getMember().getDisplayName(DefaultNameFormatter.getInstance());
        setText(displayName);
        setToolTipText(buildToolTipText(member.getMember()));
        if (member.isNode()) {
            setIcon(nodeIcon);
        } else if (member.isWay()) {
            setIcon(wayIcon);
        } else if (member.isRelation()) {
            setIcon(relationIcon);
        } else {
            // should not happen
            setIcon(null);
        }
    }

    /**
     * render the row id
     * @param row the row index
     * @param isSelected
     */
    protected  void renderRowId(int row) {
        setBorder(rowNumberBorder);
        setText(Integer.toString(row+1));
    }

    protected void renderEmptyRow() {
        setIcon(null);
        setBackground(BGCOLOR_EMPTY_ROW);
        setText("");
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        RelationMember member = (RelationMember)value;
        reset();
        renderBackground(getModel(table), member, row, column, isSelected);
        renderForeground(getModel(table), member, row, column, isSelected);
        switch(column) {
        case 0:
            renderRowId(row);
            break;
        case 1:
            if (member == null) {
                renderEmptyRow();
            } else {
                renderRole(member);
            }
            break;
        case 2:
            if (member == null) {
                renderEmptyRow();
            } else {
                renderPrimitive(member);
            }
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
    @SuppressWarnings("unchecked")
    protected ListMergeModel<Node>.EntriesTableModel getModel(JTable table) {
        return (ListMergeModel.EntriesTableModel)table.getModel();
    }
}
