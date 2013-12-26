// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * @author Tim Haussmann
 */
public class SizeButton extends JComponent {

    private int x = 0;
    private int y = 0;

    private ImageIcon enlargeImage;
    private ImageIcon shrinkImage;
    private boolean isEnlarged = false;
    private final SlippyMapBBoxChooser slippyMapBBoxChooser;

    public SizeButton(SlippyMapBBoxChooser slippyMapBBoxChooser){
        super();
        this.slippyMapBBoxChooser = slippyMapBBoxChooser;
        enlargeImage = ImageProvider.get("view-fullscreen.png");
        shrinkImage = ImageProvider.get("view-fullscreen-revert.png");
        setPreferredSize(new Dimension(enlargeImage.getIconWidth(), enlargeImage.getIconHeight()));
        addMouseListener(mouseListener);
    }

    private final MouseAdapter mouseListener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                toggle();
                slippyMapBBoxChooser.resizeSlippyMap();
            }
        }
    };


    @Override
    protected void paintComponent(Graphics g) {
        if(isEnlarged) {
            if(shrinkImage != null)
                g.drawImage(shrinkImage.getImage(),x,y, null);
        } else {
            if(enlargeImage != null)
                g.drawImage(enlargeImage.getImage(),x,y, null);
        }
    }

    public void toggle() {
        isEnlarged = !isEnlarged;
    }

    public boolean isEnlarged() {
        return isEnlarged;
    }

    public boolean hit(Point point) {
        if(x < point.x && point.x < x + enlargeImage.getIconWidth()) {
            if(y < point.y && point.y < y + enlargeImage.getIconHeight()) {
                return true;
            }
        }
        return false;
    }

}
