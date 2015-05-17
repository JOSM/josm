// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A small user interface component that consists of an image label and
 * a fixed text content to the right of the image.
 * @since 5965
 */
public class ImageLabel extends JPanel {
    private JLabel tf;
    private int charCount;

    /**
     * Constructs a new {@code ImageLabel}.
     * @param img Image name (without .png extension) to find in {@code statusline} directory
     * @param tooltip Tooltip text to display
     * @param charCount Character count used to compute min/preferred size
     * @param background The background color
     */
    public ImageLabel(String img, String tooltip, int charCount, Color background) {
        setLayout(new GridBagLayout());
        setBackground(background);
        add(new JLabel(ImageProvider.get("statusline/"+img+".png")), GBC.std().anchor(GBC.WEST).insets(0,1,1,0));
        add(tf = new JLabel(), GBC.std().fill(GBC.BOTH).anchor(GBC.WEST).insets(2,1,1,0));
        setToolTipText(tooltip);
        this.charCount = charCount;
    }

    /**
     * Sets the text to display.
     * @param t text to display
     */
    public void setText(String t) {
        tf.setText(t);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getMinimumSize().height);
    }
}
