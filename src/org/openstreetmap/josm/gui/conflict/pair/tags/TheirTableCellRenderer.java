// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;

public class TheirTableCellRenderer extends TagMergeTableCellRenderer {

    public final static Color BGCOLOR_UNDECIDED = new Color(255,197,197);
    public final static Color BGCOLOR_MINE = Color.white;
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

    protected void setTextColor(TagMergeItem item) {
        if (MergeDecisionType.KEEP_THEIR.equals(item.getMergeDecision())) {
            setForeground(Color.black);
        } else if (MergeDecisionType.KEEP_MINE.equals(item.getMergeDecision())) {
            setForeground(Color.LIGHT_GRAY);
        } else if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
            setForeground(Color.black);
        }
    }

    @Override
    protected void renderKey(TagMergeItem item, boolean isSelected) {
        setBackgroundColor(item, isSelected);
        setTextColor(item);
        if (item.getTheirTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("Their dataset does not include a tag with key {0}", item.getKey()));
        } else {
            setText(item.getKey());
            setToolTipText(item.getKey());
        }
    }

    @Override
    protected void renderValue(TagMergeItem item, boolean isSelected) {
        setBackgroundColor(item,isSelected);
        setTextColor(item);
        if (item.getTheirTagValue() == null) {
            setText(tr("<undefined>"));
            setToolTipText(tr("Their dataset does not include a tag with key {0}", item.getKey()));
        } else {
            setText(item.getTheirTagValue());
            setToolTipText(item.getTheirTagValue());
        }
    }
}
