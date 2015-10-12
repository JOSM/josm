// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

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

    /**
     * Data class to remember tags that the user has entered.
     */
    public static class UserInputTag {
        private final String key;
        private final String value;
        private final boolean defaultKey;

        /**
         * Constructor.
         *
         * @param key the tag key
         * @param value the tag value
         * @param defaultKey true, if the key was not really entered by the
         * user, e.g. for preset text fields.
         * In this case, the key will not get any higher priority, just the value.
         */
        public UserInputTag(String key, String value, boolean defaultKey) {
            this.key = key;
            this.value = value;
            this.defaultKey = defaultKey;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.key);
            hash = 59 * hash + Objects.hashCode(this.value);
            hash = 59 * hash + (this.defaultKey ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final UserInputTag other = (UserInputTag) obj;
            return Objects.equals(this.key, other.key)
                && Objects.equals(this.value, other.value)
                && this.defaultKey == other.defaultKey;
        }
    }

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
     * the same as tagCache but for the preset keys and values can be accessed directly
     */
    protected static final MultiMap<String, String> PRESET_TAG_CACHE = new MultiMap<>();

    /**
     * Cache for tags that have been entered by the user.
     */
    protected static final Set<UserInputTag> USER_INPUT_TAG_CACHE = new LinkedHashSet<>();

    /**
     * the cached list of member roles
     * only accessed by getRoleCache(), rebuild() and cacheRelationMemberRoles()
     * use getRoleCache() accessor
     */
    protected Set<String> roleCache;

    /**
     * the same as roleCache but for the preset roles can be accessed directly
     */
    protected static final Set<String> PRESET_ROLE_CACHE = new HashSet<>();

    /**
     * Constructs a new {@code AutoCompletionManager}.
     * @param ds data set
     */
    public AutoCompletionManager(DataSet ds) {
        this.ds = ds;
        this.dirty = true;
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
     */
    protected void rebuild() {
        tagCache = new MultiMap<>();
        roleCache = new HashSet<>();
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
    protected void cacheRelationMemberRoles(Relation relation) {
        for (RelationMember m: relation.getMembers()) {
            if (m.hasRole()) {
                roleCache.add(m.getRole());
            }
        }
    }

    /**
     * Initialize the cache for presets. This is done only once.
     * @param presets Tagging presets to cache
     */
    public static void cachePresets(Collection<TaggingPreset> presets) {
        for (final TaggingPreset p : presets) {
            for (TaggingPresetItem item : p.data) {
                cachePresetItem(p, item);
            }
        }
    }

    protected static void cachePresetItem(TaggingPreset p, TaggingPresetItem item) {
        if (item instanceof KeyedItem) {
            KeyedItem ki = (KeyedItem) item;
            if (ki.key != null && ki.getValues() != null) {
                try {
                    PRESET_TAG_CACHE.putAll(ki.key, ki.getValues());
                } catch (NullPointerException e) {
                    Main.error(p + ": Unable to cache " + ki);
                }
            }
        } else if (item instanceof Roles) {
            Roles r = (Roles) item;
            for (Role i : r.roles) {
                if (i.key != null) {
                    PRESET_ROLE_CACHE.add(i.key);
                }
            }
        } else if (item instanceof CheckGroup) {
            for (KeyedItem check : ((CheckGroup) item).checks) {
                cachePresetItem(p, check);
            }
        }
    }

    /**
     * Remembers user input for the given key/value.
     * @param key Tag key
     * @param value Tag value
     * @param defaultKey true, if the key was not really entered by the user, e.g. for preset text fields
     */
    public static void rememberUserInput(String key, String value, boolean defaultKey) {
        UserInputTag tag = new UserInputTag(key, value, defaultKey);
        USER_INPUT_TAG_CACHE.remove(tag); // re-add, so it gets to the last position of the LinkedHashSet
        USER_INPUT_TAG_CACHE.add(tag);
    }

    /**
     * replies the keys held by the cache
     *
     * @return the list of keys held by the cache
     */
    protected List<String> getDataKeys() {
        return new ArrayList<>(getTagCache().keySet());
    }

    protected List<String> getPresetKeys() {
        return new ArrayList<>(PRESET_TAG_CACHE.keySet());
    }

    protected Collection<String> getUserInputKeys() {
        List<String> keys = new ArrayList<>();
        for (UserInputTag tag : USER_INPUT_TAG_CACHE) {
            if (!tag.defaultKey) {
                keys.add(tag.key);
            }
        }
        Collections.reverse(keys);
        return new LinkedHashSet<>(keys);
    }

    /**
     * replies the auto completion values allowed for a specific key. Replies
     * an empty list if key is null or if key is not in {@link #getKeys()}.
     *
     * @param key OSM key
     * @return the list of auto completion values
     */
    protected List<String> getDataValues(String key) {
        return new ArrayList<>(getTagCache().getValues(key));
    }

    protected static List<String> getPresetValues(String key) {
        return new ArrayList<>(PRESET_TAG_CACHE.getValues(key));
    }

    protected static Collection<String> getUserInputValues(String key) {
        List<String> values = new ArrayList<>();
        for (UserInputTag tag : USER_INPUT_TAG_CACHE) {
            if (key.equals(tag.key)) {
                values.add(tag.value);
            }
        }
        Collections.reverse(values);
        return new LinkedHashSet<>(values);
    }

    /**
     * Replies the list of member roles
     *
     * @return the list of member roles
     */
    public List<String> getMemberRoles() {
        return new ArrayList<>(getRoleCache());
    }

    /**
     * Populates the {@link AutoCompletionList} with the currently cached
     * member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.add(PRESET_ROLE_CACHE, AutoCompletionItemPriority.IS_IN_STANDARD);
        list.add(getRoleCache(), AutoCompletionItemPriority.IS_IN_DATASET);
    }

    /**
     * Populates the {@link AutoCompletionList} with the roles used in this relation
     * plus the ones defined in its applicable presets, if any. If the relation type is unknown,
     * then all the roles known globally will be added, as in {@link #populateWithMemberRoles(AutoCompletionList)}.
     *
     * @param list the list to populate
     * @param r the relation to get roles from
     * @throws IllegalArgumentException if list is null
     * @since 7556
     */
    public void populateWithMemberRoles(AutoCompletionList list, Relation r) {
        CheckParameterUtil.ensureParameterNotNull(list, "list");
        Collection<TaggingPreset> presets = r != null ? TaggingPreset.getMatchingPresets(null, r.getKeys(), false) : null;
        if (r != null && presets != null && !presets.isEmpty()) {
            for (TaggingPreset tp : presets) {
                if (tp.roles != null) {
                    list.add(Utils.transform(tp.roles.roles, new Utils.Function<Role, String>() {
                        public String apply(Role x) {
                            return x.key;
                        }
                    }), AutoCompletionItemPriority.IS_IN_STANDARD);
                }
            }
            list.add(r.getMemberRoles(), AutoCompletionItemPriority.IS_IN_DATASET);
        } else {
            populateWithMemberRoles(list);
        }
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
        list.addUserInput(getUserInputKeys());
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
            list.addUserInput(getUserInputValues(key));
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
