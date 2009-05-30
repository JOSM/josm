/* Copyright (c) 2008, Henrik Niehaus
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.gui.historycombobox;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public class SuggestingJHistoryComboBox extends JHistoryComboBox implements KeyListener {

    private EventConsumingPlainDocument doc = new EventConsumingPlainDocument();

    public SuggestingJHistoryComboBox(List<String> history) {
        super(history);

        // add keylistener for ctrl + space
        getEditor().getEditorComponent().addKeyListener(this);

        // add specialized document, which can consume events, which are
        // produced by the suggestion
        ((JTextComponent)getEditor().getEditorComponent()).setDocument(doc);

        // add DocumentFilter to trigger suggestion
        JTextField editor = (JTextField) getEditor().getEditorComponent();
        final AbstractDocument doc = (AbstractDocument) editor.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset,
                    String string, AttributeSet attr)
                    throws BadLocationException {
                super.insertString(fb, offset, string, attr);
                if(doc.getLength() > 0) {
                    suggest();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length,
                    String text, AttributeSet attrs)
                    throws BadLocationException {
                super.replace(fb, offset, length, text, attrs);
                if(doc.getLength() > 0) {
                    suggest();
                }
            }
        });
    }

    public SuggestingJHistoryComboBox() {
        this(new ArrayList<String>());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() instanceof JTextField) {
            JTextField textField = (JTextField) e.getSource();

            // if the ActionCommand equals SUGGEST, the user confirms a suggestion
            if("SUGGEST".equals(e.getActionCommand())) {
                textField.setSelectionStart(textField.getText().length());
                textField.setSelectionEnd(textField.getText().length());
                textField.setActionCommand("");
            } else { // the user has finished the input
                super.actionPerformed(e);
            }
        }
    }

    private void suggest() {
        JTextField textField = (JTextField) getEditor().getEditorComponent();
        String text = textField.getText();

        // suggest text
        for (String suggestion : super.model) {
            if (suggestion.startsWith(text)) {
                textField.setActionCommand("SUGGEST");
                doc.setConsumeEvents(true);
                // avoid unbound recursion via setText() -> replace() ->
                // suggest() -> setText() ... in some environments
                if (! text.equals(suggestion)) {
                    textField.setText(suggestion);
                }
                textField.setSelectionStart(text.length());
                textField.setSelectionEnd(textField.getText().length());
                doc.setConsumeEvents(false);
                break;
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
            suggest();
        }
    }
    public void keyPressed(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}
