// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.im.InputContext;
import java.util.Locale;

import javax.swing.ComboBoxEditor;

import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.Logging;

/**
 * An auto-completing ComboBox.
 * <p>
 * When the user starts typing, this combobox will suggest the
 * {@link AutoCompComboBoxModel#findBestCandidate best matching item} from its list.  The items can
 * be of any type while the items' {@code toString} values are shown in the combobox and editor.
 *
 * @author guilhem.bonnefille@gmail.com
 * @author marcello@perathoner.de
 * @param <E> the type of the combobox entries
 * @since 18173
 */
public class AutoCompComboBox<E> extends JosmComboBox<E> implements AutoCompListener {

    /** force a different keyboard input locale for the editor */
    private boolean useFixedLocale;
    private final transient InputContext privateInputContext = InputContext.getInstance();

    /**
     * Constructs an {@code AutoCompletingComboBox}.
     */
    public AutoCompComboBox() {
        this(new AutoCompComboBoxModel<E>());
    }

    /**
     * Constructs an {@code AutoCompletingComboBox} with a supplied {@link AutoCompComboBoxModel}.
     *
     * @param model the model
     */
    public AutoCompComboBox(AutoCompComboBoxModel<E> model) {
        super(model);
        setEditor(new AutoCompComboBoxEditor<E>());
        setEditable(true);
        getEditorComponent().setModel(model);
        getEditorComponent().addAutoCompListener(this);
    }

    /**
     * Returns the {@link AutoCompComboBoxModel} currently used.
     *
     * @return the model or null
     */
    @Override
    public AutoCompComboBoxModel<E> getModel() {
        return (AutoCompComboBoxModel<E>) dataModel;
    }

    @Override
    public void setEditor(ComboBoxEditor newEditor) {
        if (editor != null) {
            editor.getEditorComponent().removePropertyChangeListener(this);
        }
        super.setEditor(newEditor);
        if (editor != null) {
            // listen to orientation changes in the editor
            editor.getEditorComponent().addPropertyChangeListener(this);
        }
    }

    /**
     * Returns the editor component
     *
     * @return the editor component
     * @see ComboBoxEditor#getEditorComponent()
     * @since 18221
     */
    @Override
    @SuppressWarnings("unchecked")
    public AutoCompTextField<E> getEditorComponent() {
        return getEditor() == null ? null : (AutoCompTextField<E>) getEditor().getEditorComponent();
    }

    /**
     * Selects the autocompleted item in the dropdown.
     *
     * @param item the item selected for autocomplete
     */
    private void autocomplete(Object item) {
        // Save the text in case item is null, because setSelectedItem will erase it.
        String savedText = getText();
        setSelectedItem(item);
        setText(savedText);
    }

    /**
     * Enables or disables the autocompletion.
     *
     * @param enabled {@code true} to enable autocompletion
     * @return {@code true} if autocomplete was enabled before calling this
     * @since 18173 (signature)
     */
    public boolean setAutocompleteEnabled(boolean enabled) {
        return getEditorComponent().setAutocompleteEnabled(enabled);
    }

    /**
     * Fixes the locale for keyboard input to US-English.
     * <p>
     * If the locale is fixed, English keyboard layout will be used by default for this combobox.
     * All other components can still have different keyboard layout selected.
     *
     * @param f if {@code true} use fixed locale
     */
    public void setFixedLocale(boolean f) {
        useFixedLocale = f;
        if (useFixedLocale) {
            Locale oldLocale = privateInputContext.getLocale();
            Logging.info("Using English input method");
            if (!privateInputContext.selectInputMethod(new Locale("en", "US"))) {
                // Unable to use English keyboard layout, disable the feature
                Logging.warn("Unable to use English input method");
                useFixedLocale = false;
                if (oldLocale != null) {
                    Logging.info("Restoring input method to " + oldLocale);
                    if (!privateInputContext.selectInputMethod(oldLocale)) {
                        Logging.warn("Unable to restore input method to " + oldLocale);
                    }
                }
            }
        }
    }

    @Override
    public InputContext getInputContext() {
        if (useFixedLocale) {
            return privateInputContext;
        }
        return super.getInputContext();
    }

    /** AutoCompListener Interface */

    @Override
    public void autoCompBefore(AutoCompEvent e) {
    }

    @Override
    public void autoCompPerformed(AutoCompEvent e) {
        autocomplete(e.getItem());
    }
}
