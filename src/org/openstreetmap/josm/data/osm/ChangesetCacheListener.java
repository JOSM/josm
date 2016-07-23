// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

@FunctionalInterface
public interface ChangesetCacheListener {

    void changesetCacheUpdated(ChangesetCacheEvent event);
}
