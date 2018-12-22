// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;

/**
 * Animation no-op extension. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public class NoExtension implements AnimationExtension {

    NoExtension() {
    }

    @Override
    public void adjustForSize(int w, int h) {
        // No-op
    }

    @Override
    public void animate() {
        // No-op
    }

    @Override
    public void paint(Graphics g) {
        // No-op
    }
}
