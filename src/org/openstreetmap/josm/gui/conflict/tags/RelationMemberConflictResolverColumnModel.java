// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;

public class RelationMemberConflictResolverColumnModel extends DefaultTableColumnModel{

    protected void createColumns() {
        OsmPrimitivRenderer primitiveRenderer = new OsmPrimitivRenderer();
        AutoCompletingTextField roleEditor = new AutoCompletingTextField();
        RelationMemberConflictDecisionRenderer decisionRenderer = new RelationMemberConflictDecisionRenderer();
        RelationMemberConflictDecisionEditor decisionEditor = new RelationMemberConflictDecisionEditor();

        TableColumn col = null;

        // column 0 - Relation
        col = new TableColumn(0);
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
        col.setMaxWidth(50);
        addColumn(col);

        // column 2 - Role
        col = new TableColumn(2);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
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

    public RelationMemberConflictResolverColumnModel() {
        createColumns();
    }
}
