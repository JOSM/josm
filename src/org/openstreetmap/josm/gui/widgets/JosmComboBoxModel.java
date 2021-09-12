// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A data model for the {@link JosmComboBox}
 *
 * @author marcello@perathoner.de
 * @param <E> The element type.
 * @since 18221
 */
public class JosmComboBoxModel<E> extends AbstractListModel<E> implements MutableComboBoxModel<E>, Iterable<E> {

    /** The maximum number of elements to hold, -1 for no limit. Used for histories. */
    private int maxSize = -1;

    /** the elements shown in the dropdown */
    protected ArrayList<E> elements = new ArrayList<>();
    /** the selected element in the dropdown or null */
    protected Object selected;

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

    /**
     * Returns the index of the specified element
     *
     * Note: This is not part of the {@link javax.swing.ComboBoxModel} interface but is defined in
     * {@link javax.swing.DefaultComboBoxModel}.
     *
     * @param element the element to get the index of
     * @return an int representing the index position, where 0 is the first position
     */
    public int getIndexOf(E element) {
        return elements.indexOf(element);
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
     * Finds the item that matches string.
     * <p>
     * Looks in the model for an element whose {@code toString()} matches {@code s}.
     *
     * @param s The string to match.
     * @return The item or null
     */
    public E find(String s) {
        return elements.stream().filter(o -> o.toString().equals(s)).findAny().orElse(null);
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
