// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
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
public class WMTSLayer extends AbstractCachedTileSourceLayer {
    /**
     * default setting of autozoom per layer
     */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty("imagery.wmts.default_autozoom", true);
    private static final String CACHE_REGION_NAME = "WMTS";


    /**
     * Creates WMTS layer from ImageryInfo
     * @param info Imagery Info describing the layer
     */
    public WMTSLayer(ImageryInfo info) {
        super(info);
        autoZoom = PROP_DEFAULT_AUTOZOOM.get();
    }

    @Override
    protected AbstractTMSTileSource getTileSource(ImageryInfo info) {
        try {
            if (info.getImageryType() == ImageryType.WMTS && info.getUrl() != null) {
                WMTSTileSource.checkUrl(info.getUrl());
                WMTSTileSource tileSource = new WMTSTileSource(info);
                info.setAttribution(tileSource);
                return tileSource;
            }
            return null;
        } catch (IOException e) {
            Main.warn(e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @param zoom level of the tile
     * @return how many pixels of the screen occupies one pixel of the tile
     */
    private double getTileToScreenRatio(int zoom) {
         MapView mv = Main.map.mapView;
         LatLon topLeft = mv.getLatLon(0, 0);
         LatLon botLeft = mv.getLatLon(0, tileSource.getTileSize());

         TileXY topLeftTile = tileSource.latLonToTileXY(topLeft.toCoordinate(), zoom);

         ICoordinate north = tileSource.tileXYToLatLon(topLeftTile.getXIndex(), topLeftTile.getYIndex(), zoom);
         ICoordinate south = tileSource.tileXYToLatLon(topLeftTile.getXIndex(), topLeftTile.getYIndex() + 1, zoom);

         return Math.abs((north.getLat() - south.getLat()) / (topLeft.lat() - botLeft.lat()));
    }

    @Override
    protected int getBestZoom() {
        if (!Main.isDisplayingMapView()) return 1;

        for (int i = getMinZoomLvl() + 1; i <= getMaxZoomLvl(); i++) {
            double ret = getTileToScreenRatio(i);
            if (ret < 1) {
                return i - 1;
            }
        }
        return getMaxZoomLvl();
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        Set<String> supportedProjections = ((WMTSTileSource) tileSource).getSupportedProjections();
        return supportedProjections.contains(proj.toCode());
    }

    @Override
    public String nameSupportedProjections() {
        StringBuilder ret = new StringBuilder();
        for (String e: ((WMTSTileSource) tileSource).getSupportedProjections()) {
            ret.append(e).append(", ");
        }
        return ret.length() > 2 ? ret.substring(0, ret.length()-2) : ret.toString();
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);
        ((WMTSTileSource) tileSource).initProjection(newValue);
    }

    @Override
    protected Class<? extends TileLoader> getTileLoaderClass() {
        return WMSCachedTileLoader.class;
    }

    @Override
    protected String getCacheName() {
        return CACHE_REGION_NAME;
    }

    /**
     * @return cache region for WMTS layer
     */
    public static CacheAccess<String, BufferedImageCacheEntry> getCache() {
        return AbstractCachedTileSourceLayer.getCache(CACHE_REGION_NAME);
    }
}
