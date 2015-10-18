// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * "Refers to" column in relation editor's member list.
 */
public class MemberTableMemberCellRenderer extends MemberTableCellRenderer {

	/**
	 * Constructs a new {@code MemberTableMemberCellRenderer}.
	 */
    public MemberTableMemberCellRenderer() {
        super();
    }

    protected void renderPrimitive(OsmPrimitive primitive, Rectangle cellSize) {
        // Make icon the full height of the table cell. Icon background is square.
        setIcon(ImageProvider.getPadded(primitive, cellSize));
        setText(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(DefaultNameFormatter.getInstance().buildDefaultToolTip(primitive));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        Rectangle cellSize = table.getCellRect(row, column, false);

        renderForeground(isSelected);
        OsmPrimitive primitive = (OsmPrimitive) value;
        renderBackground(getModel(table), primitive, isSelected);
        renderPrimitive(primitive, cellSize);
        return this;
    }
}
