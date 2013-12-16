// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@link TableCellRenderer} used in {@link SelectionTable}.
 *
 */
public class SelectionTableCellRenderer extends JLabel implements TableCellRenderer {
    public final static Color BGCOLOR_DOUBLE_ENTRY = new Color(254,226,214);
    public final static Color BGCOLOR_SINGLE_ENTRY = new Color(235,255,177);

    /**
     * reference to the member table model; required, in order to check whether a
     * selected primitive is already used in the member list of the currently edited
     * relation
     */
    private MemberTableModel model;

    /**
     * constructor
     */
    public SelectionTableCellRenderer() {
        setIcon(null);
        setOpaque(true);
    }

    /**
     * reset the renderer
     */
    protected void reset() {
        setBackground(UIManager.getColor("Table.background"));
        setForeground(UIManager.getColor("Table.foreground"));
        setBorder(null);
        setIcon(null);
        setToolTipText(null);
    }

    protected void renderBackground(OsmPrimitive primitive, boolean isSelected) {
        Color bgc = UIManager.getColor("Table.background");
        if (primitive != null && model != null && model.getNumMembersWithPrimitive(primitive) == 1) {
            bgc = BGCOLOR_SINGLE_ENTRY;
        } else if (primitive != null && model != null && model.getNumMembersWithPrimitive(primitive) > 1) {
            bgc = BGCOLOR_DOUBLE_ENTRY;
        }
        setBackground(bgc);
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

        renderBackground((OsmPrimitive)value, isSelected);
        renderPrimitive((OsmPrimitive)value);
        return this;
    }

    public void setMemberTableModel(MemberTableModel model) {
        this.model = model;
    }
}
