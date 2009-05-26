// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import static org.openstreetmap.josm.tools.I18n.tr;

public abstract class TagMergeTableCellRenderer extends JLabel implements TableCellRenderer {

    protected  abstract void renderKey(TagMergeItem item, boolean isSelected );
    
    protected abstract void renderValue(TagMergeItem item, boolean isSelected);
    
    protected void reset() {
        setOpaque(true);
        setBackground(Color.white);
        setForeground(Color.black);
    }
    
    
    @Override
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
            throw new IllegalArgumentException(tr("parameter 'col' must be 0 or 1. Got " + col));
        }
        return this;
    }
    
}
