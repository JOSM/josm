// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * A styled render that does the rendering on a tile basis. Note: this is currently experimental!
 * It may be extracted to an interface at a later date.
 * @since 19176
 */
public final class StyledTiledMapRenderer extends StyledMapRenderer {
    // Render to the surrounding tiles for continuity -- this probably needs to be tweaked
    private static final int BUFFER_TILES = 2;
    // The number of extra pixels to render per tile (avoids black lines in render result)
    private static final int BUFFER_PIXELS = 16;
    private CacheAccess<TileZXY, ImageCache> cache;
    private int zoom;
    private Consumer<TileZXY> notifier;

    /**
     * Constructs a new {@code StyledMapRenderer}.
     *
     * @param g              the graphics context. Must not be null.
     * @param nc             the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     *                       look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException if {@code g} is null
     * @throws IllegalArgumentException if {@code nc} is null
     */
    public StyledTiledMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);
    }

    @Override
    public void render(OsmData<?, ?, ?, ?> data, boolean renderVirtualNodes, Bounds bounds) {
        // If there is no cache, fall back to old behavior
        if (this.cache == null) {
            super.render(data, renderVirtualNodes, bounds);
            return;
        }
        final Executor worker = MainApplication.worker;
        final BufferedImage tempImage;
        final Graphics2D tempG2d;
        // I'd like to avoid two image copies, but there are some issues using the original g2d object
        tempImage = nc.getGraphicsConfiguration().createCompatibleImage(this.nc.getWidth(), this.nc.getHeight(), Transparency.TRANSLUCENT);
        tempG2d = tempImage.createGraphics();
        tempG2d.setComposite(AlphaComposite.DstAtop); // Avoid tile lines in large areas

        final List<TileZXY> toRender = TileZXY.boundsToTiles(bounds.getMinLat(), bounds.getMinLon(),
                bounds.getMaxLat(), bounds.getMaxLon(), zoom).collect(Collectors.toList());
        final Bounds box = new Bounds(bounds);
        toRender.stream().map(TileZXY::tileToBounds).forEach(box::extend);
        final int tileSize;
        if (toRender.isEmpty()) {
            tileSize = Config.getPref().getInt("mappaint.fast_render.tile_size", 256); // Mostly to keep the compiler happy
        } else {
            final TileZXY tile = toRender.get(0);
            final Bounds box2 = TileZXY.tileToBounds(tile);
            final Point min = this.nc.getPoint(box2.getMin());
            final Point max = this.nc.getPoint(box2.getMax());
            tileSize = max.x - min.x + BUFFER_PIXELS;
        }

        // Sort the tiles based off of proximity to the mouse pointer
        if (nc instanceof MapView) { // Ideally this would either be an interface or a method in NavigableComponent
            final MapView mv = (MapView) nc;
            final MouseEvent mouseEvent = mv.lastMEvent;
            final LatLon mousePosition = nc.getLatLon(mouseEvent.getX(), mouseEvent.getY());
            final TileZXY mouseTile = TileZXY.latLonToTile(mousePosition.lat(), mousePosition.lon(), zoom);
            toRender.sort(Comparator.comparingInt(tile -> {
                final int x = tile.x() - mouseTile.x();
                final int y = tile.y() - mouseTile.y();
                return x * x + y * y;
            }));
        }

        // We want to prioritize where the mouse is, but having some in the queue will reduce overall paint time
        int submittedTile = 5;
        int painted = 0;
        for (TileZXY tile : toRender) {
            final Image tileImage;
            // Needed to avoid having tiles that aren't rendered properly
            final ImageCache tImg = this.cache.get(tile);
            final boolean wasDirty = tImg != null && tImg.isDirty();
            if (tImg != null && !tImg.isDirty() && tImg.imageFuture() != null) {
                submittedTile = 0; // Don't submit new tiles if there are futures already in the queue. Not perfect.
            }
            if (submittedTile > 0 && (tImg == null || tImg.isDirty())) {
                // Ensure that we don't add a large number of render calls
                if (tImg != null && tImg.imageFuture() != null) {
                    tImg.imageFuture().cancel();
                }
                submittedTile--;
                // Note that the paint code is *not* thread safe, so all tiles must be painted on the same thread.
                // FIXME figure out how to make this thread safe? Probably not necessary, since UI isn't blocked, but it would be a nice to have
                TileLoader loader = new TileLoader(data, tile, tileSize, new ArrayList<>());
                worker.execute(loader);
                if (tImg == null) {
                    this.cache.put(tile, new ImageCache(null, loader, false));
                } else {
                    // This might cause some extra renders, but *probably* ok
                    this.cache.put(tile, new ImageCache(tImg.image(), loader, true));
                }
                tileImage = tImg != null ? tImg.image() : null;
            } else if (tImg != null) {
                tileImage = tImg.image();
            } else {
                tileImage = null;
            }
            final Point point = this.nc.getPoint(tile);
            if (tileImage != null) {
                if ((wasDirty && Logging.isTraceEnabled()) || this.isInactiveMode) {
                    tempG2d.setColor(Color.DARK_GRAY);
                    tempG2d.fillRect(point.x, point.y, tileSize, tileSize);
                } else {
                    painted++;
                }
                // There seems to be an off-by-one error somewhere.
                tempG2d.drawImage(tileImage, point.x + 1, point.y + 1, null, null);
            } else {
                Logging.trace("StyledMapRenderer did not paint tile {1}", tile);
            }
        }
        // Force another render pass if there may be more tiles to render
        if (submittedTile <= 0) {
            worker.execute(nc::invalidate);
        }
        final double percentDrawn = 100 * painted / (double) toRender.size();
        if (percentDrawn < 99.99) {
            final int x = 0;
            final int y = nc.getHeight() / 8;
            final String message = tr("Rendering Status: {0}%", Math.floor(percentDrawn));
            tempG2d.setComposite(AlphaComposite.SrcOver);
            tempG2d.setFont(new Font("sansserif", Font.BOLD, 13));
            tempG2d.setColor(Color.BLACK);
            tempG2d.drawString(message, x + 1, y);
            tempG2d.setColor(Color.LIGHT_GRAY);
            tempG2d.drawString(message, x, y);
        }
        tempG2d.dispose();
        g.drawImage(tempImage, 0, 0, null);
    }

    /**
     * Set the cache for this painter. If not set, this acts like {@link StyledMapRenderer}.
     * @param box The box we will be rendering -- any jobs for tiles outside of this box will be cancelled
     * @param cache The cache to use
     * @param zoom The zoom level to use for creating the tiles
     * @param notifier The method to call when a tile has been updated. This may or may not be called in the EDT.
     */
    public void setCache(Bounds box, CacheAccess<TileZXY, ImageCache> cache, int zoom, Consumer<TileZXY> notifier) {
        this.cache = cache;
        this.zoom = zoom;
        this.notifier = notifier != null ? notifier : tile -> { /* Do nothing */ };

        Set<TileZXY> tiles = TileZXY.boundsToTiles(box.getMinLat(), box.getMinLon(), box.getMaxLat(), box.getMaxLon(), zoom)
                .collect(Collectors.toSet());
        cache.getMatching(".*").forEach((key, value) -> {
            if (!tiles.contains(key)) {
                cancelImageFuture(cache, key, value);
            }
        });
    }

    /**
     * Cancel a job for a tile
     * @param cache The cache with the job
     * @param key The tile key
     * @param value The {@link ImageCache} to remove and cancel
     */
    private static void cancelImageFuture(CacheAccess<TileZXY, ImageCache> cache, TileZXY key, ImageCache value) {
        if (value.imageFuture() != null) {
            value.imageFuture().cancel();
            if (value.image() == null) {
                cache.remove(key);
            } else {
                cache.put(key, new ImageCache(value.image(), null, value.isDirty()));
            }
        }
    }

    /**
     * Generate tile images
     * @param data The data to generate tiles from
     * @param tiles The collection of tiles to generate (note: there is currently a bug with multiple tiles)
     * @param tileSize The size of the tile image
     * @return The image for the tiles passed in
     */
    private BufferedImage generateTiles(OsmData<?, ?, ?, ?> data, Collection<TileZXY> tiles, int tileSize) {
        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("tiles cannot be empty");
        }
        // We need to know how large of an area we are rendering; we get the min x/y and max x/y in order to get the
        // number of tiles in the x/y directions we are rendering.
        final IntSummaryStatistics xStats = tiles.stream().mapToInt(TileZXY::x).distinct().summaryStatistics();
        final IntSummaryStatistics yStats = tiles.stream().mapToInt(TileZXY::y).distinct().summaryStatistics();
        final int xCount = xStats.getMax() - xStats.getMin() + 1; // inclusive
        final int yCount = yStats.getMax() - yStats.getMin() + 1; // inclusive
        final int width = tileSize * (2 * BUFFER_TILES + xCount);
        final int height = tileSize * (2 * BUFFER_TILES + yCount);
        // getWidth and getHeight are called in the constructor; Java 22 will let us call super after we set variables.
        final NavigatableComponent temporaryView = new NavigatableComponent() {
            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }
        };
        // These bounds are used to set the render area; it includes the buffer area.
        final Bounds bounds = generateRenderArea(tiles);

        temporaryView.zoomTo(bounds.getCenter().getEastNorth(ProjectionRegistry.getProjection()), mapState.getScale());
        BufferedImage bufferedImage = Optional.ofNullable(nc.getGraphicsConfiguration())
                .map(gc -> gc.createCompatibleImage(tileSize * xCount + xCount, tileSize * yCount + xCount, Transparency.TRANSLUCENT))
                .orElseGet(() -> new BufferedImage(tileSize * xCount + xCount, tileSize * yCount + xCount, BufferedImage.TYPE_INT_ARGB));
        Graphics2D g2d = bufferedImage.createGraphics();
        try {
            g2d.setRenderingHints(Map.of(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
            g2d.setTransform(AffineTransform.getTranslateInstance(-BUFFER_TILES * (double) tileSize, -BUFFER_TILES * (double) tileSize));
            final AbstractMapRenderer tilePainter = MapRendererFactory.getInstance().createActiveRenderer(g2d, temporaryView, false);
            tilePainter.render(data, true, bounds);
        } finally {
            g2d.dispose();
        }
        return bufferedImage;
    }

    /**
     * Generate the area for rendering
     * @param tiles The tiles that we want to render
     * @return The generated render area with {@link #BUFFER_TILES} on all sides.
     */
    private static Bounds generateRenderArea(Collection<TileZXY> tiles) {
        Bounds bounds = null;
        for (TileZXY tile : tiles) {
            if (bounds == null) {
                bounds = TileZXY.tileToBounds(tile);
            }
            bounds.extend(TileZXY.tileToBounds(new TileZXY(tile.zoom(), tile.x() - BUFFER_TILES, tile.y() - BUFFER_TILES)));
            bounds.extend(TileZXY.tileToBounds(new TileZXY(tile.zoom(), tile.x() + BUFFER_TILES, tile.y() + BUFFER_TILES)));
        }
        return Objects.requireNonNull(bounds);
    }

    /**
     * A loader for tiles
     */
    class TileLoader implements Runnable {
        private final TileZXY tile;
        private final int tileSize;
        private final OsmData<?, ?, ?, ?> data;
        private boolean cancel;
        private final Collection<TileLoader> tileCollection;
        private boolean done;

        /**
         * Create a new tile loader
         * @param data The data to use for painting
         * @param tile The tile this tile loader is for
         * @param tileSize The expected size of this tile
         * @param tileCollection The collection of tiles that this tile is being rendered with (for batching)
         */
        TileLoader(OsmData<?, ?, ?, ?> data, TileZXY tile, int tileSize, Collection<TileLoader> tileCollection) {
            this.data = data;
            this.tile = tile;
            this.tileSize = tileSize;
            this.tileCollection = tileCollection;
            this.tileCollection.add(this);
        }

        @Override
        public void run() {
            if (!cancel) {
                synchronized (tileCollection) {
                    if (!done) {
                        final BufferedImage tImage = generateTiles(data,
                                tileCollection.stream().map(t -> t.tile).collect(Collectors.toList()), tileSize);
                        final int minX = tileCollection.stream().map(t -> t.tile).mapToInt(TileZXY::x).min().orElse(this.tile.x());
                        final int minY = tileCollection.stream().map(t -> t.tile).mapToInt(TileZXY::y).min().orElse(this.tile.y());
                        for (TileLoader loader : tileCollection) {
                            final TileZXY txy = loader.tile;
                            final int x = (txy.x() - minX) * (tileSize - BUFFER_PIXELS) + BUFFER_PIXELS / 2;
                            final int y = (txy.y() - minY) * (tileSize - BUFFER_PIXELS) + BUFFER_PIXELS / 2;
                            final int wh = tileSize - BUFFER_PIXELS / 2;

                            final BufferedImage tileImage = tImage.getSubimage(x, y, wh, wh);
                            loader.cacheTile(tileImage);
                        }
                    }
                }
            }
        }

        /**
         * Finish a tile generation job
         * @param tImage The tile image for this job
         */
        private synchronized void cacheTile(BufferedImage tImage) {
            cache.put(tile, new ImageCache(tImage, null, false));
            done = true;
            notifier.accept(tile);
        }

        /**
         * Cancel this job without causing a {@link java.util.concurrent.CancellationException}
         */
        void cancel() {
            this.cancel = true;
        }
    }
}
