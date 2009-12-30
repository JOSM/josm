// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;

public interface ChangesetCacheEvent {
    ChangesetCache getSource();
    Collection<Changeset> getAddedChangesets();
    Collection<Changeset> getRemovedChangesets();
    Collection<Changeset> getUpdatedChangesets();

}
