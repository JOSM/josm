// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.Document;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A JTextField that disabled all JOSM shortcuts composed of a single key without modifier (except F1 to F12),
 * in order to avoid them to be triggered while typing.
 * This allows to include text fields in toggle dialogs (needed for relation filter).
 * @since 5696
 */
public class DisableShortcutsOnFocusGainedTextField extends JosmTextField {

    /**
     * Constructs a new <code>TextField</code>. A default model is created,
     * the initial string is <code>null</code>, and the number of columns is set to 0.
     */
    public DisableShortcutsOnFocusGainedTextField() {
        // Contents can be set with parent methods
    }

    /**
     * Constructs a new <code>TextField</code> initialized with the
     * specified text. A default model is created and the number of columns is 0.
     *
     * @param text the text to be displayed, or <code>null</code>
     */
    public DisableShortcutsOnFocusGainedTextField(String text) {
        super(text);
    }

    /**
     * Constructs a new empty <code>TextField</code> with the specified number of columns.
     * A default model is created and the initial string is set to <code>null</code>.
     *
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from the component implementation
     */
    public DisableShortcutsOnFocusGainedTextField(int columns) {
        super(columns);
    }

    /**
     * Constructs a new <code>TextField</code> initialized with the
     * specified text and columns.  A default model is created.
     *
     * @param text the text to be displayed, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from the component implementation
     */
    public DisableShortcutsOnFocusGainedTextField(String text, int columns) {
        super(text, columns);
    }

    /**
     * Constructs a new <code>JTextField</code> that uses the given text
     * storage model and the given number of columns.
     * This is the constructor through which the other constructors feed.
     * If the document is <code>null</code>, a default model is created.
     *
     * @param doc  the text storage to use; if this is <code>null</code>,
     *      a default will be provided by calling the
     *      <code>createDefaultModel</code> method
     * @param text  the initial string to display, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width &gt;= 0; if <code>columns</code>
     *   is set to zero, the preferred width will be whatever
     *   naturally results from the component implementation
     * @throws IllegalArgumentException if <code>columns</code> &lt; 0
     */
    public DisableShortcutsOnFocusGainedTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
    }

    private final transient List<Pair<Action, Shortcut>> unregisteredActionShortcuts = new ArrayList<>();
    private final Set<JosmAction> disabledMenuActions = new HashSet<>();

    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        disableMenuActions();
        unregisterActionShortcuts();
    }

    @Override
    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        restoreActionShortcuts();
        restoreMenuActions();
    }

    /**
     * Disables all relevant menu actions.
     * @see #hasToBeDisabled
     */
    protected void disableMenuActions() {
        disabledMenuActions.clear();
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
                                    disabledMenuActions.add((JosmAction) action);
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
     * @see #hasToBeDisabled
     */
    protected void unregisterActionShortcuts() {
        unregisteredActionShortcuts.clear();
        // Unregister all actions without modifiers to avoid them to be triggered by typing in this text field
        for (Shortcut shortcut : Shortcut.listAll()) {
            KeyStroke ks = shortcut.getKeyStroke();
            if (hasToBeDisabled(ks)) {
                Action action = MainApplication.getRegisteredActionShortcut(shortcut);
                if (action != null) {
                    MainApplication.unregisterActionShortcut(action, shortcut);
                    unregisteredActionShortcuts.add(new Pair<>(action, shortcut));
                }
            }
        }
    }

    /**
     * Returns true if the given shortcut has no modifier and is not an actions key.
     * @param ks key stroke
     * @return {@code true} if the given shortcut has to be disabled
     * @see KeyEvent#isActionKey()
     */
    protected boolean hasToBeDisabled(KeyStroke ks) {
        return ks != null && ks.getModifiers() == 0 && !new KeyEvent(
                this, KeyEvent.KEY_PRESSED, 0, ks.getModifiers(), ks.getKeyCode(), ks.getKeyChar()).isActionKey();
    }

    /**
     * Restore all actions previously disabled
     */
    protected void restoreMenuActions() {
        for (JosmAction a : disabledMenuActions) {
            a.setEnabled(true);
        }
        disabledMenuActions.clear();
    }

    /**
     * Restore all action shortcuts previously unregistered
     */
    protected void restoreActionShortcuts() {
        for (Pair<Action, Shortcut> p : unregisteredActionShortcuts) {
            MainApplication.registerActionShortcut(p.a, p.b);
        }
        unregisteredActionShortcuts.clear();
    }
}
