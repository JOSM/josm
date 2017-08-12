// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.gui.layer.ImageProcessor;
import org.openstreetmap.josm.io.session.SessionAwareReadApply;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sharpens or blurs the image, depending on the sharpen value.
 * <p>
 * A positive sharpen level means that we sharpen the image.
 * <p>
 * A negative sharpen level let's us blur the image. -1 is the most useful value there.
 *
 * @author Michael Zangl
 * @since 10547
 */
public class SharpenImageProcessor implements ImageProcessor, SessionAwareReadApply {
    private float sharpenLevel = 1.0f;
    private ConvolveOp op;

    private static final float[] KERNEL_IDENTITY = new float[] {
        0, 0, 0,
        0, 1, 0,
        0, 0, 0
    };

    private static final float[] KERNEL_BLUR = new float[] {
        1f / 16, 2f / 16, 1f / 16,
        2f / 16, 4f / 16, 2f / 16,
        1f / 16, 2f / 16, 1f / 16
    };

    private static final float[] KERNEL_SHARPEN = new float[] {
        -.5f, -1f, -.5f,
         -1f, 7, -1f,
        -.5f, -1f, -.5f
    };

    /**
     * Gets the current sharpen level.
     * @return The level.
     */
    public float getSharpenLevel() {
        return sharpenLevel;
    }

    /**
     * Sets the sharpening level.
     * @param sharpenLevel The level. Clamped to be positive or 0.
     */
    public void setSharpenLevel(float sharpenLevel) {
        if (sharpenLevel < 0) {
            this.sharpenLevel = 0;
        } else {
            this.sharpenLevel = sharpenLevel;
        }

        if (this.sharpenLevel < 0.95) {
            op = generateMixed(this.sharpenLevel, KERNEL_IDENTITY, KERNEL_BLUR);
        } else if (this.sharpenLevel > 1.05) {
            op = generateMixed(this.sharpenLevel - 1, KERNEL_SHARPEN, KERNEL_IDENTITY);
        } else {
            op = null;
        }
    }

    private static ConvolveOp generateMixed(float aFactor, float[] a, float[] b) {
        if (a.length != 9 || b.length != 9) {
            throw new IllegalArgumentException("Illegal kernel array length.");
        }
        float[] values = new float[9];
        for (int i = 0; i < values.length; i++) {
            values[i] = aFactor * a[i] + (1 - aFactor) * b[i];
        }
        return new ConvolveOp(new Kernel(3, 3, values), ConvolveOp.EDGE_NO_OP, null);
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
        String vStr = properties.get("sharpenlevel");
        if (vStr != null) {
            try {
                setSharpenLevel(Float.parseFloat(vStr));
            } catch (NumberFormatException e) {
                // nothing
            }
        }
    }

    @Override
    public Map<String, String> toPropertiesMap() {
        if (Utils.equalsEpsilon(sharpenLevel, 1.0))
            return Collections.emptyMap();
        else
            return Collections.singletonMap("sharpenlevel", Float.toString(sharpenLevel));
    }

    @Override
    public String toString() {
        return "SharpenImageProcessor [sharpenLevel=" + sharpenLevel + ']';
    }
}
