// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

/**
 * The base class for OSM objects ({@link Node}, {@link Way}, {@link Relation}).
 *
 * It can be created, deleted and uploaded to the OSM-Server.
 *
 * Although OsmPrimitive is designed as a base class, it is not to be meant to subclass
 * it by any other than from the package {@link org.openstreetmap.josm.data.osm}. The available primitives are a fixed set that are given
 * by the server environment and not an extendible data stuff.
 *
 * @author imi
 */
abstract public class OsmPrimitive extends AbstractPrimitive implements Comparable<OsmPrimitive>, TemplateEngineDataProvider {
    private static final String SPECIAL_VALUE_ID = "id";
    private static final String SPECIAL_VALUE_LOCAL_NAME = "localname";


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
     * filter mechanism (i.e.&nbsp;FLAG_DISABLED is set).
     * Then it indicates, whether it is completely hidden or
     * just shown in gray color.
     *
     * When the primitive is not disabled, this flag should be
     * unset as well (for efficient access).
     */
    protected static final int FLAG_HIDE_IF_DISABLED = 1 << 5;

    /**
     * Flag used internally by the filter mechanism.
     */
    protected static final int FLAG_DISABLED_TYPE = 1 << 6;

    /**
     * Flag used internally by the filter mechanism.
     */
    protected static final int FLAG_HIDDEN_TYPE = 1 << 7;

    /**
     * This flag is set if the primitive is a way and
     * according to the tags, the direction of the way is important.
     * (e.g. one way street.)
     */
    protected static final int FLAG_HAS_DIRECTIONS = 1 << 8;

    /**
     * If the primitive is tagged.
     * Some trivial tags like source=* are ignored here.
     */
    protected static final int FLAG_TAGGED = 1 << 9;

    /**
     * This flag is only relevant if FLAG_HAS_DIRECTIONS is set.
     * It shows, that direction of the arrows should be reversed.
     * (E.g. oneway=-1.)
     */
    protected static final int FLAG_DIRECTION_REVERSED = 1 << 10;

    /**
     * When hovering over ways and nodes in add mode, the
     * "target" objects are visually highlighted. This flag indicates
     * that the primitive is currently highlighted.
     */
    protected static final int FLAG_HIGHLIGHTED = 1 << 11;

    /**
     * If the primitive is annotated with a tag such as note, fixme, etc.
     * Match the "work in progress" tags in default elemstyles.xml.
     */
    protected static final int FLAG_ANNOTATED = 1 << 12;

    /**
     * Replies the sub-collection of {@link OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@link OsmPrimitive}s. The result collection is a list.
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
     * Replies the sub-collection of {@link OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@link OsmPrimitive}s. The result collection is a set.
     *
     * If <code>list</code> is null, replies an empty set.
     *
     * @param <T> type of data (must be one of the {@link OsmPrimitive} types
     * @param set  the original collection
     * @param type the type to filter for
     * @return the sub-set of OSM primitives of type <code>type</code>
     */
    static public <T extends OsmPrimitive> Set<T> getFilteredSet(Collection<OsmPrimitive> set, Class<T> type) {
        Set<T> ret = new LinkedHashSet<T>();
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

    public static final Predicate<OsmPrimitive> multipolygonPredicate = new Predicate<OsmPrimitive>() {
        @Override public boolean evaluate(OsmPrimitive primitive) {
            return primitive.getClass() == Relation.class && ((Relation) primitive).isMultipolygon();
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
     * If allowNegativeId is set, provided id can be &lt; 0 and will be set to primitive without any processing.
     * If allowNegativeId is not set, then id will have to be 0 (in that case new unique id will be generated) or
     * positive number.
     *
     * @param id the id
     * @param allowNegativeId
     * @throws IllegalArgumentException thrown if id &lt; 0 and allowNegativeId is false
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
     * If allowNegativeId is set, provided id can be &lt; 0 and will be set to primitive without any processing.
     * If allowNegativeId is not set, then id will have to be 0 (in that case new unique id will be generated) or
     * positive number.
     *
     * If id is not &gt; 0 version is ignored and set to 0.
     *
     * @param id
     * @param version
     * @param allowNegativeId
     * @throws IllegalArgumentException thrown if id &lt; 0 and allowNegativeId is false
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
     * @param id the id. &gt; 0 required
     * @param version the version &gt; 0 required
     * @throws IllegalArgumentException thrown if id &lt;= 0
     * @throws IllegalArgumentException thrown if version &lt;= 0
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
     * Clears the metadata, including id and version known to the OSM API.
     * The id is a new unique id. The version, changeset and timestamp are set to 0.
     * incomplete and deleted are set to false. It's preferred to use copy constructor with clearMetadata set to true instead
     *
     * <strong>Caution</strong>: Do not use this method on primitives which are already added to a {@link DataSet}.
     *
     * @throws DataIntegrityProblemException If primitive was already added to the dataset
     * @since 6140
     */
    @Override
    public void clearOsmMetadata() {
        if (dataSet != null)
            throw new DataIntegrityProblemException("Method cannot be called after primitive was added to the dataset");
        super.clearOsmMetadata();
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
     * Make the primitive disabled (e.g.&nbsp;if a filter applies).
     *
     * To enable the primitive again, use unsetDisabledState.
     * @param hidden if the primitive should be completely hidden from view or
     *             just shown in gray color.
     * @return true, any flag has changed; false if you try to set the disabled
     * state to the value that is already preset
     */
    public boolean setDisabledState(boolean hidden) {
        boolean locked = writeLock();
        try {
            int oldFlags = flags;
            updateFlagsNoLock(FLAG_DISABLED, true);
            updateFlagsNoLock(FLAG_HIDE_IF_DISABLED, hidden);
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
     * Set binary property used internally by the filter mechanism.
     */
    public void setDisabledType(boolean isExplicit) {
        updateFlags(FLAG_DISABLED_TYPE, isExplicit);
    }

    /**
     * Set binary property used internally by the filter mechanism.
     */
    public void setHiddenType(boolean isExplicit) {
        updateFlags(FLAG_HIDDEN_TYPE, isExplicit);
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

    /**
     * Get binary property used internally by the filter mechanism.
     */
    public boolean getHiddenType() {
        return (flags & FLAG_HIDDEN_TYPE) != 0;
    }

    /**
     * Get binary property used internally by the filter mechanism.
     */
    public boolean getDisabledType() {
        return (flags & FLAG_DISABLED_TYPE) != 0;
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

    /*---------------------------------------------------
     * WORK IN PROGRESS, UNINTERESTING AND DIRECTION KEYS
     *--------------------------------------------------*/

    private static volatile Collection<String> workinprogress = null;
    private static volatile Collection<String> uninteresting = null;
    private static volatile Collection<String> discardable = null;

    /**
     * Returns a list of "uninteresting" keys that do not make an object
     * "tagged".  Entries that end with ':' are causing a whole namespace to be considered
     * "uninteresting".  Only the first level namespace is considered.
     * Initialized by isUninterestingKey()
     * @return The list of uninteresting keys.
     */
    public static Collection<String> getUninterestingKeys() {
        if (uninteresting == null) {
            LinkedList<String> l = new LinkedList<String>(Arrays.asList(
                "source", "source_ref", "source:", "comment",
                "converted_by", "watch", "watch:",
                "description", "attribution"));
            l.addAll(getDiscardableKeys());
            l.addAll(getWorkInProgressKeys());
            uninteresting = Main.pref.getCollection("tags.uninteresting", l);
        }
        return uninteresting;
    }

    /**
     * Returns a list of keys which have been deemed uninteresting to the point
     * that they can be silently removed from data which is being edited.
     * @return The list of discardable keys.
     */
    public static Collection<String> getDiscardableKeys() {
        if (discardable == null) {
            discardable = Main.pref.getCollection("tags.discardable",
                    Arrays.asList(
                            "created_by",
                            "geobase:datasetName",
                            "geobase:uuid",
                            "KSJ2:ADS",
                            "KSJ2:ARE",
                            "KSJ2:AdminArea",
                            "KSJ2:COP_label",
                            "KSJ2:DFD",
                            "KSJ2:INT",
                            "KSJ2:INT_label",
                            "KSJ2:LOC",
                            "KSJ2:LPN",
                            "KSJ2:OPC",
                            "KSJ2:PubFacAdmin",
                            "KSJ2:RAC",
                            "KSJ2:RAC_label",
                            "KSJ2:RIC",
                            "KSJ2:RIN",
                            "KSJ2:WSC",
                            "KSJ2:coordinate",
                            "KSJ2:curve_id",
                            "KSJ2:curve_type",
                            "KSJ2:filename",
                            "KSJ2:lake_id",
                            "KSJ2:lat",
                            "KSJ2:long",
                            "KSJ2:river_id",
                            "odbl",
                            "odbl:note",
                            "SK53_bulk:load",
                            "sub_sea:type",
                            "tiger:source",
                            "tiger:separated",
                            "tiger:tlid",
                            "tiger:upload_uuid",
                            "yh:LINE_NAME",
                            "yh:LINE_NUM",
                            "yh:STRUCTURE",
                            "yh:TOTYUMONO",
                            "yh:TYPE",
                            "yh:WIDTH_RANK"
                        ));
        }
        return discardable;
    }

    /**
     * Returns a list of "work in progress" keys that do not make an object
     * "tagged" but "annotated".
     * @return The list of work in progress keys.
     * @since 5754
     */
    public static Collection<String> getWorkInProgressKeys() {
        if (workinprogress == null) {
            workinprogress = Main.pref.getCollection("tags.workinprogress",
                    Arrays.asList("note", "fixme", "FIXME"));
        }
        return workinprogress;
    }

    /**
     * Determines if key is considered "uninteresting".
     * @param key The key to check
     * @return true if key is considered "uninteresting".
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

    /**
     * Returns {@link #getKeys()} for which {@code key} does not fulfill {@link #isUninterestingKey}.
     */
    public Map<String, String> getInterestingTags() {
        Map<String, String> result = new HashMap<String, String>();
        String[] keys = this.keys;
        if (keys != null) {
            for (int i = 0; i < keys.length; i += 2) {
                if (!isUninterestingKey(keys[i])) {
                    result.put(keys[i], keys[i + 1]);
                }
            }
        }
        return result;
    }

    private static volatile Match directionKeys = null;
    private static volatile Match reversedDirectionKeys = null;

    /**
     * Contains a list of direction-dependent keys that make an object
     * direction dependent.
     * Initialized by checkDirectionTagged()
     */
    static {
        String reversedDirectionDefault = "oneway=\"-1\"";

        String directionDefault = "oneway? | aerialway=* | "+
                "waterway=stream | waterway=river | waterway=canal | waterway=drain | waterway=rapids | "+
                "\"piste:type\"=downhill | \"piste:type\"=sled | man_made=\"piste:halfpipe\" | "+
                "junction=roundabout | (highway=motorway_link & -oneway=no)";

        try {
            reversedDirectionKeys = SearchCompiler.compile(Main.pref.get("tags.reversed_direction", reversedDirectionDefault), false, false);
        } catch (ParseError e) {
            Main.error("Unable to compile pattern for tags.reversed_direction, trying default pattern: " + e.getMessage());

            try {
                reversedDirectionKeys = SearchCompiler.compile(reversedDirectionDefault, false, false);
            } catch (ParseError e2) {
                throw new AssertionError("Unable to compile default pattern for direction keys: " + e2.getMessage());
            }
        }
        try {
            directionKeys = SearchCompiler.compile(Main.pref.get("tags.direction", directionDefault), false, false);
        } catch (ParseError e) {
            Main.error("Unable to compile pattern for tags.direction, trying default pattern: " + e.getMessage());

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
                // 'area' is not really uninteresting (putting it in that list may have unpredictable side effects)
                // but it's clearly not enough to consider an object as tagged (see #9261)
                if (!isUninterestingKey(key) && !"area".equals(key)) {
                    updateFlagsNoLock(FLAG_TAGGED, true);
                    return;
                }
            }
        }
        updateFlagsNoLock(FLAG_TAGGED, false);
    }

    private void updateAnnotated() {
        if (keys != null) {
            for (String key: keySet()) {
                if (getWorkInProgressKeys().contains(key)) {
                    updateFlagsNoLock(FLAG_ANNOTATED, true);
                    return;
                }
            }
        }
        updateFlagsNoLock(FLAG_ANNOTATED, false);
    }

    /**
     * Determines if this object is considered "tagged". To be "tagged", an object
     * must have one or more "interesting" tags. "created_by" and "source"
     * are typically considered "uninteresting" and do not make an object
     * "tagged".
     * @return true if this object is considered "tagged"
     */
    public boolean isTagged() {
        return (flags & FLAG_TAGGED) != 0;
    }

    /**
     * Determines if this object is considered "annotated". To be "annotated", an object
     * must have one or more "work in progress" tags, such as "note" or "fixme".
     * @return true if this object is considered "annotated"
     * @since 5754
     */
    public boolean isAnnotated() {
        return (flags & FLAG_ANNOTATED) != 0;
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
    protected void keysChangedImpl(Map<String, String> originalKeys) {
        clearCachedStyle();
        if (dataSet != null) {
            for (OsmPrimitive ref : getReferrers()) {
                ref.clearCachedStyle();
            }
        }
        updateDirectionFlags();
        updateTagged();
        updateAnnotated();
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
     * @param referrer The referrer to add
     */
    protected void addReferrer(OsmPrimitive referrer) {
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
            referrers = Utils.addInArrayCopy((OsmPrimitive[])referrers, referrer);
        }
    }

    /**
     * Remove referrer. No action is taken if referrer is not registered
     * @param referrer The referrer to remove
     */
    protected void removeReferrer(OsmPrimitive referrer) {
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
     * @param allowWithoutDataset If true, method will return empty list if primitive is not part of the dataset. If false,
     * exception will be thrown in this case
     *
     * @return a collection of all primitives that reference this primitive.
     */
    public final List<OsmPrimitive> getReferrers(boolean allowWithoutDataset) {
        // Returns only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned

        if (dataSet == null && allowWithoutDataset)
            return Collections.emptyList();

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

    public final List<OsmPrimitive> getReferrers() {
        return getReferrers(false);
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
                ref.accept(visitor);
            }
        } else if (this.referrers instanceof OsmPrimitive[]) {
            OsmPrimitive[] refs = (OsmPrimitive[]) this.referrers;
            for (OsmPrimitive ref: refs) {
                if (ref.dataSet == dataSet) {
                    ref.accept(visitor);
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
        if (referrers instanceof OsmPrimitive)
            return n<=1 && referrers instanceof Way && ((OsmPrimitive)referrers).dataSet == dataSet;
        else {
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
     *----------------*/

    /**
     * Implementation of the visitor scheme. Subclasses have to call the correct
     * visitor function.
     * @param visitor The visitor from which the visit() function must be called.
     */
    abstract public void accept(Visitor visitor);

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
     * Replies true if other isn't null and has the same interesting tags (key/value-pairs) as this.
     *
     * @param other the other object primitive
     * @return true if other isn't null and has the same interesting tags (key/value-pairs) as this.
     */
    public boolean hasSameInterestingTags(OsmPrimitive other) {
        // We cannot directly use Arrays.equals(keys, other.keys) as keys is not ordered by key
        // but we can at least check if both arrays are null or of the same size before creating
        // and comparing the key maps (costly operation, see #7159)
        return (keys == null && other.keys == null)
                || (keys != null && other.keys != null && keys.length == other.keys.length
                        && (keys.length == 0 || getInterestingTags().equals(other.getInterestingTags())));
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * semantic attributes.
     * <ol>
     *   <li>equal id</li>
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
        if (isIncomplete() ^ other.isIncomplete()) // exclusive or operator for performance (see #7159)
            return false;
        // can't do an equals check on the internal keys array because it is not ordered
        //
        return hasSameInterestingTags(other);
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * technical attributes. The attributes:
     * <ol>
     *   <li>deleted</li>
     *   <li>modified</li>
     *   <li>timestamp</li>
     *   <li>version</li>
     *   <li>visible</li>
     *   <li>user</li>
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
     * @param data The object which should be cloned
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
     * @return The saved object data
     */
    public abstract PrimitiveData save();

    /**
     * Save common parameters of primitives to the transport object
     * @param data The object to save the data into
     */
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

    /**
     * Fetch the bounding box of the primitive
     * @return Bounding box of the object
     */
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

    /**
     * Replies the display name of a primitive formatted by <code>formatter</code>
     *
     * @return the display name
     */
    public abstract String getDisplayName(NameFormatter formatter);

    @Override
    public Collection<String> getTemplateKeys() {
        Collection<String> keySet = keySet();
        List<String> result = new ArrayList<String>(keySet.size() + 2);
        result.add(SPECIAL_VALUE_ID);
        result.add(SPECIAL_VALUE_LOCAL_NAME);
        result.addAll(keySet);
        return result;
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (special) {
            String lc = name.toLowerCase();
            if (SPECIAL_VALUE_ID.equals(lc))
                return getId();
            else if (SPECIAL_VALUE_LOCAL_NAME.equals(lc))
                return getLocalName();
            else
                return null;

        } else
            return getIgnoreCase(name);
    }

    @Override
    public boolean evaluateCondition(Match condition) {
        return condition.match(this);
    }

    /**
     * Replies the set of referring relations
     *
     * @return the set of referring relations
     */
    public static Set<Relation> getParentRelations(Collection<? extends OsmPrimitive> primitives) {
        HashSet<Relation> ret = new HashSet<Relation>();
        for (OsmPrimitive w : primitives) {
            ret.addAll(OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class));
        }
        return ret;
    }

    /**
     * Determines if this primitive has tags denoting an area.
     * @return {@code true} if this primitive has tags denoting an area, {@code false} otherwise.
     * @since 6491
     */
    public final boolean hasAreaTags() {
        return hasKey("landuse")
                || "yes".equals(get("area"))
                || "riverbank".equals(get("waterway"))
                || hasKey("natural")
                || hasKey("amenity")
                || hasKey("leisure")
                || hasKey("building")
                || hasKey("building:part");
    }

    /**
     * Determines if this primitive semantically concerns an area.
     * @return {@code true} if this primitive semantically concerns an area, according to its type, geometry and tags, {@code false} otherwise.
     * @since 6491
     */
    public abstract boolean concernsArea();

    /**
     * Tests if this primitive lies outside of the downloaded area of its {@link DataSet}.
     * @return {@code true} if this primitive lies outside of the downloaded area
     */
    public abstract boolean isOutsideDownloadArea();
}
