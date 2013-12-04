// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * AutoCompletionManager holds a cache of keys with a list of
 * possible auto completion values for each key.
 *
 * Each DataSet is assigned one AutoCompletionManager instance such that
 * <ol>
 *   <li>any key used in a tag in the data set is part of the key list in the cache</li>
 *   <li>any value used in a tag for a specific key is part of the autocompletion list of
 *     this key</li>
 * </ol>
 *
 * Building up auto completion lists should not
 * slow down tabbing from input field to input field. Looping through the complete
 * data set in order to build up the auto completion list for a specific input
 * field is not efficient enough, hence this cache.
 *
 * TODO: respect the relation type for member role autocompletion
 */
public class AutoCompletionManager implements DataSetListener {

    /** If the dirty flag is set true, a rebuild is necessary. */
    protected boolean dirty;
    /** The data set that is managed */
    protected DataSet ds;

    /**
     * the cached tags given by a tag key and a list of values for this tag
     * only accessed by getTagCache(), rebuild() and cachePrimitiveTags()
     * use getTagCache() accessor
     */
    protected MultiMap<String, String> tagCache;
    /**
     * the same as tagCache but for the preset keys and values
     * can be accessed directly
     */
    protected static final MultiMap<String, String> presetTagCache = new MultiMap<String, String>();
    /**
     * the cached list of member roles
     * only accessed by getRoleCache(), rebuild() and cacheRelationMemberRoles()
     * use getRoleCache() accessor
     */
    protected Set<String> roleCache;
    /**
     * the same as roleCache but for the preset roles
     * can be accessed directly
     */
    protected static final Set<String> presetRoleCache = new HashSet<String>();

    public AutoCompletionManager(DataSet ds) {
        this.ds = ds;
        dirty = true;
    }

    protected MultiMap<String, String> getTagCache() {
        if (dirty) {
            rebuild();
            dirty = false;
        }
        return tagCache;
    }

    protected Set<String> getRoleCache() {
        if (dirty) {
            rebuild();
            dirty = false;
        }
        return roleCache;
    }

    /**
     * initializes the cache from the primitives in the dataset
     *
     */
    protected void rebuild() {
        tagCache = new MultiMap<String, String>();
        roleCache = new HashSet<String>();
        cachePrimitives(ds.allNonDeletedCompletePrimitives());
    }

    protected void cachePrimitives(Collection<? extends OsmPrimitive> primitives) {
        for (OsmPrimitive primitive : primitives) {
            cachePrimitiveTags(primitive);
            if (primitive instanceof Relation) {
                cacheRelationMemberRoles((Relation) primitive);
            }
        }
    }

    /**
     * make sure, the keys and values of all tags held by primitive are
     * in the auto completion cache
     *
     * @param primitive an OSM primitive
     */
    protected void cachePrimitiveTags(OsmPrimitive primitive) {
        for (String key: primitive.keySet()) {
            String value = primitive.get(key);
            tagCache.put(key, value);
        }
    }

    /**
     * Caches all member roles of the relation <code>relation</code>
     *
     * @param relation the relation
     */
    protected void cacheRelationMemberRoles(Relation relation){
        for (RelationMember m: relation.getMembers()) {
            if (m.hasRole()) {
                roleCache.add(m.getRole());
            }
        }
    }

    /**
     * Initialize the cache for presets. This is done only once.
     */
    public static void cachePresets(Collection<TaggingPreset> presets) {
        for (final TaggingPreset p : presets) {
            for (TaggingPresetItem item : p.data) {
                if (item instanceof TaggingPresetItems.KeyedItem) {
                    TaggingPresetItems.KeyedItem ki = (TaggingPresetItems.KeyedItem) item;
                    if (ki.key != null && ki.getValues() != null) {
                        try {
                            presetTagCache.putAll(ki.key, ki.getValues());
                        } catch (NullPointerException e) {
                            Main.error(p+": Unable to cache "+ki);
                        }
                    }
                } else if (item instanceof TaggingPresetItems.Roles) {
                    TaggingPresetItems.Roles r = (TaggingPresetItems.Roles) item;
                    for (TaggingPresetItems.Role i : r.roles) {
                        if (i.key != null) {
                            presetRoleCache.add(i.key);
                        }
                    }
                }
            }
        }
    }

    /**
     * replies the keys held by the cache
     *
     * @return the list of keys held by the cache
     */
    protected List<String> getDataKeys() {
        return new ArrayList<String>(getTagCache().keySet());
    }

    protected List<String> getPresetKeys() {
        return new ArrayList<String>(presetTagCache.keySet());
    }

    /**
     * replies the auto completion values allowed for a specific key. Replies
     * an empty list if key is null or if key is not in {@link #getKeys()}.
     *
     * @param key
     * @return the list of auto completion values
     */
    protected List<String> getDataValues(String key) {
        return new ArrayList<String>(getTagCache().getValues(key));
    }

    protected static List<String> getPresetValues(String key) {
        return new ArrayList<String>(presetTagCache.getValues(key));
    }

    /**
     * Replies the list of member roles
     *
     * @return the list of member roles
     */
    public List<String> getMemberRoles() {
        return new ArrayList<String>(getRoleCache());
    }

    /**
     * Populates the an {@link AutoCompletionList} with the currently cached
     * member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.add(presetRoleCache, AutoCompletionItemPriority.IS_IN_STANDARD);
        list.add(getRoleCache(), AutoCompletionItemPriority.IS_IN_DATASET);
    }

    /**
     * Populates the an {@link AutoCompletionList} with the currently cached tag keys
     *
     * @param list the list to populate
     */
    public void populateWithKeys(AutoCompletionList list) {
        list.add(getPresetKeys(), AutoCompletionItemPriority.IS_IN_STANDARD);
        list.add(new AutoCompletionListItem("source", AutoCompletionItemPriority.IS_IN_STANDARD));
        list.add(getDataKeys(), AutoCompletionItemPriority.IS_IN_DATASET);
    }

    /**
     * Populates the an {@link AutoCompletionList} with the currently cached
     * values for a tag
     *
     * @param list the list to populate
     * @param key the tag key
     */
    public void populateWithTagValues(AutoCompletionList list, String key) {
        populateWithTagValues(list, Arrays.asList(key));
    }

    /**
     * Populates the an {@link AutoCompletionList} with the currently cached
     * values for some given tags
     *
     * @param list the list to populate
     * @param keys the tag keys
     */
    public void populateWithTagValues(AutoCompletionList list, List<String> keys) {
        for (String key : keys) {
            list.add(getPresetValues(key), AutoCompletionItemPriority.IS_IN_STANDARD);
            list.add(getDataValues(key), AutoCompletionItemPriority.IS_IN_DATASET);
        }
    }

    /**
     * Returns the currently cached tag keys.
     * @return a list of tag keys
     */
    public List<AutoCompletionListItem> getKeys() {
        AutoCompletionList list = new AutoCompletionList();
        populateWithKeys(list);
        return list.getList();
    }

    /**
     * Returns the currently cached tag values for a given tag key.
     * @param key the tag key
     * @return a list of tag values
     */
    public List<AutoCompletionListItem> getValues(String key) {
        return getValues(Arrays.asList(key));
    }

    /**
     * Returns the currently cached tag values for a given list of tag keys.
     * @param keys the tag keys
     * @return a list of tag values
     */
    public List<AutoCompletionListItem> getValues(List<String> keys) {
        AutoCompletionList list = new AutoCompletionList();
        populateWithTagValues(list, keys);
        return list.getList();
    }

    /*********************************************************
     * Implementation of the DataSetListener interface
     *
     **/

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        if (dirty)
            return;
        cachePrimitives(event.getPrimitives());
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        dirty = true;
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        if (dirty)
            return;
        Map<String, String> newKeys = event.getPrimitive().getKeys();
        Map<String, String> oldKeys = event.getOriginalKeys();

        if (!newKeys.keySet().containsAll(oldKeys.keySet())) {
            // Some keys removed, might be the last instance of key, rebuild necessary
            dirty = true;
        } else {
            for (Entry<String, String> oldEntry: oldKeys.entrySet()) {
                if (!oldEntry.getValue().equals(newKeys.get(oldEntry.getKey()))) {
                    // Value changed, might be last instance of value, rebuild necessary
                    dirty = true;
                    return;
                }
            }
            cachePrimitives(Collections.singleton(event.getPrimitive()));
        }
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {/* ignored */}

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignored */}

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        dirty = true; // TODO: not necessary to rebuid if a member is added
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignored */}

    @Override
    public void dataChanged(DataChangedEvent event) {
        dirty = true;
    }
}
