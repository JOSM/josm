// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;

/**
 * An animated object
 * @since xxx
 */
public interface IAnimObject {

    public void paint(Graphics g);

    public void setExtend(int w, int h);

    public void animate();
}
