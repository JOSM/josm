// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is a {@see TableCellRenderer} for {@see MultiValueResolutionDecision}s.
 *
 */
public class MultiValueCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_UNDECIDED = new Color(255,197,197);

    private ImageIcon iconDecided;
    private ImageIcon iconUndecided;
    private DefaultComboBoxModel model;
    private JComboBox cbDecisionRenderer;

    public MultiValueCellRenderer() {
        setOpaque(true);
        iconDecided = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
        iconUndecided = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
        cbDecisionRenderer = new JComboBox(model = new DefaultComboBoxModel());
    }

    protected void renderColors(MultiValueResolutionDecision decision, boolean selected) {
        if (selected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
        } else{
            switch(decision.getDecisionType()) {
                case UNDECIDED:
                    setForeground(UIManager.getColor("Table.foreground"));
                    setBackground(BGCOLOR_UNDECIDED);
                    break;
                case KEEP_NONE:
                    setForeground(UIManager.getColor("Panel.foreground"));
                    setBackground(UIManager.getColor("Panel.background"));
                    break;
                default:
                    setForeground(UIManager.getColor("Table.foreground"));
                    setBackground(UIManager.getColor("Table.background"));
                    break;
            }
        }
    }

    protected void renderValue(MultiValueResolutionDecision decision) {
        model.removeAllElements();
        switch(decision.getDecisionType()) {
            case UNDECIDED:
                model.addElement(tr("Choose a value"));
                setFont(getFont().deriveFont(Font.ITALIC));
                setToolTipText(tr("Please decide which values to keep"));
                cbDecisionRenderer.setSelectedIndex(0);
                break;
            case KEEP_ONE:
                model.addElement(decision.getChosenValue());
                setToolTipText(tr("Value ''{0}'' is going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey()));
                cbDecisionRenderer.setSelectedIndex(0);
                break;
            case KEEP_NONE:
                model.addElement(tr("deleted"));
                setFont(getFont().deriveFont(Font.ITALIC));
                setToolTipText(tr("The key ''{0}'' and all its values are going to be removed", decision.getKey()));
                cbDecisionRenderer.setSelectedIndex(0);
                break;
            case KEEP_ALL:
                model.addElement(decision.getChosenValue());
                setToolTipText(tr("All values joined as ''{0}'' are going to be applied for key ''{1}''", decision.getChosenValue(), decision.getKey()));
                cbDecisionRenderer.setSelectedIndex(0);
                break;
        }
    }

    protected void reset() {
        setFont(UIManager.getFont("Table.font"));
        setIcon(null);
        setText("");
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        MultiValueResolutionDecision decision = (MultiValueResolutionDecision)value;
        renderColors(decision,isSelected);
        switch(column) {
            case 0:
                if (decision.isDecided()) {
                    setIcon(iconDecided);
                } else {
                    setIcon(iconUndecided);
                }
                return this;

            case 1:
                setText(decision.getKey());
                return this;

            case 2:
                renderValue(decision);
                return cbDecisionRenderer;
        }
        return this;
    }
}
