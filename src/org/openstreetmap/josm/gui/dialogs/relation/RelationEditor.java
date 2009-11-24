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
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public abstract class RelationEditor extends ExtendedDialog {

    /** the list of registered relation editor classes */
    private static ArrayList<Class<RelationEditor>> editors = new ArrayList<Class<RelationEditor>>();

    /**
     * Registers a relation editor class. Depending on the type of relation to be edited
     * {@see #getEditor(OsmDataLayer, Relation, Collection)} will create an instance of
     * this class.
     *
     * @param clazz the class
     */
    public void registerRelationEditor(Class<RelationEditor> clazz) {
        if (clazz == null) return;
        if (!editors.contains(clazz)) {
            editors.add(clazz);
        }
    }

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
        if (RelationDialogManager.getRelationDialogManager().isOpenInEditor(layer, r))
            return RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer, r);
        else {
            RelationEditor editor = new GenericRelationEditor(layer, r, selectedMembers);
            RelationDialogManager.getRelationDialogManager().positionOnScreen(editor);
            RelationDialogManager.getRelationDialogManager().register(layer, r, editor);
            return editor;
        }
    }

    /**
     * Creates a new relation editor
     *
     * @param layer  the {@see OsmDataLayer} in whose context a relation is edited. Must not be null.
     * @param relation the relation. Can be null if a new relation is to be edited.
     * @param selectedMembers  a collection of members in <code>relation</code> which the editor
     * should display selected when the editor is first displayed on screen
     * @throws IllegalArgumentException thrown if layer is null
     */
    protected RelationEditor(OsmDataLayer layer, Relation relation, Collection<RelationMember> selectedMembers)  throws IllegalArgumentException{
        // Initalizes ExtendedDialog
        super(Main.parent,
                "",
                new String[] { tr("Apply Changes"), tr("Cancel")},
                false
        );
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "layer"));
        this.layer = layer;
        setRelation(relation);
    }

    /**
     * updates the title of the relation editor
     */
    protected void updateTitle() {
        if (getRelation() == null) {
            setTitle(tr("Create new relation in layer ''{0}''", layer.getName()));
        } else if (getRelation().isNew()) {
            setTitle(tr("Edit new relation in layer ''{0}''", layer.getName()));
        } else {
            setTitle(tr("Edit relation #{0} in layer ''{1}''", relation.getId(), layer.getName()));
        }
    }
    /**
     * Replies the currently edited relation
     *
     * @return the currently edited relation
     */
    protected Relation getRelation() {
        return relation;
    }

    /**
     * Sets the currently edited relation. Creates a snapshot of the current
     * state of the relation. See {@see #getRelationSnapshot()}
     *
     * @param relation the relation
     */
    protected void setRelation(Relation relation) {
        this.relationSnapshot = (relation == null) ? null : new Relation(relation);
        this.relation = relation;
        updateTitle();
    }

    /**
     * Replies the {@see OsmDataLayer} in whose context this relation editor is
     * open
     *
     * @return the {@see OsmDataLayer} in whose context this relation editor is
     * open
     */
    protected OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Replies the state of the edited relation when the editor has been launched
     *
     * @return the state of the edited relation when the editor has been launched
     */
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
