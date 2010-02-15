// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The {@see TableCellRenderer} for a list of versions in {@see HistoryBrower}
 *
 */
public class VersionTableCellRenderer extends JLabel implements TableCellRenderer {

    @SuppressWarnings("unused")
    static private Logger logger = Logger.getLogger(VersionTableCellRenderer.class.getName());

    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_IS_REFERENCE_POINT = new Color(255,197,197);

    protected HashMap<OsmPrimitiveType, ImageIcon> icons = null;

    public VersionTableCellRenderer() {
        loadIcons();
        setOpaque(true);
    }

    protected void loadIcons() {
        icons = new HashMap<OsmPrimitiveType, ImageIcon>();
        icons.put(OsmPrimitiveType.NODE, ImageProvider.get("data", "node"));
        icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
        icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
    }

    protected void renderIcon(OsmPrimitiveType type) {
        ImageIcon icon = type == null? null : icons.get(type);
        setIcon(icon);
    }

    protected void renderText(HistoryOsmPrimitive primitive) {
        // render label text
        //
        StringBuilder sb = new StringBuilder();
        if (primitive == null) {
            sb.append("");
        } else {
            String msg = tr(
                    "Version {0}, {1} (by {2})",
                    Long.toString(primitive.getVersion()),
                    new SimpleDateFormat().format(primitive.getTimestamp()),
                    primitive.getUser()
            );
            sb.append(msg);
        }
        setText(sb.toString());

        // render tooltip text
        //
        sb = new StringBuilder();
        if (primitive == null) {
            sb.append("");
        } else {
            sb.append(
                    tr("Version {0} created on {1} by {2}",
                            Long.toString(primitive.getVersion()),
                            new SimpleDateFormat().format(primitive.getTimestamp()),
                            primitive.getUser()
                    )
            );
        }
        setToolTipText(sb.toString());
    }

    protected OsmDataLayer getEditLayer() {
        try {
            return Main.map.mapView.getEditLayer();
        } catch(NullPointerException e) {
            return null;
        }
    }

    protected void renderLatestText(OsmPrimitive primitive) {
        // -- label text
        StringBuffer sb = new StringBuffer();
        if (primitive == null) {
            setText("");
            return;
        }
        if (primitive.isModified()) {
            sb.append("*");
        }
        sb.append(tr("Version {0} in editor", primitive.getVersion()));
        if (primitive.isDeleted()) {
            sb.append(tr("[deleted]"));
        }
        setText(sb.toString());

        // -- tooltip text
        sb = new StringBuffer();
        OsmDataLayer l = getEditLayer();

        sb.append(
                tr(
                        "Version {0} currently edited in data layer ''{1}''",
                        primitive.getId(),
                        l == null ? tr("unknown") : l.getName()
                )
        );
        setToolTipText(sb.toString());
    }

    protected void renderBackground(JTable table, int row, boolean isSelected) {
        Color bgColor = Color.WHITE;
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        } else if (getModel(table).isReferencePointInTime(row)) {
            bgColor = BGCOLOR_IS_REFERENCE_POINT;
        }
        setBackground(bgColor);
    }

    public void renderVersionFromHistory(HistoryOsmPrimitive primitive, JTable table, int row, boolean isSelected) {
        renderIcon(primitive == null? null : primitive.getType());
        renderText(primitive);
        renderBackground(table, row, isSelected);
    }

    public void renderLatest(OsmPrimitive primitive, JTable table, int row, boolean isSelected) {
        renderIcon(primitive.getType());
        renderLatestText(getModel(table).getLatest());
        renderBackground(table, row, isSelected);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (getModel(table).isLatest(row)) {
            renderLatest(getModel(table).getLatest(),table, row, isSelected);
        } else {
            renderVersionFromHistory((HistoryOsmPrimitive)value, table, row, isSelected);
        }
        return this;
    }

    protected HistoryBrowserModel.VersionTableModel getModel(JTable table) {
        return (HistoryBrowserModel.VersionTableModel)table.getModel();
    }
}
