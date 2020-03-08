// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.gui.util.LruCache;
import org.openstreetmap.josm.tools.Logging;

/**
 * Manages list of recently used tags that will be displayed in the {@link PropertiesDialog}.
 */
class RecentTagCollection {

    private final Map<Tag, Void> recentTags;
    private SearchCompiler.Match tagsToIgnore;

    RecentTagCollection(final int capacity) {
        recentTags = new LruCache(capacity);
        tagsToIgnore = SearchCompiler.Never.INSTANCE;
    }

    public void loadFromPreference(ListProperty property) {
        recentTags.clear();
        List<String> list = property.get();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (it.hasNext()) {
                String value = it.next();
                add(new Tag(key, value));
            } else {
                Logging.error("Invalid or incomplete list property: " + list);
                break;
            }
        }
    }

    public void saveToPreference(ListProperty property) {
        List<String> c = new ArrayList<>(recentTags.size() * 2);
        for (Tag t : recentTags.keySet()) {
            c.add(t.getKey());
            c.add(t.getValue());
        }
        property.put(c);
    }

    public void add(Tag tag) {
        if (!tagsToIgnore.match(tag)) {
            recentTags.put(tag, null);
        }
    }

    public boolean isEmpty() {
        return recentTags.isEmpty();
    }

    public List<Tag> toList() {
        return new ArrayList<>(recentTags.keySet());
    }

    public void setTagsToIgnore(SearchCompiler.Match tagsToIgnore) {
        this.tagsToIgnore = tagsToIgnore;
        recentTags.keySet().removeIf(tagsToIgnore::match);
    }

    public void setTagsToIgnore(SearchSetting tagsToIgnore) throws SearchParseError {
        setTagsToIgnore(tagsToIgnore.text.isEmpty() ? SearchCompiler.Never.INSTANCE : SearchCompiler.compile(tagsToIgnore));
    }

    public SearchSetting ignoreTag(Tag tagToIgnore, SearchSetting settingToUpdate) throws SearchParseError {
        final String forTag = SearchCompiler.buildSearchStringForTag(tagToIgnore.getKey(), tagToIgnore.getValue());
        settingToUpdate.text = settingToUpdate.text.isEmpty()
                ? forTag
                : settingToUpdate.text + " OR " + forTag;
        setTagsToIgnore(settingToUpdate);
        return settingToUpdate;
    }
}
