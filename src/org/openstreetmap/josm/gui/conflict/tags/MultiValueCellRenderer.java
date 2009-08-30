// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.tools.ImageProvider;

public class MultiValueCellRenderer extends JLabel implements TableCellRenderer {

    private ImageIcon iconDecided;
    private ImageIcon iconUndecided;

    public MultiValueCellRenderer() {
        setOpaque(true);
        iconDecided = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
        iconUndecided = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
    }

    protected void renderColors(boolean selected) {
        if (selected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
        } else {
            setForeground(UIManager.getColor("Table.foreground"));
            setBackground(UIManager.getColor("Table.background"));
        }
    }

    protected void renderValue(MultiValueResolutionDecision decision) {
        switch(decision.getDecisionType()) {
            case UNDECIDED:
                setText(tr("Choose a value"));
                setFont(getFont().deriveFont(Font.ITALIC));
                setToolTipText(tr("Please decided which values to keep"));
                break;
            case KEEP_ONE:
                setText(decision.getChosenValue());
                setToolTipText(tr("Value ''{0}'' is going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey()));
                break;
            case KEEP_NONE:
                setText(tr("deleted"));
                setFont(getFont().deriveFont(Font.ITALIC));
                setToolTipText(tr("The key ''{0}'' and all it's values are going to be removed", decision.getKey()));
                break;
            case KEEP_ALL:
                setText(decision.getChosenValue());
                setToolTipText(tr("All values joined as ''{0}'' are going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey()));
                break;
        }
    }

    protected void reset() {
        setIcon(null);
        setText("");
        setFont(UIManager.getFont("Table.font"));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        renderColors(isSelected);
        MultiValueResolutionDecision decision = (MultiValueResolutionDecision)value;
        switch(column) {
            case 0:
                if (decision.isDecided()) {
                    setIcon(iconDecided);
                } else {
                    setIcon(iconUndecided);
                }
                break;

            case 1:
                setText(decision.getKey());
                break;

            case 2:
                renderValue(decision);
                break;
        }
        return this;
    }
}
