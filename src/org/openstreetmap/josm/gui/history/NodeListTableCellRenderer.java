// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item.DiffItemType;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A {@link TableCellRenderer} for the {@link NodeListViewer}.
 * 
 * Renders information about a node when comparing the node list of two
 * historical versions of a way.
 */
public class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {

    public static final Color BGCOLOR_SELECTED = new Color(143, 170, 255);

    private final ImageIcon nodeIcon;

    /**
     * Constructs a new {@code NodeListTableCellRenderer}.
     */
    public NodeListTableCellRenderer() {
        setOpaque(true);
        nodeIcon = ImageProvider.get("data", "node");
        setIcon(nodeIcon);
    }

    protected void renderIndex(DiffTableModel model, int row) {
        int rowNumber = model.getRowNumber(row);
        setText(rowNumber > 0 ? Integer.toString(rowNumber) : "");
        setToolTipText(null);
        setHorizontalAlignment(CENTER);
        setIcon(null);
    }

    protected void renderNode(TwoColumnDiff.Item item) {
        String text = "";
        setIcon(nodeIcon);
        if (item.value != null) {
            text = tr("Node {0}", item.value.toString());
        }
        if (item.state == DiffItemType.EMPTY) {
            text = "";
            setIcon(null);
        }
        setText(text);
        setHorizontalAlignment(LEFT);
    }

    // Warning: The model pads with null-rows to match the size of the opposite table. 'value' could be null
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (value == null) return this;
        TwoColumnDiff.Item item = (TwoColumnDiff.Item) value;
        GuiHelper.setBackgroundReadable(this, item.state.getColor(isSelected, hasFocus));
        switch(column) {
        case NodeListTableColumnModel.INDEX_COLUMN:
            renderIndex((DiffTableModel) table.getModel(), row);
            break;
        case NodeListTableColumnModel.NODE_COLUMN:
            renderNode(item);
            break;
        }
        return this;
    }
}
