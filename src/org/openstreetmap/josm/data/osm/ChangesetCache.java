// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ChangesetCache {
    static private final Logger logger = Logger.getLogger(ChangesetCache.class.getName());
    static private final ChangesetCache instance = new ChangesetCache();

    public static ChangesetCache getInstance() {
        return instance;
    }

    private final Map<Integer, Changeset> cache  = new HashMap<Integer, Changeset>();

    private final CopyOnWriteArrayList<ChangesetCacheListener> listeners =
        new CopyOnWriteArrayList<ChangesetCacheListener>();

    private ChangesetCache() {
    }

    public void addChangesetCacheListener(ChangesetCacheListener listener) {
        synchronized(listeners) {
            if (listener != null && ! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeChangesetCacheListener(ChangesetCacheListener listener) {
        synchronized(listeners) {
            if (listener != null && listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    protected void fireChangesetCacheEvent(ChangesetCacheEvent e) {
        for(ChangesetCacheListener l: listeners) {
            l.changesetCacheUpdated(e);
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
}
