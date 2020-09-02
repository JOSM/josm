// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.openstreetmap.josm.gui.mappaint.MapCSSRendererTest.assertImageEquals;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import com.kitfox.svg.SVGConst;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageProvider} class.
 */
public class ImageProviderTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static final class LogHandler14319 extends Handler {
        boolean failed;

        @Override
        public void publish(LogRecord record) {
            if ("Could not load image: https://host-in-the-trusted-network.com/test.jpg".equals(record.getMessage())) {
                failed = true;
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @BeforeEach
    public void resetPixelDensity() {
        GuiSizesHelper.setPixelDensity(1.0f);
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9984">#9984</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket9984() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(9984, "tile.png"));
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, true, true).getTransparency());
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, false, true).getTransparency());
        long expectedTransparency = Utils.getJavaVersion() < 11 ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
        assertEquals(expectedTransparency, ImageProvider.read(file, false, false).getTransparency());
        assertEquals(expectedTransparency, ImageProvider.read(file, true, false).getTransparency());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10030">#10030</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket10030() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(10030, "tile.jpg"));
        BufferedImage img = ImageProvider.read(file, true, true);
        assertNotNull(img);
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14319">#14319</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    @SuppressFBWarnings(value = "LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE")
    public void testTicket14319() throws IOException {
        LogHandler14319 handler = new LogHandler14319();
        Logger.getLogger(SVGConst.SVG_LOGGER).addHandler(handler);
        ImageIcon img = new ImageProvider(
                new File(TestUtils.getRegressionDataDir(14319)).getAbsolutePath(), "attack.svg").get();
        assertNotNull(img);
        assertFalse(handler.failed);
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/19551">#19551</a>
     * @throws SAXException If the type cannot be set (shouldn't throw)
     */
    @Test
    public void testTicket19551() throws SAXException {
        TaggingPreset badPreset = new TaggingPreset();
        badPreset.setType("node,way,relation,closedway");
        Key key = new Key();
        key.key = "amenity";
        key.value = "fuel";
        badPreset.data.add(key);
        TaggingPreset goodPreset = new TaggingPreset();
        goodPreset.setType("node,way,relation,closedway");
        goodPreset.data.add(key);
        goodPreset.iconName = "stop";
        TaggingPresets.addTaggingPresets(Arrays.asList(goodPreset, badPreset));
        Node node = new Node(LatLon.ZERO);
        node.put("amenity", "fuel");
        assertDoesNotThrow(() -> OsmPrimitiveImageProvider.getResource(node, Collections.emptyList()));
    }

    /**
     * Test fetching an image using {@code data:} URL.
     */
    @Test
    public void testDataUrl() {
        // Red dot image, taken from https://en.wikipedia.org/wiki/Data_URI_scheme#HTML
        assertNotNull(ImageProvider.get("data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"+
                "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="));
    }

    /**
     * Unit test of {@link ImageResource#getImageIcon(java.awt.Dimension)}
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testImageIcon() throws IOException {
        ImageResource resource = new ImageProvider("presets/misc/housenumber_small").getResource();
        testImage(12, 9, "housenumber_small-AUTO-null", resource.getImageIcon());
        testImage(12, 9, "housenumber_small-AUTO-default", resource.getImageIcon(ImageResource.DEFAULT_DIMENSION));
        testImage(8, 8, "housenumber_small-AUTO-08x08", resource.getImageIcon(new Dimension(8, 8)));
        testImage(16, 16, "housenumber_small-AUTO-16x16", resource.getImageIcon(new Dimension(16, 16)));
        testImage(24, 24, "housenumber_small-AUTO-24x24", resource.getImageIcon(new Dimension(24, 24)));
        testImage(36, 27, "housenumber_small-AUTO-36x27", resource.getImageIcon(new Dimension(36, 27)));
    }

    /**
     * Unit test of {@link ImageResource#getImageIconBounded(java.awt.Dimension)}
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testImageIconBounded() throws IOException {
        ImageResource resource = new ImageProvider("presets/misc/housenumber_small").getResource();
        testImage(8, 6, "housenumber_small-BOUNDED-08x08", resource.getImageIconBounded(new Dimension(8, 8)));
        testImage(16, 12, "housenumber_small-BOUNDED-16x16", resource.getImageIconBounded(new Dimension(16, 16)));
        testImage(24, 18, "housenumber_small-BOUNDED-24x24", resource.getImageIconBounded(new Dimension(24, 24)));
    }

    /**
     * Unit test of {@link ImageResource#getPaddedIcon(java.awt.Dimension)}
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testImageIconPadded() throws IOException {
        ImageResource resource = new ImageProvider("presets/misc/housenumber_small").getResource();
        testImage(8, 8, "housenumber_small-PADDED-08x08", resource.getPaddedIcon(new Dimension(8, 8)));
        testImage(16, 16, "housenumber_small-PADDED-16x16", resource.getPaddedIcon(new Dimension(16, 16)));
        testImage(24, 24, "housenumber_small-PADDED-24x24", resource.getPaddedIcon(new Dimension(24, 24)));
    }

    private static void testImage(int width, int height, String reference, ImageIcon icon) throws IOException {
        final BufferedImage image = (BufferedImage) icon.getImage();
        final File referenceFile = getReferenceFile(reference);
        assertEquals("width", width, image.getWidth(null));
        assertEquals("height", height, image.getHeight(null));
        assertImageEquals(reference, referenceFile, image, 0, 0, null);
    }

    private static File getReferenceFile(String reference) {
        // Java 8 renders SVG images differently, thus, use separate reference files
        final String javaSuffix = Utils.getJavaVersion() == 8 ? "-java8" : "";
        return new File(TestUtils.getTestDataRoot() + "/" + ImageProviderTest.class.getSimpleName() + javaSuffix + "/" + reference + ".png");
    }

    /**
     * Test getting a bounded icon given some UI scaling configured.
     */
    @Test
    public void testGetImageIconBounded() {
        int scale = 2;
        GuiSizesHelper.setPixelDensity(scale);

        ImageProvider imageProvider = new ImageProvider("open").setOptional(true);
        ImageResource resource = imageProvider.getResource();
        Dimension iconDimension = ImageProvider.ImageSizes.SMALLICON.getImageDimension();
        ImageIcon icon = resource.getImageIconBounded(iconDimension);
        Image image = icon.getImage();
        List<Image> resolutionVariants = HiDPISupport.getResolutionVariants(image);
        if (resolutionVariants.size() > 1) {
            assertEquals(2, resolutionVariants.size());
            int expectedVirtualWidth = ImageProvider.ImageSizes.SMALLICON.getVirtualWidth();
            assertEquals(expectedVirtualWidth * scale, resolutionVariants.get(0).getWidth(null));
            assertEquals((int) Math.round(expectedVirtualWidth * scale * HiDPISupport.getHiDPIScale()),
                         resolutionVariants.get(1).getWidth(null));
        }
    }

    /**
     * Test getting an image for a crosshair cursor.
     */
    @ParameterizedTest
    @ValueSource(floats = {1.0f, 1.5f, 3.0f})
    public void testGetCursorImageForCrosshair(float guiScale) throws IOException {
        GuiSizesHelper.setPixelDensity(guiScale);
        Point hotSpot = new Point();
        final UnaryOperator<Dimension> bestCursorSizeFunction = dim -> dim;
        Image image = ImageProvider.getCursorImage("crosshair", null, bestCursorSizeFunction, hotSpot);
        assertCursorDimensionsCorrect(new Point.Double(10.0, 10.0), image, bestCursorSizeFunction, hotSpot);
        assertImageEquals("cursor", getReferenceFile("cursor-crosshair-" + Math.round(guiScale * 10)),
                ((BufferedImage) image), 0, 0, null);
    }

    /**
     * Test getting an image for a custom cursor with overlay.
     */
    @ParameterizedTest
    @ValueSource(floats = {1.0f, 1.5f, 3.0f})
    public void testGetCursorImageWithOverlay(float guiScale) throws IOException {
        GuiSizesHelper.setPixelDensity(guiScale);
        Point hotSpot = new Point();
        UnaryOperator<Dimension> bestCursorSizeFunction = dim -> dim;
        Image image = ImageProvider.getCursorImage("normal", "selection", bestCursorSizeFunction, hotSpot);
        bestCursorSizeFunction = dim -> new Dimension((int) (dim.width * guiScale), (int) (dim.height * guiScale));
        assertCursorDimensionsCorrect(new Point.Double(3.0, 2.0), image, bestCursorSizeFunction, hotSpot);
        assertImageEquals("cursor", getReferenceFile("cursor-normal-selection-" + Math.round(guiScale * 10)),
                ((BufferedImage) image), 0, 0, null);
    }

    private void assertCursorDimensionsCorrect(Point.Double originalHotspot, Image image,
                                               UnaryOperator<Dimension> bestCursorSizeFunction, Point hotSpot) {
        int originalCursorSize = ImageProvider.CURSOR_SIZE_HOTSPOT_IS_RELATIVE_TO;
        Dimension bestCursorSize = bestCursorSizeFunction.apply(new Dimension(originalCursorSize, originalCursorSize));
        Image bestCursorImage = HiDPISupport.getResolutionVariant(image, bestCursorSize.width, bestCursorSize.height);
        int bestCursorImageWidth = bestCursorImage.getWidth(null);
        assertEquals((int) Math.round(bestCursorSize.getWidth()), bestCursorImageWidth);
        int bestCursorImageHeight = bestCursorImage.getHeight(null);
        assertEquals((int) Math.round(bestCursorSize.getHeight()), bestCursorImageHeight);
        assertEquals(originalHotspot.x / originalCursorSize * bestCursorImageWidth, hotSpot.x, 1 /* at worst one pixel off */);
        assertEquals(originalHotspot.y / originalCursorSize * bestCursorImageHeight, hotSpot.y, 1 /* at worst one pixel off */);
    }

}
