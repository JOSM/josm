// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JPasswordField;
import javax.swing.TransferHandler;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Logging;

/**
 * A subclass of {@link JPasswordField} to implement a workaround to
 * <a href="https://bugs.openjdk.java.net/browse/JDK-6322854">JDK bug 6322854</a>.
 *
 * @see <a href="https://josm.openstreetmap.de/ticket/8404">https://josm.openstreetmap.de/ticket/8404</a>
 * @see <a href="https://hg.netbeans.org/main/rev/33cb2e81b640">https://hg.netbeans.org/main/rev/33cb2e81b640</a>
 * @since 5752
 */
public class JosmPasswordField extends JPasswordField implements FocusListener {

    /**
     * Constructs a new <code>JosmPasswordField</code>,
     * with a default document, <code>null</code> starting
     * text string, and 0 column width.
     */
    public JosmPasswordField() {
        workaroundJdkBug6322854(this);
        addFocusListener(this);
    }

    /**
     * Constructs a new <code>JosmPasswordField</code> that uses the
     * given text storage model and the given number of columns.
     * This is the constructor through which the other constructors feed.
     * The echo character is set to '*', but may be changed by the current
     * Look and Feel.  If the document model is
     * <code>null</code>, a default one will be created.
     *
     * @param doc  the text storage to use
     * @param txt the text to be displayed, <code>null</code> if none
     * @param columns  the number of columns to use to calculate
     *   the preferred width &gt;= 0; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JosmPasswordField(Document doc, String txt, int columns) {
        super(doc, txt, columns);
        workaroundJdkBug6322854(this);
        addFocusListener(this);
    }

    /**
     * Constructs a new empty <code>JosmPasswordField</code> with the specified
     * number of columns.  A default model is created, and the initial string
     * is set to <code>null</code>.
     *
     * @param columns the number of columns &gt;= 0
     */
    public JosmPasswordField(int columns) {
        super(columns);
        workaroundJdkBug6322854(this);
        addFocusListener(this);
    }

    /**
     * Constructs a new <code>JPasswordField</code> initialized with
     * the specified text and columns.  The document model is set to
     * the default.
     *
     * @param text the text to be displayed, <code>null</code> if none
     * @param columns the number of columns &gt;= 0
     */
    public JosmPasswordField(String text, int columns) {
        super(text, columns);
        workaroundJdkBug6322854(this);
        addFocusListener(this);
    }

    /**
     * Constructs a new <code>JosmPasswordField</code> initialized
     * with the specified text.  The document model is set to the
     * default, and the number of columns to 0.
     *
     * @param text the text to be displayed, <code>null</code> if none
     */
    public JosmPasswordField(String text) {
        super(text);
        workaroundJdkBug6322854(this);
        addFocusListener(this);
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

    /**
     * Implements a workaround to <a href="https://bugs.openjdk.java.net/browse/JDK-6322854">JDK bug 6322854</a>.
     * This method can be deleted after Oracle decides to fix this bug...
     * @param text The {@link JTextComponent} to protect.
     */
    public static final void workaroundJdkBug6322854(final JTextComponent text) {
        if (text != null) {
            text.getActionMap().put("paste", new Action() {

                private final Action pasteAction = TransferHandler.getPasteAction();

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        pasteAction.actionPerformed(e);
                    } catch (NullPointerException npe) { // NOPMD
                        Logging.log(Logging.LEVEL_ERROR, "NullPointerException occurred because of JDK bug 6322854. "
                                +"Copy/Paste operation has not been performed. Please complain to Oracle: "+
                                "https://bugs.openjdk.java.net/browse/JDK-6322854", npe);
                    }
                }

                @Override
                public void setEnabled(boolean b) {
                    pasteAction.setEnabled(b);
                }

                @Override
                public void removePropertyChangeListener(PropertyChangeListener listener) {
                    pasteAction.removePropertyChangeListener(listener);
                }

                @Override
                public void putValue(String key, Object value) {
                    pasteAction.putValue(key, value);
                }

                @Override
                public boolean isEnabled() {
                    return pasteAction.isEnabled();
                }

                @Override
                public Object getValue(String key) {
                    return pasteAction.getValue(key);
                }

                @Override
                public void addPropertyChangeListener(PropertyChangeListener listener) {
                    pasteAction.addPropertyChangeListener(listener);
                }
            });
        }
    }
}
