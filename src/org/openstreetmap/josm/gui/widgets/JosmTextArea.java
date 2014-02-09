// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.JTextArea;
import javax.swing.text.Document;

/**
 * Subclass of {@link JTextArea} that adds a "native" context menu (cut/copy/paste/select all).
 * @since 5886
 */
public class JosmTextArea extends JTextArea {

    /**
     * Constructs a new {@code JosmTextArea}. A default model is set, the initial string
     * is null, and rows/columns are set to 0.
     */
    public JosmTextArea() {
        this(null, null, 0, 0);
    }

    /**
     * Constructs a new {@code JosmTextArea} with the specified text displayed.
     * A default model is created and rows/columns are set to 0.
     *
     * @param text the text to be displayed, or null
     */
    public JosmTextArea(String text) {
        this(null, text, 0, 0);
    }

    /**
     * Constructs a new {@code JosmTextArea} with the given document model, and defaults
     * for all of the other arguments (null, 0, 0).
     *
     * @param doc  the model to use
     */
    public JosmTextArea(Document doc) {
        this(doc, null, 0, 0);
    }

    /**
     * Constructs a new empty {@code JosmTextArea} with the specified number of
     * rows and columns. A default model is created, and the initial
     * string is null.
     *
     * @param rows the number of rows &gt;= 0
     * @param columns the number of columns &gt;= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public JosmTextArea(int rows, int columns) {
        this(null, null, rows, columns);
    }

    /**
     * Constructs a new {@code JosmTextArea} with the specified text and number
     * of rows and columns. A default model is created.
     *
     * @param text the text to be displayed, or null
     * @param rows the number of rows &gt;= 0
     * @param columns the number of columns &gt;= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public JosmTextArea(String text, int rows, int columns) {
        this(null, text, rows, columns);
    }

    /**
     * Constructs a new {@code JosmTextArea} with the specified number of rows
     * and columns, and the given model.  All of the constructors
     * feed through this constructor.
     *
     * @param doc the model to use, or create a default one if null
     * @param text the text to be displayed, null if none
     * @param rows the number of rows &gt;= 0
     * @param columns the number of columns &gt;= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public JosmTextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
        TextContextualPopupMenu.enableMenuFor(this);
    }
}
