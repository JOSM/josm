// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * This is the {@link TableCellRenderer} used in the tables of
 * {@link org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberMerger}.
 * @since 1790
 */
public abstract class MemberTableCellRenderer extends JLabel implements TableCellRenderer {
    public static final Color BGCOLOR_IN_JOSM_SELECTION = new Color(235, 255, 177);

    public static final Color BGCOLOR_DOUBLE_ENTRY = new Color(254, 226, 214);

    /**
     * constructor
     */
    protected MemberTableCellRenderer() {
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

    protected void renderBackgroundForeground(MemberTableModel model, OsmPrimitive primitive, boolean isSelected) {
        Color bgc = UIManager.getColor("Table.background");
        if (isSelected) {
            bgc = UIManager.getColor("Table.selectionBackground");
        } else if (primitive != null && model.isInJosmSelection(primitive)) {
            bgc = BGCOLOR_IN_JOSM_SELECTION;
        } else if (primitive != null && model.getNumMembersWithPrimitive(primitive) > 1) {
            bgc = BGCOLOR_DOUBLE_ENTRY;
        }
        GuiHelper.setBackgroundReadable(this, bgc);
    }

    /**
     * replies the model
     * @param table the table
     * @return the table model
     */
    protected MemberTableModel getModel(JTable table) {
        return (MemberTableModel) table.getModel();
    }
}
