// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Select the currently edited relation.
 * @since 12933
 */
public class SelectAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code SelectAction}.
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public SelectAction(OsmDataLayer layer, IRelationEditor editor) {
        super(null, null, null, layer, editor);
        putValue(NAME, tr("Select"));
        putValue(SHORT_DESCRIPTION, tr("Select the currently edited relation"));
        new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation toSelect = editor.getRelation();
        if (toSelect == null)
            return;
        layer.data.setSelected(toSelect);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editor.getRelationSnapshot() != null);
    }
}
