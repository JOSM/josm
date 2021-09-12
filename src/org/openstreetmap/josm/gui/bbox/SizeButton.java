// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Button allowing to control the dimension of a slippy map between two states (normal/enlarged).
 * @author Tim Haussmann
 * @since 1390
 */
public class SizeButton extends JComponent implements Accessible {

    private final ImageIcon enlargeImage;
    private final ImageIcon shrinkImage;
    private boolean isEnlarged;
    private final SlippyMapBBoxChooser slippyMapBBoxChooser;

    private final transient MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                toggle();
            }
        }
    };

    /**
     * Constructs a new {@code SizeButton}.
     * @param slippyMapBBoxChooser the associated slippy map
     */
    public SizeButton(SlippyMapBBoxChooser slippyMapBBoxChooser) {
        this.slippyMapBBoxChooser = slippyMapBBoxChooser;
        enlargeImage = ImageProvider.get("view-fullscreen");
        shrinkImage = ImageProvider.get("view-fullscreen-revert");
        setPreferredSize(new Dimension(enlargeImage.getIconWidth(), enlargeImage.getIconHeight()));
        addMouseListener(mouseAdapter);
        setToolTipText(tr("Enlarge"));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isEnlarged) {
            if (shrinkImage != null)
                g.drawImage(shrinkImage.getImage(), 0, 0, null);
        } else {
            if (enlargeImage != null)
                g.drawImage(enlargeImage.getImage(), 0, 0, null);
        }
    }

    /**
     * Toggles button state.
     */
    public void toggle() {
        isEnlarged = !isEnlarged;
        setToolTipText(isEnlarged ? tr("Shrink") : tr("Enlarge"));
        slippyMapBBoxChooser.resizeSlippyMap();
    }

    /**
     * Determines if the slippy map is enlarged.
     * @return {@code true} if the slippy map is enlarged, {@code false} otherwise
     */
    public boolean isEnlarged() {
        return isEnlarged;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleSizeButton();
        }
        return accessibleContext;
    }

    class AccessibleSizeButton extends AccessibleJComponent implements AccessibleAction {

        @Override
        public int getAccessibleActionCount() {
            return 1;
        }

        @Override
        public String getAccessibleActionDescription(int i) {
            return "toggle";
        }

        @Override
        public boolean doAccessibleAction(int i) {
            toggle();
            return true;
        }
    }
}
