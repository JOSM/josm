// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.openstreetmap.josm.gui.dialogs.relation.ac.AutoCompletionList;

/**
 * TagFieldEditor is an editor for tag names or tag values. It supports auto completion
 * from a list of auto completion items.
 *
 */
public class TagFieldEditor extends JTextField  {

    static private Logger logger = Logger.getLogger(TagFieldEditor.class.getName());

    /**
     * The document model for the editor
     */
    class AutoCompletionDocument extends PlainDocument {

        /**
         * inserts a string at a specific position
         * 
         */
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (autoCompletionList == null) {
                super.insertString(offs, str, a);
                return;
            }
            String currentText = getText(0, getLength());
            String prefix = currentText.substring(0, offs);
            autoCompletionList.applyFilter(prefix+str);
            if (autoCompletionList.getFilteredSize()>0) {
                // there are matches. Insert the new text and highlight the
                // auto completed suffix
                //
                String matchingString = autoCompletionList.getFilteredItem(0).getValue();
                remove(0,getLength());
                super.insertString(0,matchingString,a);

                // highlight from end to insert position
                //
                setCaretPosition(getLength());
                moveCaretPosition(offs + str.length());
            } else {
                // there are no matches. Insert the new text, do not highlight
                //
                String newText = prefix + str;
                remove(0,getLength());
                super.insertString(0,newText,a);
                setCaretPosition(getLength());
            }
        }
    }

    /** the auto completion list user input is matched against */
    protected AutoCompletionList autoCompletionList = null;

    /**
     * creates the default document model for this editor
     * 
     */
    @Override
    protected Document createDefaultModel() {
        return new AutoCompletionDocument();
    }

    /**
     * constructor
     */
    public TagFieldEditor() {

        addFocusListener(
                new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) {
                        selectAll();
                        applyFilter(getText());
                    }
                }
        );

        addKeyListener(
                new KeyAdapter() {

                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (getText().equals("")) {
                            applyFilter("");
                        }
                    }
                }
        );
    }

    protected void applyFilter(String filter) {
        if (autoCompletionList != null) {
            autoCompletionList.applyFilter(filter);
        }
    }

    /**
     * 
     * @return the auto completion list; may be null, if no auto completion list is set
     */
    public AutoCompletionList getAutoCompletionList() {
        return autoCompletionList;
    }

    /**
     * sets the auto completion list
     * @param autoCompletionList the auto completion list; if null, auto completion is
     *   disabled
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        this.autoCompletionList = autoCompletionList;
    }
}
