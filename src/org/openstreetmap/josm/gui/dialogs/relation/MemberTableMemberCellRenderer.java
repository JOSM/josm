// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

public class MemberTableMemberCellRenderer extends MemberTableCellRenderer {

    public MemberTableMemberCellRenderer() {
        super();
    }

    protected void renderPrimitive(OsmPrimitive primitive) {
        setIcon(ImageProvider.get(primitive.getDisplayType()));
        setText(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(DefaultNameFormatter.getInstance().buildDefaultToolTip(primitive));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        renderForeground(isSelected);
        OsmPrimitive primitive = (OsmPrimitive) value;
        renderBackground(getModel(table), primitive, isSelected);
        renderPrimitive(primitive);
        return this;
    }
}
