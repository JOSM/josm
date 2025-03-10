// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.RenderBenchmarkCollector.CapturingBenchmark;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.StyleRecord;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.BooleanStyleSetting;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.PerformanceTest;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Performance test of map renderer.
 */
@BasicPreferences
@Main
@PerformanceTest
@Projection
@Territories
@Timeout(value = 15, unit = TimeUnit.MINUTES)
public class MapRendererPerformanceTest {

    private static final boolean DUMP_IMAGE = false; // dump images to file for debugging purpose

    private static final int IMG_WIDTH = 2048;
    private static final int IMG_HEIGHT = 1536;

    private static Graphics2D g;
    private static BufferedImage img;
    private static NavigatableComponent nc;
    private static DataSet dsCity;
    private static final Bounds BOUNDS_CITY_ALL = new Bounds(53.4382, 13.1094, 53.6153, 13.4074, false);
    private static final LatLon LL_CITY = new LatLon(53.5574458, 13.2602781);
    private static final double SCALE_Z17 = 1.5;

    private static int defaultStyleIdx;
    private static BooleanStyleSetting hideIconsSetting;

    private static int filterStyleIdx;
    private static StyleSource filterStyle;

    private enum Feature {
        ICON, SYMBOL, NODE_TEXT, LINE, LINE_TEXT, AREA;
        public String label() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    private static final EnumMap<Feature, BooleanStyleSetting> filters = new EnumMap<>(Feature.class);

    /**
     * Initializes test environment.
     * @throws Exception if any error occurs
     */
    @BeforeAll
    public static void load() throws Exception {
        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_HEIGHT);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        nc = new NavigatableComponent() {
            {
                setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);
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
        nc.zoomTo(BOUNDS_CITY_ALL);

        MapPaintStyles.readFromPreferences();

        SourceEntry se = new MapCSSStyleSource(TestUtils.getTestDataRoot() + "styles/filter.mapcss", "filter", "");
        filterStyle = MapPaintStyles.addStyle(se);
        List<StyleSource> sources = MapPaintStyles.getStyles().getStyleSources();
        filterStyleIdx = sources.indexOf(filterStyle);
        assertEquals(1, filterStyleIdx);

        assertEquals(Feature.values().length, filterStyle.settings.size());
        for (StyleSetting set : filterStyle.settings) {
            BooleanStyleSetting bset = (BooleanStyleSetting) set;
            String prefKey = bset.getKey();
            boolean found = false;
            for (Feature f : Feature.values()) {
                if (prefKey.endsWith(":" + f.label() + "_off")) {
                    filters.put(f, bset);
                    found = true;
                    break;
                }
            }
            assertTrue(found, prefKey);
        }

        MapCSSStyleSource defaultStyle = null;
        for (int i = 0; i < sources.size(); i++) {
            StyleSource s = sources.get(i);
            if ("resource://styles/standard/elemstyles.mapcss".equals(s.url)) {
                defaultStyle = (MapCSSStyleSource) s;
                defaultStyleIdx = i;
                break;
            }
        }
        assertNotNull(defaultStyle);

        for (StyleSetting set : defaultStyle.settings) {
            if (set instanceof BooleanStyleSetting) {
                BooleanStyleSetting bset = (BooleanStyleSetting) set;
                if (bset.getKey().endsWith(":hide_icons")) {
                    hideIconsSetting = bset;
                }
            }
        }
        assertNotNull(hideIconsSetting);
        hideIconsSetting.setValue(false);
        MapPaintStyleLoader.reloadStyles(defaultStyleIdx);

        dsCity = PerformanceTestUtils.getNeubrandenburgDataSet();
    }

    /**
     * Cleanup test environment.
     */
    @AfterAll
    public static void cleanUp() {
        setFilterStyleActive(false);
        if (hideIconsSetting != null) {
            hideIconsSetting.setValue(true);
        }
        MapPaintStyleLoader.reloadStyles(defaultStyleIdx);
    }

    private static final class PerformanceTester {
        public double scale = 0;
        public LatLon center = LL_CITY;
        public Bounds bounds;
        public int noWarmup = 20;
        public int noIterations = 30;
        public boolean dumpImage = DUMP_IMAGE;
        public boolean clearStyleCache = true;
        public String label = "";
        public boolean mpGenerate = false;
        public boolean mpSort = false;
        public boolean mpDraw = false;
        public boolean mpTotal = false;

        private final List<Long> generateTimes = new ArrayList<>();
        private final List<Long> sortTimes = new ArrayList<>();
        private final List<Long> drawTimes = new ArrayList<>();
        private final List<Long> totalTimes = new ArrayList<>();

        @SuppressFBWarnings(value = "DM_GC")
        public void run() throws IOException {
            boolean checkScale = false;
            if (scale == 0) {
                checkScale = true;
                scale = SCALE_Z17;
            }
            nc.zoomTo(ProjectionRegistry.getProjection().latlon2eastNorth(center), scale);
            if (checkScale) {
                int lvl = Selector.GeneralSelector.scale2level(nc.getDist100Pixel());
                assertEquals(17, lvl);
            }

            if (bounds == null) {
                bounds = nc.getLatLonBounds(g.getClipBounds());
            }

            StyledMapRenderer renderer = new StyledMapRenderer(g, nc, false);
            assertEquals(IMG_WIDTH, (int) nc.getState().getViewWidth());
            assertEquals(IMG_HEIGHT, (int) nc.getState().getViewHeight());

            int noTotal = noWarmup + noIterations;
            for (int i = 1; i <= noTotal; i++) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
                if (clearStyleCache) {
                    MapPaintStyles.getStyles().clearCached();
                    dsCity.clearMappaintCache();
                }
                BenchmarkData data = new BenchmarkData();
                renderer.setBenchmarkFactory(() -> data);
                renderer.render(dsCity, false, bounds);

                if (i > noWarmup) {
                    generateTimes.add(data.getGenerateTime());
                    sortTimes.add(data.getSortTime());
                    drawTimes.add(data.getDrawTime());
                    totalTimes.add(data.getGenerateTime() + data.getSortTime() + data.getDrawTime());
                }
                if (i == 1) {
                    data.dumpElementCount();
                }
                data.dumpTimes();
                if (dumpImage && i == noTotal) {
                    dumpRenderedImage(label);
                }
            }

            if (mpGenerate) {
                processTimes(generateTimes, "generate");
            }
            if (mpSort) {
                processTimes(sortTimes, "sort");
            }
            if (mpDraw) {
                processTimes(drawTimes, "draw");
            }
            if (mpTotal) {
                processTimes(totalTimes, "total");
            }
        }

        private void processTimes(List<Long> times, String sublabel) {
            Collections.sort(times);
            // Take median instead of average. This should give a more stable
            // result and avoids distortions by outliers.
            long medianTime = times.get(times.size() / 2);
            PerformanceTestUtils.measurementPlotsPluginOutput(label + " " + sublabel + " (ms)", medianTime);
        }
    }

    /**
     * Test phase 1, the calculation of {@link StyleElement}s.
     * @throws IOException in case of an I/O error
     */
    @Test
    void testPerformanceGenerate() throws IOException {
        setFilterStyleActive(false);
        PerformanceTester test = new PerformanceTester();
        test.bounds = BOUNDS_CITY_ALL;
        test.label = "big";
        test.dumpImage = false;
        test.mpGenerate = true;
        test.clearStyleCache = true;
        test.run();
    }

    private static void testDrawFeature(Feature feature) throws IOException {
        PerformanceTester test = new PerformanceTester();
        test.mpDraw = true;
        test.clearStyleCache = false;
        if (feature != null) {
            BooleanStyleSetting filterSetting = filters.get(feature);
            test.label = filterSetting.label;
            setFilterStyleActive(true);
            for (Feature f : Feature.values()) {
                filters.get(f).setValue(true);
            }
            filterSetting.setValue(false);
        } else {
            test.label = "all";
            setFilterStyleActive(false);
        }
        MapPaintStyleLoader.reloadStyles(filterStyleIdx);
        dsCity.clearMappaintCache();
        test.run();
    }

    /**
     * Test phase 2, the actual drawing.
     * Several runs: Icons, lines, etc. are tested separately (+ one run with
     * all features activated)
     * @throws IOException in case of an I/O error
     */
    @Test
    void testPerformanceDrawFeatures() throws IOException {
        testDrawFeature(null);
        for (Feature f : Feature.values()) {
            testDrawFeature(f);
        }
    }

    /**
     * Resets MapPaintStyles to a single source.
     * @param source new map paint style source
     */
    public static void resetStylesToSingle(StyleSource source) {
        MapPaintStyles.getStyles().clear();
        MapPaintStyles.getStyles().add(source);
    }

    private static void setFilterStyleActive(boolean active) {
        if (filterStyle != null) {
            if (filterStyle.active != active) {
                MapPaintStyles.toggleStyleActive(filterStyleIdx);
            }
            // Assert.assertEquals(active, filterStyle.active);
        }
    }

    private static void dumpRenderedImage(String id) throws IOException {
        File outputfile = new File("test-neubrandenburg-"+id+".png");
        ImageIO.write(img, "png", outputfile);
    }

    static class BenchmarkData extends CapturingBenchmark {

        private List<StyleRecord> allStyleElems;

        @Override
        public boolean renderDraw(List<StyleRecord> allStyleElems) {
            this.allStyleElems = allStyleElems;
            return super.renderDraw(allStyleElems);
        }

        private Map<Class<? extends StyleElement>, Long> recordElementStats() {
            return allStyleElems.stream()
                    .collect(Collectors.groupingBy(r -> r.getStyle().getClass(), Collectors.counting()));
        }

        public void dumpTimes() {
            System.out.printf("gen. %4d, sort %4d, draw %4d%n", getGenerateTime(), getSortTime(), getDrawTime());
        }

        public void dumpElementCount() {
            System.out.println(recordElementStats().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getSimpleName()))
                    .map(e -> e.getKey().getSimpleName().replace("Element", "") + ":" + e.getValue())
                    .collect(Collectors.joining(" ")));
        }
    }
}
