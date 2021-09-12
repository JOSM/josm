// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * A {@link javax.swing.ComboBoxEditor} that uses an {@link JosmTextField}.
 * <p>
 * This lets us stick a {@code JosmTextField} into a {@link javax.swing.JComboBox}.
 * Used in {@link JosmComboBox}.
 *
 * @since 18221
 */
public class JosmComboBoxEditor extends BasicComboBoxEditor {

    @Override
    protected JosmTextField createEditorComponent() {
        return new JosmTextField();
    }

    @Override
    public JosmTextField getEditorComponent() {
        // this cast holds unless somebody overrides createEditorComponent()
        return (JosmTextField) editor;
    }
}
