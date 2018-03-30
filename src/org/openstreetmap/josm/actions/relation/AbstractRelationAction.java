// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.OsmPrimitiveAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Ancestor for all actions that want to work with relation collection and
 * to be disabled if the collection is empty
 * @since 5793
 */
public abstract class AbstractRelationAction extends AbstractAction implements OsmPrimitiveAction {
    /** relation collection */
    protected transient Collection<Relation> relations = Collections.<Relation>emptySet();

    /**
     * Returns the relations contained in the given collection.
     * @param primitives collection of primitives
     * @return the relation contained in {@code primitives}
     */
    protected static final Collection<Relation> getRelations(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            return Collections.<Relation>emptySet();
        } else {
            return new SubclassFilteredCollection<>(primitives, Relation.class::isInstance);
        }
    }

    @Override
    public void setPrimitives(Collection<? extends OsmPrimitive> primitives) {
        this.relations = getRelations(primitives);
        updateEnabledState();
    }

    /**
     * Override in subclasses to update the enabled state of the action when something changes.
     */
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty());
    }

    protected final boolean canDownload() {
        if (relations.isEmpty()) {
            return false;
        }
        DataSet ds = relations.iterator().next().getDataSet();
        return !Main.isOffline(OnlineResource.OSM_API)
            && ds != null && !ds.isLocked() && !DownloadPolicy.BLOCKED.equals(ds.getDownloadPolicy());
    }
}
