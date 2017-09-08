// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.image.BufferedImage;

/**
 * Processor that modifies images (sharpen, brightness, etc.).
 * This interface is used by imagery layers to filter the
 * displayed images (implemented in plugins).
 *
 * @author Nipel-Crumple
 * @since  8625 (creation)
 * @since 10600 (functional interface)
 * @since 12782 (moved from {@code gui.layer} package)
 */
@FunctionalInterface
public interface ImageProcessor {

    /**
     * This method should process given image according to image processors
     * which is contained in the layer
     *
     * @param image that should be processed
     *
     * @return processed image
     */
    BufferedImage process(BufferedImage image);
}
