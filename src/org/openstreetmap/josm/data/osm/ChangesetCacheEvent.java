// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;

/**
 * An event indicating a change in the {@link ChangesetCache}
 */
public interface ChangesetCacheEvent {
    /**
     * The changeset cache the change happened in.
     * @return The {@link ChangesetCache}
     */
    ChangesetCache getSource();

    /**
     * Gets a list of {@link Changeset}s that were added to the cache
     * @return The changesets
     */
    Collection<Changeset> getAddedChangesets();

    /**
     * Gets a list of {@link Changeset}s that were removed from the cache
     * @return The changesets
     */
    Collection<Changeset> getRemovedChangesets();

    /**
     * Gets a list of {@link Changeset}s that were changed
     * @return The changesets
     */
    Collection<Changeset> getUpdatedChangesets();
}
