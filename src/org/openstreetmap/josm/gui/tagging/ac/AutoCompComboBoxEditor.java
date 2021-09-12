// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import org.openstreetmap.josm.gui.widgets.JosmComboBoxEditor;

/**
 * A {@link javax.swing.ComboBoxEditor} that uses an {@link AutoCompTextField}.
 * <p>
 * This lets us stick an {@code AutoCompTextField} into a {@link javax.swing.JComboBox}.  This is not
 * used for {@link AutoCompComboBox}.
 *
 * @param <E> the type of the items in the editor
 * @since 18221
 */
public class AutoCompComboBoxEditor<E> extends JosmComboBoxEditor {

    @Override
    protected AutoCompTextField<E> createEditorComponent() {
        return new AutoCompTextField<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AutoCompTextField<E> getEditorComponent() {
        // this cast holds unless somebody overrides createEditorComponent()
        return (AutoCompTextField<E>) editor;
    }
}
