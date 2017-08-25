// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import java.awt.Component;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.command.conflict.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMergeModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@link TableCellRenderer} used in the node tables of {@link NodeListMerger}.
 * @since 1622
 */
public class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {

    private final ImageIcon icon;
    private final transient Border rowNumberBorder;

    /**
     * constructor
     */
    public NodeListTableCellRenderer() {
        icon = ImageProvider.get("data", "node");
        rowNumberBorder = BorderFactory.createEmptyBorder(0, 4, 0, 0);
        setOpaque(true);
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
     * @param model the model
     * @param node the node
     * @param row the row
     * @param isSelected true, if the current row is selected
     */
    protected void renderNode(AbstractListMergeModel<Node, WayNodesConflictResolverCommand>.EntriesTableModel model, Node node,
            int row, boolean isSelected) {
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
        setToolTipText(DefaultNameFormatter.getInstance().buildDefaultToolTip(node));
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
     * @param model the model
     * @param row the row index
     */
    protected void renderRowId(AbstractListMergeModel<Node, WayNodesConflictResolverCommand>.EntriesTableModel model, int row) {
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

        Node node = (Node) value;
        reset();
        if (node == null) {
            renderEmptyRow();
        } else {
            switch(column) {
            case 0:
                renderRowId(getModel(table), row);
                break;
            case 1:
                renderNode(getModel(table), node, row, isSelected);
                break;
            default:
                // should not happen
                throw new IllegalArgumentException(MessageFormat.format("Unexpected column index. Got {0}.", column));
            }
        }
        return this;
    }

    /**
     * replies the model
     * @param table the table
     * @return the table model
     */
    @SuppressWarnings("unchecked")
    protected AbstractListMergeModel<Node, WayNodesConflictResolverCommand>.EntriesTableModel getModel(JTable table) {
        return (AbstractListMergeModel<Node, WayNodesConflictResolverCommand>.EntriesTableModel) table.getModel();
    }
}
