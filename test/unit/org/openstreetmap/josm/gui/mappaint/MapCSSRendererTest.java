// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
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
     * @return The parameters.
     */
    @Parameters
    public static Collection<Object[]> runs() {
        return Stream.of(
                /** Tests for StyledMapRenderer#drawNodeSymbol */
                new TestConfig("node-shapes", AREA_DEFAULT),

                /** Tests that StyledMapRenderer#drawWay respects width */
                new TestConfig("way-width", AREA_DEFAULT)

                ).map(e -> new Object[] {e})
                .collect(Collectors.toList());
    }

    /**
     * @param testConfig The config to use for this test.
     */
    public MapCSSRendererTest(TestConfig testConfig) {
        this.testConfig = testConfig;
    }

    /**
     * Run the test using {@link #testConfig}
     * @throws Exception if an error occurs
     */
    @Test
    public void testRender() throws Exception {
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
        dataSet.allPrimitives().stream().forEach(n -> n.setHighlighted(n.isKeyTrue("highlight")));
        new StyledMapRenderer(image.createGraphics(), nc, false).render(dataSet, false, testConfig.testArea);

        BufferedImage reference = testConfig.getReference();

        // now compute differences:
        assertEquals(IMAGE_SIZE, reference.getWidth());
        assertEquals(IMAGE_SIZE, reference.getHeight());

        StringBuilder differences = new StringBuilder();

        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                int expected = reference.getRGB(x, y);
                int result = image.getRGB(x, y);
                if (expected != result && differences.length() < 500) {
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

        if (differences.length() > 0) {
            // You can use this to debug:
            ImageIO.write(image, "png", new File(testConfig.getTestDirectory() + "/test-output.png"));
            fail("Images for test " + testConfig.testDirectory + " differ: " + differences.toString());
        }
    }

    private static class TestConfig {
        private final String testDirectory;
        private final Bounds testArea;

        TestConfig(String testDirectory, Bounds testArea) {
            this.testDirectory = testDirectory;
            this.testArea = testArea;
        }

        public BufferedImage getReference() throws IOException {
            return ImageIO.read(new File(getTestDirectory() + "/reference.png"));
        }

        private String getTestDirectory() {
            return TestUtils.getTestDataRoot() + TEST_DATA_BASE + testDirectory;
        }

        public SourceEntry getStyleSourceEntry() {
            return new SourceEntry(getTestDirectory() + "/style.mapcss",
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
