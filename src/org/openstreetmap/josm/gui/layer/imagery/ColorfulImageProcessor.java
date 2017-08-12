// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.gui.layer.ImageProcessor;
import org.openstreetmap.josm.io.session.SessionAwareReadApply;
import org.openstreetmap.josm.tools.Utils;

/**
 * Adds or removes the colorfulness of the image.
 *
 * @author Michael Zangl
 * @since 10547
 */
public class ColorfulImageProcessor implements ImageProcessor, SessionAwareReadApply {
    private ColorfulFilter op;
    private double colorfulness = 1.0;

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
    public void applyFromPropertiesMap(Map<String, String> properties) {
        String cStr = properties.get("colorfulness");
        if (cStr != null) {
            try {
                setColorfulness(Double.parseDouble(cStr));
            } catch (NumberFormatException e) {
                // nothing
            }
        }
    }

    @Override
    public Map<String, String> toPropertiesMap() {
        if (Utils.equalsEpsilon(colorfulness, 1.0))
            return Collections.emptyMap();
        else
            return Collections.singletonMap("colorfulness", Double.toString(colorfulness));
    }

    @Override
    public String toString() {
        return "ColorfulImageProcessor [colorfulness=" + colorfulness + ']';
    }
}
