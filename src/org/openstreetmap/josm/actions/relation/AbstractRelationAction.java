// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.actions.OsmPrimitiveAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Ancestor for all actions that want to work with relation collection and 
 * to be disabled is the collection is empty
 * @since 5793
 */
public abstract class AbstractRelationAction extends AbstractAction implements OsmPrimitiveAction {
    protected Collection<Relation> relations = Collections.<Relation>emptySet();

    protected static final Collection<Relation> getRelations(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            return Collections.<Relation>emptySet();
        } else {
            return new SubclassFilteredCollection<OsmPrimitive, Relation>(
                    primitives, OsmPrimitive.relationPredicate);
        }
    }
    
    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.relation.RelationAction#setPrimitives
     */
    @Override
    public void setPrimitives(Collection<? extends OsmPrimitive> primitives) {
        this.relations = getRelations(primitives);
        updateEnabledState();
    }
    
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty());
    }
}
