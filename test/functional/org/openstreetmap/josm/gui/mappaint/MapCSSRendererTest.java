// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test cases for {@link StyledMapRenderer} and the MapCSS classes.
 * <p>
 * This test uses the data and reference files stored in the test data directory {@value #TEST_DATA_BASE}
 * @author Michael Zangl
 */
@RunWith(Parameterized.class)
public class MapCSSRendererTest {
    private static final String TEST_DATA_BASE = "/renderer/";
    /**
     * lat = 0..1, lon = 0..1
     */
    private static final Bounds AREA_DEFAULT = new Bounds(0, 0, 1, 1);
    private static final int IMAGE_SIZE = 256;

    /**
     * Minimal test rules required
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    private TestConfig testConfig;

    /**
     * The different configurations of this test.
     *
     * @return The parameters.
     */
    @Parameters(name = "{1}")
    public static Collection<Object[]> runs() {
        return Stream.of(
                /** Tests for StyledMapRenderer#drawNodeSymbol */
                new TestConfig("node-shapes", AREA_DEFAULT),

                /** Text for nodes */
                new TestConfig("node-text", AREA_DEFAULT).usesFont("DejaVu Sans"),

                /** Tests that StyledMapRenderer#drawWay respects width */
                new TestConfig("way-width", AREA_DEFAULT),

                /** Tests the way color property, including alpha */
                new TestConfig("way-color", AREA_DEFAULT),

                /** Tests dashed ways. */
                new TestConfig("way-dashes", AREA_DEFAULT),

                /** Tests dashed way clamping algorithm */
                new TestConfig("way-dashes-clamp", AREA_DEFAULT),

                /** Tests fill-color property */
                new TestConfig("area-fill-color", AREA_DEFAULT),

                /** Tests the fill-image property. */
                new TestConfig("area-fill-image", AREA_DEFAULT),

                /** Tests area label drawing/placement */
                new TestConfig("area-text", AREA_DEFAULT),

                /** Tests area icon drawing/placement */
                new TestConfig("area-icon", AREA_DEFAULT),

                /** Tests if all styles are sorted correctly. Tests {@link StyleRecord#compareTo(StyleRecord)} */
                new TestConfig("order", AREA_DEFAULT),

                /** Tests repeat-image feature for ways */
                new TestConfig("way-repeat-image", AREA_DEFAULT),
                /** Tests the clamping for repeat-images and repeat-image-phase */
                new TestConfig("way-repeat-image-clamp", AREA_DEFAULT),

                /** Tests text along a way */
                new TestConfig("way-text", AREA_DEFAULT)
                ).map(e -> new Object[] {e, e.testDirectory})
                .collect(Collectors.toList());
    }

    /**
     * @param testConfig The config to use for this test.
     * @param ignored The name to print it nicely
     */
    public MapCSSRendererTest(TestConfig testConfig, String ignored) {
        this.testConfig = testConfig;
    }

    /**
     * This test only runs on OpenJDK.
     * It is ignored for other Java versions since they differ slightly in their rendering engine.
     * @since 11691
     */
    @Before
    public void forOpenJDK() {
        String javaHome = System.getProperty("java.home");
        Assume.assumeTrue("Test requires openJDK", javaHome != null && javaHome.contains("openjdk"));

        List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String font : testConfig.fonts) {
            Assume.assumeTrue("Test requires font: " + font, fonts.contains(font));
        }
    }

    /**
     * Run the test using {@link #testConfig}
     * @throws Exception if an error occurs
     */
    @Test
    public void testRender() throws Exception {
        // Force reset of preferences
        StyledMapRenderer.PREFERENCE_ANTIALIASING_USE.put(true);
        StyledMapRenderer.PREFERENCE_TEXT_ANTIALIASING.put("gasp");

        // load the data
        DataSet dataSet = testConfig.getOsmDataSet();

        // load the style
        MapCSSStyleSource.STYLE_SOURCE_LOCK.writeLock().lock();
        try {
            MapPaintStyles.getStyles().clear();

            MapCSSStyleSource source = new MapCSSStyleSource(testConfig.getStyleSourceEntry());
            source.loadStyleSource();
            if (!source.getErrors().isEmpty()) {
                fail("Failed to load style file. Errors: " + source.getErrors());
            }
            MapPaintStyles.getStyles().setStyleSources(Arrays.asList(source));
            MapPaintStyles.fireMapPaintSylesUpdated();
            MapPaintStyles.getStyles().clearCached();

        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.writeLock().unlock();
        }

        // create the renderer
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        NavigatableComponent nc = new NavigatableComponent() {
            {
                setBounds(0, 0, IMAGE_SIZE, IMAGE_SIZE);
                updateLocationState();
            }

            @Override
            protected boolean isVisibleOnScreen() {
                return true;
            }

            @Override
            public Point getLocationOnScreen() {
                return new Point(0, 0);
            }
        };
        nc.zoomTo(testConfig.testArea);
        dataSet.allPrimitives().stream().forEach(this::loadPrimitiveStyle);
        dataSet.setSelected(dataSet.allPrimitives().stream().filter(n -> n.isKeyTrue("selected")).collect(Collectors.toList()));

        Graphics2D g = image.createGraphics();
        // Force all render hints to be defaults - do not use platform values
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        new StyledMapRenderer(g, nc, false).render(dataSet, false, testConfig.testArea);

        BufferedImage reference = testConfig.getReference();

        // now compute differences:
        assertEquals(IMAGE_SIZE, reference.getWidth());
        assertEquals(IMAGE_SIZE, reference.getHeight());

        StringBuilder differences = new StringBuilder();
        ArrayList<Point> differencePoints = new ArrayList<>();

        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                int expected = reference.getRGB(x, y);
                int result = image.getRGB(x, y);
                if (!colorsAreSame(expected, result)) {
                    differencePoints.add(new Point(x, y));
                    if (differences.length() < 500) {
                        differences.append("\nDifference at ")
                        .append(x)
                        .append(",")
                        .append(y)
                        .append(": Expected ")
                        .append(Integer.toHexString(expected))
                        .append(" but got ")
                        .append(Integer.toHexString(result));
                    }
                }
            }
        }

        if (differencePoints.size() > 0) {
            // You can use this to debug:
            ImageIO.write(image, "png", new File(testConfig.getTestDirectory() + "/test-output.png"));

            // Add a nice image that highlights the differences:
            BufferedImage diffImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
            for (Point p : differencePoints) {
                diffImage.setRGB(p.x, p.y, 0xffff0000);
            }
            ImageIO.write(diffImage, "png", new File(testConfig.getTestDirectory() + "/test-differences.png"));

            fail(MessageFormat.format("Images for test {0} differ at {1} points: {2}",
                    testConfig.testDirectory, differencePoints.size(), differences.toString()));
        }
    }

    private void loadPrimitiveStyle(OsmPrimitive n) {
        n.setHighlighted(n.isKeyTrue("highlight"));
        if (n.isKeyTrue("disabled")) {
            n.setDisabledState(false);
        }
    }

    /**
     * Check if two colors differ
     * @param expected The expected color
     * @param actual The actual color
     * @return <code>true</code> if they differ.
     */
    private boolean colorsAreSame(int expected, int actual) {
        int expectedAlpha = expected >> 24;
        if (expectedAlpha == 0) {
            return actual >> 24 == 0;
        } else {
            return expected == actual;
        }
    }

    private static class TestConfig {
        private final String testDirectory;
        private final Bounds testArea;
        private final ArrayList<String> fonts = new ArrayList<>();

        TestConfig(String testDirectory, Bounds testArea) {
            this.testDirectory = testDirectory;
            this.testArea = testArea;
        }

        public TestConfig usesFont(String string) {
            this.fonts.add(string);
            return this;
        }

        public BufferedImage getReference() throws IOException {
            return ImageIO.read(new File(getTestDirectory() + "/reference.png"));
        }

        private String getTestDirectory() {
            return TestUtils.getTestDataRoot() + TEST_DATA_BASE + testDirectory;
        }

        public SourceEntry getStyleSourceEntry() {
            return new SourceEntry(SourceType.MAP_PAINT_STYLE, getTestDirectory() + "/style.mapcss",
                    "test style", "a test style", true // active
            );
        }

        public DataSet getOsmDataSet() throws FileNotFoundException, IllegalDataException {
            return OsmReader.parseDataSet(new FileInputStream(getTestDirectory() + "/data.osm"), null);
        }

        @Override
        public String toString() {
            return "TestConfig [testDirectory=" + testDirectory + ", testArea=" + testArea + ']';
        }
    }
}
