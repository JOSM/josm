// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.WMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * WMTS layer based on AbstractTileSourceLayer. Overrides few methods to align WMTS to Tile based computations
 * but most magic is done within WMTSTileSource class.
 *
 * Full specification of the protocol available at:
 * http://www.opengeospatial.org/standards/wmts
 *
 * @author Wiktor NiesiobÄ™dzki
 *
 */
public class WMTSLayer extends AbstractCachedTileSourceLayer<WMTSTileSource> implements NativeScaleLayer {
    private static final String PREFERENCE_PREFIX = "imagery.wmts";

    /**
     * Registers all setting properties
     */
    static {
        new TileSourceDisplaySettings(PREFERENCE_PREFIX);
    }

    private static final String CACHE_REGION_NAME = "WMTS";

    /**
     * Creates WMTS layer from ImageryInfo
     * @param info Imagery Info describing the layer
     */
    public WMTSLayer(ImageryInfo info) {
        super(info);
    }

    @Override
    protected TileSourceDisplaySettings createDisplaySettings() {
        return new TileSourceDisplaySettings(PREFERENCE_PREFIX);
    }

    @Override
    protected WMTSTileSource getTileSource() {
        try {
            if (info.getImageryType() == ImageryType.WMTS && info.getUrl() != null) {
                WMTSTileSource.checkUrl(info.getUrl());
                WMTSTileSource tileSource = new WMTSTileSource(info);
                info.setAttribution(tileSource);
                return tileSource;
            }
            return null;
        } catch (IOException e) {
            Logging.warn(e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int getBestZoom() {
        if (!Main.isDisplayingMapView())
            return 0;
        ScaleList scaleList = getNativeScales();
        if (scaleList == null) {
            return getMaxZoomLvl();
        }
        double displayScale = Main.map.mapView.getScale();
        if (coordinateConverter.requiresReprojection()) {
            displayScale *= Main.getProjection().getMetersPerUnit();
        }
        Scale snap = scaleList.getSnapScale(displayScale, false);
        return Utils.clamp(snap != null ? snap.getIndex() : getMaxZoomLvl(),
                getMinZoomLvl(), getMaxZoomLvl());
    }

    @Override
    protected int getMinZoomLvl() {
        return 0;
    }

    @Override
    public Collection<String> getNativeProjections() {
        return tileSource.getSupportedProjections();
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);
        tileSource.initProjection(newValue);
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

    @Override
    public ScaleList getNativeScales() {
        return tileSource.getNativeScales();
    }
}
