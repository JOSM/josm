// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.tools.ImageProvider;

public class ChangesetListCellRenderer extends JLabel implements ListCellRenderer{

    public ChangesetListCellRenderer() {
        setOpaque(true);
        setIcon(ImageProvider.get("data", "changeset"));
    }

    protected void renderColors(boolean selected) {
        if (selected) {
            setForeground(UIManager.getColor("List.selectionForeground"));
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setForeground(UIManager.getColor("List.foreground"));
            setBackground(UIManager.getColor("List.background"));
        }
    }

    protected void renderLabel(Changeset cs) {
        StringBuilder sb = new StringBuilder();
        if (cs.isIncomplete()) {
            sb.append(tr("{0} [incomplete]", cs.getId()));
        } else {
            String comment = cs.get("comment");
            sb.append(cs.getId());
            sb.append(" - ");
            sb.append(cs.isOpen() ? tr("open") : tr("closed"));
            if (comment != null) {
                sb.append(" - ").append("'").append(comment).append("'");
            }
        }
        setText(sb.toString());
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        Changeset cs = (Changeset)value;
        renderColors(isSelected);
        renderLabel(cs);
        return this;
    }
}
