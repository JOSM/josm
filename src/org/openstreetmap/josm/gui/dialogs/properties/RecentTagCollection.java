// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.CollectionProperty;

class RecentTagCollection {

    private final Map<Tag, Void> recentTags;

    RecentTagCollection(final int capacity) {
        // LRU cache for recently added tags (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html)
        recentTags = new LinkedHashMap<Tag, Void>(capacity + 1, 1.1f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Tag, Void> eldest) {
                return size() > capacity;
            }
        };
    }

    public void loadFromPreference(CollectionProperty property) {
        recentTags.clear();
        Iterator<String> it = property.get().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = it.next();
            recentTags.put(new Tag(key, value), null);
        }
    }

    public void saveToPreference(CollectionProperty property) {
        List<String> c = new ArrayList<>(recentTags.size() * 2);
        for (Tag t : recentTags.keySet()) {
            c.add(t.getKey());
            c.add(t.getValue());
        }
        property.put(c);
    }

    public void add(Tag key) {
        recentTags.put(key, null);
    }

    public boolean isEmpty() {
        return recentTags.isEmpty();
    }

    public List<Tag> toList() {
        return new ArrayList<>(recentTags.keySet());
    }
}
