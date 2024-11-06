// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;

/**
 * Graphical animation extension. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public interface AnimationExtension {

    /**
     * Adjusts for size.
     * @param w width
     * @param h height
     * @param x x origin of view area
     * @param y y origin of view area
     */
    void adjustForSize(int w, int h, int x, int y);

    /**
     * Paints static contents.
     * @param g graphics object
     */
    void paint(Graphics g);

    /**
     * Performs the optional animation.
     */
    void animate();
}
