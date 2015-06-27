// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.ScanexTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.data.imagery.CachedAttributionBingAerialTileSource;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * Class that displays a slippy map layer.
 *
 * @author Frederik Ramm
 * @author LuVar &lt;lubomir.varga@freemap.sk&gt;
 * @author Dave Hansen &lt;dave@sr71.net&gt;
 * @author Upliner &lt;upliner@gmail.com&gt;
 *
 */
public class TMSLayer extends AbstractTileSourceLayer {
    private static final String PREFERENCE_PREFIX = "imagery.tms";

    /** minimum zoom level for TMS layer */
    public static final IntegerProperty PROP_MIN_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".min_zoom_lvl",
            AbstractTileSourceLayer.PROP_MIN_ZOOM_LVL.get());
    /** maximum zoom level for TMS layer */
    public static final IntegerProperty PROP_MAX_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".max_zoom_lvl",
            AbstractTileSourceLayer.PROP_MAX_ZOOM_LVL.get());
    /** shall TMS layers be added to download dialog */
    public static final BooleanProperty PROP_ADD_TO_SLIPPYMAP_CHOOSER = new BooleanProperty(PREFERENCE_PREFIX + ".add_to_slippymap_chooser",
            true);

    /** loader factory responsible for loading tiles for this layer */
    public static TileLoaderFactory loaderFactory = new CachedTileLoaderFactory("TMS"){

        @Override
        protected TileLoader getLoader(TileLoaderListener listener, String cacheName, int connectTimeout,
                int readTimeout, Map<String, String> headers, String cacheDir) throws IOException {
            return new TMSCachedTileLoader(listener, cacheName, connectTimeout, readTimeout, headers, cacheDir);
        }

    };

    /**
     * Create a layer based on ImageryInfo
     * @param info description of the layer
     */
    public TMSLayer(ImageryInfo info) {
        super(info);
    }

    /**
     * Plugins that wish to set custom tile loader should call this method
     * @param newLoaderFactory that will be used to load tiles
     */

    public static void setTileLoaderFactory(TileLoaderFactory newLoaderFactory) {
        loaderFactory = newLoaderFactory;
    }

    @Override
    protected TileLoaderFactory getTileLoaderFactory() {
        return loaderFactory;
    }

    @Override
    protected Map<String, String> getHeaders(TileSource tileSource) {
        if (tileSource instanceof TemplatedTMSTileSource) {
            return ((TemplatedTMSTileSource) tileSource).getHeaders();
        }
        return null;
    }

    /**
     * Creates and returns a new TileSource instance depending on the {@link ImageryType}
     * of the passed ImageryInfo object.
     *
     * If no appropriate TileSource is found, null is returned.
     * Currently supported ImageryType are {@link ImageryType#TMS},
     * {@link ImageryType#BING}, {@link ImageryType#SCANEX}.
     *
     *
     * @param info imagery info
     * @return a new TileSource instance or null if no TileSource for the ImageryInfo/ImageryType could be found.
     * @throws IllegalArgumentException if url from imagery info is null or invalid
     */
    @Override
    protected TileSource getTileSource(ImageryInfo info) throws IllegalArgumentException {
        return getTileSourceStatic(info);
    }

    /**
     * Adds a context menu to the mapView.
     */

    @Override
    public final boolean isProjectionSupported(Projection proj) {
        return "EPSG:3857".equals(proj.toCode()) || "EPSG:4326".equals(proj.toCode());
    }

    @Override
    public final String nameSupportedProjections() {
        return tr("EPSG:4326 and Mercator projection are supported");
    }

    /**
     * Creates and returns a new TileSource instance depending on the {@link ImageryType}
     * of the passed ImageryInfo object.
     *
     * If no appropriate TileSource is found, null is returned.
     * Currently supported ImageryType are {@link ImageryType#TMS},
     * {@link ImageryType#BING}, {@link ImageryType#SCANEX}.
     *
     * @param info imagery info
     * @return a new TileSource instance or null if no TileSource for the ImageryInfo/ImageryType could be found.
     * @throws IllegalArgumentException if url from imagery info is null or invalid
     */
    public static TileSource getTileSourceStatic(ImageryInfo info) throws IllegalArgumentException {
        if (info.getImageryType() == ImageryType.TMS) {
            TemplatedTMSTileSource.checkUrl(info.getUrl());
            TMSTileSource t = new TemplatedTMSTileSource(info);
            info.setAttribution(t);
            return t;
        } else if (info.getImageryType() == ImageryType.BING)
            return new CachedAttributionBingAerialTileSource(info);
        else if (info.getImageryType() == ImageryType.SCANEX) {
            return new ScanexTileSource(info);
        }
        return null;
    }



}
