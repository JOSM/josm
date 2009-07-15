// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

public enum WayConnectionType {

    none (""),
    head_to_head("-><-"),
    tail_to_tail("->->"),
    head_to_tail("<-<-"),
    tail_to_head ("<-->");

    private String textSymbol;

    WayConnectionType(String textSymbol) {
        this.textSymbol = textSymbol;
    }

    @Override
    public String toString() {
        return textSymbol;
    }
}
