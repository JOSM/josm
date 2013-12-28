// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Tools to work with Swing InputMap
 *
 */
public final class InputMapUtils {
    
    private InputMapUtils() {
        // Hide default constructor for utils classes
    }
    
    /**
     * Unassign Ctrl-Shift/Alt-Shift Up/Down from the given component
     * to allow global JOSM shortcuts to work in this component.
     * @param cmp The Swing component
     * @param condition one of the following values:
     * <ul>
     * <li>JComponent.FOCUS_INPUTMAP_CREATED
     * <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <li>JComponent.WHEN_IN_FOCUSED_WINDOW
     * </ul>
     */
    public static void unassignCtrlShiftUpDown(JComponent cmp, int condition) {
        InputMap inputMap=SwingUtilities.getUIInputMap(cmp, condition);
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP,InputEvent.CTRL_MASK|InputEvent.SHIFT_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,InputEvent.CTRL_MASK|InputEvent.SHIFT_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP,InputEvent.ALT_MASK|InputEvent.SHIFT_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,InputEvent.ALT_MASK|InputEvent.SHIFT_MASK));
        SwingUtilities.replaceUIInputMap(cmp,JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,inputMap);
    }

    /**
     * Unassign PageUp/PageDown from the given component
     * to allow global JOSM shortcuts to work in this component.
     * @param cmp The Swing component
     * @param condition one of the following values:
     * <ul>
     * <li>JComponent.FOCUS_INPUTMAP_CREATED
     * <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <li>JComponent.WHEN_IN_FOCUSED_WINDOW
     * </ul>
     * @since 6557
     */
    public static void unassignPageUpDown(JComponent cmp, int condition) {
        InputMap inputMap = SwingUtilities.getUIInputMap(cmp, condition);
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
        SwingUtilities.replaceUIInputMap(cmp, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
    }

    /**
     * Enable activating button on Enter (which is replaced with spacebar for certain Look-And-Feels)
     */
    public static void enableEnter(JButton b) {
         b.setFocusable(true);
         b.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
         b.getActionMap().put("enter",b.getAction());
    }

    public static void addEnterAction(JComponent c, Action a) {
         c.getActionMap().put("enter", a);
         c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
    }

    public static void addSpacebarAction(JComponent c, Action a) {
         c.getActionMap().put("spacebar", a);
         c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "spacebar");
    }
}
