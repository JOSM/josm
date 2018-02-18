// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * AutoCompletionManager holds a cache of keys with a list of
 * possible auto completion values for each key.
 *
 * Each DataSet can be assigned one AutoCompletionManager instance such that
 * <ol>
 *   <li>any key used in a tag in the data set is part of the key list in the cache</li>
 *   <li>any value used in a tag for a specific key is part of the autocompletion list of this key</li>
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
            return Objects.hash(key, value, defaultKey);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final UserInputTag other = (UserInputTag) obj;
            return this.defaultKey == other.defaultKey
                && Objects.equals(this.key, other.key)
                && Objects.equals(this.value, other.value);
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
    static final MultiMap<String, String> PRESET_TAG_CACHE = new MultiMap<>();

    /**
     * Cache for tags that have been entered by the user.
     */
    static final Set<UserInputTag> USER_INPUT_TAG_CACHE = new LinkedHashSet<>();

    /**
     * the cached list of member roles
     * only accessed by getRoleCache(), rebuild() and cacheRelationMemberRoles()
     * use getRoleCache() accessor
     */
    protected Set<String> roleCache;

    /**
     * the same as roleCache but for the preset roles can be accessed directly
     */
    static final Set<String> PRESET_ROLE_CACHE = new HashSet<>();

    private static final Map<DataSet, AutoCompletionManager> INSTANCES = new HashMap<>();

    /**
     * Constructs a new {@code AutoCompletionManager}.
     * @param ds data set
     * @throws NullPointerException if ds is null
     */
    public AutoCompletionManager(DataSet ds) {
        this.ds = Objects.requireNonNull(ds);
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
     * an empty list if key is null or if key is not in {@link #getTagKeys()}.
     *
     * @param key OSM key
     * @return the list of auto completion values
     */
    protected List<String> getDataValues(String key) {
        return new ArrayList<>(getTagCache().getValues(key));
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
     * Populates the {@link AutoCompletionList} with the currently cached member roles.
     *
     * @param list the list to populate
     */
    public void populateWithMemberRoles(AutoCompletionList list) {
        list.add(TaggingPresets.getPresetRoles(), AutoCompletionPriority.IS_IN_STANDARD);
        list.add(getRoleCache(), AutoCompletionPriority.IS_IN_DATASET);
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
        Collection<TaggingPreset> presets = r != null ? TaggingPresets.getMatchingPresets(null, r.getKeys(), false) : null;
        if (r != null && presets != null && !presets.isEmpty()) {
            for (TaggingPreset tp : presets) {
                if (tp.roles != null) {
                    list.add(Utils.transform(tp.roles.roles, (Function<Role, String>) x -> x.key), AutoCompletionPriority.IS_IN_STANDARD);
                }
            }
            list.add(r.getMemberRoles(), AutoCompletionPriority.IS_IN_DATASET);
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
        list.add(TaggingPresets.getPresetKeys(), AutoCompletionPriority.IS_IN_STANDARD);
        list.add(new AutoCompletionItem("source", AutoCompletionPriority.IS_IN_STANDARD));
        list.add(getDataKeys(), AutoCompletionPriority.IS_IN_DATASET);
        list.addUserInput(getUserInputKeys());
    }

    /**
     * Populates the an {@link AutoCompletionList} with the currently cached values for a tag
     *
     * @param list the list to populate
     * @param key the tag key
     */
    public void populateWithTagValues(AutoCompletionList list, String key) {
        populateWithTagValues(list, Arrays.asList(key));
    }

    /**
     * Populates the {@link AutoCompletionList} with the currently cached values for some given tags
     *
     * @param list the list to populate
     * @param keys the tag keys
     */
    public void populateWithTagValues(AutoCompletionList list, List<String> keys) {
        for (String key : keys) {
            list.add(TaggingPresets.getPresetValues(key), AutoCompletionPriority.IS_IN_STANDARD);
            list.add(getDataValues(key), AutoCompletionPriority.IS_IN_DATASET);
            list.addUserInput(getUserInputValues(key));
        }
    }

    private static List<AutoCompletionItem> setToList(AutoCompletionSet set, Comparator<AutoCompletionItem> comparator) {
        List<AutoCompletionItem> list = set.stream().collect(Collectors.toList());
        list.sort(comparator);
        return list;
    }

    /**
     * Returns the currently cached tag keys.
     * @return a set of tag keys
     * @since 12859
     */
    public AutoCompletionSet getTagKeys() {
        AutoCompletionList list = new AutoCompletionList();
        populateWithKeys(list);
        return list.getSet();
    }

    /**
     * Returns the currently cached tag keys.
     * @param comparator the custom comparator used to sort the list
     * @return a list of tag keys
     * @since 12859
     */
    public List<AutoCompletionItem> getTagKeys(Comparator<AutoCompletionItem> comparator) {
        return setToList(getTagKeys(), comparator);
    }

    /**
     * Returns the currently cached tag values for a given tag key.
     * @param key the tag key
     * @return a set of tag values
     * @since 12859
     */
    public AutoCompletionSet getTagValues(String key) {
        return getTagValues(Arrays.asList(key));
    }

    /**
     * Returns the currently cached tag values for a given tag key.
     * @param key the tag key
     * @param comparator the custom comparator used to sort the list
     * @return a list of tag values
     * @since 12859
     */
    public List<AutoCompletionItem> getTagValues(String key, Comparator<AutoCompletionItem> comparator) {
        return setToList(getTagValues(key), comparator);
    }

    /**
     * Returns the currently cached tag values for a given list of tag keys.
     * @param keys the tag keys
     * @return a set of tag values
     * @since 12859
     */
    public AutoCompletionSet getTagValues(List<String> keys) {
        AutoCompletionList list = new AutoCompletionList();
        populateWithTagValues(list, keys);
        return list.getSet();
    }

    /**
     * Returns the currently cached tag values for a given list of tag keys.
     * @param keys the tag keys
     * @param comparator the custom comparator used to sort the list
     * @return a set of tag values
     * @since 12859
     */
    public List<AutoCompletionItem> getTagValues(List<String> keys, Comparator<AutoCompletionItem> comparator) {
        return setToList(getTagValues(keys), comparator);
    }

    /*
     * Implementation of the DataSetListener interface
     *
     */

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

    private AutoCompletionManager registerListeners() {
        ds.addDataSetListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                if (e.getRemovedLayer() instanceof OsmDataLayer
                        && ((OsmDataLayer) e.getRemovedLayer()).data == ds) {
                    INSTANCES.remove(ds);
                    ds.removeDataSetListener(AutoCompletionManager.this);
                    MainApplication.getLayerManager().removeLayerChangeListener(this);
                }
            }

            @Override
            public void layerOrderChanged(LayerOrderChangeEvent e) {
                // Do nothing
            }

            @Override
            public void layerAdded(LayerAddEvent e) {
                // Do nothing
            }
        });
        return this;
    }

    /**
     * Returns the {@code AutoCompletionManager} for the given data set.
     * @param dataSet the data set
     * @return the {@code AutoCompletionManager} for the given data set
     * @since 12758
     */
    public static AutoCompletionManager of(DataSet dataSet) {
        return INSTANCES.computeIfAbsent(dataSet, ds -> new AutoCompletionManager(ds).registerListeners());
    }
}
