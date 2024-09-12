// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * Test helper class for images
 */
public final class ImageTestUtils {
    // development flag - set to true in order to update all reference images
    private static final boolean UPDATE_ALL = false;

    private ImageTestUtils() {
        /* Hide constructor */
    }

    /**
     * Compares the reference image file with the actual images given as {@link BufferedImage}.
     * @param testIdentifier a test identifier for error messages
     * @param referenceImageFile the reference image file to be read using {@link ImageIO#read(File)}
     * @param image the actual image
     * @param thresholdPixels maximum number of differing pixels
     * @param thresholdTotalColorDiff maximum sum of color value differences
     * @param diffImageConsumer a consumer for a rendered image highlighting the differing pixels, may be null
     * @throws IOException in case of I/O error
     */
    public static void assertImageEquals(
            String testIdentifier, File referenceImageFile, BufferedImage image,
            int thresholdPixels, int thresholdTotalColorDiff, Consumer<BufferedImage> diffImageConsumer) throws IOException {

        if (UPDATE_ALL) {
            ImageIO.write(image, "png", referenceImageFile);
            return;
        }
        final BufferedImage reference = ImageIO.read(referenceImageFile);
        assertImageEquals(testIdentifier, reference, image, thresholdPixels, thresholdTotalColorDiff, diffImageConsumer);
    }

    /**
     * Compares the reference image file with the actual images given as {@link BufferedImage}.
     * @param testIdentifier a test identifier for error messages
     * @param reference the reference image
     * @param image the actual image
     * @param thresholdPixels maximum number of differing pixels
     * @param thresholdTotalColorDiff maximum sum of color value differences
     * @param diffImageConsumer a consumer for a rendered image highlighting the differing pixels, may be null
     */
    public static void assertImageEquals(String testIdentifier, BufferedImage reference, BufferedImage image,
                                         int thresholdPixels, int thresholdTotalColorDiff, Consumer<BufferedImage> diffImageConsumer) {
        assertEquals(reference.getWidth(), image.getWidth());
        assertEquals(reference.getHeight(), image.getHeight());

        StringBuilder differences = new StringBuilder();
        ArrayList<Point> differencePoints = new ArrayList<>();
        int colorDiffSum = 0;

        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                int expected = reference.getRGB(x, y);
                int result = image.getRGB(x, y);
                int expectedAlpha = expected >> 24;
                boolean colorsAreSame = expectedAlpha == 0 ? result >> 24 == 0 : expected == result;
                if (!colorsAreSame) {
                    Color expectedColor = new Color(expected, true);
                    Color resultColor = new Color(result, true);
                    int colorDiff = Math.abs(expectedColor.getRed() - resultColor.getRed())
                            + Math.abs(expectedColor.getGreen() - resultColor.getGreen())
                            + Math.abs(expectedColor.getBlue() - resultColor.getBlue());
                    int alphaDiff = Math.abs(expectedColor.getAlpha() - resultColor.getAlpha());
                    // Ignore small alpha differences due to Java versions, rendering libraries and so on
                    if (alphaDiff <= 20) {
                        alphaDiff = 0;
                    }
                    // Ignore small color differences for the same reasons, but also completely for almost-transparent pixels
                    if (colorDiff <= 15 || resultColor.getAlpha() <= 20) {
                        colorDiff = 0;
                    }
                    if (colorDiff + alphaDiff > 0) {
                        differencePoints.add(new Point(x, y));
                        if (differences.length() < 2000) {
                            differences.append("\nDifference at ")
                                    .append(x)
                                    .append(",")
                                    .append(y)
                                    .append(": Expected ")
                                    .append(ColorHelper.color2html(expectedColor))
                                    .append(" but got ")
                                    .append(ColorHelper.color2html(resultColor))
                                    .append(" (color diff is ")
                                    .append(colorDiff)
                                    .append(", alpha diff is ")
                                    .append(alphaDiff)
                                    .append(")");
                        }
                    }
                    colorDiffSum += colorDiff + alphaDiff;
                }
            }
        }

        if (differencePoints.size() > thresholdPixels || colorDiffSum > thresholdTotalColorDiff) {
            // Add a nice image that highlights the differences:
            BufferedImage diffImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (Point p : differencePoints) {
                diffImage.setRGB(p.x, p.y, 0xffff0000);
            }
            if (diffImageConsumer != null) {
                diffImageConsumer.accept(diffImage);
            }

            if (differencePoints.size() > thresholdPixels) {
                fail(MessageFormat.format("Images for test {0} differ at {1} points, threshold is {2}: {3}",
                        testIdentifier, differencePoints.size(), thresholdPixels, differences.toString()));
            } else {
                fail(MessageFormat.format("Images for test {0} differ too much in color, value is {1}, permitted threshold is {2}: {3}",
                        testIdentifier, colorDiffSum, thresholdTotalColorDiff, differences.toString()));
            }
        }
    }

    /**
     * Write debug images to a directory
     * @param directory The directory to put the debug images in
     * @param filePrefix The file prefix for the images (e.g. test name)
     * @param diff The difference between the expected image and the actual image
     * @param oldImage The expected image
     * @param newImage The actual image
     */
    public static void writeDebugImages(@Nonnull Path directory, @Nonnull String filePrefix, @Nonnull BufferedImage diff,
                                        @Nullable BufferedImage oldImage, @Nullable BufferedImage newImage) {
        if (!UPDATE_ALL) {
            return;
        }
        try {
            if (!Files.isDirectory(directory)) {
                Files.createDirectories(directory);
            }
            final String basename = directory.resolve(filePrefix).toString();
            ImageIO.write(diff, "png", new File(basename + "-diff.png"));
            if (newImage != null) ImageIO.write(newImage, "png", new File(basename + "-new.png"));
            if (oldImage != null) ImageIO.write(oldImage, "png", new File(basename + "-old.png"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
