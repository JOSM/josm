// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A functional DocumentListener which sends all events to {@link #update(DocumentEvent)}.
 */
@FunctionalInterface
public interface DocumentAdapter extends DocumentListener {

    /**
     * Gives notification that there was an event.
     *
     * @param e the document event
     */
    void update(DocumentEvent e);

    @Override
    default void insertUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
        update(e);
    }

    /**
     * Utility function to create an adapter from a lambda expression
     * @param adapter the adapter
     * @return {@code adapter}
     */
    static DocumentAdapter create(DocumentAdapter adapter) {
        return adapter;
    }
}
