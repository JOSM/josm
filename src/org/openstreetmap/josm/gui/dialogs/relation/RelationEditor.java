// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public abstract class RelationEditor extends ExtendedDialog {

    /** keeps track of open relation editors */
    static private RelationDialogManager relationDialogManager;

    /**
     * Replies the singleton {@see RelationDialogManager}
     * 
     * @return the singleton {@see RelationDialogManager}
     */
    static public RelationDialogManager getRelationDialogManager() {
        if (relationDialogManager == null) {
            relationDialogManager = new RelationDialogManager();
            Layer.listeners.add(relationDialogManager);
        }
        return relationDialogManager;
    }

    public static ArrayList<Class<RelationEditor>> editors = new ArrayList<Class<RelationEditor>>();

    /**
     * The relation that this editor is working on.
     */
    private Relation relation;

    /**
     * The version of the relation when editing is started.  This is
     * null if a new relation is created. */
    private Relation relationSnapshot;

    /** the data layer the relation belongs to */
    private OsmDataLayer layer;

    /**
     * This is a factory method that creates an appropriate RelationEditor
     * instance suitable for editing the relation that was passed in as an
     * argument.
     *
     * This method is guaranteed to return a working RelationEditor. If no
     * specific editor has been registered for the type of relation, then
     * a generic editor will be returned.
     * 
     * Editors can be registered by adding their class to the static list "editors"
     * in the RelationEditor class. When it comes to editing a relation, all
     * registered editors are queried via their static "canEdit" method whether they
     * feel responsible for that kind of relation, and if they return true
     * then an instance of that class will be used.
     *
     * @param r the relation to be edited
     * @return an instance of RelationEditor suitable for editing that kind of relation
     */
    public static RelationEditor getEditor(OsmDataLayer layer, Relation r, Collection<RelationMember> selectedMembers) {
        for (Class<RelationEditor> e : editors) {
            try {
                Method m = e.getMethod("canEdit", Relation.class);
                Boolean canEdit = (Boolean) m.invoke(null, r);
                if (canEdit) {
                    Constructor<RelationEditor> con = e.getConstructor(Relation.class, Collection.class);
                    RelationEditor editor = con.newInstance(layer, r, selectedMembers);
                    return editor;
                }
            } catch (Exception ex) {
                // plod on
            }
        }
        if (getRelationDialogManager().isOpenInEditor(layer, r))
            return getRelationDialogManager().getEditorForRelation(layer, r);
        else {
            RelationEditor editor = new GenericRelationEditor(layer, r, selectedMembers);
            getRelationDialogManager().register(layer, r, editor);
            return editor;
        }
    }

    protected RelationEditor(OsmDataLayer layer, Relation relation, Collection<RelationMember> selectedMembers)
    {
        // Initalizes ExtendedDialog
        super(Main.parent,
                relation == null
                ? tr("Create new relation in layer ''{0}''", layer.getName())
                        : (relation.id == 0
                                ? tr ("Edit new relation in layer ''{0}''", layer.getName())
                                        : tr("Edit relation #{0} in layer ''{1}''", relation.id, layer.getName())
                        ),
                        new String[] { tr("Apply Changes"), tr("Cancel")},
                        false
        );

        this.relationSnapshot = (relation == null) ? null : new Relation(relation);
        this.relation = relation;
        this.layer = layer;
    }

    protected Relation getRelation() {
        return relation;
    }

    protected OsmDataLayer getLayer() {
        return layer;
    }

    protected Relation getRelationSnapshot() {
        return relationSnapshot;
    }

    /**
     * Replies true if the currently edited relation has been changed elsewhere.
     * 
     * In this case a relation editor can't apply updates to the relation directly. Rather,
     * it has to create a conflict.
     * 
     * @return true if the currently edited relation has been changed elsewhere.
     */
    protected boolean isDirtyRelation() {
        return ! relation.hasEqualSemanticAttributes(relationSnapshot);
    }
}
