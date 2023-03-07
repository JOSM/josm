// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JPanel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay.VisRect;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Unit tests of {@link ImageDisplay} class.
 */
@BasicPreferences
class ImageDisplayTest {
    /**
     * Unit test of {@link ImageDisplay#calculateDrawImageRectangle}.
     */
    @Test
    void testCalculateDrawImageRectangle() {
        assertEquals(new Rectangle(),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(), new Dimension()));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 20, 10), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 20, 10),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(20, 10)));
        assertEquals(new Rectangle(5, 0, 24, 12),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(35, 12)));
        assertEquals(new Rectangle(0, 1, 8, 4),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(8, 6)));
    }

    /**
     * Performance test for {@link ImageDisplay.LoadImageRunnable}
     * @throws Exception if any error occurs
     */
    @Test
    @Disabled("Set working directory to image folder and run manually")
    void testLoadImageRunnablePerformance() throws Exception {
        ImageDisplay imageDisplay = new ImageDisplay(new ImageryFilterSettings());
        imageDisplay.setSize(640, 480);
        Graphics2D graphics = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB).createGraphics();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(".").toAbsolutePath(), "*.{JPG,jpg}")) {
            for (Path p : stream) {
                Runnable loadImage = imageDisplay.setImage0(new ImageEntry(p.toFile()));
                loadImage.run();
                imageDisplay.paintComponent(graphics);
            }
        }
    }

    /**
     * Non-regression test for #22770 which occurs when a high resolution trackpad does not have a full mouse wheel event
     */
    @Test
    void testNonRegression22770() {
        final ImageDisplay imageDisplay = new ImageDisplay(new ImageryFilterSettings());
        final Field visRectField = assertDoesNotThrow(() -> ImageDisplay.class.getDeclaredField("visibleRect"));
        final Field wheelListenerField = assertDoesNotThrow(() -> ImageDisplay.class.getDeclaredField("imgMouseListener"));
        ReflectionUtils.setObjectsAccessible(wheelListenerField, visRectField);
        final Supplier<VisRect> visRectSupplier = () -> new VisRect((VisRect) assertDoesNotThrow(() -> visRectField.get(imageDisplay)));
        final MouseWheelListener listener = (MouseWheelListener) assertDoesNotThrow(() -> wheelListenerField.get(imageDisplay));
        final IntFunction<MouseWheelEvent> mouseEventProvider = wheelRotation -> new MouseWheelEvent(new JPanel(), 0, 0, 0,
                320, 240, 320, 240, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, wheelRotation, wheelRotation, wheelRotation);
        imageDisplay.setSize(640, 480);
        imageDisplay.setImage0(new TestImageEntry(640, 480)).run();
        final VisRect initialVisibleRect = visRectSupplier.get();
        assertEquals(640, initialVisibleRect.width);
        assertEquals(480, initialVisibleRect.height);
        // First, check that zoom in works
        listener.mouseWheelMoved(mouseEventProvider.apply(-1));
        assertNotEquals(initialVisibleRect, imageDisplay.getVisibleRect());
        final VisRect zoomedInVisRect = visRectSupplier.get();
        // If this fails, check to make certain that geoimage.zoom-step-factor defaults haven't changed
        assertAll(() -> assertEquals(426, zoomedInVisRect.width),
                () -> assertEquals(320, zoomedInVisRect.height));
        // Now check that a zoom event with no wheel rotation does not cause movement
        listener.mouseWheelMoved(mouseEventProvider.apply(0));
        final VisRect noZoomVisRect = visRectSupplier.get();
        assertAll(() -> assertEquals(426, noZoomVisRect.width),
                () -> assertEquals(320, noZoomVisRect.height));

        // Finally zoom out
        listener.mouseWheelMoved(mouseEventProvider.apply(1));
        final VisRect zoomOutVisRect = visRectSupplier.get();
        assertAll(() -> assertEquals(640, zoomOutVisRect.width),
                () -> assertEquals(480, zoomOutVisRect.height));
    }

    private static class TestImageEntry extends ImageEntry {
        private final int width;
        private final int height;
        TestImageEntry(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public BufferedImage read(Dimension target) throws IOException {
            return new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        }
    }
}
