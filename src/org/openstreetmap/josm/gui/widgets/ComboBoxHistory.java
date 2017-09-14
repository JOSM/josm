// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.DefaultComboBoxModel;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;

/**
 * A data model for {@link HistoryComboBox}
 */
class ComboBoxHistory extends DefaultComboBoxModel<AutoCompletionItem> implements Iterable<AutoCompletionItem> {

    private final int maxSize;

    /**
     * Constructs a {@code ComboBoxHistory} keeping track of {@code maxSize} items
     * @param size the history size
     */
    ComboBoxHistory(int size) {
        maxSize = size;
    }

    /**
     * Adds or moves an element to the top of the history
     * @param s the element to add
     */
    public void addElement(String s) {
        addElement(new AutoCompletionItem(s));
    }

    /**
     * Adds or moves an element to the top of the history
     * @param o the element to add
     */
    @Override
    public void addElement(AutoCompletionItem o) {
        String newEntry = o.getValue();

        // if history contains this object already, delete it,
        // so that it looks like a move to the top
        for (int i = 0; i < getSize(); i++) {
            String oldEntry = getElementAt(i).getValue();
            if (oldEntry.equals(newEntry)) {
                removeElementAt(i);
            }
        }

        // insert element at the top
        insertElementAt(o, 0);

        // remove an element, if the history gets too large
        if (getSize() > maxSize) {
            removeElementAt(getSize()-1);
        }

        // set selected item to the one just added
        setSelectedItem(o);
    }

    @Override
    public Iterator<AutoCompletionItem> iterator() {
        return new Iterator<AutoCompletionItem>() {

            private int position = -1;

            @Override
            public void remove() {
                removeElementAt(position);
            }

            @Override
            public boolean hasNext() {
                return position < getSize()-1 && getSize() > 0;
            }

            @Override
            public AutoCompletionItem next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                position++;
                return getElementAt(position);
            }
        };
    }

    /**
     * {@link javax.swing.DefaultComboBoxModel#removeAllElements() Removes all items}
     * and {@link ComboBoxHistory#addElement(String) adds} the given items.
     * @param items the items to set
     */
    public void setItemsAsString(List<String> items) {
        removeAllElements();
        for (int i = items.size()-1; i >= 0; i--) {
            addElement(items.get(i));
        }
    }

    /**
     * Returns the {@link AutoCompletionItem} items as strings
     * @return a list of strings
     */
    public List<String> asStringList() {
        List<String> list = new ArrayList<>(maxSize);
        for (AutoCompletionItem item : this) {
            list.add(item.getValue());
        }
        return list;
    }
}
