// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Dimension;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageResizeMode} class.
 */
public class ImageResizeModeTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    final Dimension image = new Dimension(64, 48);
    final Dimension smallImage = new Dimension(14, 10);

    /**
     * Test method for {@link ImageResizeMode#computeDimension} using {@link ImageResizeMode#AUTO}
     */
    @Test
    public void testComputeDimensionAuto() {
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.AUTO.computeDimension(new Dimension(0, 0), image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.AUTO.computeDimension(ImageResource.DEFAULT_DIMENSION, image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.AUTO.computeDimension(new Dimension(-1, -1), image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.AUTO.computeDimension(new Dimension(64, 48), image));
        assertEquals(new Dimension(32, 24), ImageResizeMode.AUTO.computeDimension(new Dimension(32, -1), image));
        assertEquals(new Dimension(21, 16), ImageResizeMode.AUTO.computeDimension(new Dimension(-1, 16), image));
        assertEquals(new Dimension(24, 32), ImageResizeMode.AUTO.computeDimension(new Dimension(24, 32), image));
    }

    /**
     * Test method for {@link ImageResizeMode#computeDimension} using {@link ImageResizeMode#BOUNDED}
     */
    @Test
    public void testComputeDimensionBounded() {
        assertEquals(new Dimension(64, 48), ImageResizeMode.BOUNDED.computeDimension(ImageResource.DEFAULT_DIMENSION, image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.BOUNDED.computeDimension(new Dimension(-1, -1), image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.BOUNDED.computeDimension(new Dimension(64, 48), image));
        assertEquals(new Dimension(32, 24), ImageResizeMode.BOUNDED.computeDimension(new Dimension(32, -1), image));
        assertEquals(new Dimension(21, 16), ImageResizeMode.BOUNDED.computeDimension(new Dimension(-1, 16), image));
        assertEquals(new Dimension(24, 18), ImageResizeMode.BOUNDED.computeDimension(new Dimension(24, 32), image));
        assertEquals(new Dimension(14, 10), ImageResizeMode.BOUNDED.computeDimension(new Dimension(64, 48), smallImage));
        assertEquals(new Dimension(14, 10), ImageResizeMode.BOUNDED.computeDimension(new Dimension(16, 16), smallImage));
        assertEquals(new Dimension(11, 8), ImageResizeMode.BOUNDED.computeDimension(new Dimension(16, 8), smallImage));
        assertEquals(new Dimension(12, 8), ImageResizeMode.BOUNDED.computeDimension(new Dimension(12, 12), smallImage));
        assertEquals(new Dimension(11, 8), ImageResizeMode.BOUNDED.computeDimension(new Dimension(12, 8), smallImage));
        assertEquals(new Dimension(8, 5), ImageResizeMode.BOUNDED.computeDimension(new Dimension(8, 16), smallImage));
        assertEquals(new Dimension(8, 5), ImageResizeMode.BOUNDED.computeDimension(new Dimension(8, 8), smallImage));
        assertEquals(new Dimension(8, 5), ImageResizeMode.BOUNDED.computeDimension(new Dimension(8, 8), smallImage));
    }

    /**
     * Test method for {@link ImageResizeMode#computeDimension} using {@link ImageResizeMode#PADDED}
     */
    @Test
    public void testComputeDimensionPadded() {
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.PADDED.computeDimension(new Dimension(0, 0), image));
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.PADDED.computeDimension(ImageResource.DEFAULT_DIMENSION, image));
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.PADDED.computeDimension(new Dimension(-1, -1), image));
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.PADDED.computeDimension(new Dimension(32, -1), image));
        assertThrows(IllegalArgumentException.class, () -> ImageResizeMode.PADDED.computeDimension(new Dimension(-1, 16), image));
        assertEquals(new Dimension(64, 48), ImageResizeMode.PADDED.computeDimension(new Dimension(64, 48), image));
        assertEquals(new Dimension(24, 32), ImageResizeMode.PADDED.computeDimension(new Dimension(24, 32), image));
    }

    /**
     * Test method for {@link ImageResizeMode#cacheKey}
     */
    @Test
    public void testCacheKey() {
        assertEquals(0x00180018, ImageResizeMode.AUTO.cacheKey(ImageProvider.ImageSizes.LARGEICON.getImageDimension()));
        assertEquals(0x10180018, ImageResizeMode.BOUNDED.cacheKey(ImageProvider.ImageSizes.LARGEICON.getImageDimension()));
        assertEquals(0x30180018, ImageResizeMode.PADDED.cacheKey(ImageProvider.ImageSizes.LARGEICON.getImageDimension()));
        assertEquals(0x31000100, ImageResizeMode.PADDED.cacheKey(ImageProvider.ImageSizes.ABOUT_LOGO.getImageDimension()));
        assertEquals(0x0fff0fff, ImageResizeMode.AUTO.cacheKey(ImageResource.DEFAULT_DIMENSION));
        assertEquals(0x3fff0fff, ImageResizeMode.PADDED.cacheKey(ImageResource.DEFAULT_DIMENSION));
    }
}
