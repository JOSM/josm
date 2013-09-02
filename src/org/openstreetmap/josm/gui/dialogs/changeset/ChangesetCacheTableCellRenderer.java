// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Font;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;

/**
 * The cell renderer for the changeset table
 * @since 2689
 */
public class ChangesetCacheTableCellRenderer extends JLabel implements TableCellRenderer{

    /**
     * Constructs a new {@code ChangesetCacheTableCellRenderer}.
     */
    public ChangesetCacheTableCellRenderer() {
        setOpaque(true);
    }

    protected void reset() {
        setBackground(UIManager.getColor("Table.background"));
        setForeground(UIManager.getColor("Table.foreground"));
        setFont(UIManager.getFont("Table.font"));
        setToolTipText("");
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

    protected void renderId(Changeset cs) {
        setText(Integer.toString(cs.getId()));
        setToolTipText("");
    }

    protected void renderUploadComment(Changeset cs) {
        String comment = cs.get("comment");
        if (comment == null || comment.trim().isEmpty()) {
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
        setToolTipText("");
    }

    protected void renderUser(Changeset cs) {
        User user = cs.getUser();
        if (user == null || user.getName().trim().isEmpty()) {
            setFont(UIManager.getFont("Table.font").deriveFont(Font.ITALIC));
            setText(tr("anonymous"));
        } else {
            setFont(UIManager.getFont("Table.font"));
            setText(user.getName());
            setToolTipText(user.getName());
        }
    }

    protected void renderDate(Date d) {
        if (d == null) {
            setText("");
        } else {
            setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(d));
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
        Changeset cs = (Changeset)value;
        switch(column) {
        case 0: /* id */ renderId(cs); break;
        case 1: /* upload comment */ renderUploadComment(cs); break;
        case 2: /* open/closed */ renderOpen(cs); break;
        case 3: /* user */ renderUser(cs); break;
        case 4: /* created at */ renderDate(cs.getCreatedAt()); break;
        case 5: /* closed at */ renderDate(cs.getClosedAt()); break;
        }
        return this;
    }
}
