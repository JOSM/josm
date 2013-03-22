// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.data.osm.Relation;

/**
 * Ancestor for all actions that want to work with relation collection and 
 * to be disabled is the collection is empty
 * @since 5793
 */
public abstract class AbstractRelationAction extends AbstractAction {
    protected Collection<Relation> relations = Collections.<Relation>emptySet();
    
    /**
     * Specifies the working set of relations.
     * @param relations The new working set of relations. Can be null or empty
     */
    public void setRelations(Collection<Relation> relations) {
        if (relations==null) {
            this.relations = Collections.<Relation>emptySet();
        } else {
            this.relations = relations;
        }
        updateEnabledState();
    }
    
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty());
    }
}
