// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Creates a new relation with a copy of the current editor state.
 * @since 9496
 */
public class DuplicateRelationAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DuplicateRelationAction}.
     * @param memberTableModel member table model
     * @param tagEditorModel tag editor model
     * @param layer OSM data layer
     */
    public DuplicateRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
        // FIXME provide an icon
        new ImageProvider("duplicate").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Duplicate"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation copy = new Relation();
        getTagModel().applyToPrimitive(copy);
        editorAccess.getMemberTableModel().applyToRelation(copy);
        if (!GraphicsEnvironment.isHeadless()) {
            RelationEditor.getEditor(getLayer(), copy, editorAccess.getMemberTableModel().getSelectedMembers()).setVisible(true);
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
