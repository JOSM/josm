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
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

/**
 * ChangesetCache is global in-memory cache for changesets downloaded from
 * an OSM API server. The unique instance is available as singleton, see
 * {@see #getInstance()}.
 *
 * Clients interested in cache updates can register for {@see ChangesetCacheEvent}s
 * using {@see #addChangesetCacheListener(ChangesetCacheListener)}. They can use
 * {@see #removeChangesetCacheListener(ChangesetCacheListener)} to unregister as
 * cache event listener.
 *
 * The cache itself listens to {@see java.util.prefs.PreferenceChangeEvent}s. It
 * clears itself if the OSM API URL is changed in the preferences.
 *
 * {@see ChangesetCacheEvent}s are delivered on the EDT.
 *
 */
public class ChangesetCache implements PreferenceChangedListener{
    @SuppressWarnings("unused")
    static private final Logger logger = Logger.getLogger(ChangesetCache.class.getName());

    /** the unique instance */
    static private final ChangesetCache instance = new ChangesetCache();

    /**
     * Replies the unique instance of the cache
     *
     * @return the unique instance of the cache
     */
    public static ChangesetCache getInstance() {
        return instance;
    }

    /** the cached changesets */
    private final Map<Integer, Changeset> cache  = new HashMap<Integer, Changeset>();

    private final CopyOnWriteArrayList<ChangesetCacheListener> listeners =
        new CopyOnWriteArrayList<ChangesetCacheListener>();

    private ChangesetCache() {
        Main.pref.addPreferenceChangeListener(this);
    }

    public void addChangesetCacheListener(ChangesetCacheListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChangesetCacheListener(ChangesetCacheListener listener) {
        listeners.remove(listener);
    }

    protected void fireChangesetCacheEvent(final ChangesetCacheEvent e) {
        Runnable r = new Runnable() {
            public void run() {
                for(ChangesetCacheListener l: listeners) {
                    l.changesetCacheUpdated(e);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    protected void update(Changeset cs, DefaultChangesetCacheEvent e) {
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

    public void update(Changeset cs) {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        update(cs, e);
        fireChangesetCacheEvent(e);
    }

    public void update(Collection<Changeset> changesets) {
        if (changesets == null || changesets.isEmpty()) return;
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        for (Changeset cs: changesets) {
            update(cs, e);
        }
        fireChangesetCacheEvent(e);
    }

    public boolean contains(int id) {
        if (id <=0) return false;
        return cache.get(id) != null;
    }

    public boolean contains(Changeset cs) {
        if (cs == null) return false;
        if (cs.isNew()) return false;
        return contains(cs.getId());
    }

    public Changeset get(int id) {
        return cache.get(id);
    }

    public Set<Changeset> getChangesets() {
        return new HashSet<Changeset>(cache.values());
    }

    protected void remove(int id, DefaultChangesetCacheEvent e) {
        if (id <= 0) return;
        Changeset cs = cache.get(id);
        if (cs == null) return;
        cache.remove(id);
        e.rememberRemovedChangeset(cs);
    }

    public void remove(int id) {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        remove(id, e);
        if (! e.isEmpty()) {
            fireChangesetCacheEvent(e);
        }
    }

    public void remove(Changeset cs) {
        if (cs == null) return;
        if (cs.isNew()) return;
        remove(cs.getId());
    }

    /**
     * Removes the changesets in <code>changesets</code> from the cache. A
     * {@see ChangesetCacheEvent} is fired.
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
        if (! evt.isEmpty()) {
            fireChangesetCacheEvent(evt);
        }
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        DefaultChangesetCacheEvent e = new DefaultChangesetCacheEvent(this);
        for (Changeset cs: cache.values()) {
            e.rememberRemovedChangeset(cs);
        }
        cache.clear();
        fireChangesetCacheEvent(e);
    }

    public List<Changeset> getOpenChangesets() {
        List<Changeset> ret = new ArrayList<Changeset>();
        for (Changeset cs: cache.values()) {
            if (cs.isOpen()) {
                ret.add(cs);
            }
        }
        return ret;
    }

    /* ------------------------------------------------------------------------- */
    /* interface PreferenceChangedListener                                       */
    /* ------------------------------------------------------------------------- */
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || ! e.getKey().equals("osm-server.url"))
            return;

        // clear the cache when the API url changes
        if (e.getOldValue() == null || e.getNewValue() == null || !e.getOldValue().equals(e.getNewValue())) {
            clear();
        }
    }
}
