// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.osm.Changeset;

/**
 * The cell renderer for the changeset table
 * @since 2689
 */
public class ChangesetCacheTableCellRenderer extends AbstractCellRenderer {

    protected void renderUploadComment(Changeset cs) {
        String comment = cs.getComment();
        if (comment.trim().isEmpty()) {
            setText(trc("changeset.upload-comment", "empty"));
            setFont(UIManager.getFont("Table.font").deriveFont(Font.ITALIC));
        } else {
            setText(comment);
            setToolTipText(comment);
            setFont(UIManager.getFont("Table.font"));
        }
    }

    protected void renderOpen(Changeset cs) {
        if (cs.isOpen()) {
            setText(trc("changeset.state", "Open"));
        } else {
            setText(trc("changeset.state", "Closed"));
        }
        setToolTipText(null);
    }

    protected void renderDiscussions(Changeset cs) {
        setText(Integer.toString(cs.getCommentsCount()));
        setToolTipText(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (value == null)
            return this;
        reset();
        renderColors(isSelected);
        Changeset cs = (Changeset) value;
        switch(column) {
        case 0: /* id */ renderId(cs.getId()); break;
        case 1: /* upload comment */ renderUploadComment(cs); break;
        case 2: /* open/closed */ renderOpen(cs); break;
        case 3: /* user */ renderUser(cs.getUser()); break;
        case 4: /* created at */ renderDate(cs.getCreatedAt()); break;
        case 5: /* closed at */ renderDate(cs.getClosedAt()); break;
        case 6: /* discussions */ renderDiscussions(cs); break;
        default: // Do nothing
        }
        return this;
    }
}
