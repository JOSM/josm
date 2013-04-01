// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.Collection;

import javax.swing.Action;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Interface used to enable/disable all primitive-related actions, even those registered by plugins.
 * @since 5821
 */
public interface OsmPrimitiveAction extends Action {

    /**
     * Specifies the working set of primitives.
     * @param primitives The new working set of primitives. Can be null or empty
     */
    public abstract void setPrimitives(Collection<? extends OsmPrimitive> primitives);
}
