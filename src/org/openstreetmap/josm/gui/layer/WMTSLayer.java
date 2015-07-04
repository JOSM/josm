// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.io.IOException;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.imagery.WMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;

/**
 * WMTS layer based on AbstractTileSourceLayer. Overrides few methods to align WMTS to Tile based computations
 * but most magic is done within WMTSTileSource class.
 *
 * Full specification of the protocol available at:
 * http://www.opengeospatial.org/standards/wmts
 *
 * @author Wiktor Niesiobędzki
 *
 */
public class WMTSLayer extends AbstractTileSourceLayer {
    /**
     * default setting of autozoom per layer
     */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty("imagery.wmts.default_autozoom", true);


    /**
     * Creates WMTS layer from ImageryInfo
     * @param info Imagery Info describing the layer
     */
    public WMTSLayer(ImageryInfo info) {
        super(info);
    }

    private static TileLoaderFactory loaderFactory = new CachedTileLoaderFactory("WMTS") {
        @Override
        protected TileLoader getLoader(TileLoaderListener listener, String cacheName, int connectTimeout,
                int readTimeout, Map<String, String> headers, String cacheDir) throws IOException {
            return new WMSCachedTileLoader(listener, cacheName, connectTimeout, readTimeout, headers, cacheDir);
        }

    };

    @Override
    protected TileLoaderFactory getTileLoaderFactory() {
        return loaderFactory;
    }

    @Override
    protected TileSource getTileSource(ImageryInfo info) {
        try {
            if (info.getImageryType() == ImageryType.WMTS && info.getUrl() != null) {
                WMTSTileSource.checkUrl(info.getUrl());
                WMTSTileSource tileSource = new WMTSTileSource(info);
                info.setAttribution(tileSource);
                return tileSource;
            }
        } catch (Exception e) {
            Main.warn("Could not create imagery layer:");
            Main.warn(e);
        }
        return null;
    }

    /**
     * @param zoom level of the tile
     * @return how many pixels of the screen occupies one pixel of the tile
     */
    private double getTileToScreenRatio(int zoom) {
         ICoordinate north = tileSource.tileXYToLatLon(0, 0, zoom);
         ICoordinate south = tileSource.tileXYToLatLon(0, 1, zoom);

         MapView mv = Main.map.mapView;
         LatLon topLeft = mv.getLatLon(0, 0);
         LatLon botLeft = mv.getLatLon(0, tileSource.getTileSize());

         return Math.abs((north.getLat() - south.getLat()) / ( topLeft.lat() - botLeft.lat()));
    }

    @Override
    protected int getBestZoom() {
        if (!Main.isDisplayingMapView()) return 1;

        for(int i=getMinZoomLvl(); i <= getMaxZoomLvl(); i++) {
            double ret = getTileToScreenRatio(i);
            if (ret < 1) {
                return i;
            }
        }
        return getMaxZoomLvl();
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        return ((WMTSTileSource)tileSource).getSupportedProjections().contains(proj.toCode());
    }

    @Override
    public String nameSupportedProjections() {
        StringBuilder ret = new StringBuilder();
        for (String e: ((WMTSTileSource)tileSource).getSupportedProjections()) {
            ret.append(e).append(", ");
        }
        return ret.substring(0, ret.length()-2);
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);
        ((WMTSTileSource)tileSource).initProjection(newValue);
    }

}
