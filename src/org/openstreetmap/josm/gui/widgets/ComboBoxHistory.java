// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;

public class ComboBoxHistory extends DefaultComboBoxModel<AutoCompletionListItem> implements Iterable<AutoCompletionListItem> {

    private int maxSize = 10;

    private transient List<HistoryChangedListener> listeners = new ArrayList<>();

    public ComboBoxHistory(int size) {
        maxSize = size;
    }

    public void addElement(String s) {
        addElement(new AutoCompletionListItem(s));
    }

    /**
     * Adds or moves an element to the top of the history
     */
    @Override
    public void addElement(AutoCompletionListItem o) {
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

        fireHistoryChanged();
    }

    @Override
    public Iterator<AutoCompletionListItem> iterator() {
        return new Iterator<AutoCompletionListItem>() {

            private int position = -1;

            @Override
            public void remove() {
                removeElementAt(position);
            }

            @Override
            public boolean hasNext() {
                if (position < getSize()-1 && getSize() > 0)
                    return true;
                return false;
            }

            @Override
            public AutoCompletionListItem next() {
                position++;
                return getElementAt(position);
            }
        };
    }

    public void setItemsAsString(List<String> items) {
        removeAllElements();
        for (int i = items.size()-1; i >= 0; i--) {
            addElement(new AutoCompletionListItem(items.get(i)));
        }
    }

    public List<String> asStringList() {
        List<String> list = new ArrayList<>(maxSize);
        for (AutoCompletionListItem item : this) {
            list.add(item.getValue());
        }
        return list;
    }

    public void addHistoryChangedListener(HistoryChangedListener l) {
        listeners.add(l);
    }

    public void removeHistoryChangedListener(HistoryChangedListener l) {
        listeners.remove(l);
    }

    private void fireHistoryChanged() {
        for (HistoryChangedListener l : listeners) {
            l.historyChanged(asStringList());
        }
    }
}
