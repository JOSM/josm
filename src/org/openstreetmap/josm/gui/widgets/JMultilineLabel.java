// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JEditorPane;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

/**
 * Creates a normal label that will wrap its contents if there less width than
 * required to print it in one line. Additionally the maximum width of the text
 * can be set using <code>setMaxWidth</code>.
 *
 * Note that this won't work if JMultilineLabel is put into a JScrollBox or
 * similar as the bounds will never change. Instead scrollbars will be displayed.
 *
 * @since 6340
 */
public class JMultilineLabel extends JEditorPane {
    private int maxWidth = Integer.MAX_VALUE;
    private Rectangle oldbounds;
    private Dimension oldPreferred;

    /**
     * Constructs a normal label but adds HTML tags if not already done so.
     * Supports both newline characters (<code>\n</code>) as well as the HTML
     * <code>&lt;br&gt;</code> to insert new lines.
     *
     * Use setMaxWidth to limit the width of the label.
     * @param text The text to display
     */
    public JMultilineLabel(String text) {
        this(text, false);
    }

    /**
     * Constructs a normal label but adds HTML tags if not already done so.
     * Supports both newline characters (<code>\n</code>) as well as the HTML
     * <code>&lt;br&gt;</code> to insert new lines.
     *
     * Use setMaxWidth to limit the width of the label.
     * @param text The text to display
     * @param allBold If {@code true}, makes all text to be displayed in bold
     */
    public JMultilineLabel(String text, boolean allBold) {
        this(text, allBold, false);
    }

    /**
     * Constructs a normal label but adds HTML tags if not already done so.
     * Supports both newline characters (<code>\n</code>) as well as the HTML
     * <code>&lt;br&gt;</code> to insert new lines.
     *
     * Use setMaxWidth to limit the width of the label.
     * @param text The text to display
     * @param allBold If {@code true}, makes all text to be displayed in bold
     * @param focusable indicates whether this label is focusable
     * @since 13157
     */
    public JMultilineLabel(String text, boolean allBold, boolean focusable) {
        JosmEditorPane.makeJLabelLike(this, allBold);
        String html = text.trim().replaceAll("\n", "<br>");
        if (!html.startsWith("<html>")) {
            html = "<html>" + html + "</html>";
        }
        setFocusable(focusable);
        super.setText(html);
    }

    /**
     * Set the maximum width. Use this method instead of setMaximumSize because
     * this saves a little bit of overhead and is actually taken into account.
     *
     * @param width the maximum width
     */
    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    /**
     * Tries to determine a suitable height for the given contents and return that dimension.
     */
    @Override
    public Dimension getPreferredSize() {
        // Without this check it will result in an infinite loop calling getPreferredSize.
        // Remember the old bounds and only recalculate if the size actually changed.
        if (oldPreferred != null && this.getBounds().equals(oldbounds)) {
            return oldPreferred;
        }
        oldbounds = this.getBounds();

        Dimension superPreferred = super.getPreferredSize();
        // Make it not larger than required
        int width = Math.min(superPreferred.width, maxWidth);

        // Calculate suitable width and height
        final View v = (View) super.getClientProperty(BasicHTML.propertyKey);

        if (v == null) {
            return superPreferred;
        }

        v.setSize(width, 0);
        int w = (int) Math.ceil(v.getPreferredSpan(View.X_AXIS));
        int h = (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS));

        oldPreferred = new Dimension(w, h);
        return oldPreferred;
    }
}
