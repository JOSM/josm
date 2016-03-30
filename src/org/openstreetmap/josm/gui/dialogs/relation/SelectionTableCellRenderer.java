// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * This is the {@link TableCellRenderer} used in {@link SelectionTable}.
 * @since 1806
 */
public class SelectionTableCellRenderer extends MemberTableMemberCellRenderer {
    public static final Color BGCOLOR_SINGLE_ENTRY = BGCOLOR_IN_JOSM_SELECTION;

    /**
     * reference to the member table model; required, in order to check whether a
     * selected primitive is already used in the member list of the currently edited relation
     */
    private final MemberTableModel model;

    /**
     * constructor
     * @param model member table model
     */
    public SelectionTableCellRenderer(MemberTableModel model) {
        this.model = model;
    }

    @Override
    protected void renderBackgroundForeground(MemberTableModel model, OsmPrimitive primitive, boolean isSelected) {
        Color bgc = UIManager.getColor("Table.background");
        if (primitive != null && model != null && model.getNumMembersWithPrimitive(primitive) == 1) {
            bgc = BGCOLOR_SINGLE_ENTRY;
        } else if (primitive != null && model != null && model.getNumMembersWithPrimitive(primitive) > 1) {
            bgc = BGCOLOR_DOUBLE_ENTRY;
        }
        GuiHelper.setBackgroundReadable(this, bgc);
    }

    @Override
    protected MemberTableModel getModel(JTable table) {
        return model;
    }
}
