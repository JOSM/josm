// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JLabel;
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
public class JMultilineLabel extends JLabel {
    private int maxWidth = Integer.MAX_VALUE;
    private Rectangle oldbounds = null;
    private Dimension oldPreferred = null;

    /**
     * Constructs a normal label but adds HTML tags if not already done so.
     * Supports both newline characters (<code>\n</code>) as well as the HTML
     * <code>&lt;br&gt;</code> to insert new lines.
     *
     * Use setMaxWidth to limit the width of the label.
     * @param text The text to display
     */
    public JMultilineLabel(String text) {
        super();
        String html = text.trim().replaceAll("\n", "<br>");
        if (!html.startsWith("<html>")) {
            html = "<html>" + html + "</html>";
        }
        super.setText(html);
    }

    /**
     * Set the maximum width. Use this method instead of setMaximumSize because
     * this saves a little bit of overhead and is actually taken into account.
     *
     * @param width
     */
    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    /**
     * Tries to determine a suitable height for the given contents and return
     * that dimension.
     */
    @Override
    public Dimension getPreferredSize() {
        // Without this check it will result in an infinite loop calling
        // getPreferredSize. Remember the old bounds and only recalculate if
        // the size actually changed.
        if (this.getBounds().equals(oldbounds) && oldPreferred != null) {
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
