// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.StyleConstants;

/**
 * A {@link DocumentFilter} to limit the text length in the editor.
 * @since 18221
 */
public class MaxLengthDocumentFilter extends DocumentFilter {
    /** the document will not accept text longer than this. -1 to disable */
    private int maxLength = -1;
    private static final String DIFFERENT = tr("<different>");

    /**
     * Sets the maximum text length.
     *
     * @param length the maximum no. of charactes allowed in this document. -1 to disable
     */
    public void setMaxLength(int length) {
        maxLength = length;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        if (mustInsertOrReplace(fb, 0, string, attr)) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr)
            throws BadLocationException {
        if (mustInsertOrReplace(fb, length, string, attr)) {
            super.replace(fb, offset, length, string, attr);
        }
    }

    private boolean mustInsertOrReplace(FilterBypass fb, int length, String string, AttributeSet attr) {
        int newLen = fb.getDocument().getLength() - length + ((string == null) ? 0 : string.length());
        return (maxLength == -1 || newLen <= maxLength || DIFFERENT.equals(string) ||
                // allow longer text while composing characters or it will be hard to compose
                // the last characters before the limit
                ((attr != null) && attr.isDefined(StyleConstants.ComposedTextAttribute)));
    }
}
