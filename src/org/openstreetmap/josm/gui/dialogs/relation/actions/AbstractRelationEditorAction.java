// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Abstract superclass of relation editor actions.
 * @since 9496
 */
public abstract class AbstractRelationEditorAction extends AbstractAction implements TableModelListener, ListSelectionListener {
    protected final MemberTable memberTable;
    protected final MemberTableModel memberTableModel;
    protected final transient OsmDataLayer layer;
    protected final transient IRelationEditor editor;

    protected AbstractRelationEditorAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey) {
        this(memberTable, memberTableModel, actionMapKey, null, null);
    }

    protected AbstractRelationEditorAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey,
            OsmDataLayer layer, IRelationEditor editor) {
        this.memberTable = memberTable;
        this.memberTableModel = memberTableModel;
        this.layer = layer;
        this.editor = editor;
        if (actionMapKey != null) {
            this.memberTable.getActionMap().put(actionMapKey, this);
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateEnabledState();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateEnabledState();
    }

    protected abstract void updateEnabledState();
}
