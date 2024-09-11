// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.openstreetmap.josm.gui.mappaint.MapCSSRendererTest.assertImageEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.jcs3.access.CacheAccess;
import org.awaitility.Awaitility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link StyledTiledMapRenderer}
 */
@Main
@Projection
class StyledTiledMapRendererTest {
    static Stream<Arguments> testRender() {
        final Function<TileZXY, OsmPrimitive[]> generateNodes = tile -> {
            final Bounds bounds = TileZXY.tileToBounds(tile);
            return new OsmPrimitive[] {
                    new Node(bounds.getCenter()),
                    new Node(bounds.getMin()),
                    new Node(bounds.getMax()),
                    new Node(new LatLon(bounds.getMinLat(), bounds.getMaxLon())),
                    new Node(new LatLon(bounds.getMaxLat(), bounds.getMinLon())),
            };
        };
        // Everything but the 5 point nodes is just to make it easier to figure out why it is failing
        final Function<TileZXY, Stream<Arguments>> generateTests = tile -> Stream.of(
                Arguments.of("Center node", (Supplier<DataSet>) () -> new DataSet(generateNodes.apply(tile)[0]), tile)
        );
        return Stream.concat(
                // Tiles around 0, 0
                IntStream.rangeClosed(2097151, 2097152).mapToObj(x -> IntStream.rangeClosed(2097151, 2097152)
                        .mapToObj(y -> new TileZXY(22, x, y))).flatMap(Function.identity()),
                // Tiles in the four quadrants far away from 0, 0
                Stream.of(new TileZXY(16, 13005, 25030),
                        new TileZXY(14, 5559, 10949),
                        new TileZXY(15, 31687, 21229),
                        new TileZXY(13, 8135, 2145)))
                .flatMap(generateTests);
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource
    void testRender(String testIdentifier, final Supplier<DataSet> dataSetSupplier, final TileZXY tile)
            throws InterruptedException, ExecutionException {
        final int zoom = tile.zoom();
        final Bounds viewArea = TileZXY.tileToBounds(tile);
        final CacheAccess<TileZXY, ImageCache> cache = JCSCacheManager.getCache("StyledTiledMapRendererTest:testRender");
        cache.clear();
        final DataSet ds = dataSetSupplier.get();
        final NavigatableComponent nc = new NavigatableComponent() {
            @Override
            public int getWidth() {
                return 800;
            }

            @Override
            public int getHeight() {
                return 600;
            }
        };
        nc.zoomTo(viewArea);
        final ExecutorService worker = MainApplication.worker;
        final BufferedImage oldRenderStyle = render((g2d) -> new StyledMapRenderer(g2d, nc, false), ds, nc);
        final Function<Graphics2D, StyledTiledMapRenderer> newRenderer = (g2d) -> {
            StyledTiledMapRenderer stmr = new StyledTiledMapRenderer(g2d, nc, false);
            stmr.setCache(viewArea, cache, zoom, ignored -> { /* ignored */ });
            return stmr;
        };
        // First "renders" schedules off-thread rendering. We need to loop here since a render call may only schedule a small amount of tiles.
        int size = -1;
        while (size != cache.getMatching(".*").size()) {
            size = cache.getMatching(".*").size();
            render(newRenderer, ds, nc);
            Awaitility.await().until(() -> cache.getMatching(".*").values().stream().allMatch(i -> i.imageFuture() == null));
        }
        worker.submit(() -> { /* Sync */ }).get();
        // Second render actually does the painting
        final BufferedImage newRenderStyle = render(newRenderer, ds, nc);
        worker.submit(() -> { /* Sync */ }).get();
        var entries = cache.getMatching(".*").entrySet().stream().filter(e -> {
            BufferedImage image = (BufferedImage) e.getValue().image();
            return Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth())).anyMatch(i -> i != 0);
        }).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().image()));
        try {
            assertImageEquals(testIdentifier, oldRenderStyle, newRenderStyle, 0, 0, diff -> {
                try {
                    if (!Files.isDirectory(Paths.get(TestUtils.getTestDataRoot(), "output"))) {
                        Files.createDirectories(Paths.get(TestUtils.getTestDataRoot(), "output"));
                    }
                    final String basename = TestUtils.getTestDataRoot() + "output/" +
                            testIdentifier + ' ' + tile.zoom() + '-' + tile.x() + '-' + tile.y();
                    ImageIO.write(diff, "png", new File(basename + "-diff.png"));
                    ImageIO.write(newRenderStyle, "png", new File(basename + "-new.png"));
                    ImageIO.write(oldRenderStyle, "png", new File(basename + "-old.png"));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } finally {
            cache.clear();
        }
    }

    private static BufferedImage render(Function<Graphics2D, ? extends StyledMapRenderer> renderer,
                                        final DataSet ds, final NavigatableComponent nc) {
        final BufferedImage bufferedImage = new BufferedImage(nc.getWidth(), nc.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = bufferedImage.createGraphics();
        final StyledMapRenderer styledMapRenderer = renderer.apply(g2d);
        styledMapRenderer.render(ds, true, nc.getRealBounds());
        g2d.dispose();
        return bufferedImage;
    }
}
