// License: GPL. See LICENSE file for details.
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
 * Moved from @link org.openstreetmap.josm.gui.MapStatus @since 5965
 */
public class ImageLabel extends JPanel {
    public static final Color backColor = Color.decode("#b8cfe5");
    public static final Color backColorActive = Color.decode("#aaff5e");

    private JLabel tf;
    private int charCount;
    
    public ImageLabel(String img, String tooltip, int charCount) {
        super();
        setLayout(new GridBagLayout());
        setBackground(backColor);
        add(new JLabel(ImageProvider.get("statusline/"+img+".png")), GBC.std().anchor(GBC.WEST).insets(0,1,1,0));
        add(tf = new JLabel(), GBC.std().fill(GBC.BOTH).anchor(GBC.WEST).insets(2,1,1,0));
        setToolTipText(tooltip);
        this.charCount = charCount;
    }
    
    public void setText(String t) {
        tf.setText(t);
    }
    @Override public Dimension getPreferredSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getPreferredSize().height);
    }
    @Override public Dimension getMinimumSize() {
        return new Dimension(25 + charCount*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getMinimumSize().height);
    }
}