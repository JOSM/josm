// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Super interface of relation editors.
 * @since 9659
 */
public interface IRelationEditor {

    /**
     * Replies the currently edited relation
     *
     * @return the currently edited relation
     */
    Relation getRelation();

    /**
     * Sets the currently edited relation. Creates a snapshot of the current
     * state of the relation. See {@link #getRelationSnapshot()}
     *
     * @param relation the relation
     */
    void setRelation(Relation relation);

    /**
     * Replies the state of the edited relation when the editor has been launched.
     * @return the state of the edited relation when the editor has been launched
     */
    Relation getRelationSnapshot();

    /**
     * Replies true if the currently edited relation has been changed elsewhere.
     *
     * In this case a relation editor can't apply updates to the relation directly. Rather,
     * it has to create a conflict.
     *
     * @return true if the currently edited relation has been changed elsewhere.
     */
    default boolean isDirtyRelation() {
        return isDirtyRelation(false);
    }

    /**
     * Replies true if the currently edited relation has been changed elsewhere.
     *
     * In this case a relation editor can't apply updates to the relation directly. Rather,
     * it has to create a conflict.
     *
     * @param ignoreUninterestingTags whether to ignore uninteresting tag changes
     * @return true if the currently edited relation has been changed elsewhere.
     */
    boolean isDirtyRelation(boolean ignoreUninterestingTags);

    /**
     * Replies true if the relation has been changed in the editor (but not yet applied).
     *
     * Reloading data from the relation would cause the pending changes to be lost.
     *
     * @return true if the currently edited relation has been changed in the editor.
     */
    boolean isDirtyEditor();

    /**
     * Reloads data from relation.
     */
    void reloadDataFromRelation();

    /**
     * Replies the {@link OsmDataLayer} in whose context this relation editor is open
     *
     * @return the {@link OsmDataLayer} in whose context this relation editor is open
     */
    OsmDataLayer getLayer();
}
