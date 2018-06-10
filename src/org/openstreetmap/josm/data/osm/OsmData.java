// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;

/**
 * Abstraction of {@link DataSet}.
 * This class holds OSM data but does not rely on implementation types,
 * allowing plugins to define their own representation of OSM data if needed.
 * @param <O> the base type of OSM primitives
 * @param <N> type representing OSM nodes
 * @param <W> type representing OSM ways
 * @param <R> type representing OSM relations
 * @since 13764
 */
public interface OsmData<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>> extends Data, Lockable {

    // --------------
    //    Metadata
    // --------------

    /**
     * Replies the API version this dataset was created from. May be null.
     * @return the API version this dataset was created from. May be null.
     */
    String getVersion();

    /**
     * Returns the name of this data set (optional).
     * @return the name of this data set. Can be {@code null}
     * @since 12718
     */
    String getName();

    /**
     * Sets the name of this data set.
     * @param name the new name of this data set. Can be {@code null} to reset it
     * @since 12718
     */
    void setName(String name);

    // --------------------
    //    OSM primitives
    // --------------------

    /**
     * Adds a primitive.
     * @param primitive the primitive
     */
    void addPrimitive(O primitive);

    /**
     * Removes all primitives.
     */
    void clear();

    /**
     * Searches for nodes in the given bounding box.
     * @param bbox the bounding box
     * @return List of nodes in the given bbox. Can be empty but not null
     */
    List<N> searchNodes(BBox bbox);

    /**
     * Determines if the given node can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * @param n The node to search
     * @return {@code true} if {@code n} can be retrieved in this data set, {@code false} otherwise
     */
    boolean containsNode(N n);

    /**
     * Searches for ways in the given bounding box.
     * @param bbox the bounding box
     * @return List of ways in the given bbox. Can be empty but not null
     */
    List<W> searchWays(BBox bbox);

    /**
     * Determines if the given way can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * @param w The way to search
     * @return {@code true} if {@code w} can be retrieved in this data set, {@code false} otherwise
     */
    boolean containsWay(W w);

    /**
     * Searches for relations in the given bounding box.
     * @param bbox the bounding box
     * @return List of relations in the given bbox. Can be empty but not null
     */
    List<R> searchRelations(BBox bbox);

    /**
     * Determines if the given relation can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * @param r The relation to search
     * @return {@code true} if {@code r} can be retrieved in this data set, {@code false} otherwise
     */
    boolean containsRelation(R r);

    /**
     * Returns a primitive with a given id from the data set. null, if no such primitive exists
     *
     * @param id uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @throws NullPointerException if type is null
     */
    default O getPrimitiveById(long id, OsmPrimitiveType type) {
        return getPrimitiveById(new SimplePrimitiveId(id, type));
    }

    /**
     * Returns a primitive with a given id from the data set. null, if no such primitive exists
     *
     * @param primitiveId type and uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @return the primitive
     */
    O getPrimitiveById(PrimitiveId primitiveId);

    /**
     * Gets a filtered collection of primitives matching the given predicate.
     * @param <T> The primitive type.
     * @param predicate The predicate to match
     * @return The list of primtives.
     * @since 10590
     */
    <T extends O> Collection<T> getPrimitives(Predicate<? super O> predicate);

    /**
     * Replies an unmodifiable collection of nodes in this dataset
     *
     * @return an unmodifiable collection of nodes in this dataset
     */
    Collection<N> getNodes();

    /**
     * Replies an unmodifiable collection of ways in this dataset
     *
     * @return an unmodifiable collection of ways in this dataset
     */
    Collection<W> getWays();

    /**
     * Replies an unmodifiable collection of relations in this dataset
     *
     * @return an unmodifiable collection of relations in this dataset
     */
    Collection<R> getRelations();

    /**
     * Returns a collection containing all primitives of the dataset.
     * @return A collection containing all primitives of the dataset. Data is not ordered
     */
    default Collection<O> allPrimitives() {
        return getPrimitives(o -> true);
    }

    /**
     * Returns a collection containing all not-deleted primitives.
     * @return A collection containing all not-deleted primitives.
     * @see OsmPrimitive#isDeleted
     */
    default Collection<O> allNonDeletedPrimitives() {
        return getPrimitives(p -> !p.isDeleted());
    }

    /**
     * Returns a collection containing all not-deleted complete primitives.
     * @return A collection containing all not-deleted complete primitives.
     * @see OsmPrimitive#isDeleted
     * @see OsmPrimitive#isIncomplete
     */
    default Collection<O> allNonDeletedCompletePrimitives() {
        return getPrimitives(primitive -> !primitive.isDeleted() && !primitive.isIncomplete());
    }

    /**
     * Returns a collection containing all not-deleted complete physical primitives.
     * @return A collection containing all not-deleted complete physical primitives (nodes and ways).
     * @see OsmPrimitive#isDeleted
     * @see OsmPrimitive#isIncomplete
     */
    default Collection<O> allNonDeletedPhysicalPrimitives() {
        return getPrimitives(
                primitive -> !primitive.isDeleted() && !primitive.isIncomplete() && !(primitive instanceof IRelation));
    }

    /**
     * Returns a collection containing all modified primitives.
     * @return A collection containing all modified primitives.
     * @see OsmPrimitive#isModified
     */
    default Collection<O> allModifiedPrimitives() {
        return getPrimitives(IPrimitive::isModified);
    }

    /**
     * Returns a collection containing all primitives preserved from filtering.
     * @return A collection containing all primitives preserved from filtering.
     * @see OsmPrimitive#isPreserved
     * @since 13309
     */
    default Collection<O> allPreservedPrimitives() {
        return getPrimitives(IPrimitive::isPreserved);
    }

    // --------------
    //    Policies
    // --------------

    /**
     * Get the download policy.
     * @return the download policy
     * @see #setDownloadPolicy(DownloadPolicy)
     * @since 13453
     */
    DownloadPolicy getDownloadPolicy();

    /**
     * Sets the download policy.
     * @param downloadPolicy the download policy
     * @see #getUploadPolicy()
     * @since 13453
     */
    void setDownloadPolicy(DownloadPolicy downloadPolicy);

    /**
     * Get the upload policy.
     * @return the upload policy
     * @see #setUploadPolicy(UploadPolicy)
     */
    UploadPolicy getUploadPolicy();

    /**
     * Sets the upload policy.
     * @param uploadPolicy the upload policy
     * @see #getUploadPolicy()
     */
    void setUploadPolicy(UploadPolicy uploadPolicy);

    // --------------
    //    Locks
    // --------------

    /**
     * Returns the lock used for reading.
     * @return the lock used for reading
     */
    Lock getReadLock();

    // ---------------
    //    Highlight
    // ---------------

    /**
     * Returns an unmodifiable collection of *WaySegments* whose virtual
     * nodes should be highlighted. WaySegments are used to avoid having
     * to create a VirtualNode class that wouldn't have much purpose otherwise.
     *
     * @return unmodifiable collection of WaySegments
     */
    Collection<WaySegment> getHighlightedVirtualNodes();

    /**
     * Returns an unmodifiable collection of WaySegments that should be highlighted.
     *
     * @return unmodifiable collection of WaySegments
     */
    Collection<WaySegment> getHighlightedWaySegments();

    /**
     * clear all highlights of virtual nodes
     */
    default void clearHighlightedVirtualNodes() {
        setHighlightedVirtualNodes(new ArrayList<WaySegment>());
    }

    /**
     * clear all highlights of way segments
     */
    default void clearHighlightedWaySegments() {
        setHighlightedWaySegments(new ArrayList<WaySegment>());
    }

    /**
     * set what virtual nodes should be highlighted. Requires a Collection of
     * *WaySegments* to avoid a VirtualNode class that wouldn't have much use otherwise.
     * @param waySegments Collection of way segments
     */
    void setHighlightedVirtualNodes(Collection<WaySegment> waySegments);

    /**
     * set what virtual ways should be highlighted.
     * @param waySegments Collection of way segments
     */
    void setHighlightedWaySegments(Collection<WaySegment> waySegments);

    /**
     * Adds a listener that gets notified whenever way segment / virtual nodes highlights change.
     * @param listener The Listener
     * @since 12014
     */
    void addHighlightUpdateListener(HighlightUpdateListener listener);

    /**
     * Removes a listener that was added with {@link #addHighlightUpdateListener(HighlightUpdateListener)}
     * @param listener The Listener
     * @since 12014
     */
    void removeHighlightUpdateListener(HighlightUpdateListener listener);

    // ---------------
    //    Selection
    // ---------------

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, except deleted ones. May be empty, but not null.
     *
     * When iterating through the set it is ordered by the order in which the primitives were added to the selection.
     *
     * @return unmodifiable collection of primitives
     */
    Collection<O> getSelected();

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, including deleted ones. May be empty, but not null.
     *
     * When iterating through the set it is ordered by the order in which the primitives were added to the selection.
     *
     * @return unmodifiable collection of primitives
     */
    Collection<O> getAllSelected();

    /**
     * Returns selected nodes.
     * @return selected nodes
     */
    Collection<N> getSelectedNodes();

    /**
     * Returns selected ways.
     * @return selected ways
     */
    Collection<W> getSelectedWays();

    /**
     * Returns selected relations.
     * @return selected relations
     */
    Collection<R> getSelectedRelations();

    /**
     * Determines whether the selection is empty or not
     * @return whether the selection is empty or not
     */
    boolean selectionEmpty();

    /**
     * Determines whether the given primitive is selected or not
     * @param osm the primitive
     * @return whether {@code osm} is selected or not
     */
    boolean isSelected(O osm);

    /**
     * Toggles the selected state of the given collection of primitives.
     * @param osm The primitives to toggle
     */
    void toggleSelected(Collection<? extends PrimitiveId> osm);

    /**
     * Toggles the selected state of the given collection of primitives.
     * @param osm The primitives to toggle
     */
    void toggleSelected(PrimitiveId... osm);

    /**
     * Sets the current selection to the primitives in <code>selection</code>
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param selection the selection
     */
    void setSelected(Collection<? extends PrimitiveId> selection);

    /**
     * Sets the current selection to the primitives in <code>osm</code>
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param osm the primitives to set. <code>null</code> values are ignored for now, but this may be removed in the future.
     */
    void setSelected(PrimitiveId... osm);

    /**
     * Adds the primitives in <code>selection</code> to the current selection
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param selection the selection
     */
    void addSelected(Collection<? extends PrimitiveId> selection);

    /**
     * Adds the primitives in <code>osm</code> to the current selection
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param osm the primitives to add
     */
    void addSelected(PrimitiveId... osm);

    /**
     * Removes the selection from every value in the collection.
     * @param osm The collection of ids to remove the selection from.
     */
    void clearSelection(PrimitiveId... osm);

    /**
     * Removes the selection from every value in the collection.
     * @param list The collection of ids to remove the selection from.
     */
    void clearSelection(Collection<? extends PrimitiveId> list);

    /**
     * Clears the current selection.
     */
    void clearSelection();

    /**
     * Add a listener that listens to selection changes in this specific data set.
     * @param listener The listener.
     * @see #removeSelectionListener(DataSelectionListener)
     * @see SelectionEventManager#addSelectionListener(SelectionChangedListener,
     *      org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode)
     *      To add a global listener.
     */
    void addSelectionListener(DataSelectionListener listener);

    /**
     * Remove a listener that listens to selection changes in this specific data set.
     * @param listener The listener.
     * @see #addSelectionListener(DataSelectionListener)
     */
    void removeSelectionListener(DataSelectionListener listener);

    // -------------------
    //    Miscellaneous
    // -------------------

    /**
     * Returns the data sources bounding box.
     * @return the data sources bounding box
     */
    default ProjectionBounds getDataSourceBoundingBox() {
        BoundingXYVisitor bbox = new BoundingXYVisitor();
        for (DataSource source : getDataSources()) {
            bbox.visit(source.bounds);
        }
        if (bbox.hasExtend()) {
            return bbox.getBounds();
        }
        return null;
    }

    /**
     * Clear the mappaint cache for this DataSet.
     * @since 13420
     */
    void clearMappaintCache();

    /**
     * Replies true if there is at least one primitive in this dataset with
     * {@link IPrimitive#isModified()} == <code>true</code>.
     *
     * @return true if there is at least one primitive in this dataset with
     * {@link IPrimitive#isModified()} == <code>true</code>.
     */
    default boolean isModified() {
        return false;
    }
}
