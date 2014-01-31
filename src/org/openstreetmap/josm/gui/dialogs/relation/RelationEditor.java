// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public abstract class RelationEditor extends ExtendedDialog {
    /** the property name for the current relation.
     * @see #setRelation(Relation)
     * @see #getRelation()
     */
    static public final String RELATION_PROP = RelationEditor.class.getName() + ".relation";

    /** the property name for the current relation snapshot
     * @see #getRelationSnapshot()
     */
    static public final String RELATION_SNAPSHOT_PROP = RelationEditor.class.getName() + ".relationSnapshot";

    /** the list of registered relation editor classes */
    private static List<Class<RelationEditor>> editors = new ArrayList<Class<RelationEditor>>();

    /**
     * Registers a relation editor class. Depending on the type of relation to be edited
     * {@link #getEditor(OsmDataLayer, Relation, Collection)} will create an instance of
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
     * @param layer the data layer the relation is a member of
     * @param r the relation to be edited
     * @param selectedMembers a collection of relation members which shall be selected when the
     * editor is first launched
     * @return an instance of RelationEditor suitable for editing that kind of relation
     */
    public static RelationEditor getEditor(OsmDataLayer layer, Relation r, Collection<RelationMember> selectedMembers) {
        for (Class<RelationEditor> e : editors) {
            try {
                Method m = e.getMethod("canEdit", Relation.class);
                Boolean canEdit = (Boolean) m.invoke(null, r);
                if (canEdit) {
                    Constructor<RelationEditor> con = e.getConstructor(Relation.class, Collection.class);
                    return con.newInstance(layer, r, selectedMembers);
                }
            } catch (Exception ex) {
                Main.warn(ex);
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
     * @param layer  the {@link OsmDataLayer} in whose context a relation is edited. Must not be null.
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
                false,
                false
        );
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
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
     * state of the relation. See {@link #getRelationSnapshot()}
     *
     * @param relation the relation
     */
    protected void setRelation(Relation relation) {
        setRelationSnapshot((relation == null) ? null : new Relation(relation));
        Relation oldValue = this.relation;
        this.relation = relation;
        if (this.relation != oldValue) {
            support.firePropertyChange(RELATION_PROP, oldValue, this.relation);
        }
        updateTitle();
    }

    /**
     * Replies the {@link OsmDataLayer} in whose context this relation editor is
     * open
     *
     * @return the {@link OsmDataLayer} in whose context this relation editor is
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

    protected void setRelationSnapshot(Relation snapshot) {
        Relation oldValue = relationSnapshot;
        relationSnapshot = snapshot;
        if (relationSnapshot != oldValue) {
            support.firePropertyChange(RELATION_SNAPSHOT_PROP, oldValue, relationSnapshot);
        }
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

    /* ----------------------------------------------------------------------- */
    /* property change support                                                 */
    /* ----------------------------------------------------------------------- */
    final private PropertyChangeSupport support = new PropertyChangeSupport(this);

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.support.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.support.removePropertyChangeListener(listener);
    }
}
