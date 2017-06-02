// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Make shortcuts from main window work in dialog windows.
 *
 * It's not possible to simply set component input map parent to be Main.contentPane.getInputMap
 * because there is check in setParent that InputMap is for the same component.
 * Yes, this is a hack.
 * Another possibility would be simply copy InputMap, but that would require to
 * keep copies synchronized when some shortcuts are changed later.
 */
public class RedirectInputMap extends ComponentInputMap {

    private final InputMap target;

    /**
     * Create a new {@link RedirectInputMap}
     * @param component The component the input map will be added to
     * @param target The target input map that should be mirrored.
     */
    public RedirectInputMap(JComponent component, InputMap target) {
        super(component);
        this.target = target;
    }

    @Override
    public Object get(KeyStroke keyStroke) {
        return target.get(keyStroke);
    }

    @Override
    public KeyStroke[] keys() {
        return target.keys();
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public KeyStroke[] allKeys() {
        return target.allKeys();
    }

    @Override
    public void put(KeyStroke keyStroke, Object actionMapKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(KeyStroke key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Redirects the key inputs from one component to an other component
     * @param source The source component
     * @param target The target component to send the keystrokes to.
     */
    public static void redirect(JComponent source, JComponent target) {
        InputMap lastParent = source.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        while (lastParent.getParent() != null) {
            lastParent = lastParent.getParent();
        }
        lastParent.setParent(new RedirectInputMap(source, target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)));
        source.getActionMap().setParent(target.getActionMap());
    }
}
