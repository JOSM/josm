// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;

/**
 * The conflict resolver receives the result of a {@link ConflictDialog}. It should then apply the resulution the user selected.
 */
public interface IConflictResolver {

    void deletePrimitive(boolean deleted);

    void populate(Conflict<? extends OsmPrimitive> conflict);

    void decideRemaining(MergeDecisionType decision);
}
