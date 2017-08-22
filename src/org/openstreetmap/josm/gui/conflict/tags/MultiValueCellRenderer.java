// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is a {@link TableCellRenderer} for {@link MultiValueResolutionDecision}s.
 *
 */
public class MultiValueCellRenderer extends JLabel implements TableCellRenderer {

    private final ImageIcon iconDecided;
    private final ImageIcon iconUndecided;
    private final DefaultComboBoxModel<Object> model;
    private final JosmComboBox<Object> cbDecisionRenderer;

    /**
     * Constructs a new {@code MultiValueCellRenderer}.
     */
    public MultiValueCellRenderer() {
        setOpaque(true);
        iconDecided = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
        iconUndecided = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
        model = new DefaultComboBoxModel<>();
        cbDecisionRenderer = new JosmComboBox<>(model);
    }

    protected void renderColors(MultiValueResolutionDecision decision, boolean selected, boolean conflict) {
        if (selected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
        } else {
            switch (decision.getDecisionType()) {
            case UNDECIDED:
                setForeground(ConflictColors.FGCOLOR_UNDECIDED.get());
                setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
                break;
            case KEEP_NONE:
                setForeground(ConflictColors.FGCOLOR_TAG_KEEP_NONE.get());
                setBackground(ConflictColors.BGCOLOR_TAG_KEEP_NONE.get());
                break;
            default:
                if (conflict) {
                    switch (decision.getDecisionType()) {
                    case KEEP_ONE:
                        setForeground(ConflictColors.FGCOLOR_TAG_KEEP_ONE.get());
                        setBackground(ConflictColors.BGCOLOR_TAG_KEEP_ONE.get());
                        break;
                    case KEEP_ALL:
                        setForeground(ConflictColors.FGCOLOR_TAG_KEEP_ALL.get());
                        setBackground(ConflictColors.BGCOLOR_TAG_KEEP_ALL.get());
                        break;
                    case SUM_ALL_NUMERIC:
                        setForeground(ConflictColors.FGCOLOR_TAG_SUM_ALL_NUM.get());
                        setBackground(ConflictColors.BGCOLOR_TAG_SUM_ALL_NUM.get());
                        break;
                    default:
                        Logging.error("Unknown decision type in renderColors(): "+decision.getDecisionType());
                    }
                } else {
                    setForeground(UIManager.getColor("Table.foreground"));
                    setBackground(UIManager.getColor("Table.background"));
                }
                break;
            }
        }
    }

    protected void renderValue(MultiValueResolutionDecision decision) {
        model.removeAllElements();
        switch (decision.getDecisionType()) {
        case UNDECIDED:
            model.addElement(tr("Choose a value"));
            cbDecisionRenderer.setFont(getFont().deriveFont(Font.ITALIC));
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        case KEEP_NONE:
            model.addElement(tr("deleted"));
            cbDecisionRenderer.setFont(getFont().deriveFont(Font.ITALIC));
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        case KEEP_ONE:
        case KEEP_ALL:
        case SUM_ALL_NUMERIC:
            model.addElement(decision.getChosenValue());
            cbDecisionRenderer.setFont(getFont());
            cbDecisionRenderer.setSelectedIndex(0);
            break;
        default:
            Logging.error("Unknown decision type in renderValue(): "+decision.getDecisionType());
        }
    }

    /**
     * Sets the text of the tooltip for both renderers, this (the JLabel) and the combobox renderer.
     * @param decision conflict resolution decision
     */
    protected void renderToolTipText(MultiValueResolutionDecision decision) {
        String toolTipText = null;
        switch (decision.getDecisionType()) {
        case UNDECIDED:
            toolTipText = tr("Please decide which values to keep");
            break;
        case KEEP_ONE:
            toolTipText = tr("Value ''{0}'' is going to be applied for key ''{1}''",
                    decision.getChosenValue(), decision.getKey());
            break;
        case SUM_ALL_NUMERIC:
            toolTipText = tr("All numeric values sumed as ''{0}'' are going to be applied for key ''{1}''",
                    decision.getChosenValue(), decision.getKey());
            break;
        case KEEP_NONE:
            toolTipText = tr("The key ''{0}'' and all its values are going to be removed", decision.getKey());
            break;
        case KEEP_ALL:
            toolTipText = tr("All values joined as ''{0}'' are going to be applied for key ''{1}''",
                    decision.getChosenValue(), decision.getKey());
            break;
        }
        setToolTipText(toolTipText);
        cbDecisionRenderer.setToolTipText(toolTipText);
    }

    protected void reset() {
        setFont(UIManager.getFont("Table.font"));
        setIcon(null);
        setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        reset();
        if (value == null)
            return this;

        MultiValueResolutionDecision decision = (MultiValueResolutionDecision) value;
        TagConflictResolverModel tagModel = (TagConflictResolverModel) table.getModel();
        boolean conflict = tagModel.getKeysWithConflicts().contains(tagModel.getKey(row));
        renderColors(decision, isSelected, conflict);
        renderToolTipText(decision);
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
