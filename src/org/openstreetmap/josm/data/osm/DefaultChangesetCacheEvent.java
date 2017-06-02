// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The default event implementation that is used to indicate a change in the {@link ChangesetCache}
 */
public class DefaultChangesetCacheEvent implements ChangesetCacheEvent {

    private final Set<Changeset> added;
    private final Set<Changeset> modified;
    private final Set<Changeset> removed;
    private final ChangesetCache source;

    /**
     * Creates a basic, empty {@link ChangesetCacheEvent}
     * @param source The source changeset
     */
    public DefaultChangesetCacheEvent(ChangesetCache source) {
        this.source = source;
        added = new HashSet<>();
        modified = new HashSet<>();
        removed = new HashSet<>();
    }

    @Override
    public ChangesetCache getSource() {
        return source;
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
    public Collection<Changeset> getUpdatedChangesets() {
        return Collections.unmodifiableCollection(modified);
    }

    /**
     * Adds a {@link Changeset} to the added list
     * @param cs the {@link Changeset}
     */
    public void rememberAddedChangeset(Changeset cs) {
        if (cs == null) return;
        added.add(cs);
    }

    /**
     * Adds a {@link Changeset} to the updated list
     * @param cs the {@link Changeset}
     */
    public void rememberUpdatedChangeset(Changeset cs) {
        if (cs == null) return;
        modified.add(cs);
    }

    /**
     * Adds a {@link Changeset} to the removed list
     * @param cs the {@link Changeset}
     */
    public void rememberRemovedChangeset(Changeset cs) {
        if (cs == null) return;
        removed.add(cs);
    }

    /**
     * Checks if this event contains any {@link Changeset}s
     * @return <code>true</code> if changesets were added
     */
    public boolean isEmpty() {
        return added.isEmpty() && modified.isEmpty() && removed.isEmpty();
    }
}
