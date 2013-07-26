// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DefaultChangesetCacheEvent implements ChangesetCacheEvent{

    private final Set<Changeset> added;
    private final Set<Changeset> modified;
    private final Set<Changeset> removed;
    private final ChangesetCache source;

    public DefaultChangesetCacheEvent(ChangesetCache source) {
        this.source = source;
        added = new HashSet<Changeset>();
        modified = new HashSet<Changeset>();
        removed = new HashSet<Changeset>();
    }

    @Override
    public Collection<Changeset> getAddedChangesets() {
        return Collections.unmodifiableCollection(added);
    }
    @Override
    public Collection<Changeset> getRemovedChangesets() {
        return Collections.unmodifiableCollection(removed);
    }
    @Override
    public ChangesetCache getSource() {
        return source;
    }
    @Override
    public Collection<Changeset> getUpdatedChangesets() {
        return Collections.unmodifiableCollection(modified);
    }

    public void rememberAddedChangeset(Changeset cs) {
        if (cs == null) return;
        added.add(cs);
    }

    public void rememberUpdatedChangeset(Changeset cs) {
        if (cs == null) return;
        modified.add(cs);
    }

    public void rememberRemovedChangeset(Changeset cs) {
        if (cs == null) return;
        removed.add(cs);
    }

    public boolean isEmpty() {
        return added.isEmpty() && modified.isEmpty() && removed.isEmpty();
    }
}
