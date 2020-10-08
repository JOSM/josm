// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * A small user interface component that consists of an image label and
 * a fixed text content to the right of the image.
 * @since 5965
 */
public class ImageLabel extends JPanel {
    private final JLabel imgLabel = new JLabel();
    private final JLabel tf = new JLabel();
    private int charCount;

    /**
     * Constructs a new {@code ImageLabel}.
     * @param img Image name (without extension) to find in {@code statusline} directory
     * @param tooltip Tooltip text to display
     * @param charCount Character count used to compute min/preferred size
     * @param background The background color
     */
    public ImageLabel(String img, String tooltip, int charCount, Color background) {
        setLayout(new GridBagLayout());
        setBackground(background);
        add(imgLabel, GBC.std().anchor(GBC.WEST).insets(0, 1, 1, 0));
        setIcon(img);
        add(tf, GBC.std().fill(GBC.BOTH).anchor(GBC.WEST).insets(2, 1, 1, 0));
        setToolTipText(tooltip);
        setCharCount(charCount);
    }

    /**
     * Sets the text to display.
     * @param t text to display
     */
    public void setText(String t) {
        tf.setText(t);
    }

    /**
     * Sets the image to display.
     * @param img Image name (without extension) to find in {@code statusline} directory
     */
    public void setIcon(String img) {
        imgLabel.setIcon(ImageProvider.get("statusline/", img, ImageSizes.STATUSLINE));
    }

    /**
     * Sets the foreground color of the text.
     * @param fg text color
     */
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (tf != null) {
            tf.setForeground(fg);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getMinimumSize().height);
    }

    /**
     * Returns the preferred char count.
     * @return the preferred char count
     * @since 10191
     */
    public final int getCharCount() {
        return charCount;
    }

    /**
     * Sets the preferred char count.
     * @param charCount the preferred char count
     * @since 10191
     */
    public final void setCharCount(int charCount) {
        this.charCount = charCount;
    }
}
