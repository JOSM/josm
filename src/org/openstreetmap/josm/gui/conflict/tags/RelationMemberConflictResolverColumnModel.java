// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberTable;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;

/**
 * This class defines the columns of a {@link RelationMemberTable}
 */
public class RelationMemberConflictResolverColumnModel extends DefaultTableColumnModel {

    static final class MemberRenderer extends PrimitiveRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return setColors(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column),
                    table, isSelected, row);
        }
    }

    private final DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

    private final transient PrimitiveRenderer primitiveRenderer = new MemberRenderer();

    private final transient TableCellRenderer tableRenderer = (table, value, isSelected, hasFocus, row, column)
            -> setColors(defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column),
            table, isSelected, row);

    /**
     * Constructs a new {@code RelationMemberConflictResolverColumnModel}.
     */
    public RelationMemberConflictResolverColumnModel() {
        createColumns();
    }

    private static Component setColors(Component comp, JTable table, boolean isSelected, int row) {
        if (table.getModel() instanceof RelationMemberConflictResolverModel) {
            RelationMemberConflictResolverModel model = (RelationMemberConflictResolverModel) table.getModel();

            if (!isSelected && comp != null) {
                switch (model.getDecision(row).getDecision()) {
                case UNDECIDED:
                    comp.setForeground(ConflictColors.FGCOLOR_UNDECIDED.get());
                    comp.setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
                    break;
                case KEEP:
                    comp.setForeground(ConflictColors.FGCOLOR_MEMBER_KEEP.get());
                    comp.setBackground(ConflictColors.BGCOLOR_MEMBER_KEEP.get());
                    break;
                case REMOVE:
                    comp.setForeground(ConflictColors.FGCOLOR_MEMBER_REMOVE.get());
                    comp.setBackground(ConflictColors.BGCOLOR_MEMBER_REMOVE.get());
                    break;
                }
            }
        }
        return comp;
    }

    protected final void createColumns() {

        AutoCompletingTextField roleEditor = new AutoCompletingTextField(0, false);
        RelationMemberConflictDecisionRenderer decisionRenderer = new RelationMemberConflictDecisionRenderer();
        RelationMemberConflictDecisionEditor decisionEditor = new RelationMemberConflictDecisionEditor();

        // column 0 - Relation
        TableColumn col = new TableColumn(0);
        col.setHeaderValue("Relation");
        col.setResizable(true);
        col.setWidth(100);
        col.setPreferredWidth(100);
        col.setCellRenderer(primitiveRenderer);
        addColumn(col);

        // column 1 - Position
        col = new TableColumn(1);
        col.setHeaderValue(tr("Pos."));
        col.setResizable(true);
        col.setWidth(40);
        col.setPreferredWidth(40);
        col.setCellRenderer(tableRenderer);
        col.setMaxWidth(50);
        addColumn(col);

        // column 2 - Role
        col = new TableColumn(2);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setCellRenderer(tableRenderer);
        col.setCellEditor(roleEditor);
        col.setWidth(50);
        col.setPreferredWidth(50);
        addColumn(col);

        // column 3 - Original Way
        col = new TableColumn(3);
        col.setHeaderValue(tr("Orig. Way"));
        col.setResizable(true);
        col.setCellRenderer(primitiveRenderer);
        col.setWidth(100);
        col.setPreferredWidth(100);
        addColumn(col);

        // column 4 - New Way
        col = new TableColumn(4);
        col.setHeaderValue(tr("Decision"));
        col.setResizable(true);
        col.setCellRenderer(decisionRenderer);
        col.setCellEditor(decisionEditor);
        col.setWidth(100);
        col.setPreferredWidth(100);
        col.setMaxWidth(100);
        addColumn(col);
    }
}
