// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.ImageProcessor;
import org.openstreetmap.josm.io.session.SessionAwareReadApply;
import org.openstreetmap.josm.tools.Utils;

/**
 * An image processor which adjusts the gamma value of an image.
 * @since 10547
 */
public class GammaImageProcessor implements ImageProcessor, SessionAwareReadApply {
    private double gamma = 1.0;
    final short[] gammaChange = new short[256];
    private final LookupOp op3 = new LookupOp(
            new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange}), null);
    private final LookupOp op4 = new LookupOp(
            new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange, gammaChange}), null);

    /**
     * Returns the currently set gamma value.
     * @return the currently set gamma value
     */
    public double getGamma() {
        return gamma;
    }

    /**
     * Sets a new gamma value, {@code 1} stands for no correction.
     * @param gamma new gamma value
     */
    public void setGamma(double gamma) {
        this.gamma = gamma;
        for (int i = 0; i < 256; i++) {
            gammaChange[i] = (short) (255 * Math.pow(i / 255., gamma));
        }
    }

    @Override
    public BufferedImage process(BufferedImage image) {
        if (gamma == 1.0) {
            return image;
        }
        try {
            final int bands = image.getRaster().getNumBands();
            if (image.getType() != BufferedImage.TYPE_CUSTOM && bands == 3) {
                return op3.filter(image, null);
            } else if (image.getType() != BufferedImage.TYPE_CUSTOM && bands == 4) {
                return op4.filter(image, null);
            }
        } catch (IllegalArgumentException ignore) {
            Main.trace(ignore);
        }
        final int type = image.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        final BufferedImage to = new BufferedImage(image.getWidth(), image.getHeight(), type);
        to.getGraphics().drawImage(image, 0, 0, null);
        return process(to);
    }

    @Override
    public void applyFromPropertiesMap(Map<String, String> properties) {
        String cStr = properties.get("gamma");
        if (cStr != null) {
            try {
                setGamma(Double.parseDouble(cStr));
            } catch (NumberFormatException e) {
                if (Main.isTraceEnabled()) {
                    Main.trace(e);
                }
            }
        }
    }

    @Override
    public Map<String, String> toPropertiesMap() {
        if (Utils.equalsEpsilon(gamma, 1.0))
            return Collections.emptyMap();
        else
            return Collections.singletonMap("gamma", Double.toString(gamma));
    }

    @Override
    public String toString() {
        return "GammaImageProcessor [gamma=" + gamma + ']';
    }
}
