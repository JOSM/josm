// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.text.Document;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * Subclass of {@link JTextField} that:<ul>
 * <li>adds a "native" context menu (undo/redo/cut/copy/paste/select all)</li>
 * <li>adds an optional "hint" displayed when no text has been entered</li>
 * <li>disables the global advanced key press detector when focused</li>
 * <li>implements a workaround to <a href="https://bugs.openjdk.java.net/browse/JDK-6322854">JDK bug 6322854</a></li>
 * </ul><br>This class must be used everywhere in core and plugins instead of {@code JTextField}.
 * @since 5886
 */
public class JosmTextField extends JTextField implements Destroyable, FocusListener {

    private final PopupMenuLauncher launcher;
    private String hint;

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
     * @throws IllegalArgumentException if <code>columns</code> &lt; 0
     */
    public JosmTextField(Document doc, String text, int columns) {
        this(doc, text, columns, true);
    }

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
     * @param undoRedo Enables or not Undo/Redo feature. Not recommended for table cell editors, unless each cell provides its own editor
     * @throws IllegalArgumentException if <code>columns</code> &lt; 0
     */
    public JosmTextField(Document doc, String text, int columns, boolean undoRedo) {
        super(doc, text, columns);
        launcher = TextContextualPopupMenu.enableMenuFor(this, undoRedo);
        // Fix minimum size when columns are specified
        if (columns > 0) {
            setMinimumSize(getPreferredSize());
        }
        addFocusListener(this);
        // Workaround for Java bug 6322854
        JosmPasswordField.workaroundJdkBug6322854(this);
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

    /**
     * Replies the hint displayed when no text has been entered.
     * @return the hint
     * @since 7505
     */
    public final String getHint() {
        return hint;
    }

    /**
     * Sets the hint to display when no text has been entered.
     * @param hint the hint to set
     * @since 7505
     */
    public final void setHint(String hint) {
        this.hint = hint;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (hint != null && !hint.isEmpty() && getText().isEmpty() && !isFocusOwner()) {
            // Taken from http://stackoverflow.com/a/24571681/2257172
            int h = getHeight();
            if (g instanceof Graphics2D) {
                ((Graphics2D) g).setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            Insets ins = getInsets();
            FontMetrics fm = g.getFontMetrics();
            int c0 = getBackground().getRGB();
            int c1 = getForeground().getRGB();
            int m = 0xfefefefe;
            int c2 = ((c0 & m) >>> 1) + ((c1 & m) >>> 1);
            g.setColor(new Color(c2, true));
            g.drawString(hint, ins.left, h / 2 + fm.getAscent() / 2 - 2);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(false);
        }
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(true);
        }
        repaint();
    }

    @Override
    public void destroy() {
        removeFocusListener(this);
        TextContextualPopupMenu.disableMenuFor(this, launcher);
    }
}
