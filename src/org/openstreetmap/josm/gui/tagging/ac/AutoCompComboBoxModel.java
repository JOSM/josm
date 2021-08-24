// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A data model for the {@link AutoCompComboBox}
 *
 * @author marcello@perathoner.de
 * @param <E> The element type.
 * @since 18173
 */
public class AutoCompComboBoxModel<E> extends AbstractListModel<E> implements MutableComboBoxModel<E>, Iterable<E> {

    /**
     * The comparator used by {@link #findBestCandidate}
     * <p>
     * The comparator is used exclusively for autocompleting, and not for sorting the combobox
     * entries.  The default comparator sorts elements in alphabetical order according to
     * {@code E::toString}.
     */
    private Comparator<E> comparator;
    /** The maximum number of elements to hold, -1 for no limit. Used for histories. */
    private int maxSize = -1;

    /** the elements shown in the dropdown */
    protected ArrayList<E> elements = new ArrayList<>();
    /** the selected element in the dropdown or null */
    protected Object selected;

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
     * Sets the maximum number of elements.
     *
     * @param size The maximal number of elements in the model.
     */
    public void setSize(int size) {
        maxSize = size;
    }

    /**
     * Returns a copy of the element list.
     * @return a copy of the data
     */
    public Collection<E> asCollection() {
        return new ArrayList<>(elements);
    }

    //
    // interface java.lang.Iterable
    //

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    //
    // interface javax.swing.MutableComboBoxModel
    //

    /**
     * Adds an element to the end of the model. Does nothing if max size is already reached.
     */
    @Override
    public void addElement(E element) {
        if (element != null && (maxSize == -1 || getSize() < maxSize)) {
            elements.add(element);
        }
    }

    @Override
    public void removeElement(Object elem) {
        elements.remove(elem);
    }

    @Override
    public void removeElementAt(int index) {
        Object elem = getElementAt(index);
        if (elem == selected) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }
        elements.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    /**
     * Adds an element at a specific index.
     *
     * @param element The element to add
     * @param index Location to add the element
     */
    @Override
    public void insertElementAt(E element, int index) {
        if (maxSize != -1 && maxSize <= getSize()) {
            removeElementAt(getSize() - 1);
        }
        elements.add(index, element);
    }

    //
    // javax.swing.ComboBoxModel
    //

    /**
     * Set the value of the selected item. The selected item may be null.
     *
     * @param elem The combo box value or null for no selection.
     */
    @Override
    public void setSelectedItem(Object elem) {
        if ((selected != null && !selected.equals(elem)) ||
            (selected == null && elem != null)) {
            selected = elem;
            fireContentsChanged(this, -1, -1);
        }
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    //
    // javax.swing.ListModel
    //

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public E getElementAt(int index) {
        if (index >= 0 && index < elements.size())
            return elements.get(index);
        else
            return null;
    }

    //
    // end interfaces
    //

    /**
     * Adds all elements from the collection.
     *
     * @param elems The elements to add.
     */
    public void addAllElements(Collection<E> elems) {
        elems.forEach(e -> addElement(e));
    }

    /**
     * Adds all elements from the collection of string representations.
     *
     * @param strings The string representation of the elements to add.
     * @param buildE A {@link java.util.function.Function} that builds an {@code <E>} from a
     *               {@code String}.
     */
    public void addAllElements(Collection<String> strings, Function<String, E> buildE) {
        strings.forEach(s -> addElement(buildE.apply(s)));
    }

    /**
     * Adds an element to the top of the list.
     * <p>
     * If the element is already in the model, moves it to the top.  If the model gets too big,
     * deletes the last element.
     *
     * @param newElement the element to add
     * @return The element that is at the top now.
     */
    public E addTopElement(E newElement) {
        // if the element is already at the top, do nothing
        if (newElement.equals(getElementAt(0)))
            return getElementAt(0);

        removeElement(newElement);
        insertElementAt(newElement, 0);
        return newElement;
    }

    /**
     * Empties the list.
     */
    public void removeAllElements() {
        if (!elements.isEmpty()) {
            int firstIndex = 0;
            int lastIndex = elements.size() - 1;
            elements.clear();
            selected = null;
            fireIntervalRemoved(this, firstIndex, lastIndex);
        } else {
            selected = null;
        }
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

    /**
     * Gets a preference loader and saver.
     *
     * @param readE A {@link Function} that builds an {@code <E>} from a {@link String}.
     * @param writeE A {@code Function} that serializes an {@code <E>} to a {@code String}
     * @return The {@link Preferences} instance.
     */
    public Preferences prefs(Function<String, E> readE, Function<E, String> writeE) {
        return new Preferences(readE, writeE);
    }

    /**
     * Loads and saves the model to the JOSM preferences.
     * <p>
     * Obtainable through {@link #prefs}.
     */
    public final class Preferences {

        /** A {@link Function} that builds an {@code <E>} from a {@code String}. */
        private Function<String, E> readE;
        /** A {@code Function} that serializes {@code <E>} to a {@code String}. */
        private Function<E, String> writeE;

        /**
         * Private constructor
         *
         * @param readE A {@link Function} that builds an {@code <E>} from a {@code String}.
         * @param writeE A {@code Function} that serializes an {@code <E>} to a {@code String}
         */
        private Preferences(Function<String, E> readE, Function<E, String> writeE) {
            this.readE = readE;
            this.writeE = writeE;
        }

        /**
         * Loads the model from the JOSM preferences.
         * @param key The preferences key
         */
        public void load(String key) {
            removeAllElements();
            addAllElements(Config.getPref().getList(key), readE);
        }

        /**
         * Loads the model from the JOSM preferences.
         *
         * @param key The preferences key
         * @param defaults A list of default values.
         */
        public void load(String key, List<String> defaults) {
            removeAllElements();
            addAllElements(Config.getPref().getList(key, defaults), readE);
        }

        /**
         * Loads the model from the JOSM preferences.
         *
         * @param prop The property holding the strings.
         */
        public void load(ListProperty prop) {
            removeAllElements();
            addAllElements(prop.get(), readE);
        }

        /**
         * Returns the model elements as list of strings.
         *
         * @return a list of strings
         */
        public List<String> asStringList() {
            List<String> list = new ArrayList<>(getSize());
            forEach(element -> list.add(writeE.apply(element)));
            return list;
        }

        /**
         * Saves the model to the JOSM preferences.
         *
        * @param key The preferences key
        */
        public void save(String key) {
            Config.getPref().putList(key, asStringList());
        }

        /**
         * Saves the model to the JOSM preferences.
         *
         * @param prop The property to write to.
         */
        public void save(ListProperty prop) {
            prop.put(asStringList());
        }
    }
}
