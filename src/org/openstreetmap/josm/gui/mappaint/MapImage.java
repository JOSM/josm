// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;
import java.awt.Image;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

public class MapImage<I extends Image> {
    /**
     * ImageIcon can chage while the image is loading.
     */
    public I img;

    public int alpha = 255;

    /**
     * The 4 following fields are only used to check for equality.
     */
    public String name;
    public StyleSource source;
    public int width = -1;
    public int height = -1;

    private Image disabledImg;

    public MapImage(String name, StyleSource source) {
        this.name = name;
        this.source = source;
    }

    public Image getDisabled() {
        if (disabledImg != null)
            return disabledImg;
        return disabledImg = GrayFilter.createDisabledImage(img);
    }

    // img changes when image is fully loaded and can't be used for equality check.
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final MapImage other = (MapImage) obj;
        return  alpha == other.alpha &&
                equal(name, other.name) &&
                equal(source, other.source) &&
                width == other.width &&
                height == other.height;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + alpha;
        hash = 67 * hash + name.hashCode();
        hash = 67 * hash + source.hashCode();
        hash = 67 * hash + width;
        hash = 67 * hash + height;
        return hash;
    }

}
