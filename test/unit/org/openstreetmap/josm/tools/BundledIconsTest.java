// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;

import org.junit.jupiter.api.Test;

/**
 * Checks the icons bundled in {@code resources/images}.
 */
class BundledIconsTest {

    /**
     * Every bundled icon must load and render at least one visible pixel.
     * See <a href="https://josm.openstreetmap.de/ticket/24992">#24992</a>.
     * @throws IOException if the icon directory cannot be read
     */
    @Test
    void testIconsAreNotBlank() throws IOException {
        Path root = Paths.get("resources/images");
        List<Path> files;
        try (Stream<Path> stream = Files.walk(root)) {
            files = stream.filter(p -> p.toString().endsWith(".svg") || p.toString().endsWith(".png"))
                    .sorted().collect(Collectors.toList());
        }
        assertFalse(files.isEmpty(), "no icons found in " + root.toAbsolutePath());
        List<String> broken = new ArrayList<>();
        for (Path file : files) {
            String name = root.relativize(file).toString().replace('\\', '/');
            ImageIcon icon = new ImageProvider(name).setSize(new Dimension(32, 32)).setOptional(true).get();
            if (icon == null) {
                broken.add(name + " (not loaded)");
            } else if (isBlank((BufferedImage) icon.getImage())) {
                broken.add(name + " (blank)");
            }
        }
        assertTrue(broken.isEmpty(), "Broken icons: " + broken);
    }

    /**
     * Checks whether an image has no visible pixel, i.e. every pixel is fully transparent.
     * This is what an SVG renderer produces when it skips content it does not support.
     * @param image the rendered icon
     * @return {@code true} if the alpha channel of every pixel is zero
     */
    private static boolean isBlank(BufferedImage image) {
        for (int rgb : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())) {
            if (rgb >>> 24 != 0) {
                return false;
            }
        }
        return true;
    }
}
