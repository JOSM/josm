// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Creates a new relation with a copy of the current editor state
 * @since 5799
 */
public class DuplicateRelationAction extends AbstractRelationAction {

    /**
     * Constructs a new {@code DuplicateRelationAction}.
     */
    public DuplicateRelationAction() {
        putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
        new ImageProvider("duplicate").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Duplicate"));
    }

    /**
     * Duplicates the given relation and launches the relation editor for the created copy.
     * @param original The relation to duplicate
     */
    public static void duplicateRelationAndLaunchEditor(Relation original) {
        Relation copy = new Relation(original, true);
        copy.setModified(true);
        RelationEditor editor = RelationEditor.getEditor(
                MainApplication.getLayerManager().getEditLayer(),
                copy,
                null /* no selected members */
                );
        editor.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty())
            return;
        Relation r = relations.iterator().next();
        duplicateRelationAndLaunchEditor(r);
    }

    @Override
    protected void updateEnabledState() {
        // only one selected relation can be edited
        setEnabled(relations.size() == 1 && !relations.iterator().next().getDataSet().isReadOnly());
    }
}
