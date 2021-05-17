// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.util.GuiHelper;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * The {@link TableCellRenderer} for a list of tags in {@link HistoryBrowser}
 *
 */
public class TagTableCellRenderer extends JLabel implements TableCellRenderer {
    /**
     * The background color for a selected row that has the focus.
     */
    public static final Color BGCOLOR_SELECTED_FOCUS = new Color(0xff8faaff);
    /**
     * The background color for a selected row while the table is not focused.
     */
    public static final Color BGCOLOR_SELECTED = new Color(0xffafc2ff);

    /**
     * Constructs a new {@code TagTableCellRenderer}.
     */
    public TagTableCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (value == null)
            return this;

        String key = (String) value;
        TagTableModel model = getTagTableModel(table);

        String text = "";
        String tooltip = null;
        setBorder(null);
        if (model.hasTag(key)) {
            switch(column) {
            case TagTableColumnModel.COLUMN_KEY:
                // the name column
                text = key;
                break;
            case TagTableColumnModel.COLUMN_VALUE:
                // the value column
                text = model.getValue(key);
                break;
            case TagTableColumnModel.COLUMN_VERSION:
                HistoryOsmPrimitive primitive = model.getWhichChangedTag(key);
                if (primitive != null) {
                    text = model.getVersionString(primitive);
                    tooltip = tr("Key ''{0}'' was changed in version {1}", key, primitive.getVersion());
                    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, model.getVersionColor(primitive)));
                }
            default: // Do nothing
            }
        }

        setText(text);
        setToolTipText(tooltip != null ? tooltip : text);
        setHorizontalAlignment(column == TagTableColumnModel.COLUMN_VERSION ? SwingConstants.TRAILING : SwingConstants.LEADING);
        TwoColumnDiff.Item.DiffItemType diffItemType = model.getDiffItemType(key, column == TagTableColumnModel.COLUMN_VALUE);
        GuiHelper.setBackgroundReadable(this, diffItemType.getColor(isSelected, table.hasFocus()));
        return this;
    }

    protected TagTableModel getTagTableModel(JTable table) {
        return (TagTableModel) table.getModel();
    }
}
