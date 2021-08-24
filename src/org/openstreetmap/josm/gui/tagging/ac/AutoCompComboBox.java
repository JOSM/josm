// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.im.InputContext;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
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
public class AutoCompComboBox<E> extends JosmComboBox<E> implements KeyListener {

    /** a regex that matches numbers */
    private static final Pattern IS_NUMBER = Pattern.compile("^\\d+$");
    /** true if the combobox should autocomplete */
    private boolean autocompleteEnabled = true;
    /** the editor will not accept text longer than this. -1 to disable */
    private int maxTextLength = -1;
    /** force a different keyboard input locale for the editor */
    private boolean useFixedLocale;

    /** Whether to autocomplete numbers */
    private final boolean AUTOCOMPLETE_NUMBERS = !Config.getPref().getBoolean("autocomplete.dont_complete_numbers", true);

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
     * A {@link DocumentFilter} to limit the text length in the editor.
     */
    private class MaxLengthDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            int newLen = fb.getDocument().getLength() + string.length();
            if (maxTextLength == -1 || newLen <= maxTextLength ||
                    // allow longer text while composing characters or it will be hard to compose
                    // the last characters before the limit
                    ((attr != null) && attr.isDefined(StyleConstants.ComposedTextAttribute))) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr)
                throws BadLocationException {
            int newLen = fb.getDocument().getLength() - length + string.length();
            if (maxTextLength == -1 || newLen <= maxTextLength ||
                    // allow longer text while composing characters or it will be hard to compose
                    // the last characters before the limit
                    ((attr != null) && attr.isDefined(StyleConstants.ComposedTextAttribute))) {
                super.replace(fb, offset, length, string, attr);
            }
        }
    }

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
        Objects.requireNonNull(model, "A model cannot be null.");
        setEditable(true);
        final JTextComponent editorComponent = getEditorComponent();
        editorComponent.addFocusListener(new InnerFocusListener(editorComponent));
        editorComponent.addKeyListener(this);
        ((AbstractDocument) editorComponent.getDocument()).setDocumentFilter(new MaxLengthDocumentFilter());
    }

    /**
     * Returns the {@link AutoCompComboBoxModel} currently used.
     *
     * @return the model
     */
    @Override
    public AutoCompComboBoxModel<E> getModel() {
        return (AutoCompComboBoxModel<E>) dataModel;
    }

    /**
     * Autocompletes what the user typed in.
     * <p>
     * Gets the user input from the editor, finds the best matching item in the model, selects it in
     * the list, sets the editor text, and highlights the autocompleted part. If there is no
     * matching item, removes the list selection.
     */
    private void autocomplete() {
        JTextField editor = getEditorComponent();
        String prefix = editor.getText();
        if (!AUTOCOMPLETE_NUMBERS && IS_NUMBER.matcher(prefix).matches())
            return;

        E item = getModel().findBestCandidate(prefix);
        if (item != null) {
            String text = item.toString();
            // This calls setItem() if the selected item changed
            // See: javax.swing.plaf.basic.BasicComboBoxUI.Handler.contentsChanged(ListDataEvent e)
            setSelectedItem(item);
            // set manually in case the selected item didn't change
            editor.setText(text);
            // select the autocompleted suffix in the editor
            editor.select(prefix.length(), text.length());
            // copy the whole autocompleted string to the unix system-wide selection (aka
            // middle-click), else only the selected suffix would be copied
            copyToSysSel(text);
        } else {
            setSelectedItem(null);
            // avoid setItem because it selects the whole text (on windows only)
            editor.setText(prefix);
        }
    }

    /**
     * Copies a String to the UNIX system-wide selection (aka middle-click).
     *
     * @param s the string to copy
     */
    void copyToSysSel(String s) {
        Clipboard sysSel = ClipboardUtils.getSystemSelection();
        if (sysSel != null) {
            Transferable transferable = new StringSelection(s);
            sysSel.setContents(transferable, null);
        }
    }

    /**
     * Sets the maximum text length.
     *
     * @param length the maximum text length in number of characters
     */
    public void setMaxTextLength(int length) {
        maxTextLength = length;
    }

    /**
     * Sets the items of the combobox to the given {@code String}s in reversed order (last element
     * first).
     *
     * @param elems The string items to set
     * @deprecated Has been moved to the model, where it belongs. Use
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel#addAllStrings} instead. Probably you want to use
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel.Preferences#load} and
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel.Preferences#save}.
     */
    @Deprecated
    public void setPossibleItems(Collection<E> elems) {
        // We have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        LinkedList<E> reversed = new LinkedList<>(elems);
        Collections.reverse(reversed);
        setPossibleAcItems(reversed);
    }

    /**
     * Sets the items of the combobox to the given {@code String}s in top down order.
     *
     * @param elems The strings to set.
     * @since 15011
     * @deprecated Has been moved to the model, where it belongs. Use
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel#addAllStrings} instead. Probably you want to use
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel.Preferences#load} and
     *     {@link org.openstreetmap.josm.gui.widgets.HistoryComboBoxModel.Preferences#save}.
     */
    @Deprecated
    public void setPossibleItemsTopDown(Collection<E> elems) {
        setPossibleAcItems(elems);
    }

    /**
     * Sets the items of the combobox to the given {@code AutoCompletionItem}s.
     *
     * @param elems AutoCompletionItem items
     * @since 12859
     * @deprecated Use {@link AutoCompComboBoxModel#addAllElements} instead.
     */
    @Deprecated
    public void setPossibleAcItems(Collection<E> elems) {
        Object oldValue = getEditor().getItem();
        getModel().removeAllElements();
        getModel().addAllElements(elems);
        getEditor().setItem(oldValue);
    }

    /**
     * Returns {@code true} if autocompletion is enabled.
     *
     * @return {@code true} if autocompletion is enabled.
     */
    public final boolean isAutocompleteEnabled() {
        return autocompleteEnabled;
    }

    /**
     * Enables or disables the autocompletion.
     *
     * @param enabled {@code true} to enable autocompletion
     * @return {@code true} if autocomplete was enabled before calling this
     * @since 18173 (signature)
     */
    public boolean setAutocompleteEnabled(boolean enabled) {
        boolean oldEnabled = this.autocompleteEnabled;
        this.autocompleteEnabled = enabled;
        return oldEnabled;
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

    /*
     * The KeyListener interface
     */

    /**
     * Listens to key events and eventually schedules an autocomplete.
     *
     * @param e the key event
     */
    @Override
    public void keyTyped(KeyEvent e) {
        if (autocompleteEnabled
                // and selection is at the end
                && getEditorComponent().getSelectionEnd() == getEditorComponent().getText().length()
                // and something visible was typed
                && !Character.isISOControl(e.getKeyChar())) {
            // We got the event before the editor component could see it. Let the editor do its job first.
            SwingUtilities.invokeLater(() -> autocomplete());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
