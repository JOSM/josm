// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.Comparator;
import java.util.Objects;

import org.openstreetmap.josm.gui.widgets.JosmComboBoxModel;

/**
 * A data model for the {@link AutoCompComboBox}
 *
 * @author marcello@perathoner.de
 * @param <E> The element type.
 * @since 18173
 */
public class AutoCompComboBoxModel<E> extends JosmComboBoxModel<E> {

    /**
     * The comparator used by {@link #findBestCandidate}
     * <p>
     * The comparator is used exclusively for autocompleting, and not for sorting the combobox
     * entries.  The default comparator sorts elements in alphabetical order according to
     * {@code E::toString}.
     */
    private Comparator<E> comparator;

    /**
     * Constructs a new empty model with a default {@link #comparator}.
     */
    public AutoCompComboBoxModel() {
        setComparator(Comparator.comparing(E::toString));
    }

    /**
     * Constructs a new empty model with a custom {@link #comparator}.
     *
     * @param comparator A custom {@link #comparator}.
     */
    public AutoCompComboBoxModel(Comparator<E> comparator) {
        setComparator(comparator);
    }

    /**
     * Sets a custom {@link #comparator}.
     * <p>
     * Example:
     * {@code setComparator(Comparator.comparing(E::getPriority).thenComparing(E::toString));}
     * <p>
     * If {@code <E>} implements {@link java.lang.Comparable Comparable} you can automagically create a
     * comparator with {@code setComparator(Comparator.naturalOrder());}.
     *
     * @param comparator A custom comparator.
     */
    public void setComparator(Comparator<E> comparator) {
        Objects.requireNonNull(comparator, "A comparator cannot be null.");
        this.comparator = comparator;
    }

    /**
     * Finds the best candidate for autocompletion.
     * <p>
     * Looks in the model for an element whose prefix matches {@code prefix}. If more than one
     * element matches {@code prefix}, returns the first of the matching elements (first according
     * to {@link #comparator}). An element that is equal to {@code prefix} is always preferred.
     *
     * @param prefix The prefix to match.
     * @return The best candidate (may be null)
     */
    public E findBestCandidate(String prefix) {
        return elements.stream()
            .filter(o -> o.toString().startsWith(prefix))
            // an element equal to the prefix is always the best candidate
            .min((x, y) -> x.toString().equals(prefix) ? -1 :
                           y.toString().equals(prefix) ? 1 :
                           comparator.compare(x, y))
            .orElse(null);
    }
}
