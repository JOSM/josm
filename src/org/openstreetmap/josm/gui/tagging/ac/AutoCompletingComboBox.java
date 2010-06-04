// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyleConstants;

import org.openstreetmap.josm.Main;

/**
 * @author guilhem.bonnefille@gmail.com
 */
public class AutoCompletingComboBox extends JComboBox {

    private boolean autocompleteEnabled = true;

    /**
     * Auto-complete a JComboBox.
     *
     * Inspired by http://www.orbital-computer.de/JComboBox/
     */
    class AutoCompletingComboBoxDocument extends PlainDocument {
        private JComboBox comboBox;
        private boolean selecting = false;

        public AutoCompletingComboBoxDocument(final JComboBox comboBox) {
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
            
            // if the text starts with a number we don't autocomplete
            if (Main.pref.getBoolean("autocomplete.dont_complete_numbers", true)) {
                try {
                    Long.parseLong(str);
                    if (curText.length() == 0) {
                        // we don't autocomplete on numbers
                        return;
                    }
                    Long.parseLong(curText);
                    return;
                } catch (NumberFormatException e) {
                    // either the new text or the current text isn't a number. We continue with
                    // autocompletion
                }
            }
            
            // lookup and select a matching item
            Object item = lookupItem(curText);
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
            editor.setSelectionStart(start);
            editor.setSelectionEnd(end);
        }

        private void setSelectedItem(Object item) {
            selecting = true;
            comboBox.setSelectedItem(item);
            selecting = false;
        }

        private Object lookupItem(String pattern) {
            ComboBoxModel model = comboBox.getModel();
            AutoCompletionListItem bestItem = null;
            for (int i = 0, n = model.getSize(); i < n; i++) {
                AutoCompletionListItem currentItem = (AutoCompletionListItem) model.getElementAt(i);;
                if (currentItem.getValue().startsWith(pattern)) {
                    if (bestItem == null || currentItem.getPriority().compareTo(bestItem.getPriority()) > 0) {
                        bestItem = currentItem;
                    }
                }
            }
            return bestItem; // may be null
        }
    }

    public AutoCompletingComboBox() {
        setRenderer(new AutoCompleteListCellRenderer());
        final JTextComponent editor = (JTextComponent) this.getEditor().getEditorComponent();
        editor.setDocument(new AutoCompletingComboBoxDocument(this));
        editor.addFocusListener(
                new FocusListener() {
                    public void focusLost(FocusEvent e) {
                    }
                    public void focusGained(FocusEvent e) {
                        editor.selectAll();
                    }
                }
        );
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
            super.setSelectedItem(new AutoCompletionListItem(s, AutoCompletionItemPritority.UNKNOWN));
        } else
            throw new IllegalArgumentException();
    }

    /**
     * sets the items of the combobox to the given strings
     */
    public void setPossibleItems(Collection<String> elems) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
        Object oldValue = this.getEditor().getItem();
        model.removeAllElements();
        for (String elem : elems) {
            model.addElement(new AutoCompletionListItem(elem, AutoCompletionItemPritority.UNKNOWN));
        }
        this.getEditor().setItem(oldValue);
    }

    /**
     * sets the items of the combobox to the given AutoCompletionListItems
     */
    public void setPossibleACItems(Collection<AutoCompletionListItem> elems) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
        Object oldValue = this.getEditor().getItem();
        model.removeAllElements();
        for (AutoCompletionListItem elem : elems) {
            model.addElement(elem);
        }
        this.getEditor().setItem(oldValue);
    }


    protected boolean isAutocompleteEnabled() {
        return autocompleteEnabled;
    }

    protected void setAutocompleteEnabled(boolean autocompleteEnabled) {
        this.autocompleteEnabled = autocompleteEnabled;
    }

    /**
     * ListCellRenderer for AutoCompletingComboBox
     * renders an AutoCompletionListItem by showing only the string value part
     */
    public class AutoCompleteListCellRenderer extends JLabel implements ListCellRenderer {

        public AutoCompleteListCellRenderer() {
            setOpaque(true);
        }

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
