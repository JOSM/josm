// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Abstract relation editor.
 * @since 1599
 */
public abstract class RelationEditor extends ExtendedDialog implements IRelationEditor {
    private static final long serialVersionUID = 1L;

    /** the property name for the current relation.
     * @see #setRelation(Relation)
     * @see #getRelation()
     */
    public static final String RELATION_PROP = RelationEditor.class.getName() + ".relation";

    /** the property name for the current relation snapshot
     * @see #getRelationSnapshot()
     */
    public static final String RELATION_SNAPSHOT_PROP = RelationEditor.class.getName() + ".relationSnapshot";

    /** The relation that this editor is working on. */
    private transient Relation relation;

    /** The version of the relation when editing is started. This is null if a new relation is created. */
    private transient Relation relationSnapshot;

    /** The data layer the relation belongs to */
    private final transient OsmDataLayer layer;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * Creates a new relation editor
     *
     * @param layer the {@link OsmDataLayer} in whose context a relation is edited. Must not be null.
     * @param relation the relation. Can be null if a new relation is to be edited.
     * @throws IllegalArgumentException if layer is null
     */
    protected RelationEditor(OsmDataLayer layer, Relation relation) {
        super(Main.parent,
                "",
                new String[] {tr("Apply Changes"), tr("Cancel")},
                false,
                false
        );
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        setRelation(relation);
        layer.removeRecentRelation(relation);
    }

    /**
     * This is a factory method that creates an appropriate RelationEditor instance suitable for editing the relation
     * that was passed in as an argument.
     *
     * This method is guaranteed to return a working RelationEditor.
     *
     * @param layer the data layer the relation is a member of
     * @param r the relation to be edited
     * @param selectedMembers a collection of relation members which shall be selected when the editor is first launched
     * @return an instance of RelationEditor suitable for editing that kind of relation
     */
    public static RelationEditor getEditor(OsmDataLayer layer, Relation r, Collection<RelationMember> selectedMembers) {
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

    @Override
    public final Relation getRelation() {
        return relation;
    }

    @Override
    public final void setRelation(Relation relation) {
        setRelationSnapshot((relation == null) ? null : new Relation(relation));
        Relation oldValue = this.relation;
        this.relation = relation;
        if (this.relation != oldValue) {
            support.firePropertyChange(RELATION_PROP, oldValue, this.relation);
        }
        updateTitle();
    }

    @Override
    public final OsmDataLayer getLayer() {
        return layer;
    }

    @Override
    public final Relation getRelationSnapshot() {
        return relationSnapshot;
    }

    protected final void setRelationSnapshot(Relation snapshot) {
        Relation oldValue = relationSnapshot;
        relationSnapshot = snapshot;
        if (relationSnapshot != oldValue) {
            support.firePropertyChange(RELATION_SNAPSHOT_PROP, oldValue, relationSnapshot);
        }
    }

    @Override
    public final boolean isDirtyRelation() {
        return !relation.hasEqualSemanticAttributes(relationSnapshot);
    }

    /* ----------------------------------------------------------------------- */
    /* property change support                                                 */
    /* ----------------------------------------------------------------------- */

    @Override
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        this.support.addPropertyChangeListener(listener);
    }

    @Override
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        this.support.removePropertyChangeListener(listener);
    }

    @Override
    public void dispose() {
        layer.setRecentRelation(relation);
        super.dispose();
    }
}
