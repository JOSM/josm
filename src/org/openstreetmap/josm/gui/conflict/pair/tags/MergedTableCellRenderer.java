// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;

/**
 * This {@link TableCellRenderer} displays the decision a user made regarding a tag conflict
 */
public class MergedTableCellRenderer extends TagMergeTableCellRenderer {
    protected void setBackgroundColor(TagMergeItem item, boolean isSelected) {
        if (isSelected) {
            setBackground(ConflictColors.BGCOLOR_SELECTED.get());
            return;
        }
        if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision())) {
            setBackground(ConflictColors.BGCOLOR_COMBINED.get());
        } else if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision())) {
            setBackground(ConflictColors.BGCOLOR_COMBINED.get());
        } else if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
            setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
        }
    }

    @Override
    protected void renderKey(TagMergeItem item, boolean isSelected) {
        setBackgroundColor(item, isSelected);
        if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision()) && item.getMyTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("The merged dataset will not include a tag with key {0}", item.getKey()));
        } else if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision()) && item.getTheirTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("The merged dataset will not include a tag with key {0}", item.getKey()));
        } else if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
            setText("");
        } else {
            setText(item.getKey());
            setToolTipText(item.getKey());
        }
    }

    @Override
    protected void renderValue(TagMergeItem item, boolean isSelected) {
        setBackgroundColor(item, isSelected);
        if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision()) && item.getMyTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("The merged dataset will not include a tag with key {0}", item.getKey()));
        } else if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision()) && item.getTheirTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("The merged dataset will not include a tag with key {0}", item.getKey()));
        } else if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
            setText("");
        } else {
            if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision())) {
                setText(item.getMyTagValue());
                setToolTipText(item.getMyTagValue());
            } else if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision())) {
                setText(item.getTheirTagValue());
                setToolTipText(item.getTheirTagValue());
            }
        }
    }
}
