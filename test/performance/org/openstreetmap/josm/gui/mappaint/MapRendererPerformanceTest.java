// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.RenderBenchmarkCollector.CapturingBenchmark;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.StyleRecord;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.BooleanStyleSetting;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    @BeforeClass
    public static void load() throws Exception {
        JOSMFixture.createPerformanceTestFixture().init(true);

        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_WIDTH);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_WIDTH);
        nc = Main.map.mapView;
        nc.setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);

        MapPaintStyles.readFromPreferences();

        SourceEntry se = new MapCSSStyleSource(TestUtils.getTestDataRoot() + "styles/filter.mapcss", "filter", "");
        filterStyle = MapPaintStyles.addStyle(se);
        List<StyleSource> sources = MapPaintStyles.getStyles().getStyleSources();
        filterStyleIdx = sources.indexOf(filterStyle);
        Assert.assertEquals(2, filterStyleIdx);

        Assert.assertEquals(Feature.values().length, filterStyle.settings.size());
        for (StyleSetting set : filterStyle.settings) {
            BooleanStyleSetting bset = (BooleanStyleSetting) set;
            String prefKey = bset.prefKey;
            boolean found = false;
            for (Feature f : Feature.values()) {
                if (prefKey.endsWith(":" + f.label() + "_off")) {
                    filters.put(f, bset);
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(prefKey, found);
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
        Assert.assertNotNull(defaultStyle);

        for (StyleSetting set : defaultStyle.settings) {
            if (set instanceof BooleanStyleSetting) {
                BooleanStyleSetting bset = (BooleanStyleSetting) set;
                if (bset.prefKey.endsWith(":hide_icons")) {
                    hideIconsSetting = bset;
                }
            }
        }
        Assert.assertNotNull(hideIconsSetting);
        hideIconsSetting.setValue(false);
        MapPaintStyles.reloadStyles(defaultStyleIdx);

        try (
            InputStream fisC = Compression.getUncompressedFileInputStream(new File("data_nodist/neubrandenburg.osm.bz2"));
        ) {
            dsCity = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
        }
    }

    @AfterClass
    public static void cleanUp() {
        setFilterStyleActive(false);
        if (hideIconsSetting != null) {
            hideIconsSetting.setValue(true);
        }
        MapPaintStyles.reloadStyles(defaultStyleIdx);
    }

    private static class PerformanceTester {
        public double scale = 0;
        public LatLon center = LL_CITY;
        public Bounds bounds;
        public int noWarmup = 3;
        public int noIterations = 7;
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
            nc.zoomTo(Projections.project(center), scale);
            if (checkScale) {
                int lvl = Selector.OptimizedGeneralSelector.scale2level(nc.getDist100Pixel());
                Assert.assertEquals(17, lvl);
            }

            if (bounds == null) {
                bounds = nc.getLatLonBounds(g.getClipBounds());
            }

            StyledMapRenderer renderer = new StyledMapRenderer(g, nc, false);

            int noTotal = noWarmup + noIterations;
            for (int i = 1; i <= noTotal; i++) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, IMG_WIDTH, IMG_WIDTH);
                if (clearStyleCache) {
                    MapPaintStyles.getStyles().clearCached();
                }
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    Logging.warn(ex);
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
                    dumpElementCount(data);
                }
                dumpTimes(data);
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
    public void testPerformanceGenerate() throws IOException {
        setFilterStyleActive(false);
        PerformanceTester test = new PerformanceTester();
        test.bounds = BOUNDS_CITY_ALL;
        test.label = "big";
        test.dumpImage = false;
        test.noWarmup = 3;
        test.noIterations = 10;
        test.mpGenerate = true;
        test.clearStyleCache = true;
        test.run();
    }

    private static void testDrawFeature(Feature feature) throws IOException {
        PerformanceTester test = new PerformanceTester();
        test.noWarmup = 3;
        test.noIterations = 10;
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
        MapPaintStyles.reloadStyles(filterStyleIdx);
        test.run();
    }

    /**
     * Test phase 2, the actual drawing.
     * Several runs: Icons, lines, etc. are tested separately (+ one run with
     * all features activated)
     * @throws IOException in case of an I/O error
     */
    @Test
    public void testPerformanceDrawFeatures() throws IOException {
        testDrawFeature(null);
        for (Feature f : Feature.values()) {
            testDrawFeature(f);
        }
    }

    private static void setFilterStyleActive(boolean active) {
        if (filterStyle.active != active) {
            MapPaintStyles.toggleStyleActive(filterStyleIdx);
        }
        Assert.assertEquals(active, filterStyle.active);
    }

    private static void dumpRenderedImage(String id) throws IOException {
        File outputfile = new File("test-neubrandenburg-"+id+".png");
        ImageIO.write(img, "png", outputfile);
    }

    public static void dumpTimes(BenchmarkData bd) {
        System.out.print(String.format("gen. %3d, sort %3d, draw %3d%n", bd.getGenerateTime(), bd.getSortTime(), bd.getDrawTime()));
    }

    public static void dumpElementCount(BenchmarkData bd) {
        System.out.println(bd.recordElementStats().entrySet().stream()
                .map(e -> e.getKey().getSimpleName().replace("Element", "") + ":" + e.getValue()).collect(Collectors.joining(" ")));
    }

    public static class BenchmarkData extends CapturingBenchmark {

        private List<StyleRecord> allStyleElems;

        @Override
        public boolean renderDraw(List<StyleRecord> allStyleElems) {
            this.allStyleElems = allStyleElems;
            return super.renderDraw(allStyleElems);
        }

        private Map<Class<? extends StyleElement>, Integer> recordElementStats() {
            Map<Class<? extends StyleElement>, Integer> styleElementCount = new HashMap<>();
            for (StyleRecord r : allStyleElems) {
                Class<? extends StyleElement> klass = r.getStyle().getClass();
                Integer count = styleElementCount.get(klass);
                if (count == null) {
                    count = 0;
                }
                styleElementCount.put(klass, count + 1);
            }
            return styleElementCount;
        }
    }
}
