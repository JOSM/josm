// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.conflict;

/**
 * Interface for listeners that get notified when conflicts are added to or
 * removed from a {@link ConflictCollection}.
 */
public interface IConflictListener {
    /**
     * Called when conflicts are added.
     * @param conflicts collection to which conflicts have been added
     */
    void onConflictsAdded(ConflictCollection conflicts);

    /**
     * Called when conflicts are removed.
     * @param conflicts collection from which conflicts have been removed
     */
    void onConflictsRemoved(ConflictCollection conflicts);
}
