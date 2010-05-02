// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
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
    protected static MultiMap<String, String> presetTagCache = new MultiMap<String, String>();
    /** 
     * the cached list of member roles 
     * only accessed by getRoleCache(), rebuild() and cacheRelationMemberRoles()
     * use getRoleCache() accessor
     */
    protected  Set<String> roleCache;

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
            for (TaggingPreset.Item item : p.data) {
                if (item instanceof TaggingPreset.Check) {
                    TaggingPreset.Check ch = (TaggingPreset.Check) item;
                    if (ch.key == null) {
                        continue;
                    }
                    presetTagCache.put(ch.key, OsmUtils.falseval);
                    presetTagCache.put(ch.key, OsmUtils.trueval);
                } else if (item instanceof TaggingPreset.Combo) {
                    TaggingPreset.Combo co = (TaggingPreset.Combo) item;
                    if (co.key == null || co.values == null) {
                        continue;
                    }
                    for (String value : co.values.split(",")) {
                        presetTagCache.put(co.key, value);
                    }
                } else if (item instanceof TaggingPreset.Key) {
                    TaggingPreset.Key ky = (TaggingPreset.Key) item;
                    if (ky.key == null || ky.value == null) {
                        continue;
                    }
                    presetTagCache.put(ky.key, ky.value);
                } else if (item instanceof TaggingPreset.Text) {
                    TaggingPreset.Text tt = (TaggingPreset.Text) item;
                    if (tt.key == null) {
                        continue;
                    }
                    presetTagCache.putVoid(tt.key);
                    if (tt.default_ != null && !tt.default_.equals("")) {
                        presetTagCache.put(tt.key, tt.default_);
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
     * Populates the an {@see AutoCompletionList} with the currently cached
     * member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.add(getRoleCache(), AutoCompletionItemPritority.IS_IN_DATASET);
    }

    /**
     * Populates the an {@see AutoCompletionList} with the currently cached
     * tag keys
     *
     * @param list the list to populate
     * @param append true to add the keys to the list; false, to replace the keys
     * in the list by the keys in the cache
     */
    public void populateWithKeys(AutoCompletionList list) {
        list.add(getPresetKeys(), AutoCompletionItemPritority.IS_IN_STANDARD);
        list.add(getDataKeys(), AutoCompletionItemPritority.IS_IN_DATASET);
    }

    /**
     * Populates the an {@see AutoCompletionList} with the currently cached
     * values for a tag
     *
     * @param list the list to populate
     * @param key the tag key
     * @param append true to add the values to the list; false, to replace the values
     * in the list by the tag values
     */
    public void populateWithTagValues(AutoCompletionList list, String key) {
        list.add(getPresetValues(key), AutoCompletionItemPritority.IS_IN_STANDARD);
        list.add(getDataValues(key), AutoCompletionItemPritority.IS_IN_DATASET);
    }

    public List<AutoCompletionListItem> getKeys() {
        AutoCompletionList list = new AutoCompletionList();
        populateWithKeys(list);
        return new ArrayList<AutoCompletionListItem>(list.getList());
    }

    public List<AutoCompletionListItem> getValues(String key) {
        AutoCompletionList list = new AutoCompletionList();
        populateWithTagValues(list, key);
        return new ArrayList<AutoCompletionListItem>(list.getList());
    }

    /*********************************************************
     * Implementation of the DataSetListener interface
     *
     **/

    public void primtivesAdded(PrimitivesAddedEvent event) {
        if (dirty)
            return;
        cachePrimitives(event.getPrimitives());
    }

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        dirty = true;
    }

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

    public void nodeMoved(NodeMovedEvent event) {/* ignored */}

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignored */}

    public void relationMembersChanged(RelationMembersChangedEvent event) {
        dirty = true; // TODO: not necessary to rebuid if a member is added
    }

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignored */}

    public void dataChanged(DataChangedEvent event) {
        dirty = true;
    }
}
