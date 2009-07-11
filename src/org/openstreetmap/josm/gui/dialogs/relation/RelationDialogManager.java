// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import org.openstreetmap.josm.data.osm.Relation;

/**
 * RelationDialogManager keeps track of the open relation editors.
 *
 */
public class RelationDialogManager extends WindowAdapter {

    private HashMap<Relation, RelationEditor> openDialogs;

    public RelationDialogManager(){
        openDialogs = new HashMap<Relation, RelationEditor>();
    }

    public void register(Relation relation, RelationEditor editor) {
        openDialogs.put(relation, editor);
        editor.addWindowListener(this);
    }

    public boolean isOpenInEditor(Relation relation) {
        return openDialogs.keySet().contains(relation);
    }

    public RelationEditor getEditorForRelation(Relation relation) {
        return openDialogs.get(relation);
    }

    @Override
    public void windowClosed(WindowEvent e) {
        RelationEditor editor = ((RelationEditor)e.getWindow());
        Relation editedRelation = null;
        for (Relation r : openDialogs.keySet()) {
            if (openDialogs.get(r).equals(editor)) {
                editedRelation = r;
                break;
            }
        }
        if (editedRelation != null) {
            openDialogs.remove(editedRelation);
        }
    }
}
