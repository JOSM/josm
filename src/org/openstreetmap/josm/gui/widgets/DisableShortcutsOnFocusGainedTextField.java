// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.text.Document;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A JTextField that disabled all JOSM shortcuts composed of a single key without modifier (except F1 to F12),
 * in order to avoid them to be triggered while typing.
 * This allows to include text fields in toggle dialogs (needed for relation filter).
 * @since 5696
 */
public class DisableShortcutsOnFocusGainedTextField extends JosmTextField implements DisableShortcutsOnFocusGainedComponent {

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
        DisableShortcutsOnFocusGainedComponent.super.focusGained(e);
    }

    @Override
    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        DisableShortcutsOnFocusGainedComponent.super.focusLost(e);
    }

    @Override
    public List<Pair<Action, Shortcut>> getUnregisteredActionShortcuts() {
        return this.unregisteredActionShortcuts;
    }

    @Override
    public Set<JosmAction> getDisabledMenuActions() {
        return this.disabledMenuActions;
    }
}
