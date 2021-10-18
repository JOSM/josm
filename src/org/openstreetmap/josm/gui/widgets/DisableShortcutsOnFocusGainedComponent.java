// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An interface for components with code that can be used for disabling shortcuts while they hold focus.
 *
 * @author Taylor Smock
 * @since 18285 (code extracted for {@link DisableShortcutsOnFocusGainedTextField}
 */
public interface DisableShortcutsOnFocusGainedComponent extends FocusListener {

    @Override
    default void focusGained(FocusEvent e) {
        disableMenuActions();
        unregisterActionShortcuts();
    }

    @Override
    default void focusLost(FocusEvent e) {
        restoreActionShortcuts();
        restoreMenuActions();
    }

    /**
     * Get the unregistered action shortcuts.
     * This should not be used outside the {@link DisableShortcutsOnFocusGainedComponent} interface.
     * @return The list of unregistered action shortcuts (modifiable)
     */
    List<Pair<Action, Shortcut>> getUnregisteredActionShortcuts();

    /**
     * Get the disabled menu action list
     * This should not be used outside the {@link DisableShortcutsOnFocusGainedComponent} interface.
     * @return The list of disabled menu actions (modifiable)
     */
    Set<JosmAction> getDisabledMenuActions();

    /**
     * Disables all relevant menu actions.
     * Note: This was protected
     * @see #hasToBeDisabled
     */
    default void disableMenuActions() {
        getDisabledMenuActions().clear();
        for (int i = 0; i < MainApplication.getMenu().getMenuCount(); i++) {
            JMenu menu = MainApplication.getMenu().getMenu(i);
            if (menu != null) {
                for (int j = 0; j < menu.getItemCount(); j++) {
                    JMenuItem item = menu.getItem(j);
                    if (item != null) {
                        Action action = item.getAction();
                        if (action instanceof JosmAction && action.isEnabled()) {
                            Shortcut shortcut = ((JosmAction) action).getShortcut();
                            if (shortcut != null) {
                                KeyStroke ks = shortcut.getKeyStroke();
                                if (hasToBeDisabled(ks)) {
                                    action.setEnabled(false);
                                    getDisabledMenuActions().add((JosmAction) action);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Unregisters all relevant action shortcuts.
     * Note: This was protected
     * @see #hasToBeDisabled
     */
    default void unregisterActionShortcuts() {
        getUnregisteredActionShortcuts().clear();
        // Unregister all actions with Shift modifier or without modifiers to avoid them to be triggered by typing in this text field
        for (Shortcut shortcut : Shortcut.listAll()) {
            KeyStroke ks = shortcut.getKeyStroke();
            if (hasToBeDisabled(ks)) {
                Action action = MainApplication.getRegisteredActionShortcut(shortcut);
                if (action != null) {
                    MainApplication.unregisterActionShortcut(action, shortcut);
                    getUnregisteredActionShortcuts().add(new Pair<>(action, shortcut));
                }
            }
        }
    }

    /**
     * Returns true if the given shortcut has Shift modifier or no modifier and is not an actions key.
     * Note: This was protected
     * @param ks key stroke
     * @return {@code true} if the given shortcut has to be disabled
     * @see KeyEvent#isActionKey()
     */
    default boolean hasToBeDisabled(KeyStroke ks) {
        if (this instanceof Component) {
            return ks != null && (ks.getModifiers() == 0 || isOnlyShift(ks.getModifiers())) && !new KeyEvent((Component) this,
                    KeyEvent.KEY_PRESSED, 0, ks.getModifiers(), ks.getKeyCode(), ks.getKeyChar()).isActionKey();
        }
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " is not an instanceof Component");
    }

    /**
     * Check if the modifiers is only shift
     * Note: This was private
     * @param modifiers The modifiers to check
     * @return {@code true} if the only modifier is {@link InputEvent#SHIFT_DOWN_MASK}
     */
    static boolean isOnlyShift(int modifiers) {
        return (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0
                && (modifiers & InputEvent.CTRL_DOWN_MASK) == 0
                && (modifiers & InputEvent.ALT_DOWN_MASK) == 0
                && (modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) == 0
                && (modifiers & InputEvent.META_DOWN_MASK) == 0;
    }

    /**
     * Restore all actions previously disabled
     * Note: This was protected
     */
    default void restoreMenuActions() {
        for (JosmAction a : getDisabledMenuActions()) {
            a.setEnabled(true);
        }
        getDisabledMenuActions().clear();
    }

    /**
     * Restore all action shortcuts previously unregistered
     * Note: This was protected
     */
    default void restoreActionShortcuts() {
        for (Pair<Action, Shortcut> p : getUnregisteredActionShortcuts()) {
            MainApplication.registerActionShortcut(p.a, p.b);
        }
        getUnregisteredActionShortcuts().clear();
    }
}
