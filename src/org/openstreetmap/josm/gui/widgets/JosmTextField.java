// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.RepaintManager;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Utils;

/**
 * Subclass of {@link JTextField} that:<ul>
 * <li>adds a "native" context menu (undo/redo/cut/copy/paste/select all)</li>
 * <li>adds an optional "hint" displayed when no text has been entered</li>
 * <li>disables the global advanced key press detector when focused</li>
 * <li>implements a workaround to <a href="https://bugs.openjdk.java.net/browse/JDK-6322854">JDK bug 6322854</a></li>
 * </ul><br>This class must be used everywhere in core and plugins instead of {@code JTextField}.
 * @since 5886
 */
public class JosmTextField extends JTextField implements Destroyable, ComponentListener, FocusListener, PropertyChangeListener {

    private final PopupMenuLauncher launcher;
    private String hint;
    private Icon icon;
    private Point iconPos;
    private Insets originalMargin;
    private OrientationAction orientationAction;

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

        // There seems to be a bug in Swing 8 that components with Bidi enabled are smaller than
        // without. (eg. 23px vs 21px in height, maybe a font thing).  Usually Bidi starts disabled
        // but gets enabled whenever RTL text is loaded.  To avoid trashing the layout we enable
        // Bidi by default.  See also {@link #drawHint()}.
        getDocument().putProperty("i18n", Boolean.TRUE);

        // the menu and hotkey to change text orientation
        orientationAction = new OrientationAction(this);
        orientationAction.addPropertyChangeListener(this);
        JPopupMenu menu = launcher.getMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(orientationAction));
        getInputMap().put(OrientationAction.getShortcutKey(), orientationAction);

        // Fix minimum size when columns are specified
        if (columns > 0) {
            setMinimumSize(getPreferredSize());
        }
        addFocusListener(this);
        addComponentListener(this);
        // Workaround for Java bug 6322854
        JosmPasswordField.workaroundJdkBug6322854(this);
        originalMargin = getMargin();
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
     * @return the old hint
     * @since 18221 (signature)
     */
    public String setHint(String hint) {
        String old = this.hint;
        this.hint = hint;
        return old;
    }

    /**
     * Return true if the textfield should display the hint text.
     *
     * @return whether to display the hint text
     * @since 18221
     */
    public boolean displayHint() {
        return !Utils.isEmpty(hint) && getText().isEmpty() && !isFocusOwner();
    }

    /**
     * Returns the icon to display
     * @return the icon to display
     * @since 17768
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Sets the icon to display
     * @param icon the icon to set
     * @since 17768
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
        if (icon == null) {
            setMargin(originalMargin);
        }
        positionIcon();
    }

    private void positionIcon() {
        if (icon != null) {
            Insets margin = (Insets) originalMargin.clone();
            int hGap = (getHeight() - icon.getIconHeight()) / 2;
            if (getComponentOrientation() == ComponentOrientation.RIGHT_TO_LEFT) {
                margin.right += icon.getIconWidth() + 2 * hGap;
                iconPos = new Point(getWidth() - icon.getIconWidth() - hGap, hGap);
            } else {
                margin.left += icon.getIconWidth() + 2 * hGap;
                iconPos = new Point(hGap, hGap);
            }
            setMargin(margin);
        }
    }

    @Override
    public void setComponentOrientation(ComponentOrientation o) {
        if (o.isLeftToRight() != getComponentOrientation().isLeftToRight()) {
            super.setComponentOrientation(o);
            positionIcon();
        }
    }

    /**
     * Empties the internal undo manager.
     * @since 14977
     */
    public final void discardAllUndoableEdits() {
        launcher.discardAllUndoableEdits();
    }

    /**
     * Returns the color for hint texts.
     * @return the Color for hint texts
     */
    public static Color getHintTextColor() {
        Color color = UIManager.getColor("TextField[Disabled].textForeground"); // Nimbus?
        if (color == null)
            color = UIManager.getColor("TextField.inactiveForeground");
        if (color == null)
            color = Color.GRAY;
        return color;
    }

    /**
     * Returns the font for hint texts.
     * @return the font for hint texts
     */
    public static Font getHintFont() {
        return UIManager.getFont("TextField.font");
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (icon != null) {
            icon.paintIcon(this, g, iconPos.x, iconPos.y);
        }
        if (displayHint()) {
            // Logging.debug("drawing textfield hint: {0}", getHint());
            drawHint(g);
        }
    }

    /**
     * Draws the hint text over the editor component.
     *
     * @param g the graphics context
     */
    public void drawHint(Graphics g) {
        int x;
        try {
            x = modelToView(0).x;
        } catch (BadLocationException exc) {
            return; // can't happen
        }
        // Taken from http://stackoverflow.com/a/24571681/2257172
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g.setColor(getHintTextColor());
        g.setFont(getHintFont());
        if (getComponentOrientation().isLeftToRight()) {
            g.drawString(getHint(), x, getBaseline(getWidth(), getHeight()));
        } else {
            FontMetrics metrics = g.getFontMetrics(g.getFont());
            int dx = metrics.stringWidth(getHint());
            g.drawString(getHint(), x - dx, getBaseline(getWidth(), getHeight()));
        }
        // Needed to avoid endless repaint loop if we accidentally draw over the insets.  This may
        // easily happen because a change in text orientation invalidates the textfield and
        // following that the preferred size gets smaller. (Bug in Swing?)
        RepaintManager.currentManager(this).markCompletelyClean(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(false);
        }
        if (e != null && e.getOppositeComponent() != null) {
            // Select all characters when the change of focus occurs inside JOSM only.
            // When switching from another application, it is annoying, see #13747
            selectAll();
        }
        positionIcon();
        repaint(); // get rid of hint
    }

    @Override
    public void focusLost(FocusEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.keyDetector.setEnabled(true);
        }
        repaint(); // paint hint
    }

    @Override
    public void destroy() {
        removeFocusListener(this);
        TextContextualPopupMenu.disableMenuFor(this, launcher);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        positionIcon();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // command from the menu / shortcut key
        if ("orientationAction".equals(evt.getPropertyName())) {
            setComponentOrientation((ComponentOrientation) evt.getNewValue());
        }
    }
}
