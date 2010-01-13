// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public abstract class TagMergeTableCellRenderer extends JLabel implements TableCellRenderer {

    protected  abstract void renderKey(TagMergeItem item, boolean isSelected );

    protected abstract void renderValue(TagMergeItem item, boolean isSelected);

    protected void reset() {
        setOpaque(true);
        setBackground(Color.white);
        setForeground(Color.black);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
            int col) {

        reset();
        TagMergeItem item = (TagMergeItem)value;
        switch(col) {
        case 0:
            renderKey(item, isSelected);
            break;
        case 1:
            renderValue(item, isSelected);
            break;
        default:
            // should not happen, but just in case
            throw new IllegalArgumentException(MessageFormat.format("Parameter 'col' must be 0 or 1. Got {0}.", col));
        }
        return this;
    }

}
