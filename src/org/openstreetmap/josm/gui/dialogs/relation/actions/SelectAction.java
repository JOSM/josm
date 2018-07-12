// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Select the currently edited relation.
 * @since 12933
 */
public class SelectAction extends AbstractRelationEditorAction {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code SelectAction}.
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public SelectAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        putValue(NAME, tr("Select"));
        putValue(SHORT_DESCRIPTION, tr("Select the currently edited relation"));
        new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation toSelect = editorAccess.getEditor().getRelation();
        if (toSelect == null)
            return;
        getLayer().data.setSelected(toSelect);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getEditor().getRelationSnapshot() != null);
    }
}
