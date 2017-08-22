// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 *
 * Can be used in lists and tables.
 *
 * @author imi
 * @author Frederik Ramm
 */
public class OsmPrimitivRenderer implements ListCellRenderer<OsmPrimitive>, TableCellRenderer {
    private final DefaultNameFormatter formatter = DefaultNameFormatter.getInstance();

    /**
     * Default list cell renderer - delegate for ListCellRenderer operation
     */
    private final DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();

    /**
     * Default table cell renderer - delegate for TableCellRenderer operation
     */
    private final DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

    /**
     * Adapter method supporting the ListCellRenderer interface.
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends OsmPrimitive> list, OsmPrimitive value, int index,
            boolean isSelected, boolean cellHasFocus) {
        Component def = defaultListCellRenderer.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        return renderer(def, value, list.getModel().getSize() > 1000);
    }

    /**
     * Adapter method supporting the TableCellRenderer interface.
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component def = defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof OsmPrimitive)
            return renderer(def, (OsmPrimitive) value, table.getModel().getRowCount() > 1000);
        else if (value instanceof HistoryOsmPrimitive)
            return renderer(def, (HistoryOsmPrimitive) value);
        else
            return def;
    }

    /**
     * Internal method that stuffs information into the rendering component
     * provided that it's a kind of JLabel.
     * @param def the rendering component
     * @param value the OsmPrimitive to render
     * @param fast whether the icons should be loaded fast since many items are being displayed
     * @return the modified rendering component
     */
    private Component renderer(Component def, OsmPrimitive value, boolean fast) {
        if (value != null && def instanceof JLabel) {
            ((JLabel) def).setText(getComponentText(value));
            final ImageIcon icon = fast
                    ? ImageProvider.get(value.getType())
                    : ImageProvider.getPadded(value,
                        // Height of component no yet known, assume the default 16px.
                        ImageProvider.ImageSizes.SMALLICON.getImageDimension());
            if (icon != null) {
                ((JLabel) def).setIcon(icon);
            } else {
                Logging.warn("Null icon for "+value.getDisplayType());
            }
            ((JLabel) def).setToolTipText(getComponentToolTipText(value));
        }
        return def;
    }

    /**
     * Internal method that stuffs information into the rendering component
     * provided that it's a kind of JLabel.
     * @param def the rendering component
     * @param value the HistoryOsmPrimitive to render
     * @return the modified rendering component
     */
    private Component renderer(Component def, HistoryOsmPrimitive value) {
        if (value != null && def instanceof JLabel) {
            ((JLabel) def).setText(value.getDisplayName(DefaultNameFormatter.getInstance()));
            ((JLabel) def).setIcon(ImageProvider.get(value.getType()));
            ((JLabel) def).setToolTipText(formatter.buildDefaultToolTip(value));
        }
        return def;
    }

    /**
     * Returns the text representing an OSM primitive in a component.
     * Can be overridden to customize the text
     * @param value OSM primitive
     * @return text representing the OSM primitive
     */
    protected String getComponentText(OsmPrimitive value) {
        return value.getDisplayName(DefaultNameFormatter.getInstance());
    }

    /**
     * Returns the text representing an OSM primitive in a tooltip.
     * Can be overridden to customize the ToolTipText
     * @param value OSM primitive
     * @return text representing the OSM primitive
     */
    protected String getComponentToolTipText(OsmPrimitive value) {
        return formatter.buildDefaultToolTip(value);
    }
}
