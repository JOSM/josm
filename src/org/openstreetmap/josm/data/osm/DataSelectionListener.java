// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is a listener that listens to selection change events in the data set.
 * @author Michael Zangl
 * @since 12048
 */
@FunctionalInterface
public interface DataSelectionListener {

    /**
     * Called whenever the selection is changed.
     *
     * You get notified about the new selection, the elements that were added and removed and the layer that triggered the event.
     * @param event The selection change event.
     * @see SelectionChangeEvent
     */
    void selectionChanged(SelectionChangeEvent event);

    /**
     * The event that is fired when the selection changed.
     * @author Michael Zangl
     * @since 12048
     */
    interface SelectionChangeEvent {
        /**
         * Gets the previous selection
         * <p>
         * This collection cannot be modified and will not change.
         * @return The old selection
         */
        Set<OsmPrimitive> getOldSelection();

        /**
         * Gets the new selection. New elements are added to the end of the collection.
         * <p>
         * This collection cannot be modified and will not change.
         * @return The new selection
         */
        Set<OsmPrimitive> getSelection();

        /**
         * Gets the primitives that have been removed from the selection.
         * <p>
         * Those are the primitives contained in {@link #getOldSelection()} but not in {@link #getSelection()}
         * <p>
         * This collection cannot be modified and will not change.
         * @return The primitives that were removed
         */
        Set<OsmPrimitive> getRemoved();

        /**
         * Gets the primitives that have been added to the selection.
         * <p>
         * Those are the primitives contained in {@link #getSelection()} but not in {@link #getOldSelection()}
         * <p>
         * This collection cannot be modified and will not change.
         * @return The primitives that were added
         */
        Set<OsmPrimitive> getAdded();

        /**
         * Gets the data set that triggered this selection event.
         * @return The data set.
         */
        DataSet getSource();

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
     * @since 12048
     */
    abstract class AbstractSelectionEvent implements SelectionChangeEvent {
        private final DataSet source;
        private final Set<OsmPrimitive> old;

        protected AbstractSelectionEvent(DataSet source, Set<OsmPrimitive> old) {
            CheckParameterUtil.ensureParameterNotNull(source, "source");
            CheckParameterUtil.ensureParameterNotNull(old, "old");
            this.source = source;
            this.old = Collections.unmodifiableSet(old);
        }

        @Override
        public Set<OsmPrimitive> getOldSelection() {
            return old;
        }

        @Override
        public DataSet getSource() {
            return source;
        }
    }

    /**
     * The selection is replaced by a new selection
     * @author Michael Zangl
     * @since 12048
     */
    class SelectionReplaceEvent extends AbstractSelectionEvent {
        private final Set<OsmPrimitive> current;
        private Set<OsmPrimitive> removed;
        private Set<OsmPrimitive> added;

        /**
         * Create a {@link SelectionReplaceEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param newSelection The primitives of the new selection.
         */
        public SelectionReplaceEvent(DataSet source, Set<OsmPrimitive> old, Stream<OsmPrimitive> newSelection) {
            super(source, old);
            this.current = newSelection.collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public Set<OsmPrimitive> getSelection() {
            return current;
        }

        @Override
        public synchronized Set<OsmPrimitive> getRemoved() {
            if (removed == null) {
                removed = getOldSelection().stream()
                        .filter(p -> !current.contains(p))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            return removed;
        }

        @Override
        public synchronized Set<OsmPrimitive> getAdded() {
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
     * @since 12048
     */
    class SelectionAddEvent extends AbstractSelectionEvent {
        private final Set<OsmPrimitive> add;
        private final Set<OsmPrimitive> current;

        /**
         * Create a {@link SelectionAddEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toAdd The primitives to add.
         */
        public SelectionAddEvent(DataSet source, Set<OsmPrimitive> old, Stream<OsmPrimitive> toAdd) {
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
        public Set<OsmPrimitive> getSelection() {
            return Collections.unmodifiableSet(current);
        }

        @Override
        public Set<OsmPrimitive> getRemoved() {
            return Collections.emptySet();
        }

        @Override
        public Set<OsmPrimitive> getAdded() {
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
     * @since 12048
     */
    class SelectionRemoveEvent extends AbstractSelectionEvent {
        private final Set<OsmPrimitive> remove;
        private final Set<OsmPrimitive> current;

        /**
         * Create a {@link SelectionRemoveEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toRemove The primitives to remove.
         */
        public SelectionRemoveEvent(DataSet source, Set<OsmPrimitive> old, Stream<OsmPrimitive> toRemove) {
            super(source, old);
            this.remove = toRemove
                    .filter(old::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (this.remove.isEmpty()) {
                this.current = this.getOldSelection();
            } else {
                HashSet<OsmPrimitive> currentSet = new LinkedHashSet<>(old);
                currentSet.removeAll(remove);
                current = currentSet;
            }
        }

        @Override
        public Set<OsmPrimitive> getSelection() {
            return Collections.unmodifiableSet(current);
        }

        @Override
        public Set<OsmPrimitive> getRemoved() {
            return Collections.unmodifiableSet(remove);
        }

        @Override
        public Set<OsmPrimitive> getAdded() {
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
     * @since 12048
     */
    class SelectionToggleEvent extends AbstractSelectionEvent {
        private final Set<OsmPrimitive> current;
        private final Set<OsmPrimitive> remove;
        private final Set<OsmPrimitive> add;

        /**
         * Create a {@link SelectionToggleEvent}
         * @param source The source dataset
         * @param old The old primitives that were previously selected. The caller needs to ensure that this set is not modified.
         * @param toToggle The primitives to toggle.
         */
        public SelectionToggleEvent(DataSet source, Set<OsmPrimitive> old, Stream<OsmPrimitive> toToggle) {
            super(source, old);
            HashSet<OsmPrimitive> currentSet = new LinkedHashSet<>(old);
            HashSet<OsmPrimitive> removeSet = new LinkedHashSet<>();
            HashSet<OsmPrimitive> addSet = new LinkedHashSet<>();
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
        public Set<OsmPrimitive> getSelection() {
            return Collections.unmodifiableSet(current);
        }

        @Override
        public Set<OsmPrimitive> getRemoved() {
            return Collections.unmodifiableSet(remove);
        }

        @Override
        public Set<OsmPrimitive> getAdded() {
            return Collections.unmodifiableSet(add);
        }

        @Override
        public String toString() {
            return "SelectionToggleEvent [current=" + current + ", remove=" + remove + ", add=" + add + ']';
        }
    }
}
