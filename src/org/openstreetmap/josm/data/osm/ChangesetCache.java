// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * ChangesetCache is global in-memory cache for changesets downloaded from
 * an OSM API server. The unique instance is available as singleton, see
 * {@link #getInstance()}.
 *
 * Clients interested in cache updates can register for {@link ChangesetCacheEvent}s
 * using {@link #addChangesetCacheListener(ChangesetCacheListener)}. They can use
 * {@link #removeChangesetCacheListener(ChangesetCacheListener)} to unregister as
 * cache event listener.
 *
 * The cache itself listens to {@link java.util.prefs.PreferenceChangeEvent}s. It
 * clears itself if the OSM API URL is changed in the preferences.
 *
 * {@link ChangesetCacheEvent}s are delivered on the EDT.
 *
 */
public final class ChangesetCache implements PreferenceChangedListener {
    /** the unique instance */
    private static final ChangesetCache INSTANCE = new ChangesetCache();

    /** the cached changesets */
    private final Map<Integer, Changeset> cache = new HashMap<>();

    private final CopyOnWriteArrayList<ChangesetCacheListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code ChangesetCache}.
     */
    private ChangesetCache() {
        Main.pref.addPreferenceChangeListener(this);
    }

    /**
     * Replies the unique instance of the cache
     * @return the unique instance of the cache
     */
    public static ChangesetCache getInstance() {
        return INSTANCE;
    }

    /**
     * Add a changeset cache listener.
     * @param listener changeset cache listener to add
     */
    public void addChangesetCacheListener(ChangesetCacheListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * Remove a changeset cache listener.
     * @param listener changeset cache listener to remove
     */
    public void removeChangesetCacheListener(ChangesetCacheListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void fireChangesetCacheEvent(final ChangesetCacheEvent e) {
        GuiHelper.runInEDT(() -> {
            for (ChangesetCacheListener l: listeners) {
                l.changesetCacheUpdated(e);
            }
        });
    }

    private void update(Changeset cs, DefaultChangesetCacheEvent e) {
        if (cs == null) return;
        if (cs.isNew()) return;
        Changeset inCache = cache.get(cs.getId());
        if (inCache != null) {
            inCache.mergeFrom(cs);
            e.rememberUpdatedChangeset(inCache);
        } else {
            e.rememberAddedChangeset(cs);
            cache.put(cs.getId(), cs);
        }
    }

    /**
     * Update a single changeset.
     * @param cs changeset to update
     */
    public void update(Changeset cs) {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        update(cs, e);
        fireChangesetCacheEvent(e);
    }

    /**
     * Update a collection of changesets.
     * @param changesets changesets to update
     */
    public void update(Collection<Changeset> changesets) {
        if (changesets == null || changesets.isEmpty()) return;
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        for (Changeset cs: changesets) {
            update(cs, e);
        }
        fireChangesetCacheEvent(e);
    }

    /**
     * Determines if the cache contains an entry for given changeset identifier.
     * @param id changeset id
     * @return {@code true} if the cache contains an entry for {@code id}
     */
    public boolean contains(int id) {
        if (id <= 0) return false;
        return cache.get(id) != null;
    }

    /**
     * Determines if the cache contains an entry for given changeset.
     * @param cs changeset
     * @return {@code true} if the cache contains an entry for {@code cs}
     */
    public boolean contains(Changeset cs) {
        if (cs == null) return false;
        if (cs.isNew()) return false;
        return contains(cs.getId());
    }

    /**
     * Returns the entry for given changeset identifier.
     * @param id changeset id
     * @return the entry for given changeset identifier, or null
     */
    public Changeset get(int id) {
        return cache.get(id);
    }

    /**
     * Returns the list of changesets contained in the cache.
     * @return the list of changesets contained in the cache
     */
    public Set<Changeset> getChangesets() {
        return new HashSet<>(cache.values());
    }

    private void remove(int id, DefaultChangesetCacheEvent e) {
        if (id <= 0) return;
        Changeset cs = cache.get(id);
        if (cs == null) return;
        cache.remove(id);
        e.rememberRemovedChangeset(cs);
    }

    /**
     * Remove the entry for the given changeset identifier.
     * A {@link ChangesetCacheEvent} is fired.
     * @param id changeset id
     */
    public void remove(int id) {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        remove(id, e);
        if (!e.isEmpty()) {
            fireChangesetCacheEvent(e);
        }
    }

    /**
     * Remove the entry for the given changeset.
     * A {@link ChangesetCacheEvent} is fired.
     * @param cs changeset
     */
    public void remove(Changeset cs) {
        if (cs == null) return;
        if (cs.isNew()) return;
        remove(cs.getId());
    }

    /**
     * Removes the changesets in <code>changesets</code> from the cache.
     * A {@link ChangesetCacheEvent} is fired.
     *
     * @param changesets the changesets to remove. Ignored if null.
     */
    public void remove(Collection<Changeset> changesets) {
        if (changesets == null) return;
        DefaultChangesetCacheEvent evt = new DefaultChangesetCacheEvent(this);
        for (Changeset cs : changesets) {
            if (cs == null || cs.isNew()) {
                continue;
            }
            remove(cs.getId(), evt);
        }
        if (!evt.isEmpty()) {
            fireChangesetCacheEvent(evt);
        }
    }

    /**
     * Returns the number of changesets contained in the cache.
     * @return the number of changesets contained in the cache
     */
    public int size() {
        return cache.size();
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        for (Changeset cs: cache.values()) {
            e.rememberRemovedChangeset(cs);
        }
        cache.clear();
        fireChangesetCacheEvent(e);
    }

    /**
     * Replies the list of open changesets.
     * @return The list of open changesets
     */
    public List<Changeset> getOpenChangesets() {
        return cache.values().stream()
                .filter(Changeset::isOpen)
                .collect(Collectors.toList());
    }

    /**
     * If the current user {@link UserIdentityManager#isAnonymous() is known}, the {@link #getOpenChangesets() open changesets}
     * for the {@link UserIdentityManager#isCurrentUser(User) current user} are returned. Otherwise,
     * the unfiltered {@link #getOpenChangesets() open changesets} are returned.
     *
     * @return a list of changesets
     */
    public List<Changeset> getOpenChangesetsForCurrentUser() {
        if (UserIdentityManager.getInstance().isAnonymous()) {
            return getOpenChangesets();
        } else {
            return new ArrayList<>(SubclassFilteredCollection.filter(getOpenChangesets(),
                    object -> UserIdentityManager.getInstance().isCurrentUser(object.getUser())));
        }
    }

    /* ------------------------------------------------------------------------- */
    /* interface PreferenceChangedListener                                       */
    /* ------------------------------------------------------------------------- */
    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || !"osm-server.url".equals(e.getKey()))
            return;

        // clear the cache when the API url changes
        if (e.getOldValue() == null || e.getNewValue() == null || !e.getOldValue().equals(e.getNewValue())) {
            clear();
        }
    }
}
