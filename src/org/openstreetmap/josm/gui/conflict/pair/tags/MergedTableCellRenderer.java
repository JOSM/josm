// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;

public class MergedTableCellRenderer extends TagMergeTableCellRenderer {

    public final static Color BGCOLOR_UNDECIDED = new Color(255,197,197);
    public final static Color BGCOLOR_MINE = new Color(217,255,217);
    public final static Color BGCOLOR_THEIR = new Color(217,255,217);
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    protected void setBackgroundColor(TagMergeItem item, boolean isSelected) {
        if (isSelected) {
            setBackground(BGCOLOR_SELECTED);
            return;
        }
        if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision())) {
            setBackground(BGCOLOR_MINE);
        } else if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision())) {
            setBackground(BGCOLOR_THEIR);
        } else if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
            setBackground(BGCOLOR_UNDECIDED);
        }
    }

    @Override
    protected void renderKey(TagMergeItem item, boolean isSelected) {
        setBackgroundColor(item,isSelected);
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
        setBackgroundColor(item,isSelected);
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
            } else {
                // should not happen
            }
        }
    }

}
