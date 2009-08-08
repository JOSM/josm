package org.openstreetmap.josm.gui.dialogs.relation.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;

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
        Layer.listeners.add(new LayerChangeListener() {
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


    /** the cache */
    private HashMap<String, ArrayList<String>> cache;
    private  ArrayList<String> roleCache;
    private OsmDataLayer layer;

    /**
     * constructor
     */
    public AutoCompletionCache(OsmDataLayer layer) {
        cache = new HashMap<String, ArrayList<String>>();
        roleCache = new ArrayList<String>();
        this.layer = layer;
    }

    public AutoCompletionCache() {
        this(null);
    }

    /**
     * make sure, <code>key</code> is in the cache
     *
     * @param key  the key
     */
    protected void cacheKey(String key) {
        if (cache.containsKey(key))
            return;
        else {
            cache.put(key, new ArrayList<String>());
        }
    }

    /**
     * make sure, value is one of the auto completion values allowed for key
     *
     * @param key the key
     * @param value the value
     */
    protected void cacheValue(String key, String value) {
        cacheKey(key);
        ArrayList<String> values = cache.get(key);
        if (!values.contains(value)) {
            values.add(value);
        }
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
            cacheValue(key, value);
        }
    }

    /**
     * Caches all member roles of the relation <code>relation</code>
     *
     * @param relation the relation
     */
    protected void cacheRelationMemberRoles(Relation relation){
        for (RelationMember m: relation.getMembers()) {
            if (m.role == null || m.role.trim().equals("")) {
                continue;
            }
            if (!roleCache.contains(m.role)) {
                roleCache.add(m.role);
            }
        }
    }

    /**
     * initializes the cache from the primitives in the dataset of
     * {@see #layer}
     *
     */
    public void initFromJOSMDataset() {
        cache = new HashMap<String, ArrayList<String>>();
        if (layer == null)
            return;
        Collection<OsmPrimitive> ds = layer.data.allNonDeletedPrimitives();
        for (OsmPrimitive primitive : ds) {
            cachePrimitive(primitive);
        }
        for (Relation relation : layer.data.relations) {
            if (relation.incomplete || relation.deleted) {
                continue;
            }
            cacheRelationMemberRoles(relation);
            Collections.sort(roleCache);
        }
    }

    /**
     * replies the keys held by the cache
     *
     * @return the list of keys held by the cache
     */
    public List<String> getKeys() {
        return new ArrayList<String>(cache.keySet());
    }

    /**
     * replies the auto completion values allowed for a specific key. Replies
     * an empty list if key is null or if key is not in {@link #getKeys()}.
     *
     * @param key
     * @return the list of auto completion values
     */
    public List<String> getValues(String key) {
        if (!cache.containsKey(key))
            return new ArrayList<String>();
        return cache.get(key);
    }

    /**
     * Replies the list of member roles
     *
     * @return the list of member roles
     */
    public List<String> getMemberRoles() {
        return roleCache;
    }

    /**
     * Populates the an {@see AutoCompletionList} with the currently cached
     * member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.clear();
        for (String role: roleCache) {
            list.add(new AutoCompletionListItem(role, AutoCompletionItemPritority.IS_IN_DATASET));
        }
    }
}
