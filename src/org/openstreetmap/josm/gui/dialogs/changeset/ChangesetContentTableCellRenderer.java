// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;

/**
 * The table cell renderer used in the changeset content table, except for the "name"
 * column in which we use a {@link org.openstreetmap.josm.gui.OsmPrimitivRenderer}.
 *
 */
public class ChangesetContentTableCellRenderer extends JLabel implements TableCellRenderer{

    public ChangesetContentTableCellRenderer() {
        setOpaque(true);
    }

    protected void reset() {
        setBackground(UIManager.getColor("Table.background"));
        setForeground(UIManager.getColor("Table.foreground"));
        setFont(UIManager.getFont("Table.font"));
    }

    protected void renderColors(boolean isSelected) {
        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background"));
            setForeground(UIManager.getColor("Table.foreground"));
        }
    }

    protected void renderId(HistoryOsmPrimitive primitive) {
        setText(Long.toString(primitive.getId()));
        setToolTipText("");
    }

    protected void renderModificationType(ChangesetModificationType type) {
        switch(type) {
        case CREATED: setText(tr("Created")); break;
        case UPDATED: setText(tr("Updated")); break;
        case DELETED: setText(tr("Deleted")); break;
        }
        setToolTipText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (value == null)
            return this;
        reset();
        renderColors(isSelected);
        switch(column) {
        case 0:
            ChangesetModificationType type = (ChangesetModificationType)value;
            renderModificationType(type);
            break;
        case 1:
            HistoryOsmPrimitive primitive = (HistoryOsmPrimitive)value;
            renderId(primitive);
            break;
        default:
            /* do nothing */
        }
        return this;
    }
}
