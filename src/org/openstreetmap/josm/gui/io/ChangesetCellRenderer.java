// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.time.Instant;
import java.time.format.FormatStyle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A {@link ListCellRenderer} for the list of changesets in the upload dialog.
 *
 * @since 2115
 */
public class ChangesetCellRenderer extends JLabel implements ListCellRenderer<Changeset> {
    private final ImageIcon icon;

    /**
     * Constructs a new {@code ChangesetCellRenderer}.
     */
    public ChangesetCellRenderer() {
        icon = ImageProvider.get("data", "changeset");
        setOpaque(true);
    }

    protected String buildToolTipText(Changeset cs) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("<html><strong>").append(tr("Changeset id:")).append("</strong>").append(cs.getId()).append("<br>");
        Instant createdDate = cs.getCreatedAt();
        if (createdDate != null) {
            sb.append("<strong>").append(tr("Created at:")).append("</strong>").append(
                    DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.SHORT).format(createdDate)).append("<br>");
        }
        String comment = cs.getComment();
        if (!comment.isEmpty()) {
            sb.append("<strong>").append(tr("Changeset comment:")).append("</strong>")
              .append(Utils.escapeReservedCharactersHTML(comment)).append("<br>");
        }
        return sb.toString();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Changeset> list, Changeset cs, int index, boolean isSelected,
            boolean cellHasFocus) {
        if (isSelected) {
            setForeground(UIManager.getColor("List.selectionForeground"));
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setForeground(UIManager.getColor("List.foreground"));
            setBackground(UIManager.getColor("List.background"));
        }
        if (cs != null) {
            setIcon(icon);
            if (cs.getId() == 0) {
                setText("New changeset");
            } else {
                StringBuilder sb = new StringBuilder();
                String comment = cs.getComment();
                if (!comment.isEmpty()) {
                    sb.append(cs.getId()).append(" - ").append(comment);
                } else if (cs.get("name") != null) {
                    sb.append(cs.getId()).append(" - ").append(cs.get("name"));
                } else {
                    sb.append(tr("Changeset {0}", cs.getId()));
                }
                setText(sb.toString());
            }
            setToolTipText(buildToolTipText(cs));
        } else {
            setIcon(null);
            setText("");
        }
        return this;
    }
}
