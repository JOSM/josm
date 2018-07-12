// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Delete the currently edited relation.
 * @since 9496
 */
public class DeleteCurrentRelationAction extends AbstractRelationEditorAction implements PropertyChangeListener {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DeleteCurrentRelationAction}.
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public DeleteCurrentRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        putValue(SHORT_DESCRIPTION, tr("Delete the currently edited relation"));
        new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Delete"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation toDelete = getEditor().getRelation();
        if (toDelete == null)
            return;
        DeleteAction.deleteRelation(getLayer(), toDelete);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditor().getRelationSnapshot() != null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (GenericRelationEditor.RELATION_SNAPSHOT_PROP.equals(evt.getPropertyName())) {
            updateEnabledState();
        }
    }
}
