// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.image.BufferedImage;

import org.openstreetmap.josm.gui.layer.ImageProcessor;

/**
 * Adds or removes the colorfulness of the image.
 *
 * @author Michael Zangl
 * @since 10547
 */
public class ColorfulImageProcessor implements ImageProcessor {
    private ColorfulFilter op;
    private double colorfulness = 1;

    /**
     * Gets the colorfulness value.
     * @return The value
     */
    public double getColorfulness() {
        return colorfulness;
    }

    /**
     * Sets the colorfulness value. Clamps it to 0+
     * @param colorfulness The value
     */
    public void setColorfulness(double colorfulness) {
        if (colorfulness < 0) {
            this.colorfulness = 0;
        } else {
            this.colorfulness = colorfulness;
        }

        if (this.colorfulness < .95 || this.colorfulness > 1.05) {
            op = new ColorfulFilter(this.colorfulness);
        } else {
            op = null;
        }
    }

    @Override
    public BufferedImage process(BufferedImage image) {
        if (op != null) {
            return op.filter(image, null);
        } else {
            return image;
        }
    }

    @Override
    public String toString() {
        return "ColorfulImageProcessor [colorfulness=" + colorfulness + ']';
    }
}
