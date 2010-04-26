// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

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
            if(selecting || (offs == 0 && str.equals(getText(0, getLength()))))
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

            int size = getLength();
            int start = offs+str.length();
            int end = start;
            String curText = getText(0, size);
            // lookup and select a matching item
            Object item = lookupItem(curText);
            setSelectedItem(item);
            if(initial) {
                start = 0;
            }
            if (item != null) {
                String newText = item.toString();
                if(!newText.equals(curText))
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
            for (int i = 0, n = model.getSize(); i < n; i++) {
                Object currentItem = model.getElementAt(i);
                if (currentItem.toString().startsWith(pattern))
                    return currentItem;
            }
            return null;
        }
    }

    public AutoCompletingComboBox() {
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

    public void setPossibleItems(Collection<String> elems) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
        Object oldValue = this.getEditor().getItem();
        model.removeAllElements();
        for (String elem : elems) {
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
}
