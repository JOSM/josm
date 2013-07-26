// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is the {@link TableCellRenderer} used in the tables of
 * {@link org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberMerger}.
 *
 */
public abstract class MemberTableCellRenderer extends JLabel implements TableCellRenderer {
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234, 234, 234);
    public final static Color BGCOLOR_IN_JOSM_SELECTION = new Color(235,255,177);

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255, 197, 197);
    public final static Color BGCOLOR_DOUBLE_ENTRY = new Color(254,226,214);

    /**
     * constructor
     */
    public MemberTableCellRenderer() {
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

    protected void renderBackground(MemberTableModel model, OsmPrimitive primitive, boolean isSelected) {
        Color bgc = UIManager.getColor("Table.background");
        if (isSelected) {
            bgc = UIManager.getColor("Table.selectionBackground");
        } else if (primitive != null && model.isInJosmSelection(primitive)) {
            bgc = BGCOLOR_IN_JOSM_SELECTION;
        } else if (primitive != null && model.getNumMembersWithPrimitive(primitive) > 1) {
            bgc = BGCOLOR_DOUBLE_ENTRY;
        }
        setBackground(bgc);
    }

    protected void renderForeground(boolean isSelected) {
        Color fgc;
        if (isSelected) {
            fgc = UIManager.getColor("Table.selectionForeground");
        } else {
            fgc = UIManager.getColor("Table.foreground");
        }
        setForeground(fgc);
    }

    @Override
    abstract public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column);

    /**
     * replies the model
     * @param table the table
     * @return the table model
     */
    protected MemberTableModel getModel(JTable table) {
        return (MemberTableModel) table.getModel();
    }
}
