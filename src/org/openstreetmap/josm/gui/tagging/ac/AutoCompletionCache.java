package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * AutoCompletionCache temporarily holds a cache of keys with a list of
 * possible auto completion values for each key.
 *
 * The cache can initialize itself from the current JOSM data set such that
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
 */
public class AutoCompletionCache {
    static private final Logger logger = Logger.getLogger(AutoCompletionCache.class.getName());

    private static HashMap<OsmDataLayer, AutoCompletionCache> caches;

    static {
        caches = new HashMap<OsmDataLayer, AutoCompletionCache>();
        MapView.addLayerChangeListener(new MapView.LayerChangeListener() {
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                // do nothing
            }

            public void layerAdded(Layer newLayer) {
                // do noting
            }

            public void layerRemoved(Layer oldLayer) {
                if (oldLayer instanceof OsmDataLayer) {
                    caches.remove(oldLayer);
                }
            }
        }
        );
    }

    static public AutoCompletionCache getCacheForLayer(OsmDataLayer layer) {
        AutoCompletionCache cache = caches.get(layer);
        if (cache == null) {
            cache = new AutoCompletionCache(layer);
            caches.put(layer, cache);
        }
        return cache;
    }

    /** the cached tags given by a tag key and a list of values for this tag */
    private MultiMap<String, String> tagCache;
    /**  the layer this cache is built for */
    private OsmDataLayer layer;
    /** the same as tagCache but for the preset keys and values */
    private static MultiMap<String, String> presetTagCache = new MultiMap<String, String>();
    /** the cached list of member roles */
    private  Set<String> roleCache;

    /**
     * constructor
     */
    public AutoCompletionCache(OsmDataLayer layer) {
        tagCache = new MultiMap<String, String>();
        roleCache = new HashSet<String>();
        this.layer = layer;
    }

    public AutoCompletionCache() {
        this(null);
    }

    /**
     * make sure, the keys and values of all tags held by primitive are
     * in the auto completion cache
     *
     * @param primitive an OSM primitive
     */
    protected void cachePrimitive(OsmPrimitive primitive) {
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
            if (m.hasRole() && !roleCache.contains(m.getRole())) {
                roleCache.add(m.getRole());
            }
        }
    }

    /**
     * initializes the cache from the primitives in the dataset of
     * {@see #layer}
     *
     */
    public void initFromDataSet() {
        tagCache = new MultiMap<String, String>();
        if (layer == null)
            return;
        Collection<OsmPrimitive> ds = layer.data.allNonDeletedPrimitives();
        for (OsmPrimitive primitive : ds) {
            cachePrimitive(primitive);
        }
        for (Relation relation : layer.data.getRelations()) {
            if (relation.isIncomplete() || relation.isDeleted()) {
                continue;
            }
            cacheRelationMemberRoles(relation);
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
                    if (ch.key == null) continue;
                    presetTagCache.put(ch.key, OsmUtils.falseval);
                    presetTagCache.put(ch.key, OsmUtils.trueval);
                } else if (item instanceof TaggingPreset.Combo) {
                    TaggingPreset.Combo co = (TaggingPreset.Combo) item;
                    if (co.key == null || co.values == null) continue;
                    for (String value : co.values.split(",")) {
                        presetTagCache.put(co.key, value);
                    }
                } else if (item instanceof TaggingPreset.Key) {
                    TaggingPreset.Key ky = (TaggingPreset.Key) item;
                    if (ky.key == null || ky.value == null) continue;
                    presetTagCache.put(ky.key, ky.value);
                } else if (item instanceof TaggingPreset.Text) {
                    TaggingPreset.Text tt = (TaggingPreset.Text) item;
                    if (tt.key == null) continue;
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
        return new ArrayList<String>(tagCache.keySet());
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
        return new ArrayList<String>(tagCache.getValues(key));
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
        return new ArrayList<String>(roleCache);
    }

    /**
     * Populates the an {@see AutoCompletionList} with the currently cached
     * member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.clear();
        list.add(roleCache, AutoCompletionItemPritority.IS_IN_DATASET);
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
    public void populateWithTagValues(AutoCompletionList list, String key, boolean append) {
        if (!append) {
            list.clear();
        }
        list.add(getDataValues(key), AutoCompletionItemPritority.IS_IN_DATASET);
        list.add(getPresetValues(key), AutoCompletionItemPritority.IS_IN_STANDARD);
    }

    /**
     * Populates the an {@see AutoCompletionList} with the currently cached
     * tag keys
     *
     * @param list the list to populate
     * @param append true to add the keys to the list; false, to replace the keys
     * in the list by the keys in the cache
     */
    public void populateWithKeys(AutoCompletionList list, boolean append) {
        if (!append) {
            list.clear();
        }
        list.add(getDataKeys(), AutoCompletionItemPritority.IS_IN_DATASET);
        list.add(getPresetKeys(), AutoCompletionItemPritority.IS_IN_STANDARD);
    }
}
