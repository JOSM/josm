// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.Collection;

import javax.swing.Action;

import org.openstreetmap.josm.data.osm.IPrimitive;

/**
 * Interface used to enable/disable all primitive-related actions, even those registered by plugins.
 * @since 13957
 */
public interface IPrimitiveAction extends Action {

    /**
     * Specifies the working set of primitives.
     * @param primitives The new working set of primitives. Can be null or empty
     */
    void setPrimitives(Collection<? extends IPrimitive> primitives);
}
