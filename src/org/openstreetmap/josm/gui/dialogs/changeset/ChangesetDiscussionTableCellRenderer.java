// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.awt.Component;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;

/**
 * The cell renderer for the changeset discussion table
 * @since 7715
 */
public class ChangesetDiscussionTableCellRenderer extends AbstractCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (value == null)
            return this;
        JComponent comp = this;
        reset(comp, true);
        renderColors(comp, isSelected);
        switch(column) {
        case 0:
            renderDate((Date) value);
            break;
        case 1:
            renderUser((User) value);
            break;
        case 2:
            comp = new JosmTextArea((String) value);
            ((JosmTextArea) comp).setLineWrap(true);
            ((JosmTextArea) comp).setWrapStyleWord(true);
            reset(comp, false);
            renderColors(comp, isSelected);
            break;
        default: // Do nothing
        }
        return comp;
    }
}
