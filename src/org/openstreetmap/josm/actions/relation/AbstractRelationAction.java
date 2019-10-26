// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.actions.IPrimitiveAction;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Ancestor for all actions that want to work with relation collection and
 * to be disabled if the collection is empty
 * @since 5793
 * @since 13957 (signature)
 */
public abstract class AbstractRelationAction extends AbstractAction implements IPrimitiveAction {
    /** relation collection */
    protected transient Collection<IRelation<?>> relations = Collections.<IRelation<?>>emptySet();

    /**
     * Returns the relations contained in the given collection.
     * @param primitives collection of primitives
     * @return the relation contained in {@code primitives}
     */
    protected static final Collection<IRelation<?>> getRelations(Collection<? extends IPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            return Collections.<IRelation<?>>emptySet();
        } else {
            return new SubclassFilteredCollection<>(primitives, IRelation.class::isInstance);
        }
    }

    @Override
    public void setPrimitives(Collection<? extends IPrimitive> primitives) {
        this.relations = getRelations(primitives);
        updateEnabledState();
    }

    /**
     * Override in subclasses to update the enabled state of the action when something changes.
     */
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty());
    }

    protected final boolean canModify() {
        SubclassFilteredCollection<IRelation<?>, Relation> filteredRelations = Utils.filteredCollection(relations, Relation.class);
        return OsmUtils.isOsmCollectionEditable(filteredRelations) && filteredRelations.parallelStream().anyMatch(r -> !r.isDeleted());
    }

    protected final boolean canDownload() {
        if (relations.isEmpty()) {
            return false;
        }
        OsmData<?, ?, ?, ?> ds = relations.iterator().next().getDataSet();
        return !NetworkManager.isOffline(OnlineResource.OSM_API)
            && ds != null && !ds.isLocked() && DownloadPolicy.BLOCKED != ds.getDownloadPolicy();
    }

    protected void setHelpId(String helpId) {
        putValue("help", helpId);
    }
}
