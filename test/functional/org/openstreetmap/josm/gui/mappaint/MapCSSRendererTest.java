// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ColorHelper;

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

    private final TestConfig testConfig;

    // development flag - set to true in order to update all reference images
    private static final boolean UPDATE_ALL = false;

    /**
     * The different configurations of this test.
     *
     * @return The parameters.
     */
    @Parameters(name = "{1}")
    public static Collection<Object[]> runs() {
        return Stream.of(
                /** Tests for StyledMapRenderer#drawNodeSymbol */
                new TestConfig("node-shapes", AREA_DEFAULT)
                        .setThresholdPixels(100).setThresholdTotalColorDiff(2_110),

                /** Text for nodes */
                new TestConfig("node-text", AREA_DEFAULT).usesFont("DejaVu Sans")
                        .setThresholdPixels(530).setThresholdTotalColorDiff(23_800),

                /** Tests that StyledMapRenderer#drawWay respects width */
                new TestConfig("way-width", AREA_DEFAULT)
                        .setThresholdPixels(280).setThresholdTotalColorDiff(22_500),

                /** Tests the way color property, including alpha */
                new TestConfig("way-color", AREA_DEFAULT)
                        .setThresholdPixels(100).setThresholdTotalColorDiff(3_400),

                /** Tests dashed ways. */
                new TestConfig("way-dashes", AREA_DEFAULT)
                        .setThresholdPixels(460).setThresholdTotalColorDiff(12_100),

                /** Tests dashed way clamping algorithm */
                new TestConfig("way-dashes-clamp", AREA_DEFAULT)
                        .setThresholdPixels(200).setThresholdTotalColorDiff(6_800),

                /** Tests fill-color property */
                new TestConfig("area-fill-color", AREA_DEFAULT),

                /** Tests the fill-image property. */
                new TestConfig("area-fill-image", AREA_DEFAULT)
                        .setThresholdPixels(420).setThresholdTotalColorDiff(11_200),

                /** Tests area label drawing/placement */
                new TestConfig("area-text", AREA_DEFAULT)
                        .setThresholdPixels(550).setThresholdTotalColorDiff(17_400),

                /** Tests area icon drawing/placement */
                new TestConfig("area-icon", AREA_DEFAULT)
                        .setThresholdPixels(680).setThresholdTotalColorDiff(23_000),

                /** Tests if all styles are sorted correctly. Tests {@link StyleRecord#compareTo(StyleRecord)} */
                new TestConfig("order", AREA_DEFAULT)
                        .setThresholdPixels(2050).setThresholdTotalColorDiff(101_800),

                /** Tests repeat-image feature for ways */
                new TestConfig("way-repeat-image", AREA_DEFAULT)
                        .setThresholdPixels(2100).setThresholdTotalColorDiff(93_000),
                /** Tests the clamping for repeat-images and repeat-image-phase */
                new TestConfig("way-repeat-image-clamp", AREA_DEFAULT)
                        .setThresholdPixels(80).setThresholdTotalColorDiff(2_300),

                /** Tests text along a way */
                new TestConfig("way-text", AREA_DEFAULT)
                        .setThresholdPixels(3400).setThresholdTotalColorDiff(122_700),

                /** Another test for node shapes */
                new TestConfig("node-shapes2").setImageWidth(600)
                        .setThresholdPixels(1230).setThresholdTotalColorDiff(43_700),
                /** Tests default values for node shapes */
                new TestConfig("node-shapes-default")
                        .setThresholdPixels(10).setThresholdTotalColorDiff(270),
                /** Tests node shapes with both fill and stroke combined */
                new TestConfig("node-shapes-combined")
                        .setThresholdPixels(360).setThresholdTotalColorDiff(9_200),
                /** Another test for dashed ways */
                new TestConfig("way-dashes2")
                        .setThresholdPixels(340).setThresholdTotalColorDiff(16_100),
                /** Tests node text placement */
                new TestConfig("node-text2")
                        .setThresholdPixels(1020).setThresholdTotalColorDiff(345_000),
                /** Tests relation link selector */
                new TestConfig("relation-linkselector")
                        .setThresholdPixels(430).setThresholdTotalColorDiff(13_000),
                /** Tests parent selector on relation */
                new TestConfig("relation-parentselector")
                        .setThresholdPixels(310).setThresholdTotalColorDiff(8_200),

                /** Tests evaluation of expressions */
                new TestConfig("eval").setImageWidth(600)
                        .setThresholdPixels(6610).setThresholdTotalColorDiff(3_304_000)

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
        Assume.assumeTrue("Test requires openJDK", javaHome != null && javaHome.toLowerCase(Locale.ENGLISH).contains("openjdk"));

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
        dataSet.allPrimitives().forEach(this::loadPrimitiveStyle);
        dataSet.setSelected(dataSet.allPrimitives().stream().filter(n -> n.isKeyTrue("selected")).collect(Collectors.toList()));

        ProjectionBounds pb = new ProjectionBounds();
        pb.extend(ProjectionRegistry.getProjection().latlon2eastNorth(testConfig.getTestArea().getMin()));
        pb.extend(ProjectionRegistry.getProjection().latlon2eastNorth(testConfig.getTestArea().getMax()));
        double scale = (pb.maxEast - pb.minEast) / testConfig.imageWidth;

        RenderingHelper.StyleData sd = new RenderingHelper.StyleData();
        sd.styleUrl = testConfig.getStyleSourceUrl();
        RenderingHelper rh = new RenderingHelper(dataSet, testConfig.getTestArea(), scale, Collections.singleton(sd));
        rh.setFillBackground(false);
        rh.setDebugStream(System.out);
        System.out.println("Running " + getClass() + "[" + testConfig.testDirectory + "]");
        BufferedImage image = rh.render();

        if (UPDATE_ALL) {
            ImageIO.write(image, "png", new File(testConfig.getTestDirectory() + "/reference.png"));
            return;
        }

        BufferedImage reference = testConfig.getReference();

        // now compute differences:
        assertEquals(image.getWidth(), reference.getWidth());
        assertEquals(image.getHeight(), reference.getHeight());

        StringBuilder differences = new StringBuilder();
        ArrayList<Point> differencePoints = new ArrayList<>();
        int colorDiffSum = 0;

        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                int expected = reference.getRGB(x, y);
                int result = image.getRGB(x, y);
                if (!colorsAreSame(expected, result)) {
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

        if (differencePoints.size() > testConfig.thresholdPixels || colorDiffSum > testConfig.thresholdTotalColorDiff) {
            // You can use this to debug:
            ImageIO.write(image, "png", new File(testConfig.getTestDirectory() + "/test-output.png"));

            // Add a nice image that highlights the differences:
            BufferedImage diffImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (Point p : differencePoints) {
                diffImage.setRGB(p.x, p.y, 0xffff0000);
            }
            ImageIO.write(diffImage, "png", new File(testConfig.getTestDirectory() + "/test-differences.png"));

            if (differencePoints.size() > testConfig.thresholdPixels) {
                fail(MessageFormat.format("Images for test {0} differ at {1} points, threshold is {2}: {3}",
                        testConfig.testDirectory, differencePoints.size(), testConfig.thresholdPixels, differences.toString()));
            } else {
                fail(MessageFormat.format("Images for test {0} differ too much in color, value is {1}, permitted threshold is {2}: {3}",
                        testConfig.testDirectory, colorDiffSum, testConfig.thresholdTotalColorDiff, differences.toString()));
            }
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
        private Bounds testArea;
        private final ArrayList<String> fonts = new ArrayList<>();
        private DataSet ds;
        private int imageWidth = IMAGE_SIZE;
        private int thresholdPixels;
        private int thresholdTotalColorDiff;

        TestConfig(String testDirectory, Bounds testArea) {
            this.testDirectory = testDirectory;
            this.testArea = testArea;
        }

        TestConfig(String testDirectory) {
            this.testDirectory = testDirectory;
        }

        public TestConfig setImageWidth(int imageWidth) {
            this.imageWidth = imageWidth;
            return this;
        }

        /**
         * Set the number of pixels that can differ.
         *
         * Needed due to somewhat platform dependent font rendering.
         * @param thresholdPixels the number of pixels that can differ
         * @return this object, for convenience
         */
        public TestConfig setThresholdPixels(int thresholdPixels) {
            this.thresholdPixels = thresholdPixels;
            return this;
        }

        /**
         * Set the threshold for total color difference.
         * Every difference in any color component (and alpha) will be added up and must not exceed this threshold.
         * Needed due to somewhat platform dependent font rendering.
         * @param thresholdTotalColorDiff he threshold for total color difference
         * @return this object, for convenience
         */
        public TestConfig setThresholdTotalColorDiff(int thresholdTotalColorDiff) {
            this.thresholdTotalColorDiff = thresholdTotalColorDiff;
            return this;
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

        public String getStyleSourceUrl() {
            return getTestDirectory() + "/style.mapcss";
        }

        public DataSet getOsmDataSet() throws IllegalDataException, IOException {
            if (ds == null) {
                ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(getTestDirectory(), "data.osm")), null);
            }
            return ds;
        }

        public Bounds getTestArea() throws IllegalDataException, IOException {
            if (testArea == null) {
                testArea = getOsmDataSet().getDataSourceBounds().get(0);
            }
            return testArea;
        }

        @Override
        public String toString() {
            return "TestConfig [testDirectory=" + testDirectory + ", testArea=" + testArea + ']';
        }
    }
}
