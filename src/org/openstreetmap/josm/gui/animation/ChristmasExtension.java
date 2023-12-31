// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Christmas animation extension. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public class ChristmasExtension implements AnimationExtension {

    private final List<IAnimObject> objs = new ArrayList<>(50);

    @Override
    public void paint(Graphics g) {
        objs.forEach(o -> o.paint(g));
    }

    @Override
    public void animate() {
        objs.forEach(IAnimObject::animate);
    }

    @Override
    public final void adjustForSize(int w, int h) {
        Random seed = new Random();
        int count = w / (2 * (Star.averageStarWidth + 1));
        while (objs.size() > count) {
            objs.remove(objs.size() - 1);
        }
        objs.forEach(o -> o.setExtend(w, h));
        while (objs.size() < count) {
            objs.add(seed.nextInt(5) > 0 ? new Star(w, h) : new DropImage(w, h));
        }
    }
}
