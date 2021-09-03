// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This interface is the same as {@link DataSelectionListener}, except it isn't {@link OsmPrimitive} specific.
 * @author Taylor Smock, Michael Zangl (original code)
 * @param <O> the base type of OSM primitives
 * @param <N> type representing OSM nodes
 * @param <W> type representing OSM ways
 * @param <R> type representing OSM relations
 * @param <D> The dataset type
 * @since 17862
 */
@FunctionalInterface
public interface IDataSelectionListener<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>,
       D extends OsmData<O, N, W, R>> {
    /**
     * Called whenever the selection is changed.
     *
     * You get notified about the new selection, the elements that were added and removed and the layer that triggered the event.
     * @param event The selection change event.
     * @see SelectionChangeEvent
     */
    void selectionChanged(SelectionChangeEvent<O, N, W, R, D> event);

    /**
     * The event that is fired when the selection changed.
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 17862 (generics)
     */
    interface SelectionChangeEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>,
              D extends OsmData<O, N, W, R>> {
        /**
         * Gets the previous selection
         * <p>
         * This collection cannot be modified and will not change.
         * @return The old selection
         */
        Set<O> getOldSelection();

        /**
         * Gets the new selection. New elements are added to the end of the collection.
         * <p>
         * This collection cannot be modified and will not change.
         * @return The new selection
         */
        Set<O> getSelection();

        /**
         * Gets the primitives that have been removed from the selection.
         * <p>
         * Those are the primitives contained in {@link #getOldSelection()} but not in {@link #getSelection()}
         * <p>
         * This collection cannot be modified and will not change.
         * @return The primitives that were removed
         */
        Set<O> getRemoved();

        /**
         * Gets the primitives that have been added to the selection.
         * <p>
         * Those are the primitives contained in {@link #getSelection()} but not in {@link #getOldSelection()}
         * <p>
         * This collection cannot be modified and will not change.
         * @return The primitives that were added
         */
        Set<O> getAdded();

        /**
         * Gets the data set that triggered this selection event.
         * @return The data set.
         */
        D getSource();

        /**
         * Test if this event did not change anything.
         * <p>
         * This will return <code>false</code> for all events that are sent to listeners, so you don't need to test it.
         * @return <code>true</code> if this did not change the selection.
         */
        default boolean isNop() {
            return getAdded().isEmpty() && getRemoved().isEmpty();
        }
    }

    /**
     * The base class for selection events
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 12048, 17862 (generics)
     */
    abstract class AbstractSelectionEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>,
             D extends OsmData<O, N, W, R>> implements SelectionChangeEvent<O, N, W, R, D> {
        private final D source;
        private final Set<O> old;

        protected AbstractSelectionEvent(D source, Set<O> old) {
            CheckParameterUtil.ensureParameterNotNull(source, "source");
            CheckParameterUtil.ensureParameterNotNull(old, "old");
            this.source = source;
            this.old = Collections.unmodifiableSet(old);
        }

        @Override
        public Set<O> getOldSelection() {
            return old;
        }

        @Override
        public D getSource() {
            return source;
        }
    }

    /**
     * The selection is replaced by a new selection
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 17862 (generics)
     */
    class SelectionReplaceEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>, D extends OsmData<O, N, W, R>>
        extends AbstractSelectionEvent<O, N, W, R, D> {
        private final Set<O> current;
        private Set<O> removed;
        private Set<O> added;

        /**
         * Create a {@link SelectionReplaceEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param newSelection The primitives of the new selection.
         */
        public SelectionReplaceEvent(D source, Set<O> old, Stream<O> newSelection) {
            super(source, old);
            this.current = newSelection.collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public Set<O> getSelection() {
            return current;
        }

        @Override
        public synchronized Set<O> getRemoved() {
            if (removed == null) {
                removed = getOldSelection().stream()
                        .filter(p -> !current.contains(p))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            return removed;
        }

        @Override
        public synchronized Set<O> getAdded() {
            if (added == null) {
                added = current.stream()
                        .filter(p -> !getOldSelection().contains(p)).collect(Collectors.toCollection(LinkedHashSet::new));
            }
            return added;
        }

        @Override
        public String toString() {
            return "SelectionReplaceEvent [current=" + current + ", removed=" + removed + ", added=" + added + ']';
        }
    }

    /**
     * Primitives are added to the selection
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 17862 (generics)
     */
    class SelectionAddEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>, D extends OsmData<O, N, W, R>>
        extends AbstractSelectionEvent<O, N, W, R, D> {
        private final Set<O> add;
        private final Set<O> current;

        /**
         * Create a {@link SelectionAddEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toAdd The primitives to add.
         */
        public SelectionAddEvent(D source, Set<O> old, Stream<O> toAdd) {
            super(source, old);
            this.add = toAdd
                    .filter(p -> !old.contains(p))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (this.add.isEmpty()) {
                this.current = this.getOldSelection();
            } else {
                this.current = new LinkedHashSet<>(old);
                this.current.addAll(add);
            }
        }

        @Override
        public Set<O> getSelection() {
            return Collections.unmodifiableSet(current);
        }

        @Override
        public Set<O> getRemoved() {
            return Collections.emptySet();
        }

        @Override
        public Set<O> getAdded() {
            return Collections.unmodifiableSet(add);
        }

        @Override
        public String toString() {
            return "SelectionAddEvent [add=" + add + ", current=" + current + ']';
        }
    }

    /**
     * Primitives are removed from the selection
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 12048, 17862 (generics)
     */
    class SelectionRemoveEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>, D extends OsmData<O, N, W, R>>
        extends AbstractSelectionEvent<O, N, W, R, D> {
        private final Set<O> remove;
        private final Set<O> current;

        /**
         * Create a {@code SelectionRemoveEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toRemove The primitives to remove.
         */
        public SelectionRemoveEvent(D source, Set<O> old, Stream<O> toRemove) {
            super(source, old);
            this.remove = toRemove
                    .filter(old::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (this.remove.isEmpty()) {
                this.current = this.getOldSelection();
            } else {
                HashSet<O> currentSet = new LinkedHashSet<>(old);
                currentSet.removeAll(remove);
                current = currentSet;
            }
        }

        @Override
        public Set<O> getSelection() {
            return Collections.unmodifiableSet(current);
        }

        @Override
        public Set<O> getRemoved() {
            return Collections.unmodifiableSet(remove);
        }

        @Override
        public Set<O> getAdded() {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "SelectionRemoveEvent [remove=" + remove + ", current=" + current + ']';
        }
    }

    /**
     * Toggle the selected state of a primitive
     * @author Michael Zangl
     * @param <O> the base type of OSM primitives
     * @param <N> type representing OSM nodes
     * @param <W> type representing OSM ways
     * @param <R> type representing OSM relations
     * @param <D> The dataset type
     * @since 17862 (generics)
     */
    class SelectionToggleEvent<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>, D extends OsmData<O, N, W, R>>
        extends AbstractSelectionEvent<O, N, W, R, D> {
        private final Set<O> current;
        private final Set<O> remove;
        private final Set<O> add;

        /**
         * Create a {@link SelectionToggleEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toToggle The primitives to toggle.
         */
        public SelectionToggleEvent(D source, Set<O> old, Stream<O> toToggle) {
            super(source, old);
            HashSet<O> currentSet = new LinkedHashSet<>(old);
            HashSet<O> removeSet = new LinkedHashSet<>();
            HashSet<O> addSet = new LinkedHashSet<>();
            toToggle.forEach(p -> {
                if (currentSet.remove(p)) {
                    removeSet.add(p);
                } else {
                    addSet.add(p);
                    currentSet.add(p);
                }
            });
            this.current = Collections.unmodifiableSet(currentSet);
            this.remove = Collections.unmodifiableSet(removeSet);
            this.add = Collections.unmodifiableSet(addSet);
        }

        @Override
        public Set<O> getSelection() {
            return current;
        }

        @Override
        public Set<O> getRemoved() {
            return remove;
        }

        @Override
        public Set<O> getAdded() {
            return add;
        }

        @Override
        public String toString() {
            return "SelectionToggleEvent [current=" + current + ", remove=" + remove + ", add=" + add + ']';
        }
    }
}
