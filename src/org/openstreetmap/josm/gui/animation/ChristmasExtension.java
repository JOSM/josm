// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * Christmas animation extension. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public class ChristmasExtension implements AnimationExtension {

    private final List<Star> stars = new ArrayList<>(50);

    @Override
    public void paint(Graphics g) {
        stars.forEach(s -> s.paint(g));
    }

    @Override
    public void animate() {
        stars.forEach(Star::animate);
    }

    @Override
    public final void adjustForSize(int w, int h) {
        int count = w / (2 * (Star.averageStarWidth + 1));
        while (stars.size() > count) {
            stars.remove(stars.size() - 1);
        }
        while (stars.size() < count) {
            stars.add(new Star(w, h));
        }
    }
}
