// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.im.InputContext;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
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

/**
 * Auto-completing ComboBox.
 * @author guilhem.bonnefille@gmail.com
 * @since 272
 */
public class AutoCompletingComboBox extends JosmComboBox<AutoCompletionItem> {

    private boolean autocompleteEnabled = true;

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
        private final JosmComboBox<AutoCompletionItem> comboBox;
        private boolean selecting;

        /**
         * Constructs a new {@code AutoCompletingComboBoxDocument}.
         * @param comboBox the combobox
         */
        AutoCompletingComboBoxDocument(final JosmComboBox<AutoCompletionItem> comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            if (selecting)
                return;
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

            if (selecting || (offs == 0 && str.equals(getText(0, getLength()))))
                return;
            if (maxTextLength > -1 && str.length()+getLength() > maxTextLength)
                return;
            boolean initial = offs == 0 && getLength() == 0 && str.length() > 1;
            super.insertString(offs, str, a);

            // return immediately when selecting an item
            // Note: this is done after calling super method because we need
            // ActionListener informed
            if (selecting)
                return;
            if (!autocompleteEnabled)
                return;
            // input method for non-latin characters (e.g. scim)
            if (a != null && a.isDefined(StyleConstants.ComposedTextAttribute))
                return;

            // if the current offset isn't at the end of the document we don't autocomplete.
            // If a highlighted autocompleted suffix was present and we get here Swing has
            // already removed it from the document. getLength() therefore doesn't include the autocompleted suffix.
            if (offs + str.length() < getLength()) {
                return;
            }

            int size = getLength();
            int start = offs+str.length();
            int end = start;
            String curText = getText(0, size);

            // item for lookup and selection
            Object item;
            // if the text is a number we don't autocomplete
            if (Config.getPref().getBoolean("autocomplete.dont_complete_numbers", true)) {
                try {
                    Long.parseLong(str);
                    if (!curText.isEmpty())
                        Long.parseLong(curText);
                    item = lookupItem(curText, true);
                } catch (NumberFormatException e) {
                    // either the new text or the current text isn't a number. We continue with autocompletion
                    item = lookupItem(curText, false);
                }
            } else {
                item = lookupItem(curText, false);
            }

            setSelectedItem(item);
            if (initial) {
                start = 0;
            }
            if (item != null) {
                String newText = ((AutoCompletionItem) item).getValue();
                if (!newText.equals(curText)) {
                    selecting = true;
                    super.remove(0, size);
                    super.insertString(0, newText, a);
                    selecting = false;
                    start = size;
                    end = getLength();
                }
            }
            final JTextComponent editorComponent = comboBox.getEditorComponent();
            // save unix system selection (middle mouse paste)
            Clipboard sysSel = ClipboardUtils.getSystemSelection();
            if (sysSel != null) {
                Transferable old = ClipboardUtils.getClipboardContent(sysSel);
                editorComponent.select(start, end);
                if (old != null) {
                    sysSel.setContents(old, null);
                }
            } else {
                editorComponent.select(start, end);
            }
        }

        private void setSelectedItem(Object item) {
            selecting = true;
            comboBox.setSelectedItem(item);
            selecting = false;
        }

        private Object lookupItem(String pattern, boolean match) {
            ComboBoxModel<AutoCompletionItem> model = comboBox.getModel();
            AutoCompletionItem bestItem = null;
            for (int i = 0, n = model.getSize(); i < n; i++) {
                AutoCompletionItem currentItem = model.getElementAt(i);
                if (currentItem.getValue().equals(pattern))
                    return currentItem;
                if (!match && currentItem.getValue().startsWith(pattern)
                && (bestItem == null || currentItem.getPriority().compareTo(bestItem.getPriority()) > 0)) {
                    bestItem = currentItem;
                }
            }
            return bestItem; // may be null
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
        setRenderer(new AutoCompleteListCellRenderer());
        final JTextComponent editorComponent = this.getEditorComponent();
        editorComponent.setDocument(new AutoCompletingComboBoxDocument(this));
        editorComponent.addFocusListener(new InnerFocusListener(editorComponent));
    }

    /**
     * Sets the maximum text length.
     * @param length the maximum text length in number of characters
     */
    public void setMaxTextLength(int length) {
        this.maxTextLength = length;
    }

    /**
     * Convert the selected item into a String that can be edited in the editor component.
     *
     * @param cbEditor    the editor
     * @param item      excepts AutoCompletionListItem, String and null
     */
    @Override
    public void configureEditor(ComboBoxEditor cbEditor, Object item) {
        if (item == null) {
            cbEditor.setItem(null);
        } else if (item instanceof String) {
            cbEditor.setItem(item);
        } else if (item instanceof AutoCompletionItem) {
            cbEditor.setItem(((AutoCompletionItem) item).getValue());
        } else
            throw new IllegalArgumentException("Unsupported item: "+item);
    }

    /**
     * Selects a given item in the ComboBox model
     * @param item      excepts AutoCompletionItem, String and null
     */
    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            super.setSelectedItem(null);
        } else if (item instanceof AutoCompletionItem) {
            super.setSelectedItem(item);
        } else if (item instanceof String) {
            String s = (String) item;
            // find the string in the model or create a new item
            for (int i = 0; i < getModel().getSize(); i++) {
                AutoCompletionItem acItem = getModel().getElementAt(i);
                if (s.equals(acItem.getValue())) {
                    super.setSelectedItem(acItem);
                    return;
                }
            }
            super.setSelectedItem(new AutoCompletionItem(s, AutoCompletionPriority.UNKNOWN));
        } else {
            throw new IllegalArgumentException("Unsupported item: "+item);
        }
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
        // disable autocomplete to prevent unnecessary actions in AutoCompletingComboBoxDocument#insertString
        autocompleteEnabled = false;
        this.setSelectedItem(oldValue);
        autocompleteEnabled = true;
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

    protected void setAutocompleteEnabled(boolean autocompleteEnabled) {
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
     * ListCellRenderer for AutoCompletingComboBox
     * renders an AutoCompletionListItem by showing only the string value part
     */
    public static class AutoCompleteListCellRenderer extends JLabel implements ListCellRenderer<AutoCompletionItem> {

        /**
         * Constructs a new {@code AutoCompleteListCellRenderer}.
         */
        public AutoCompleteListCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends AutoCompletionItem> list,
                AutoCompletionItem item,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(item.getValue());
            return this;
        }
    }
}
