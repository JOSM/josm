// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.TemplatedWMSTileSource;
import org.openstreetmap.josm.data.imagery.WMSCachedTileLoader;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 *
 */
public class WMSLayer extends AbstractCachedTileSourceLayer {
    private static final String PREFERENCE_PREFIX   = "imagery.wms.";

    /** default tile size for WMS Layer */
    public static final IntegerProperty PROP_IMAGE_SIZE = new IntegerProperty(PREFERENCE_PREFIX + "imageSize", 512);

    /** should WMS layer autozoom in default mode */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty(PREFERENCE_PREFIX + "default_autozoom", true);

    /** limit of concurrent connections to WMS tile source (per source) */
    public static final IntegerProperty THREAD_LIMIT = new IntegerProperty(PREFERENCE_PREFIX + "simultaneousConnections", 3);

    private static final String CACHE_REGION_NAME = "WMS";

    private Set<String> supportedProjections;

    /**
     * Constructs a new {@code WMSLayer}.
     * @param info ImageryInfo description of the layer
     */
    public WMSLayer(ImageryInfo info) {
        super(info);
        this.supportedProjections = new TreeSet<>(info.getServerProjections());
        this.autoZoom = PROP_DEFAULT_AUTOZOOM.get();

    }

    @Override
    public Action[] getMenuEntries() {
        List<Action> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(super.getMenuEntries()));
        ret.add(SeparatorLayerAction.INSTANCE);
        ret.add(new LayerSaveAction(this));
        ret.add(new LayerSaveAsAction(this));
        ret.add(new BookmarkWmsAction());
        return ret.toArray(new Action[]{});
    }

    @Override
    protected AbstractTMSTileSource getTileSource(ImageryInfo info) {
        if (info.getImageryType() == ImageryType.WMS && info.getUrl() != null) {
            TemplatedWMSTileSource.checkUrl(info.getUrl());
            TemplatedWMSTileSource tileSource = new TemplatedWMSTileSource(info);
            info.setAttribution(tileSource);
            return tileSource;
        }
        return null;
    }

    /**
     * This action will add a WMS layer menu entry with the current WMS layer
     * URL and name extended by the current resolution.
     * When using the menu entry again, the WMS cache will be used properly.
     */
    public class BookmarkWmsAction extends AbstractAction {
        /**
         * Constructs a new {@code BookmarkWmsAction}.
         */
        public BookmarkWmsAction() {
            super(tr("Set WMS Bookmark"));
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            ImageryLayerInfo.addLayer(new ImageryInfo(info));
        }
    }

    @Override
    protected Map<String, String> getHeaders(TileSource tileSource) {
        if (tileSource instanceof TemplatedWMSTileSource) {
            return ((TemplatedWMSTileSource) tileSource).getHeaders();
        }
        return null;
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        return supportedProjections == null || supportedProjections.isEmpty() || supportedProjections.contains(proj.toCode());
    }

    @Override
    public String nameSupportedProjections() {
        StringBuilder ret = new StringBuilder();
        for (String e: supportedProjections) {
            ret.append(e).append(", ");
        }
        String appendix = "";

        if (supportedProjections.contains("EPSG:4326") &&  "EPSG:3857".equals(Main.getProjection().toCode())) {
            appendix = ". " + tr("JOSM will use EPSG:4326 to query the server, but results may vary "
                    + "depending on the WMS server");
        }
        return ret.substring(0, ret.length()-2) + appendix;
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);

        if (!newValue.equals(oldValue) && tileSource instanceof TemplatedWMSTileSource) {
            ((TemplatedWMSTileSource) tileSource).initProjection(newValue);
        }
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
     * @return cache region for WMS layer
     */
    public static CacheAccess<String, BufferedImageCacheEntry> getCache() {
        return AbstractCachedTileSourceLayer.getCache(CACHE_REGION_NAME);
    }
}
