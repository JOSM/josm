// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * A listener that listens to changes on the {@link ChangesetCache}
 * @see ChangesetCacheEvent
 */
@FunctionalInterface
public interface ChangesetCacheListener {

    /**
     * Gets notified on changeset cache updates
     * @param event The event that happened
     */
    void changesetCacheUpdated(ChangesetCacheEvent event);
}
