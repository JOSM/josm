// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Predicate;

/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 *
 * Although OsmPrimitive is designed as a base class, it is not to be meant to subclass
 * it by any other than from the package {@link org.openstreetmap.josm.data.osm}. The available primitives are a fixed set that are given
 * by the server environment and not an extendible data stuff.
 *
 * @author imi
 */
abstract public class OsmPrimitive extends AbstractPrimitive implements Comparable<OsmPrimitive> {

    /**
     * An object can be disabled by the filter mechanism.
     * Then it will show in a shade of gray on the map or it is completely
     * hidden from the view.
     * Disabled objects usually cannot be selected or modified
     * while the filter is active.
     */
    protected static final int FLAG_DISABLED = 1 << 4;

    /**
     * This flag is only relevant if an object is disabled by the
     * filter mechanism (i.e. FLAG_DISABLED is set).
     * Then it indicates, whether it is completely hidden or
     * just shown in gray color.
     *
     * When the primitive is not disabled, this flag should be
     * unset as well (for efficient access).
     */
    protected static final int FLAG_HIDE_IF_DISABLED = 1 << 5;

    /**
     * This flag is set if the primitive is a way and
     * according to the tags, the direction of the way is important.
     * (e.g. one way street.)
     */
    protected static final int FLAG_HAS_DIRECTIONS = 1 << 6;

    /**
     * If the primitive is tagged.
     * Some trivial tags like source=* are ignored here.
     */
    protected static final int FLAG_TAGGED = 1 << 7;

    /**
     * This flag is only relevant if FLAG_HAS_DIRECTIONS is set.
     * It shows, that direction of the arrows should be reversed.
     * (E.g. oneway=-1.)
     */
    protected static final int FLAG_DIRECTION_REVERSED = 1 << 8;

    /**
     * When hovering over ways and nodes in add mode, the
     * "target" objects are visually highlighted. This flag indicates
     * that the primitive is currently highlighted.
     */
    protected static final int FLAG_HIGHLIGHTED = 1 << 9;

    /**
     * Replies the sub-collection of {@see OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@see OsmPrimitive}s. The result collection is a list.
     *
     * If <code>list</code> is null, replies an empty list.
     *
     * @param <T>
     * @param list  the original list
     * @param type the type to filter for
     * @return the sub-list of OSM primitives of type <code>type</code>
     */
    static public <T extends OsmPrimitive>  List<T> getFilteredList(Collection<OsmPrimitive> list, Class<T> type) {
        if (list == null) return Collections.emptyList();
        List<T> ret = new LinkedList<T>();
        for(OsmPrimitive p: list) {
            if (type.isInstance(p)) {
                ret.add(type.cast(p));
            }
        }
        return ret;
    }

    /**
     * Replies the sub-collection of {@see OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@see OsmPrimitive}s. The result collection is a set.
     *
     * If <code>list</code> is null, replies an empty set.
     *
     * @param <T>
     * @param list  the original collection
     * @param type the type to filter for
     * @return the sub-set of OSM primitives of type <code>type</code>
     */
    static public <T extends OsmPrimitive>  LinkedHashSet<T> getFilteredSet(Collection<OsmPrimitive> set, Class<T> type) {
        LinkedHashSet<T> ret = new LinkedHashSet<T>();
        if (set != null) {
            for(OsmPrimitive p: set) {
                if (type.isInstance(p)) {
                    ret.add(type.cast(p));
                }
            }
        }
        return ret;
    }

    /**
     * Replies the collection of referring primitives for the primitives in <code>primitives</code>.
     *
     * @param primitives the collection of primitives.
     * @return the collection of referring primitives for the primitives in <code>primitives</code>;
     * empty set if primitives is null or if there are no referring primitives
     */
    static public Set<OsmPrimitive> getReferrer(Collection<? extends OsmPrimitive> primitives) {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        if (primitives == null || primitives.isEmpty()) return ret;
        for (OsmPrimitive p: primitives) {
            ret.addAll(p.getReferrers());
        }
        return ret;
    }

    /**
     * Some predicates, that describe conditions on primitives.
     */
    public static final Predicate<OsmPrimitive> isUsablePredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.isUsable();
        }
    };

    public static final Predicate<OsmPrimitive> isSelectablePredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.isSelectable();
        }
    };

    public static final Predicate<OsmPrimitive> nonDeletedPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return !primitive.isDeleted();
        }
    };

    public static final Predicate<OsmPrimitive> nonDeletedCompletePredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return !primitive.isDeleted() && !primitive.isIncomplete();
        }
    };

    public static final Predicate<OsmPrimitive> nonDeletedPhysicalPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return !primitive.isDeleted() && !primitive.isIncomplete() && !(primitive instanceof Relation);
        }
    };

    public static final Predicate<OsmPrimitive> modifiedPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.isModified();
        }
    };

    public static final Predicate<OsmPrimitive> nodePredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.getClass() == Node.class;
        }
    };

    public static final Predicate<OsmPrimitive> wayPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.getClass() == Way.class;
        }
    };

    public static final Predicate<OsmPrimitive> relationPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.getClass() == Relation.class;
        }
    };

    public static final Predicate<OsmPrimitive> allPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return true;
        }
    };

    /**
     * Creates a new primitive for the given id.
     *
     * If allowNegativeId is set, provided id can be < 0 and will be set to primitive without any processing.
     * If allowNegativeId is not set, then id will have to be 0 (in that case new unique id will be generated) or
     * positive number.
     *
     * @param id the id
     * @param allowNegativeId
     * @throws IllegalArgumentException thrown if id < 0 and allowNegativeId is false
     */
    protected OsmPrimitive(long id, boolean allowNegativeId) throws IllegalArgumentException {
        if (allowNegativeId) {
            this.id = id;
        } else {
            if (id < 0)
                throw new IllegalArgumentException(MessageFormat.format("Expected ID >= 0. Got {0}.", id));
            else if (id == 0) {
                this.id = generateUniqueId();
            } else {
                this.id = id;
            }

        }
        this.version = 0;
        this.setIncomplete(id > 0);
    }

    /**
     * Creates a new primitive for the given id and version.
     *
     * If allowNegativeId is set, provided id can be < 0 and will be set to primitive without any processing.
     * If allowNegativeId is not set, then id will have to be 0 (in that case new unique id will be generated) or
     * positive number.
     *
     * If id is not > 0 version is ignored and set to 0.
     *
     * @param id
     * @param version
     * @param allowNegativeId
     * @throws IllegalArgumentException thrown if id < 0 and allowNegativeId is false
     */
    protected OsmPrimitive(long id, int version, boolean allowNegativeId) throws IllegalArgumentException {
        this(id, allowNegativeId);
        this.version = (id > 0 ? version : 0);
        setIncomplete(id > 0 && version == 0);
    }


    /*----------
     * MAPPAINT
     *--------*/
    public StyleCache mappaintStyle = null;
    public int mappaintCacheIdx;

    /* This should not be called from outside. Fixing the UI to add relevant
       get/set functions calling this implicitely is preferred, so we can have
       transparent cache handling in the future. */
    public void clearCachedStyle()
    {
        mappaintStyle = null;
    }
    /* end of mappaint data */

    /*---------
     * DATASET
     *---------*/

    /** the parent dataset */
    private DataSet dataSet;

    /**
     * This method should never ever by called from somewhere else than Dataset.addPrimitive or removePrimitive methods
     * @param dataSet
     */
    void setDataset(DataSet dataSet) {
        if (this.dataSet != null && dataSet != null && this.dataSet != dataSet)
            throw new DataIntegrityProblemException("Primitive cannot be included in more than one Dataset");
        this.dataSet = dataSet;
    }

    /**
     *
     * @return DataSet this primitive is part of.
     */
    public DataSet getDataSet() {
        return dataSet;
    }

    /**
     * Throws exception if primitive is not part of the dataset
     */
    public void checkDataset() {
        if (dataSet == null)
            throw new DataIntegrityProblemException("Primitive must be part of the dataset: " + toString());
    }

    protected boolean writeLock() {
        if (dataSet != null) {
            dataSet.beginUpdate();
            return true;
        } else
            return false;
    }

    protected void writeUnlock(boolean locked) {
        if (locked) {
            // It shouldn't be possible for dataset to become null because method calling setDataset would need write lock which is owned by this thread
            dataSet.endUpdate();
        }
    }

    /**
     * Sets the id and the version of this primitive if it is known to the OSM API.
     *
     * Since we know the id and its version it can't be incomplete anymore. incomplete
     * is set to false.
     *
     * @param id the id. > 0 required
     * @param version the version > 0 required
     * @throws IllegalArgumentException thrown if id <= 0
     * @throws IllegalArgumentException thrown if version <= 0
     * @throws DataIntegrityProblemException If id is changed and primitive was already added to the dataset
     */
    @Override
    public void setOsmId(long id, int version) {
        boolean locked = writeLock();
        try {
            if (id <= 0)
                throw new IllegalArgumentException(tr("ID > 0 expected. Got {0}.", id));
            if (version <= 0)
                throw new IllegalArgumentException(tr("Version > 0 expected. Got {0}.", version));
            if (dataSet != null && id != this.id) {
                DataSet datasetCopy = dataSet;
                // Reindex primitive
                datasetCopy.removePrimitive(this);
                this.id = id;
                datasetCopy.addPrimitive(this);
            }
            super.setOsmId(id, version);
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Clears the id and version known to the OSM API. The id and the version is set to 0.
     * incomplete is set to false. It's preferred to use copy constructor with clearId set to true instead
     * of calling this method.
     *
     * <strong>Caution</strong>: Do not use this method on primitives which are already added to a {@see DataSet}.
     *
     * @throws DataIntegrityProblemException If primitive was already added to the dataset
     */
    @Override
    public void clearOsmId() {
        if (dataSet != null)
            throw new DataIntegrityProblemException("Method cannot be called after primitive was added to the dataset");
        super.clearOsmId();
    }

    @Override
    public void setUser(User user) {
        boolean locked = writeLock();
        try {
            super.setUser(user);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setChangesetId(int changesetId) throws IllegalStateException, IllegalArgumentException {
        boolean locked = writeLock();
        try {
            int old = this.changesetId;
            super.setChangesetId(changesetId);
            if (dataSet != null) {
                dataSet.fireChangesetIdChanged(this, old, changesetId);
            }
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setTimestamp(Date timestamp) {
        boolean locked = writeLock();
        try {
            super.setTimestamp(timestamp);
        } finally {
            writeUnlock(locked);
        }
    }


    /* -------
    /* FLAGS
    /* ------*/

    private void updateFlagsNoLock (int flag, boolean value) {
        super.updateFlags(flag, value);
    }
    
    @Override
    protected final void updateFlags(int flag, boolean value) {
        boolean locked = writeLock();
        try {
            updateFlagsNoLock(flag, value);
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Make the primitive disabled (e.g. if a filter applies).
     * To enable the primitive again, use unsetDisabledState.
     * @param hide if the primitive should be completely hidden from view or
     *             just shown in gray color.
     */
    public boolean setDisabledState(boolean hide) {
        boolean locked = writeLock();
        try {
            int oldFlags = flags;
            updateFlagsNoLock(FLAG_DISABLED, true);
            updateFlagsNoLock(FLAG_HIDE_IF_DISABLED, hide);
            return oldFlags != flags;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Remove the disabled flag from the primitive.
     * Afterwards, the primitive is displayed normally and can be selected
     * again.
     */
    public boolean unsetDisabledState() {
        boolean locked = writeLock();
        try {
            int oldFlags = flags;
            updateFlagsNoLock(FLAG_DISABLED + FLAG_HIDE_IF_DISABLED, false);
            return oldFlags != flags;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Replies true, if this primitive is disabled. (E.g. a filter
     * applies)
     */
    public boolean isDisabled() {
        return (flags & FLAG_DISABLED) != 0;
    }

    /**
     * Replies true, if this primitive is disabled and marked as
     * completely hidden on the map.
     */
    public boolean isDisabledAndHidden() {
        return (((flags & FLAG_DISABLED) != 0) && ((flags & FLAG_HIDE_IF_DISABLED) != 0));
    }

    @Deprecated
    public boolean isFiltered() {
        return isDisabledAndHidden();
    }


    public boolean isSelectable() {
        return (flags & (FLAG_DELETED + FLAG_INCOMPLETE + FLAG_DISABLED + FLAG_HIDE_IF_DISABLED)) == 0;
    }

    public boolean isDrawable() {
        return (flags & (FLAG_DELETED + FLAG_INCOMPLETE + FLAG_HIDE_IF_DISABLED)) == 0;
    }

    @Override
    public void setVisible(boolean visible) throws IllegalStateException {
        boolean locked = writeLock();
        try {
            super.setVisible(visible);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setDeleted(boolean deleted) {
        boolean locked = writeLock();
        try {
            super.setDeleted(deleted);
            if (dataSet != null) {
                if (deleted) {
                    dataSet.firePrimitivesRemoved(Collections.singleton(this), false);
                } else {
                    dataSet.firePrimitivesAdded(Collections.singleton(this), false);
                }
            }
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    protected void setIncomplete(boolean incomplete) {
        boolean locked = writeLock();
        try {
            if (dataSet != null && incomplete != this.isIncomplete()) {
                if (incomplete) {
                    dataSet.firePrimitivesRemoved(Collections.singletonList(this), true);
                } else {
                    dataSet.firePrimitivesAdded(Collections.singletonList(this), true);
                }
            }
            super.setIncomplete(incomplete);
        }  finally {
            writeUnlock(locked);
        }
    }

    public boolean isSelected() {
        return dataSet != null && dataSet.isSelected(this);
    }

    public boolean isMemberOfSelected() {
        if (referrers == null)
            return false;
        if (referrers instanceof OsmPrimitive)
            return referrers instanceof Relation && ((OsmPrimitive) referrers).isSelected();
        for (OsmPrimitive ref : (OsmPrimitive[]) referrers) {
            if (ref instanceof Relation && ref.isSelected())
                return true;
        }
        return false;
    }

    public void setHighlighted(boolean highlighted) {
        if (isHighlighted() != highlighted) {
            updateFlags(FLAG_HIGHLIGHTED, highlighted);
            if (dataSet != null) {
                dataSet.fireHighlightingChanged(this);
            }
        }
    }

    public boolean isHighlighted() {
        return (flags & FLAG_HIGHLIGHTED) != 0;
    }

    /*----------------------------------
     * UNINTERESTING AND DIRECTION KEYS
     *----------------------------------*/


    private static volatile Collection<String> uninteresting = null;
    /**
     * Contains a list of "uninteresting" keys that do not make an object
     * "tagged".  Entries that end with ':' are causing a whole namespace to be considered
     * "uninteresting".  Only the first level namespace is considered.
     * Initialized by isUninterestingKey()
     */
    public static Collection<String> getUninterestingKeys() {
        if (uninteresting == null) {
            uninteresting = Main.pref.getCollection("tags.uninteresting",
                    Arrays.asList(new String[]{"source", "source_ref", "source:", "note", "comment",
                            "converted_by", "created_by", "watch", "watch:", "fixme", "FIXME",
                            "description", "attribution"}));
        }
        return uninteresting;
    }

    /**
     * Returns true if key is considered "uninteresting".
     */
    public static boolean isUninterestingKey(String key) {
        getUninterestingKeys();
        if (uninteresting.contains(key))
            return true;
        int pos = key.indexOf(':');
        if (pos > 0)
            return uninteresting.contains(key.substring(0, pos + 1));
        return false;
    }

    private static volatile Match directionKeys = null;
    private static volatile Match reversedDirectionKeys = null;

    /**
     * Contains a list of direction-dependent keys that make an object
     * direction dependent.
     * Initialized by checkDirectionTagged()
     */
    static {
        // Legacy support - convert list of keys to search pattern
        if (Main.pref.isCollection("tags.direction", false)) {
            System.out.println("Collection of keys in tags.direction is no longer supported, value will converted to search pattern");
            Collection<String> keys = Main.pref.getCollection("tags.direction", null);
            StringBuilder builder = new StringBuilder();
            for (String key:keys) {
                builder.append(key);
                builder.append("=* | ");
            }
            builder.delete(builder.length() - 3, builder.length());
            Main.pref.put("tags.direction", builder.toString());
        }

        // FIXME: incline=\"-*\" search pattern does not work.
        String reversedDirectionDefault = "oneway=\"-1\" | incline=down | incline=\"-*\"";

        String directionDefault = "oneway? | incline=* | aerialway=* | "+
        "waterway=stream | waterway=river | waterway=canal | waterway=drain | waterway=rapids | "+
        "\"piste:type\"=downhill | \"piste:type\"=sled | man_made=\"piste:halfpipe\" | "+
        "junction=roundabout";

        try {
            reversedDirectionKeys = SearchCompiler.compile(Main.pref.get("tags.reversed_direction", reversedDirectionDefault), false, false);
        } catch (ParseError e) {
            System.err.println("Unable to compile pattern for tags.reversed_direction, trying default pattern: " + e.getMessage());

            try {
                reversedDirectionKeys = SearchCompiler.compile(reversedDirectionDefault, false, false);
            } catch (ParseError e2) {
                throw new AssertionError("Unable to compile default pattern for direction keys: " + e2.getMessage());
            }
        }
        try {
            directionKeys = SearchCompiler.compile(Main.pref.get("tags.direction", directionDefault), false, false);
        } catch (ParseError e) {
            System.err.println("Unable to compile pattern for tags.direction, trying default pattern: " + e.getMessage());

            try {
                directionKeys = SearchCompiler.compile(directionDefault, false, false);
            } catch (ParseError e2) {
                throw new AssertionError("Unable to compile default pattern for direction keys: " + e2.getMessage());
            }
        }
    }

    private void updateTagged() {
        if (keys != null) {
            for (String key: keySet()) {
                if (!isUninterestingKey(key)) {
                    updateFlagsNoLock(FLAG_TAGGED, true);
                    return;
                }
            }
        }
        updateFlagsNoLock(FLAG_TAGGED, false);
    }

    /**
     * true if this object is considered "tagged". To be "tagged", an object
     * must have one or more "interesting" tags. "created_by" and "source"
     * are typically considered "uninteresting" and do not make an object
     * "tagged".
     */
    public boolean isTagged() {
        return (flags & FLAG_TAGGED) != 0;
    }

    private void updateDirectionFlags() {
        boolean hasDirections = false;
        boolean directionReversed = false;
        if (reversedDirectionKeys.match(this)) {
            hasDirections = true;
            directionReversed = true;
        }
        if (directionKeys.match(this)) {
            hasDirections = true;
        }

        updateFlagsNoLock(FLAG_DIRECTION_REVERSED, directionReversed);
        updateFlagsNoLock(FLAG_HAS_DIRECTIONS, hasDirections);
    }

    /**
     * true if this object has direction dependent tags (e.g. oneway)
     */
    public boolean hasDirectionKeys() {
        return (flags & FLAG_HAS_DIRECTIONS) != 0;
    }

    public boolean reversedDirection() {
        return (flags & FLAG_DIRECTION_REVERSED) != 0;
    }

    /*------------
     * Keys handling
     ------------*/
    
    @Override
    public final void setKeys(Map<String, String> keys) {
        boolean locked = writeLock();
        try {
            super.setKeys(keys);
        } finally {
            writeUnlock(locked);
        }
    }
    
    @Override
    public final void put(String key, String value) {
        boolean locked = writeLock();
        try {
            super.put(key, value);
        } finally {
            writeUnlock(locked);
        }
    }  
    
    @Override
    public final void remove(String key) {
        boolean locked = writeLock();
        try {
            super.remove(key);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public final void removeAll() {
        boolean locked = writeLock();
        try {
            super.removeAll();
        } finally {
            writeUnlock(locked);
        }
    }  
    
    @Override
    protected final void keysChangedImpl(Map<String, String> originalKeys) {
        clearCachedStyle();
        if (dataSet != null) {
            for (OsmPrimitive ref : getReferrers()) {
                ref.clearCachedStyle();
            }
        }
        updateDirectionFlags();
        updateTagged();
        if (dataSet != null) {
            dataSet.fireTagsChanged(this, originalKeys);
        }
    }

    /*------------
     * Referrers
     ------------*/

    private Object referrers;

    /**
     * Add new referrer. If referrer is already included then no action is taken
     * @param referrer
     */
    protected void addReferrer(OsmPrimitive referrer) {
        // Based on methods from josm-ng
        if (referrers == null) {
            referrers = referrer;
        } else if (referrers instanceof OsmPrimitive) {
            if (referrers != referrer) {
                referrers = new OsmPrimitive[] { (OsmPrimitive)referrers, referrer };
            }
        } else {
            for (OsmPrimitive primitive:(OsmPrimitive[])referrers) {
                if (primitive == referrer)
                    return;
            }
            OsmPrimitive[] orig = (OsmPrimitive[])referrers;
            OsmPrimitive[] bigger = new OsmPrimitive[orig.length+1];
            System.arraycopy(orig, 0, bigger, 0, orig.length);
            bigger[orig.length] = referrer;
            referrers = bigger;
        }
    }

    /**
     * Remove referrer. No action is taken if referrer is not registered
     * @param referrer
     */
    protected void removeReferrer(OsmPrimitive referrer) {
        // Based on methods from josm-ng
        if (referrers instanceof OsmPrimitive) {
            if (referrers == referrer) {
                referrers = null;
            }
        } else if (referrers instanceof OsmPrimitive[]) {
            OsmPrimitive[] orig = (OsmPrimitive[])referrers;
            int idx = -1;
            for (int i=0; i<orig.length; i++) {
                if (orig[i] == referrer) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1)
                return;

            if (orig.length == 2) {
                referrers = orig[1-idx]; // idx is either 0 or 1, take the other
            } else { // downsize the array
                OsmPrimitive[] smaller = new OsmPrimitive[orig.length-1];
                System.arraycopy(orig, 0, smaller, 0, idx);
                System.arraycopy(orig, idx+1, smaller, idx, smaller.length-idx);
                referrers = smaller;
            }
        }
    }
    /**
     * Find primitives that reference this primitive. Returns only primitives that are included in the same
     * dataset as this primitive. <br>
     *
     * For example following code will add wnew as referer to all nodes of existingWay, but this method will
     * not return wnew because it's not part of the dataset <br>
     *
     * <code>Way wnew = new Way(existingWay)</code>
     *
     * @return a collection of all primitives that reference this primitive.
     */

    public final List<OsmPrimitive> getReferrers() {
        // Method copied from OsmPrimitive in josm-ng
        // Returns only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned
        checkDataset();
        Object referrers = this.referrers;
        List<OsmPrimitive> result = new ArrayList<OsmPrimitive>();
        if (referrers != null) {
            if (referrers instanceof OsmPrimitive) {
                OsmPrimitive ref = (OsmPrimitive)referrers;
                if (ref.dataSet == dataSet) {
                    result.add(ref);
                }
            } else {
                for (OsmPrimitive o:(OsmPrimitive[])referrers) {
                    if (dataSet == o.dataSet) {
                        result.add(o);
                    }
                }
            }
        }
        return result;
    }

    /**
     * <p>Visits {@code visitor} for all referrers.</p>
     * 
     * @param visitor the visitor. Ignored, if null.
     */
    public void visitReferrers(Visitor visitor){
        if (visitor == null) return;
        if (this.referrers == null)
            return;
        else if (this.referrers instanceof OsmPrimitive) {
            OsmPrimitive ref = (OsmPrimitive) this.referrers;
            if (ref.dataSet == dataSet) {
                ref.visit(visitor);
            }
        } else if (this.referrers instanceof OsmPrimitive[]) {
            OsmPrimitive[] refs = (OsmPrimitive[]) this.referrers;
            for (OsmPrimitive ref: refs) {
                if (ref.dataSet == dataSet) {
                    ref.visit(visitor);
                }
            }
        }
    }

    /**
      Return true, if this primitive is referred by at least n ways
      @param n Minimal number of ways to return true. Must be positive
     */
    public final boolean isReferredByWays(int n) {
        // Count only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned
        Object referrers = this.referrers;
        if (referrers == null) return false;
        checkDataset();
        if (referrers instanceof OsmPrimitive) {
          return n<=1 && referrers instanceof Way && ((OsmPrimitive)referrers).dataSet == dataSet;
        } else {
          int counter=0;
          for (OsmPrimitive o : (OsmPrimitive[])referrers) {
            if (dataSet == o.dataSet && o instanceof Way) {
              if (++counter >= n) 
                return true;
            }
          }
          return false;
        }
    }


    /*-----------------
     * OTHER METHODS
     *----------------/

    /**
     * Implementation of the visitor scheme. Subclasses have to call the correct
     * visitor function.
     * @param visitor The visitor from which the visit() function must be called.
     */
    abstract public void visit(Visitor visitor);

    /**
     * Get and write all attributes from the parameter. Does not fire any listener, so
     * use this only in the data initializing phase
     */
    public void cloneFrom(OsmPrimitive other) {
        // write lock is provided by subclasses
        if (id != other.id && dataSet != null)
            throw new DataIntegrityProblemException("Osm id cannot be changed after primitive was added to the dataset");

        super.cloneFrom(other);
        clearCachedStyle();
    }

    /**
     * Merges the technical and semantical attributes from <code>other</code> onto this.
     *
     * Both this and other must be new, or both must be assigned an OSM ID. If both this and <code>other</code>
     * have an assigend OSM id, the IDs have to be the same.
     *
     * @param other the other primitive. Must not be null.
     * @throws IllegalArgumentException thrown if other is null.
     * @throws DataIntegrityProblemException thrown if either this is new and other is not, or other is new and this is not
     * @throws DataIntegrityProblemException thrown if other isn't new and other.getId() != this.getId()
     */
    public void mergeFrom(OsmPrimitive other) {
        boolean locked = writeLock();
        try {
            CheckParameterUtil.ensureParameterNotNull(other, "other");
            if (other.isNew() ^ isNew())
                throw new DataIntegrityProblemException(tr("Cannot merge because either of the participating primitives is new and the other is not"));
            if (! other.isNew() && other.getId() != id)
                throw new DataIntegrityProblemException(tr("Cannot merge primitives with different ids. This id is {0}, the other is {1}", id, other.getId()));

            setKeys(other.getKeys());
            timestamp = other.timestamp;
            version = other.version;
            setIncomplete(other.isIncomplete());
            flags = other.flags;
            user= other.user;
            changesetId = other.changesetId;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * semantic attributes.
     * <ol>
     *   <li>equal id</ol>
     *   <li>both are complete or both are incomplete</li>
     *   <li>both have the same tags</li>
     * </ol>
     * @param other
     * @return true if this primitive and other are equal with respect to their
     * semantic attributes.
     */
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (!isNew() &&  id != other.id)
            return false;
        if (isIncomplete() && ! other.isIncomplete() || !isIncomplete()  && other.isIncomplete())
            return false;
        // can't do an equals check on the internal keys array because it is not ordered
        //
        return hasSameTags(other);
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * technical attributes. The attributes:
     * <ol>
     *   <li>deleted</ol>
     *   <li>modified</ol>
     *   <li>timestamp</ol>
     *   <li>version</ol>
     *   <li>visible</ol>
     *   <li>user</ol>
     * </ol>
     * have to be equal
     * @param other the other primitive
     * @return true if this primitive and other are equal with respect to their
     * technical attributes
     */
    public boolean hasEqualTechnicalAttributes(OsmPrimitive other) {
        if (other == null) return false;

        return
        isDeleted() == other.isDeleted()
        && isModified() == other.isModified()
        && timestamp == other.timestamp
        && version == other.version
        && isVisible() == other.isVisible()
        && (user == null ? other.user==null : user==other.user)
        && changesetId == other.changesetId;
    }

    /**
     * Loads (clone) this primitive from provided PrimitiveData
     * @param data
     */
    public void load(PrimitiveData data) {
        // Write lock is provided by subclasses
        setKeys(data.getKeys());
        setTimestamp(data.getTimestamp());
        user = data.getUser();
        setChangesetId(data.getChangesetId());
        setDeleted(data.isDeleted());
        setModified(data.isModified());
        setIncomplete(data.isIncomplete());
        version = data.getVersion();
    }

    /**
     * Save parameters of this primitive to the transport object
     * @return
     */
    public abstract PrimitiveData save();

    protected void saveCommonAttributes(PrimitiveData data) {
        data.setId(id);
        data.setKeys(getKeys());
        data.setTimestamp(getTimestamp());
        data.setUser(user);
        data.setDeleted(isDeleted());
        data.setModified(isModified());
        data.setVisible(isVisible());
        data.setIncomplete(isIncomplete());
        data.setChangesetId(changesetId);
        data.setVersion(version);
    }

    public abstract BBox getBBox();

    /**
     * Called by Dataset to update cached position information of primitive (bbox, cached EarthNorth, ...)
     */
    public abstract void updatePosition();

    /*----------------
     * OBJECT METHODS
     *---------------*/

    @Override
    protected String getFlagsAsString() {
        StringBuilder builder = new StringBuilder(super.getFlagsAsString());

        if (isDisabled()) {
            if (isDisabledAndHidden()) {
                builder.append("h");
            } else {
                builder.append("d");
            }
        }
        if (isTagged()) {
            builder.append("T");
        }
        if (hasDirectionKeys()) {
            if (reversedDirection()) {
                builder.append("<");
            } else {
                builder.append(">");
            }
        }
        return builder.toString();
    }

    /**
     * Equal, if the id (and class) is equal.
     *
     * An primitive is equal to its incomplete counter part.
     */
    @Override public boolean equals(Object obj) {
        if (obj instanceof OsmPrimitive)
            return ((OsmPrimitive)obj).id == id && obj.getClass() == getClass();
        return false;
    }

    /**
     * Return the id plus the class type encoded as hashcode or super's hashcode if id is 0.
     *
     * An primitive has the same hashcode as its incomplete counterpart.
     */
    @Override public final int hashCode() {
        return (int)id;
    }

}
