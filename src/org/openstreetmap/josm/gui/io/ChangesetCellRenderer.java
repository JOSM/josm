// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.text.DateFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A {@link ListCellRenderer} for the list of changesets in the upload dialog.
 *
 *
 */
public class ChangesetCellRenderer extends JLabel implements ListCellRenderer {
    private ImageIcon icon ;
    public ChangesetCellRenderer() {
        icon = ImageProvider.get("data", "changeset");
        setOpaque(true);
    }

    protected String buildToolTipText(Changeset cs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>").append(tr("Changeset id:")).append("</strong>").append(cs.getId()).append("<br>");
        if (cs.getCreatedAt() != null) {
            sb.append("<strong>").append(tr("Created at:")).append("</strong>").append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(cs.getCreatedAt())).append("<br>");
        }
        if (cs.get("comment") != null) {
            sb.append("<strong>").append(tr("Changeset comment:")).append("</strong>").append(cs.get("comment")).append("<br>");
        }
        return sb.toString();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        Changeset cs = (Changeset)value;
        if (isSelected) {
            setForeground(UIManager.getColor("List.selectionForeground"));
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setForeground(UIManager.getColor("List.foreground"));
            setBackground(UIManager.getColor("List.background"));
        }
        if (cs != null) {
            setIcon(icon);
            StringBuilder sb = new StringBuilder();
            if (cs.get("comment") != null) {
                sb.append(cs.getId()).append(" - ").append(cs.get("comment"));
            } else if (cs.get("name") != null) {
                sb.append(cs.getId()).append(" - ").append(cs.get("name"));
            } else {
                sb.append(tr("Changeset {0}", cs.getId()));
            }
            setText(sb.toString());
            setToolTipText(buildToolTipText(cs));
        } else {
            setText(tr("No open changeset"));
        }
        return this;
    }
}
