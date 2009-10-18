// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

public enum WayConnectionType {

    none("", tr("Not connected")),
    head_to_head("-><-", tr("Last points of ways are connected")),
    tail_to_tail("<-->", tr("First points of ways are connected")),
    head_to_tail("->->", tr("First point of second way connects to last point of first way")),
    tail_to_head("<-<-", tr("First point of first way connects to last point of second way"));

    private String textSymbol;
    private String toolTip;

    WayConnectionType(String textSymbol, String toolTip) {
        this.textSymbol = textSymbol;
        this.toolTip = toolTip;
    }

    @Override
    public String toString() {
        return textSymbol;
    }

    public String getToolTip() {
        return toolTip;
    }
}
