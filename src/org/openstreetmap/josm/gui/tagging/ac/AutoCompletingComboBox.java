// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.im.InputContext;
import java.util.Collection;
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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;

/**
 * @author guilhem.bonnefille@gmail.com
 */
public class AutoCompletingComboBox extends JosmComboBox {

    private boolean autocompleteEnabled = true;

    private int maxTextLength = -1;
    private boolean useFixedLocale;

    /**
     * Auto-complete a JosmComboBox.
     *
     * Inspired by http://www.orbital-computer.de/JComboBox/
     */
    class AutoCompletingComboBoxDocument extends PlainDocument {
        private JosmComboBox comboBox;
        private boolean selecting = false;

        public AutoCompletingComboBoxDocument(final JosmComboBox comboBox) {
            this.comboBox = comboBox;
        }

        @Override public void remove(int offs, int len) throws BadLocationException {
            if (selecting)
                return;
            super.remove(offs, len);
        }

        @Override public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (selecting || (offs == 0 && str.equals(getText(0, getLength()))))
                return;
            if (maxTextLength > -1 && str.length()+getLength() > maxTextLength)
                return;
            boolean initial = (offs == 0 && getLength() == 0 && str.length() > 1);
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

            int size = getLength();
            int start = offs+str.length();
            int end = start;
            String curText = getText(0, size);

            // item for lookup and selection
            Object item = null;
            // if the text is a number we don't autocomplete
            if (Main.pref.getBoolean("autocomplete.dont_complete_numbers", true)) {
                try {
                    Long.parseLong(str);
                    if (curText.length() != 0)
                        Long.parseLong(curText);
                    item = lookupItem(curText, true);
                } catch (NumberFormatException e) {
                    // either the new text or the current text isn't a number. We continue with
                    // autocompletion
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
                String newText = ((AutoCompletionListItem) item).getValue();
                if (!newText.equals(curText))
                {
                    selecting = true;
                    super.remove(0, size);
                    super.insertString(0, newText, a);
                    selecting = false;
                    start = size;
                    end = getLength();
                }
            }
            JTextComponent editor = (JTextComponent)comboBox.getEditor().getEditorComponent();
            // save unix system selection (middle mouse paste)
            Clipboard sysSel = Toolkit.getDefaultToolkit().getSystemSelection();
            if(sysSel != null) {
                Transferable old = sysSel.getContents(null);
                editor.select(start, end);
                sysSel.setContents(old, null);
            } else {
                editor.select(start, end);
            }
        }

        private void setSelectedItem(Object item) {
            selecting = true;
            comboBox.setSelectedItem(item);
            selecting = false;
        }

        private Object lookupItem(String pattern, boolean match) {
            ComboBoxModel model = comboBox.getModel();
            AutoCompletionListItem bestItem = null;
            for (int i = 0, n = model.getSize(); i < n; i++) {
                AutoCompletionListItem currentItem = (AutoCompletionListItem) model.getElementAt(i);
                if (currentItem.getValue().equals(pattern))
                    return currentItem;
                if (!match && currentItem.getValue().startsWith(pattern)) {
                    if (bestItem == null || currentItem.getPriority().compareTo(bestItem.getPriority()) > 0) {
                        bestItem = currentItem;
                    }
                }
            }
            return bestItem; // may be null
        }
    }

    /**
     * Creates a <code>AutoCompletingComboBox</code> with a default prototype display value.
     */
    public AutoCompletingComboBox() {
        this(JosmComboBox.DEFAULT_PROTOTYPE_DISPLAY_VALUE);
    }

    /**
     * Creates a <code>AutoCompletingComboBox</code> with the specified prototype display value.
     * @param prototype the <code>Object</code> used to compute the maximum number of elements to be displayed at once before displaying a scroll bar.
     *                  It also affects the initial width of the combo box.
     * @since 5520
     */
    public AutoCompletingComboBox(String prototype) {
        super(new AutoCompletionListItem(prototype));
        setRenderer(new AutoCompleteListCellRenderer());
        final JTextComponent editor = (JTextComponent) this.getEditor().getEditorComponent();
        editor.setDocument(new AutoCompletingComboBoxDocument(this));
        editor.addFocusListener(
                new FocusListener() {
                    @Override
                    public void focusLost(FocusEvent e) {
                    }
                    @Override
                    public void focusGained(FocusEvent e) {
                        // save unix system selection (middle mouse paste)
                        Clipboard sysSel = Toolkit.getDefaultToolkit().getSystemSelection();
                        if(sysSel != null) {
                            Transferable old = sysSel.getContents(null);
                            editor.selectAll();
                            sysSel.setContents(old, null);
                        } else {
                            editor.selectAll();
                        }
                    }
                }
        );
    }

    public void setMaxTextLength(int length)
    {
        this.maxTextLength = length;
    }

    /**
     * Convert the selected item into a String
     * that can be edited in the editor component.
     *
     * @param editor    the editor
     * @param item      excepts AutoCompletionListItem, String and null
     */
    @Override public void configureEditor(ComboBoxEditor editor, Object item) {
        if (item == null) {
            editor.setItem(null);
        } else if (item instanceof String) {
            editor.setItem(item);
        } else if (item instanceof AutoCompletionListItem) {
            editor.setItem(((AutoCompletionListItem)item).getValue());
        } else
            throw new IllegalArgumentException();
    }

    /**
     * Selects a given item in the ComboBox model
     * @param item      excepts AutoCompletionListItem, String and null
     */
    @Override public void setSelectedItem(Object item) {
        if (item == null) {
            super.setSelectedItem(null);
        } else if (item instanceof AutoCompletionListItem) {
            super.setSelectedItem(item);
        } else if (item instanceof String) {
            String s = (String) item;
            // find the string in the model or create a new item
            for (int i=0; i< getModel().getSize(); i++) {
                AutoCompletionListItem acItem = (AutoCompletionListItem) getModel().getElementAt(i);
                if (s.equals(acItem.getValue())) {
                    super.setSelectedItem(acItem);
                    return;
                }
            }
            super.setSelectedItem(new AutoCompletionListItem(s, AutoCompletionItemPriority.UNKNOWN));
        } else
            throw new IllegalArgumentException();
    }

    /**
     * sets the items of the combobox to the given strings
     */
    public void setPossibleItems(Collection<String> elems) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
        Object oldValue = this.getEditor().getItem(); // Do not use getSelectedItem(); (fix #8013)
        model.removeAllElements();
        for (String elem : elems) {
            model.addElement(new AutoCompletionListItem(elem, AutoCompletionItemPriority.UNKNOWN));
        }
        // disable autocomplete to prevent unnecessary actions in
        // AutoCompletingComboBoxDocument#insertString
        autocompleteEnabled = false;
        this.getEditor().setItem(oldValue); // Do not use setSelectedItem(oldValue); (fix #8013)
        autocompleteEnabled = true;
    }

    /**
     * sets the items of the combobox to the given AutoCompletionListItems
     */
    public void setPossibleACItems(Collection<AutoCompletionListItem> elems) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
        Object oldValue = getSelectedItem();
        Object editorOldValue = this.getEditor().getItem();
        model.removeAllElements();
        for (AutoCompletionListItem elem : elems) {
            model.addElement(elem);
        }
        setSelectedItem(oldValue);
        this.getEditor().setItem(editorOldValue);
    }


    protected boolean isAutocompleteEnabled() {
        return autocompleteEnabled;
    }

    protected void setAutocompleteEnabled(boolean autocompleteEnabled) {
        this.autocompleteEnabled = autocompleteEnabled;
    }

    /**
     * If the locale is fixed, English keyboard layout will be used by default for this combobox
     * all other components can still have different keyboard layout selected
     */
    public void setFixedLocale(boolean f) {
        useFixedLocale = f;
        if (useFixedLocale) {
            privateInputContext.selectInputMethod(new Locale("en", "US"));
        }
    }

    private static InputContext privateInputContext = InputContext.getInstance();

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
    public static class AutoCompleteListCellRenderer extends JLabel implements ListCellRenderer {

        public AutoCompleteListCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            AutoCompletionListItem item = (AutoCompletionListItem) value;
            setText(item.getValue());
            return this;
        }
    }
}
