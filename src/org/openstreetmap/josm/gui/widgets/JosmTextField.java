// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * Subclass of {@link JTextField} that adds a "native" context menu (cut/copy/paste/select all).
 * @since 5886
 */
public class JosmTextField extends JTextField {

    /**
     * Constructs a new <code>JosmTextField</code> that uses the given text
     * storage model and the given number of columns.
     * This is the constructor through which the other constructors feed.
     * If the document is <code>null</code>, a default model is created.
     *
     * @param doc  the text storage to use; if this is <code>null</code>,
     *      a default will be provided by calling the
     *      <code>createDefaultModel</code> method
     * @param text  the initial string to display, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width &gt;= 0; if <code>columns</code>
     *   is set to zero, the preferred width will be whatever
     *   naturally results from the component implementation
     * @exception IllegalArgumentException if <code>columns</code> &lt; 0
     */
    public JosmTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
        TextContextualPopupMenu.enableMenuFor(this);
    }

    /**
     * Constructs a new <code>JosmTextField</code> initialized with the
     * specified text and columns.  A default model is created.
     *
     * @param text the text to be displayed, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JosmTextField(String text, int columns) {
        this(null, text, columns);
    }

    /**
     * Constructs a new <code>JosmTextField</code> initialized with the
     * specified text. A default model is created and the number of
     * columns is 0.
     *
     * @param text the text to be displayed, or <code>null</code>
     */
    public JosmTextField(String text) {
        this(null, text, 0);
    }

    /**
     * Constructs a new empty <code>JosmTextField</code> with the specified
     * number of columns.
     * A default model is created and the initial string is set to
     * <code>null</code>.
     *
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JosmTextField(int columns) {
        this(null, null, columns);
    }

    /**
     * Constructs a new <code>JosmTextField</code>.  A default model is created,
     * the initial string is <code>null</code>,
     * and the number of columns is set to 0.
     */
    public JosmTextField() {
        this(null, null, 0);
    }
}
