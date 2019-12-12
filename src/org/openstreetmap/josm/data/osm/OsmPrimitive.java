// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
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
public abstract class OsmPrimitive extends AbstractPrimitive implements TemplateEngineDataProvider {
    private static final String SPECIAL_VALUE_ID = "id";
    private static final String SPECIAL_VALUE_LOCAL_NAME = "localname";

    /**
     * A tagged way that matches this pattern has a direction.
     * @see #FLAG_HAS_DIRECTIONS
     */
    static volatile Match directionKeys;

    /**
     * A tagged way that matches this pattern has a direction that is reversed.
     * <p>
     * This pattern should be a subset of {@link #directionKeys}
     * @see #FLAG_DIRECTION_REVERSED
     */
    private static volatile Match reversedDirectionKeys;

    static {
        String reversedDirectionDefault = "oneway=\"-1\"";

        String directionDefault = "oneway? | "+
                "(aerialway=chair_lift & -oneway=no) | "+
                "(aerialway=rope_tow & -oneway=no) | "+
                "(aerialway=magic_carpet & -oneway=no) | "+
                "(aerialway=zip_line & -oneway=no) | "+
                "(aerialway=drag_lift & -oneway=no) | "+
                "(aerialway=t-bar & -oneway=no) | "+
                "(aerialway=j-bar & -oneway=no) | "+
                "(aerialway=platter & -oneway=no) | "+
                "waterway=stream | waterway=river | waterway=ditch | waterway=drain | waterway=tidal_channel | "+
                "(\"piste:type\"=downhill & -area=yes) | (\"piste:type\"=sled & -area=yes) | (man_made=\"piste:halfpipe\" & -area=yes) | "+
                "junction=circular | junction=roundabout | (highway=motorway & -oneway=no & -oneway=reversible) | "+
                "(highway=motorway_link & -oneway=no & -oneway=reversible)";

        reversedDirectionKeys = compileDirectionKeys("tags.reversed_direction", reversedDirectionDefault);
        directionKeys = compileDirectionKeys("tags.direction", directionDefault);
    }

    /**
     * Replies the collection of referring primitives for the primitives in <code>primitives</code>.
     *
     * @param primitives the collection of primitives.
     * @return the collection of referring primitives for the primitives in <code>primitives</code>;
     * empty set if primitives is null or if there are no referring primitives
     */
    public static Set<OsmPrimitive> getReferrer(Collection<? extends OsmPrimitive> primitives) {
        return (primitives != null ? primitives.stream() : Stream.<OsmPrimitive>empty())
                .flatMap(p -> p.referrers(OsmPrimitive.class))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a new primitive for the given id.
     *
     * If allowNegativeId is set, provided id can be &lt; 0 and will be set to primitive without any processing.
     * If allowNegativeId is not set, then id will have to be 0 (in that case new unique id will be generated) or
     * positive number.
     *
     * @param id the id
     * @param allowNegativeId {@code true} to allow negative id
     * @throws IllegalArgumentException if id &lt; 0 and allowNegativeId is false
     */
    protected OsmPrimitive(long id, boolean allowNegativeId) {
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
     * @param id the id
     * @param version the version (positive integer)
     * @param allowNegativeId {@code true} to allow negative id
     * @throws IllegalArgumentException if id &lt; 0 and allowNegativeId is false
     */
    protected OsmPrimitive(long id, int version, boolean allowNegativeId) {
        this(id, allowNegativeId);
        this.version = id > 0 ? version : 0;
        setIncomplete(id > 0 && version == 0);
    }

    /*----------
     * MAPPAINT
     *--------*/
    private StyleCache mappaintStyle;
    private short mappaintCacheIdx;

    @Override
    public final StyleCache getCachedStyle() {
        return mappaintStyle;
    }

    @Override
    public final void setCachedStyle(StyleCache mappaintStyle) {
        this.mappaintStyle = mappaintStyle;
    }

    @Override
    public final boolean isCachedStyleUpToDate() {
        return mappaintStyle != null && mappaintCacheIdx == dataSet.getMappaintCacheIndex();
    }

    @Override
    public final void declareCachedStyleUpToDate() {
        this.mappaintCacheIdx = dataSet.getMappaintCacheIndex();
    }

    /* end of mappaint data */

    /*---------
     * DATASET
     *---------*/

    /** the parent dataset */
    private DataSet dataSet;

    /**
     * This method should never ever by called from somewhere else than Dataset.addPrimitive or removePrimitive methods
     * @param dataSet the parent dataset
     */
    void setDataset(DataSet dataSet) {
        if (this.dataSet != null && dataSet != null && this.dataSet != dataSet)
            throw new DataIntegrityProblemException("Primitive cannot be included in more than one Dataset");
        this.dataSet = dataSet;
    }

    @Override
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

    /**
     * Throws exception if primitive is in a read-only dataset
     */
    protected final void checkDatasetNotReadOnly() {
        if (dataSet != null && dataSet.isLocked())
            throw new DataIntegrityProblemException("Primitive cannot be modified in read-only dataset: " + toString());
    }

    protected boolean writeLock() {
        if (dataSet != null) {
            dataSet.beginUpdate();
            return true;
        } else
            return false;
    }

    protected void writeUnlock(boolean locked) {
        if (locked && dataSet != null) {
            // It shouldn't be possible for dataset to become null because
            // method calling setDataset would need write lock which is owned by this thread
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
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if version &lt;= 0
     * @throws DataIntegrityProblemException if id is changed and primitive was already added to the dataset
     */
    @Override
    public void setOsmId(long id, int version) {
        checkDatasetNotReadOnly();
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
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.setUser(user);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setChangesetId(int changesetId) {
        checkDatasetNotReadOnly();
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
        checkDatasetNotReadOnly();
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

    private void updateFlagsNoLock(short flag, boolean value) {
        super.updateFlags(flag, value);
    }

    @Override
    protected final void updateFlags(short flag, boolean value) {
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
     * Afterwards, the primitive is displayed normally and can be selected again.
     * @return {@code true} if a change occurred
     */
    public boolean unsetDisabledState() {
        boolean locked = writeLock();
        try {
            int oldFlags = flags;
            updateFlagsNoLock(FLAG_DISABLED, false);
            updateFlagsNoLock(FLAG_HIDE_IF_DISABLED, false);
            return oldFlags != flags;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Set binary property used internally by the filter mechanism.
     * @param isExplicit new "disabled type" flag value
     */
    public void setDisabledType(boolean isExplicit) {
        updateFlags(FLAG_DISABLED_TYPE, isExplicit);
    }

    /**
     * Set binary property used internally by the filter mechanism.
     * @param isExplicit new "hidden type" flag value
     */
    public void setHiddenType(boolean isExplicit) {
        updateFlags(FLAG_HIDDEN_TYPE, isExplicit);
    }

    /**
     * Set binary property used internally by the filter mechanism.
     * @param isPreserved new "preserved" flag value
     * @since 13309
     */
    public void setPreserved(boolean isPreserved) {
        updateFlags(FLAG_PRESERVED, isPreserved);
    }

    @Override
    public boolean isDisabled() {
        return (flags & FLAG_DISABLED) != 0;
    }

    @Override
    public boolean isDisabledAndHidden() {
        return ((flags & FLAG_DISABLED) != 0) && ((flags & FLAG_HIDE_IF_DISABLED) != 0);
    }

    /**
     * Get binary property used internally by the filter mechanism.
     * @return {@code true} if this object has the "hidden type" flag enabled
     */
    public boolean getHiddenType() {
        return (flags & FLAG_HIDDEN_TYPE) != 0;
    }

    /**
     * Get binary property used internally by the filter mechanism.
     * @return {@code true} if this object has the "disabled type" flag enabled
     */
    public boolean getDisabledType() {
        return (flags & FLAG_DISABLED_TYPE) != 0;
    }

    @Override
    public boolean isPreserved() {
        return (flags & FLAG_PRESERVED) != 0;
    }

    @Override
    public boolean isSelectable() {
        // not synchronized -> check disabled twice just to be sure we did not have a race condition.
        return !isDisabled() && isDrawable() && !isDisabled();
    }

    @Override
    public boolean isDrawable() {
        return (flags & (FLAG_DELETED + FLAG_INCOMPLETE + FLAG_HIDE_IF_DISABLED)) == 0;
    }

    @Override
    public void setModified(boolean modified) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.setModified(modified);
            if (dataSet != null) {
                dataSet.firePrimitiveFlagsChanged(this);
            }
            clearCachedStyle();
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.setVisible(visible);
            clearCachedStyle();
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setDeleted(boolean deleted) {
        checkDatasetNotReadOnly();
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
            clearCachedStyle();
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    protected final void setIncomplete(boolean incomplete) {
        checkDatasetNotReadOnly();
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
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public boolean isSelected() {
        return dataSet != null && dataSet.isSelected(this);
    }

    @Override
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

    @Override
    public boolean isOuterMemberOfSelected() {
        if (referrers == null)
            return false;
        if (referrers instanceof OsmPrimitive) {
            return isOuterMemberOfMultipolygon((OsmPrimitive) referrers);
        }
        for (OsmPrimitive ref : (OsmPrimitive[]) referrers) {
            if (isOuterMemberOfMultipolygon(ref))
                return true;
        }
        return false;
    }

    private boolean isOuterMemberOfMultipolygon(OsmPrimitive ref) {
        if (ref instanceof Relation && ref.isSelected() && ((Relation) ref).isMultipolygon()) {
            for (RelationMember rm : ((Relation) ref).getMembersFor(Collections.singleton(this))) {
                if ("outer".equals(rm.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setHighlighted(boolean highlighted) {
        if (isHighlighted() != highlighted) {
            updateFlags(FLAG_HIGHLIGHTED, highlighted);
            if (dataSet != null) {
                dataSet.fireHighlightingChanged();
            }
        }
    }

    @Override
    public boolean isHighlighted() {
        return (flags & FLAG_HIGHLIGHTED) != 0;
    }

    /*---------------
     * DIRECTION KEYS
     *---------------*/

    private static Match compileDirectionKeys(String prefName, String defaultValue) throws AssertionError {
        try {
            return SearchCompiler.compile(Config.getPref().get(prefName, defaultValue));
        } catch (SearchParseError e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to compile pattern for " + prefName + ", trying default pattern:", e);
        }

        try {
            return SearchCompiler.compile(defaultValue);
        } catch (SearchParseError e2) {
            throw new AssertionError("Unable to compile default pattern for direction keys: " + e2.getMessage(), e2);
        }
    }

    private void updateTagged() {
        for (String key: keySet()) {
            // 'area' is not really uninteresting (putting it in that list may have unpredictable side effects)
            // but it's clearly not enough to consider an object as tagged (see #9261)
            if (!isUninterestingKey(key) && !"area".equals(key)) {
                updateFlagsNoLock(FLAG_TAGGED, true);
                return;
            }
        }
        updateFlagsNoLock(FLAG_TAGGED, false);
    }

    private void updateAnnotated() {
        for (String key: keySet()) {
            if (getWorkInProgressKeys().contains(key)) {
                updateFlagsNoLock(FLAG_ANNOTATED, true);
                return;
            }
        }
        updateFlagsNoLock(FLAG_ANNOTATED, false);
    }

    @Override
    public boolean isTagged() {
        return (flags & FLAG_TAGGED) != 0;
    }

    @Override
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

    @Override
    public boolean hasDirectionKeys() {
        return (flags & FLAG_HAS_DIRECTIONS) != 0;
    }

    @Override
    public boolean reversedDirection() {
        return (flags & FLAG_DIRECTION_REVERSED) != 0;
    }

    /*------------
     * Keys handling
     ------------*/

    @Override
    public final void setKeys(TagMap keys) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.setKeys(keys);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public final void setKeys(Map<String, String> keys) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.setKeys(keys);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public final void put(String key, String value) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.put(key, value);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public final void remove(String key) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            super.remove(key);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public final void removeAll() {
        checkDatasetNotReadOnly();
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
        checkDatasetNotReadOnly();
        if (referrers == null) {
            referrers = referrer;
        } else if (referrers instanceof OsmPrimitive) {
            if (referrers != referrer) {
                referrers = new OsmPrimitive[] {(OsmPrimitive) referrers, referrer};
            }
        } else {
            for (OsmPrimitive primitive:(OsmPrimitive[]) referrers) {
                if (primitive == referrer)
                    return;
            }
            referrers = Utils.addInArrayCopy((OsmPrimitive[]) referrers, referrer);
        }
    }

    /**
     * Remove referrer. No action is taken if referrer is not registered
     * @param referrer The referrer to remove
     */
    protected void removeReferrer(OsmPrimitive referrer) {
        checkDatasetNotReadOnly();
        if (referrers instanceof OsmPrimitive) {
            if (referrers == referrer) {
                referrers = null;
            }
        } else if (referrers instanceof OsmPrimitive[]) {
            OsmPrimitive[] orig = (OsmPrimitive[]) referrers;
            int idx = -1;
            for (int i = 0; i < orig.length; i++) {
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

    private <T extends OsmPrimitive> Stream<T> referrers(boolean allowWithoutDataset, Class<T> filter) {
        // Returns only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned

        if (dataSet == null && allowWithoutDataset) {
            return Stream.empty();
        }
        checkDataset();
        if (referrers == null) {
            return Stream.empty();
        }
        final Stream<OsmPrimitive> stream = referrers instanceof OsmPrimitive
                ? Stream.of((OsmPrimitive) referrers)
                : Arrays.stream((OsmPrimitive[]) referrers);
        return stream
                .filter(p -> p.dataSet == dataSet)
                .filter(filter::isInstance)
                .map(filter::cast);
    }

    /**
     * Gets all primitives in the current dataset that reference this primitive.
     * @param filter restrict primitives to subclasses
     * @param <T> type of primitives
     * @return the referrers as Stream
     * @since 14654
     */
    public final <T extends OsmPrimitive> Stream<T> referrers(Class<T> filter) {
        return referrers(false, filter);
    }

    @Override
    public final List<OsmPrimitive> getReferrers(boolean allowWithoutDataset) {
        return referrers(allowWithoutDataset, OsmPrimitive.class)
                .collect(Collectors.toList());
    }

    @Override
    public final List<OsmPrimitive> getReferrers() {
        return getReferrers(false);
    }

    /**
     * <p>Visits {@code visitor} for all referrers.</p>
     *
     * @param visitor the visitor. Ignored, if null.
     * @since 12809
     */
    public void visitReferrers(OsmPrimitiveVisitor visitor) {
        if (visitor != null)
            doVisitReferrers(o -> o.accept(visitor));
    }

    @Override
    public void visitReferrers(PrimitiveVisitor visitor) {
        if (visitor != null)
            doVisitReferrers(o -> o.accept(visitor));
    }

    private void doVisitReferrers(Consumer<OsmPrimitive> visitor) {
        if (this.referrers == null)
            return;
        else if (this.referrers instanceof OsmPrimitive) {
            OsmPrimitive ref = (OsmPrimitive) this.referrers;
            if (ref.dataSet == dataSet) {
                visitor.accept(ref);
            }
        } else if (this.referrers instanceof OsmPrimitive[]) {
            OsmPrimitive[] refs = (OsmPrimitive[]) this.referrers;
            for (OsmPrimitive ref: refs) {
                if (ref.dataSet == dataSet) {
                    visitor.accept(ref);
                }
            }
        }
    }

    /**
     * Return true, if this primitive is a node referred by at least n ways
     * @param n Minimal number of ways to return true. Must be positive
     * @return {@code true} if this primitive is referred by at least n ways
     */
    protected final boolean isNodeReferredByWays(int n) {
        // Count only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned
        if (referrers == null) return false;
        checkDataset();
        if (referrers instanceof OsmPrimitive)
            return n <= 1 && referrers instanceof Way && ((OsmPrimitive) referrers).dataSet == dataSet;
        else {
            int counter = 0;
            for (OsmPrimitive o : (OsmPrimitive[]) referrers) {
                if (dataSet == o.dataSet && o instanceof Way && ++counter >= n)
                    return true;
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
     * @since 12809
     */
    public abstract void accept(OsmPrimitiveVisitor visitor);

    /**
     * Get and write all attributes from the parameter. Does not fire any listener, so
     * use this only in the data initializing phase
     * @param other other primitive
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
     * have an assigned OSM id, the IDs have to be the same.
     *
     * @param other the other primitive. Must not be null.
     * @throws IllegalArgumentException if other is null.
     * @throws DataIntegrityProblemException if either this is new and other is not, or other is new and this is not
     * @throws DataIntegrityProblemException if other isn't new and other.getId() != this.getId()
     */
    public void mergeFrom(OsmPrimitive other) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            CheckParameterUtil.ensureParameterNotNull(other, "other");
            if (other.isNew() ^ isNew())
                throw new DataIntegrityProblemException(
                        tr("Cannot merge because either of the participating primitives is new and the other is not"));
            if (!other.isNew() && other.getId() != id)
                throw new DataIntegrityProblemException(
                        tr("Cannot merge primitives with different ids. This id is {0}, the other is {1}", id, other.getId()));

            setKeys(other.hasKeys() ? other.getKeys() : null);
            timestamp = other.timestamp;
            version = other.version;
            setIncomplete(other.isIncomplete());
            flags = other.flags;
            user = other.user;
            changesetId = other.changesetId;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Replies true if this primitive and other are equal with respect to their semantic attributes.
     * <ol>
     *   <li>equal id</li>
     *   <li>both are complete or both are incomplete</li>
     *   <li>both have the same tags</li>
     * </ol>
     * @param other other primitive to compare
     * @return true if this primitive and other are equal with respect to their semantic attributes.
     */
    public final boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        return hasEqualSemanticAttributes(other, true);
    }

    boolean hasEqualSemanticFlags(final OsmPrimitive other) {
        if (!isNew() && id != other.id)
            return false;
        return !(isIncomplete() ^ other.isIncomplete()); // exclusive or operator for performance (see #7159)
    }

    boolean hasEqualSemanticAttributes(final OsmPrimitive other, final boolean testInterestingTagsOnly) {
        return hasEqualSemanticFlags(other)
                && (testInterestingTagsOnly ? hasSameInterestingTags(other) : getKeys().equals(other.getKeys()));
    }

    /**
     * Replies true if this primitive and other are equal with respect to their technical attributes.
     * The attributes:
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
     * @return true if this primitive and other are equal with respect to their technical attributes
     */
    public boolean hasEqualTechnicalAttributes(OsmPrimitive other) {
        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        return other != null
            && timestamp == other.timestamp
            && version == other.version
            && changesetId == other.changesetId
            && isDeleted() == other.isDeleted()
            && isModified() == other.isModified()
            && isVisible() == other.isVisible()
            && Objects.equals(user, other.user);
        // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    /**
     * Loads (clone) this primitive from provided PrimitiveData
     * @param data The object which should be cloned
     */
    public void load(PrimitiveData data) {
        checkDatasetNotReadOnly();
        // Write lock is provided by subclasses
        setKeys(data.hasKeys() ? data.getKeys() : null);
        setRawTimestamp(data.getRawTimestamp());
        user = data.getUser();
        setChangesetId(data.getChangesetId());
        setDeleted(data.isDeleted());
        setModified(data.isModified());
        setVisible(data.isVisible());
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
        data.setKeys(hasKeys() ? getKeys() : null);
        data.setRawTimestamp(getRawTimestamp());
        data.setUser(user);
        data.setDeleted(isDeleted());
        data.setModified(isModified());
        data.setVisible(isVisible());
        data.setIncomplete(isIncomplete());
        data.setChangesetId(changesetId);
        data.setVersion(version);
    }

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
                builder.append('h');
            } else {
                builder.append('d');
            }
        }
        if (isTagged()) {
            builder.append('T');
        }
        if (hasDirectionKeys()) {
            if (reversedDirection()) {
                builder.append('<');
            } else {
                builder.append('>');
            }
        }
        return builder.toString();
    }

    /**
     * Equal, if the id (and class) is equal.
     *
     * An primitive is equal to its incomplete counter part.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            OsmPrimitive that = (OsmPrimitive) obj;
            return id == that.id;
        }
    }

    /**
     * Return the id plus the class type encoded as hashcode or super's hashcode if id is 0.
     *
     * An primitive has the same hashcode as its incomplete counterpart.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public Collection<String> getTemplateKeys() {
        Collection<String> keySet = keySet();
        List<String> result = new ArrayList<>(keySet.size() + 2);
        result.add(SPECIAL_VALUE_ID);
        result.add(SPECIAL_VALUE_LOCAL_NAME);
        result.addAll(keySet);
        return result;
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (special) {
            String lc = name.toLowerCase(Locale.ENGLISH);
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
     * @param primitives primitives to fetch relations from
     *
     * @return the set of referring relations
     */
    public static Set<Relation> getParentRelations(Collection<? extends OsmPrimitive> primitives) {
        return primitives.stream()
                .flatMap(p -> p.referrers(Relation.class))
                .collect(Collectors.toSet());
    }

    /**
     * Determines if this primitive has tags denoting an area.
     * @return {@code true} if this primitive has tags denoting an area, {@code false} otherwise.
     * @since 6491
     */
    public final boolean hasAreaTags() {
        return hasKey("landuse", "amenity", "building", "building:part")
                || hasTag("area", OsmUtils.TRUE_VALUE)
                || hasTag("waterway", "riverbank")
                || hasTagDifferent("leisure", "picnic_table", "slipway", "firepit")
                || hasTag("natural", "water", "wood", "scrub", "wetland", "grassland", "heath", "rock", "bare_rock",
                                     "sand", "beach", "scree", "bay", "glacier", "shingle", "fell", "reef", "stone",
                                     "mud", "landslide", "sinkhole", "crevasse", "desert");
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

    /**
     * If necessary, extend the bbox to contain this primitive
     * @param box a bbox instance
     * @param visited a set of visited members  or null
     * @since 11269
     */
    protected abstract void addToBBox(BBox box, Set<PrimitiveId> visited);
}
