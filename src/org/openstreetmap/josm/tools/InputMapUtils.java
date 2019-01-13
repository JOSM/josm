// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Tools to work with Swing InputMap.
 * @since 5200
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
        InputMap inputMap = SwingUtilities.getUIInputMap(cmp, condition);
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        SwingUtilities.replaceUIInputMap(cmp, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
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
     * Enable activating button on Enter (which is replaced with spacebar for certain Look-And-Feels).
     * @param b Button
     */
    public static void enableEnter(JButton b) {
         b.setFocusable(true);
         addEnterAction(b, b.getAction());
    }

    /**
     * Add an action activated with Enter key on a component.
     * @param c The Swing component
     * @param a action activated with Enter key
     * @see JComponent#WHEN_FOCUSED
     */
    public static void addEnterAction(JComponent c, Action a) {
        addEnterAction(c, a, JComponent.WHEN_FOCUSED);
    }

    /**
     * Add an action activated with Enter key on a component or its children.
     * @param c The Swing component
     * @param a action activated with Enter key
     * @see JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * @since 10790
     */
    public static void addEnterActionWhenAncestor(JComponent c, Action a) {
         addEnterAction(c, a, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private static void addEnterAction(JComponent c, Action a, int condition) {
         c.getActionMap().put("enter", a);
         c.getInputMap(condition).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
    }

    /**
     * Add an action activated with Spacebar key on a component.
     * @param c The Swing component
     * @param a action activated with Spacebar key
     */
    public static void addSpacebarAction(JComponent c, Action a) {
         c.getActionMap().put("spacebar", a);
         c.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "spacebar");
    }

    /**
     * Add an action activated with ESCAPE key on a component or its children.
     * @param c The Swing component
     * @param a action activated with ESCAPE key
     * @see JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * @since 10791
     */
    public static void addEscapeAction(JComponent c, Action a) {
         c.getActionMap().put("escape", a);
         c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
    }

    /**
     * Add an action activated with Ctrl+Enter key on a component.
     * @param c The Swing component
     * @param a action activated with Ctrl+Enter key
     * @see JComponent#WHEN_IN_FOCUSED_WINDOW
     */
    public static void addCtrlEnterAction(JComponent c, Action a) {
        final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        c.getActionMap().put("ctrl_enter", a);
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "ctrl_enter");
        Optional.ofNullable(a.getValue(Action.SHORT_DESCRIPTION))
                .map(String::valueOf)
                .ifPresent(text -> Shortcut.setTooltip(a, text, stroke));
    }
}
