// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Apply the current updates.
 * @since 9496
 */
public class ApplyAction extends SavingAction implements PropertyChangeListener, TableModelListener {

    /**
     * Constructs a new {@code ApplyAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tagModel tag editor model
     */
    public ApplyAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            IRelationEditor editor) {
        super(memberTable, memberTableModel, tagModel, layer, editor, null);
        putValue(SHORT_DESCRIPTION, tr("Apply the current updates"));
        putValue(SMALL_ICON, ImageProvider.get("save"));
        putValue(NAME, tr("Apply"));
        updateEnabledState();
        memberTableModel.addTableModelListener(this);
        tagModel.addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (applyChanges()) {
            editor.reloadDataFromRelation();
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(isEditorDirty());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateEnabledState();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateEnabledState();
    }
}
