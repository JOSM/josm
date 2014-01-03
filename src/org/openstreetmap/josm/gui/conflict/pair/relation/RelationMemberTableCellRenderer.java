// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@link TableCellRenderer} used in the tables of {@link RelationMemberMerger}.
 *
 */
public  class RelationMemberTableCellRenderer extends JLabel implements TableCellRenderer {
    private  Border rowNumberBorder = null;

    /**
     * constructor
     */
    public RelationMemberTableCellRenderer() {
        setIcon(null);
        setOpaque(true);
        rowNumberBorder = BorderFactory.createEmptyBorder(0,4,0,0);
    }

    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
        .append("<br>");
        List<String> keyList = new ArrayList<String>(primitive.keySet());
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
        setBackground(ConflictColors.BGCOLOR.get());
        setForeground(ConflictColors.FGCOLOR.get());
        setBorder(null);
        setIcon(null);
        setToolTipText(null);
    }

    protected void renderBackground(ListMergeModel<Node>.EntriesTableModel model, RelationMember member, int row, int col, boolean isSelected) {
        Color bgc = ConflictColors.BGCOLOR.get();
        if (col == 0) {
            if (model.getListMergeModel().isFrozen()) {
                bgc = ConflictColors.BGCOLOR_FROZEN.get();
            } else if (model.isParticipatingInCurrentComparePair()) {
                bgc = ConflictColors.BGCOLOR_PARTICIPATING_IN_COMPARISON.get();
            } else if (isSelected) {
                bgc = ConflictColors.BGCOLOR_SELECTED.get();
            }
        } else {
            if (model.getListMergeModel().isFrozen()) {
                bgc = ConflictColors.BGCOLOR_FROZEN.get();
            } else if (member == null) {
                bgc = ConflictColors.BGCOLOR_EMPTY_ROW.get();
            } else if (isSelected) {
                bgc = ConflictColors.BGCOLOR_SELECTED.get();
            } else {
                if (model.isParticipatingInCurrentComparePair()) {
                    if (model.isSamePositionInOppositeList(row)) {
                        bgc = ConflictColors.BGCOLOR_SAME_POSITION_IN_OPPOSITE.get();
                    } else if (model.isIncludedInOppositeList(row)) {
                        bgc = ConflictColors.BGCOLOR_IN_OPPOSITE.get();
                    } else {
                        bgc = ConflictColors.BGCOLOR_NOT_IN_OPPOSITE.get();
                    }
                }
            }
        }
        setBackground(bgc);
    }

    protected void renderForeground(ListMergeModel<Node>.EntriesTableModel model, RelationMember member, int row, int col, boolean isSelected) {
        Color fgc = ConflictColors.FGCOLOR.get();
        if (col == 0 && model.isParticipatingInCurrentComparePair() && ! model.getListMergeModel().isFrozen()) {
            fgc = ConflictColors.FGCOLOR_PARTICIPATING_IN_COMPARISON.get();
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
        setIcon(ImageProvider.get(member.getDisplayType()));
    }

    /**
     * render the row id
     * @param row the row index
     */
    protected  void renderRowId(int row) {
        setBorder(rowNumberBorder);
        setText(Integer.toString(row+1));
    }

    protected void renderEmptyRow() {
        setIcon(null);
        setBackground(ConflictColors.BGCOLOR_EMPTY_ROW.get());
        setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        RelationMember member = (RelationMember)value;
        reset();
        if (member == null) {
            renderEmptyRow();
        } else {
            renderBackground(getModel(table), member, row, column, isSelected);
            renderForeground(getModel(table), member, row, column, isSelected);
            switch(column) {
            case 0:
                renderRowId(row);
                break;
            case 1:
                renderRole(member);
                break;
            case 2:
                renderPrimitive(member);
                break;
            default:
                // should not happen
            }
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
