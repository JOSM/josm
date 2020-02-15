// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.AbstractCachedTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * An extension of {@link JMapViewer} that implements JOSM-specific tile loading mechanisms.
 * @since 15145
 */
public class JosmMapViewer extends JMapViewer {

    /**
     * A list of tile sources that can be used for displaying the map.
     */
    @FunctionalInterface
    public interface TileSourceProvider {
        /**
         * Gets the tile sources that can be displayed
         * @return The tile sources
         */
        List<TileSource> getTileSources();
    }

    /**
     * TileSource provider.
     */
    public abstract static class AbstractImageryInfoBasedTileSourceProvider implements TileSourceProvider {
        /**
         * Returns the list of imagery infos backing tile sources.
         * @return the list of imagery infos backing tile sources
         */
        public abstract List<ImageryInfo> getImageryInfos();

        @Override
        public List<TileSource> getTileSources() {
            if (!TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get()) return Collections.<TileSource>emptyList();
            return imageryInfosToTileSources(getImageryInfos());
        }
    }

    static List<TileSource> imageryInfosToTileSources(List<ImageryInfo> imageryInfos) {
        List<TileSource> sources = new ArrayList<>();
        for (ImageryInfo info : imageryInfos) {
            try {
                TileSource source = TMSLayer.getTileSourceStatic(info);
                if (source != null) {
                    sources.add(source);
                }
            } catch (IllegalArgumentException ex) {
                Logging.trace(ex);
                Logging.warn(ex.getMessage());
                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    new Notification(ex.getMessage()).setIcon(JOptionPane.WARNING_MESSAGE).show();
                }
            }
        }
        return sources;
    }

    /**
     * TileSource provider - providing default OSM tile source
     */
    public static class DefaultOsmTileSourceProvider implements TileSourceProvider {

        protected static final StringProperty DEFAULT_OSM_TILE_URL = new StringProperty(
                "default.osm.tile.source.url", "https://{switch:a,b,c}.tile.openstreetmap.org/{zoom}/{x}/{y}.png");

        @Override
        public List<TileSource> getTileSources() {
            List<TileSource> result = imageryInfosToTileSources(ImageryLayerInfo.instance.getLayers().stream()
                   .filter(l -> l.getUrl().equals(DEFAULT_OSM_TILE_URL.get())).collect(Collectors.toList()));
            if (result.isEmpty()) {
                result.add(new OsmTileSource.Mapnik());
            }
            return result;
        }

        /**
         * Returns the default OSM tile source.
         * @return the default OSM tile source
         */
        public static TileSource get() {
            return new DefaultOsmTileSourceProvider().getTileSources().get(0);
        }
    }

    /**
     * TileSource provider - providing sources from imagery sources menu
     */
    public static class TMSTileSourceProvider extends AbstractImageryInfoBasedTileSourceProvider {
        @Override
        public List<ImageryInfo> getImageryInfos() {
            return ImageryLayerInfo.instance.getLayers();
        }
    }

    /**
     * TileSource provider - providing sources from current layers
     */
    public static class CurrentLayersTileSourceProvider extends AbstractImageryInfoBasedTileSourceProvider {
        @Override
        public List<ImageryInfo> getImageryInfos() {
            return MainApplication.getLayerManager().getLayers().stream().filter(
                layer -> layer instanceof ImageryLayer
            ).map(
                layer -> ((ImageryLayer) layer).getInfo()
            ).collect(Collectors.toList());
        }
    }

    protected final transient TileLoader cachedLoader;
    protected final transient OsmTileLoader uncachedLoader;

    /**
     * Constructs a new {@code JosmMapViewer}.
     */
    public JosmMapViewer() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Version.getInstance().getFullAgentString());

        TileLoaderFactory cachedLoaderFactory = AbstractCachedTileSourceLayer.getTileLoaderFactory("TMS", TMSCachedTileLoader.class);
        if (cachedLoaderFactory != null) {
            cachedLoader = cachedLoaderFactory.makeTileLoader(this, headers, TimeUnit.HOURS.toSeconds(1));
        } else {
            cachedLoader = null;
        }

        uncachedLoader = new OsmTileLoader(this);
        uncachedLoader.headers.putAll(headers);
        setFileCacheEnabled(true);
    }

    /**
     * Enables the disk tile cache.
     * @param enabled true to enable, false to disable
     */
    public final void setFileCacheEnabled(boolean enabled) {
        if (enabled && cachedLoader != null) {
            setTileLoader(cachedLoader);
        } else {
            setTileLoader(uncachedLoader);
        }
    }

    /**
     * Sets the maximum number of tiles that may be held in memory
     * @param tiles The maximum number of tiles.
     */
    public final void setMaxTilesInMemory(int tiles) {
        ((MemoryTileCache) getTileCache()).setCacheSize(tiles);
    }
}
