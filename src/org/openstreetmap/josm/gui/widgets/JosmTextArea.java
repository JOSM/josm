// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Subclass of {@link JTextArea} that adds a "native" context menu (cut/copy/paste/select all).
 * @since 5886
 */
public class JosmTextArea extends JTextArea implements FocusListener {

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
     * @throws IllegalArgumentException if the rows or columns
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
     * @throws IllegalArgumentException if the rows or columns
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
     * @throws IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public JosmTextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
        TextContextualPopupMenu.enableMenuFor(this, true);
        addFocusListener(this);
    }

    /**
     * Restore default behaviour of focus transfer with TAB, overriden by {@link JTextArea}.
     * @return {@code this}
     * @since 11308
     */
    public JosmTextArea transferFocusOnTab() {
        // http://stackoverflow.com/a/525867/2257172
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        return this;
    }

    @Override
    public void focusGained(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(false);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(true);
        }
    }
}
