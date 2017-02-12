// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;
import java.awt.Cursor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class manages multiple cursors for multiple components.
 * All components share the same cursor that was last set using {@link #setNewCursor(Cursor, Object)}
 *
 * @author Michael Zangl
 */
public class CursorManager {

    private final LinkedHashMap<Object, Cursor> cursors = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Component> components = new CopyOnWriteArrayList<>();

    /**
     * Creates a new NavigationCursorManager
     * @param forComponent The initial component the cursor should be managed for.
     */
    public CursorManager(Component forComponent) {
        addComponent(forComponent);
    }

    /**
     * Adds a component that this manager should send cursor changes to.
     * @param forComponent The component.
     */
    public synchronized void addComponent(Component forComponent) {
        components.addIfAbsent(forComponent);
        forComponent.setCursor(getCurrentCursor());
    }

    /**
     * Removes a component that this manager should send cursor changes to. The current cursor is not reset.
     * @param forComponent The component.
     */
    public synchronized void removeComponent(Component forComponent) {
        components.remove(forComponent);
    }

    /**
     * Set new cursor.
     * @param cursor The new cursor to use.
     * @param reference A reference object that can be passed to the next set/reset calls to identify the caller.
     */
    public synchronized void setNewCursor(Cursor cursor, Object reference) {
        Objects.requireNonNull(reference, "Cannot register a cursor that can never be removed.");
        // re-insert to allow overriding.
        cursors.remove(reference);
        cursors.put(reference, cursor);
        updateCursor();
    }

    /**
     * Remove the new cursor that was set with the given reference object. and reset to previous
     * @param reference A reference object that can be passed to the next set/reset calls to identify the caller.
     */
    public synchronized void resetCursor(Object reference) {
        if (reference == null) {
            return;
        }
        cursors.remove(reference);
        updateCursor();
    }

    private void updateCursor() {
        Cursor cursor = getCurrentCursor();
        for (Component c : components) {
            c.setCursor(cursor);
        }
    }

    private Cursor getCurrentCursor() {
        Iterator<Cursor> it = cursors.values().iterator();
        Cursor cursor = null;
        while (it.hasNext()) {
            cursor = it.next();
        }
        return cursor;
    }

}
