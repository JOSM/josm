// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.im.InputContext;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyleConstants;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Auto-completing ComboBox.
 * @author guilhem.bonnefille@gmail.com
 * @since 272
 */
public class AutoCompletingComboBox extends JosmComboBox<AutoCompletionItem> {

    private boolean autocompleteEnabled = true;
    private boolean locked;

    private int maxTextLength = -1;
    private boolean useFixedLocale;

    private final transient InputContext privateInputContext = InputContext.getInstance();

    static final class InnerFocusListener implements FocusListener {
        private final JTextComponent editorComponent;

        InnerFocusListener(JTextComponent editorComponent) {
            this.editorComponent = editorComponent;
        }

        @Override
        public void focusLost(FocusEvent e) {
            MapFrame map = MainApplication.getMap();
            if (map != null) {
                map.keyDetector.setEnabled(true);
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            MapFrame map = MainApplication.getMap();
            if (map != null) {
                map.keyDetector.setEnabled(false);
            }
            // save unix system selection (middle mouse paste)
            Clipboard sysSel = ClipboardUtils.getSystemSelection();
            if (sysSel != null) {
                Transferable old = ClipboardUtils.getClipboardContent(sysSel);
                editorComponent.selectAll();
                if (old != null) {
                    sysSel.setContents(old, null);
                }
            } else if (e != null && e.getOppositeComponent() != null) {
                // Select all characters when the change of focus occurs inside JOSM only.
                // When switching from another application, it is annoying, see #13747
                editorComponent.selectAll();
            }
        }
    }

    /**
     * Auto-complete a JosmComboBox.
     * <br>
     * Inspired by <a href="http://www.orbital-computer.de/JComboBox">Thomas Bierhance example</a>.
     */
    class AutoCompletingComboBoxDocument extends PlainDocument {

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            try {
                super.remove(offs, len);
            } catch (IllegalArgumentException e) {
                // IAE can happen with Devanagari script, see #15825
                Logging.error(e);
            }
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            // TODO get rid of code duplication w.r.t. AutoCompletingTextField.AutoCompletionDocument.insertString

            if (maxTextLength > -1 && str.length() + getLength() > maxTextLength)
                return;

            super.insertString(offs, str, a);

            if (locked)
                return; // don't get in a loop

            if (!autocompleteEnabled)
                return;

            // input method for non-latin characters (e.g. scim)
            if (a != null && a.isDefined(StyleConstants.ComposedTextAttribute))
                return;

            // if the cursor isn't at the end of the text we don't autocomplete.
            // If a highlighted autocompleted suffix was present and we get here Swing has
            // already removed it from the document. getLength() therefore doesn't include the autocompleted suffix.
            if (offs + str.length() < getLength()) {
                return;
            }

            String prefix = getText(0, getLength()); // the whole text after insertion

            if (Config.getPref().getBoolean("autocomplete.dont_complete_numbers", true)
                    && prefix.matches("^\\d+$"))
                return;

            autocomplete(prefix);

            // save unix system selection (middle mouse paste)
            Clipboard sysSel = ClipboardUtils.getSystemSelection();
            if (sysSel != null) {
                Transferable old = ClipboardUtils.getClipboardContent(sysSel);
                if (old != null) {
                    sysSel.setContents(old, null);
                }
            }
        }
    }

    /**
     * Creates a <code>AutoCompletingComboBox</code> with a default prototype display value.
     */
    public AutoCompletingComboBox() {
        this("Foo");
    }

    /**
     * Creates a <code>AutoCompletingComboBox</code> with the specified prototype display value.
     * @param prototype the <code>Object</code> used to compute the maximum number of elements to be displayed at once
     *                  before displaying a scroll bar. It also affects the initial width of the combo box.
     * @since 5520
     */
    public AutoCompletingComboBox(String prototype) {
        super(new AutoCompletionItem(prototype));
        final JTextComponent editorComponent = this.getEditorComponent();
        editorComponent.setDocument(new AutoCompletingComboBoxDocument());
        editorComponent.addFocusListener(new InnerFocusListener(editorComponent));
    }

    /**
     * Autocomplete a string.
     * <p>
     * Look in the model for an item whose true prefix matches the string. If
     * found, set the editor to the item and select the item in the model too.
     *
     * @param prefix The prefix to autocomplete.
     */
    private void autocomplete(String prefix) {
        // candidate item for autocomplete
        AutoCompletionItem item = findBestCandidate(prefix);
        if (item != null) {
            try {
                locked = true;
                setSelectedItem(item);
                getEditor().setItem(item);
                // select the autocompleted suffix in the editor
                getEditorComponent().select(prefix.length(), item.getValue().length());
            } finally {
                locked = false;
            }
        }
    }

    /**
     * Find the best candidate for autocompletion.
     * @param prefix The true prefix to match.
     * @return The best candidate (may be null)
     */
    private AutoCompletionItem findBestCandidate(String prefix) {
        ComboBoxModel<AutoCompletionItem> model = getModel();
        AutoCompletionItem bestCandidate = null;
        for (int i = 0, n = model.getSize(); i < n; i++) {
            AutoCompletionItem currentItem = model.getElementAt(i);
            // the "same" string is always the best candidate, but it is of
            // no use for autocompletion
            if (currentItem.getValue().equals(prefix))
                return null;
            if (currentItem.getValue().startsWith(prefix)
            && (bestCandidate == null || currentItem.getPriority().compareTo(bestCandidate.getPriority()) > 0)) {
                bestCandidate = currentItem;
            }
        }
        return bestCandidate;
    }

    /**
     * Sets the maximum text length.
     * @param length the maximum text length in number of characters
     */
    public void setMaxTextLength(int length) {
        this.maxTextLength = length;
    }

    /**
     * Selects a given item in the ComboBox model
     * @param item the item of type AutoCompletionItem, String or null
     * @param disableAutoComplete if true, autocomplete {@linkplain #setAutocompleteEnabled is disabled} during the operation
     * @since 15885
     */
    public void setSelectedItem(Object item, final boolean disableAutoComplete) {
        final boolean previousState = isAutocompleteEnabled();
        if (disableAutoComplete) {
            // disable autocomplete to prevent unnecessary actions in AutoCompletingComboBoxDocument#insertString
            setAutocompleteEnabled(false);
        }
        setSelectedItem(item);
        setAutocompleteEnabled(previousState);
    }

    /**
     * Sets the items of the combobox to the given {@code String}s in reversed order (last element first).
     * @param elems String items
     */
    public void setPossibleItems(Collection<String> elems) {
        DefaultComboBoxModel<AutoCompletionItem> model = (DefaultComboBoxModel<AutoCompletionItem>) this.getModel();
        Object oldValue = this.getEditor().getItem(); // Do not use getSelectedItem(); (fix #8013)
        model.removeAllElements();
        for (String elem : elems) {
            model.addElement(new AutoCompletionItem(elem, AutoCompletionPriority.UNKNOWN));
        }
        this.setSelectedItem(null);
        this.setSelectedItem(oldValue, true);
    }

    /**
     * Sets the items of the combobox to the given {@code String}s in top down order.
     * @param elems Collection of String items (is not changed)
     * @since 15011
     */
    public void setPossibleItemsTopDown(Collection<String> elems) {
        // We have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        LinkedList<String> reversed = new LinkedList<>(elems);
        Collections.reverse(reversed);
        setPossibleItems(reversed);
    }

    /**
     * Sets the items of the combobox to the given {@code AutoCompletionItem}s.
     * @param elems AutoCompletionItem items
     * @since 12859
     */
    public void setPossibleAcItems(Collection<AutoCompletionItem> elems) {
        DefaultComboBoxModel<AutoCompletionItem> model = (DefaultComboBoxModel<AutoCompletionItem>) this.getModel();
        Object oldValue = getSelectedItem();
        Object editorOldValue = this.getEditor().getItem();
        model.removeAllElements();
        for (AutoCompletionItem elem : elems) {
            model.addElement(elem);
        }
        setSelectedItem(oldValue);
        this.getEditor().setItem(editorOldValue);
    }

    /**
     * Determines if autocompletion is enabled.
     * @return {@code true} if autocompletion is enabled, {@code false} otherwise.
     */
    public final boolean isAutocompleteEnabled() {
        return autocompleteEnabled;
    }

    /**
     * Sets whether the autocompletion is enabled
     * @param autocompleteEnabled {@code true} to enable autocompletion
     * @since 15567 (visibility)
     */
    public void setAutocompleteEnabled(boolean autocompleteEnabled) {
        this.autocompleteEnabled = autocompleteEnabled;
    }

    /**
     * If the locale is fixed, English keyboard layout will be used by default for this combobox
     * all other components can still have different keyboard layout selected
     * @param f fixed locale
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

    /**
     * Returns the edited item with whitespaces removed
     * @return the edited item with whitespaces removed
     * @since 15835
     */
    public String getEditItem() {
        return Utils.removeWhiteSpaces(getEditor().getItem().toString());
    }

    /**
     * Returns the selected item or the edited item as string
     * @return the selected item or the edited item as string
     * @see #getSelectedItem()
     * @see #getEditItem()
     * @since 15835
     */
    public String getSelectedOrEditItem() {
        final Object selectedItem = getSelectedItem();
        if (selectedItem instanceof AutoCompletionItem) {
            return ((AutoCompletionItem) selectedItem).getValue();
        } else if (selectedItem instanceof String) {
            return (String) selectedItem;
        } else {
            return getEditItem();
        }
    }
}
