// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;

/**
 * An animated object
 * @since 18929
 */
public interface IAnimObject {

    /** Paint the object
    * @param g the graphics object to paint to
    */
    void paint(Graphics g);

    /** Set the extend when window size changed
    * @param w window width
    * @param h window height
    */
    void setExtend(int w, int h);

    /** Animate the object - Cause next step of animation
    */
    void animate();
}
