// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@link TableCellRenderer} used in the node tables of {@link NodeListMerger}.
 *
 */
public  class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {

    private final ImageIcon icon;
    private final Border rowNumberBorder;

    /**
     * constructor
     */
    public NodeListTableCellRenderer() {
        icon = ImageProvider.get("data", "node");
        rowNumberBorder = BorderFactory.createEmptyBorder(0,4,0,0);
        setOpaque(true);
    }

    /**
     * build the tool tip text for an {@link OsmPrimitive}. It consist of the formatted
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

    /**
     * render a node
     * @param model  the model
     * @param node the node
     * @param isSelected true, if the current row is selected
     */
    protected  void renderNode(ListMergeModel<Node>.EntriesTableModel model, Node node, int row, boolean isSelected) {
        setIcon(icon);
        setBorder(null);
        if (model.getListMergeModel().isFrozen()) {
            setBackground(ConflictColors.BGCOLOR_FROZEN.get());
        } else if (isSelected) {
            setBackground(ConflictColors.BGCOLOR_SELECTED.get());
        } else if (model.isParticipatingInCurrentComparePair()) {
            if (model.isSamePositionInOppositeList(row)) {
                setBackground(ConflictColors.BGCOLOR_SAME_POSITION_IN_OPPOSITE.get());
            } else if (model.isIncludedInOppositeList(row)) {
                setBackground(ConflictColors.BGCOLOR_IN_OPPOSITE.get());
            } else {
                setBackground(ConflictColors.BGCOLOR_NOT_IN_OPPOSITE.get());
            }
        }
        setText(node.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(buildToolTipText(node));
    }

    /**
     * render an empty row
     */
    protected void renderEmptyRow() {
        setIcon(null);
        setBackground(ConflictColors.BGCOLOR_EMPTY_ROW.get());
        setText("");
    }

    /**
     * render the row id
     * @param model  the model
     * @param row the row index
     * @param isSelected true, if the current row is selected
     */
    protected  void renderRowId( ListMergeModel<Node>.EntriesTableModel model, int row, boolean isSelected) {
        setIcon(null);
        setBorder(rowNumberBorder);
        if (model.getListMergeModel().isFrozen()) {
            setBackground(ConflictColors.BGCOLOR_FROZEN.get());
        } else if (model.isParticipatingInCurrentComparePair()) {
            setBackground(ConflictColors.BGCOLOR_PARTICIPATING_IN_COMPARISON.get());
            setForeground(ConflictColors.FGCOLOR_PARTICIPATING_IN_COMPARISON.get());
        }
        setText(Integer.toString(row+1));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Node node = (Node)value;
        reset();
        if (node == null) {
            renderEmptyRow();
        } else {
            switch(column) {
            case 0:
                renderRowId(getModel(table),row, isSelected);
                break;
            case 1:
                renderNode(getModel(table), node, row, isSelected);
                break;
            default:
                // should not happen
                throw new RuntimeException(MessageFormat.format("Unexpected column index. Got {0}.", column));
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
