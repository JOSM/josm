// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

public class MemberTableMemberCellRenderer extends MemberTableCellRenderer {
    private HashMap<OsmPrimitiveType, ImageIcon> icons;

    public MemberTableMemberCellRenderer() {
        super();
        loadIcons();
    }

    /**
     * Load the image icon for an OSM primitive of type node
     *
     * @return the icon; null, if not found
     */
    protected void loadIcons() {
        icons = new HashMap<OsmPrimitiveType, ImageIcon>();
        icons.put(OsmPrimitiveType.NODE, ImageProvider.get("data", "node"));
        icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
        icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
    }

    protected void renderPrimitive(OsmPrimitive primitive) {
        setIcon(icons.get(OsmPrimitiveType.from(primitive)));
        setText(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(buildToolTipText(primitive));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();

        renderForeground(isSelected);
        OsmPrimitive primitive = (OsmPrimitive) value;
        renderBackground(getModel(table), primitive, isSelected);
        renderPrimitive(primitive);
        return this;
    }
}
