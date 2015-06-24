// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.TemplatedWMSTileSource;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.imagery.WMSCachedTileLoader;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 *
 */
public class WMSLayer extends AbstractTileSourceLayer {
    /** default tile size for WMS Layer */
    public static final IntegerProperty PROP_IMAGE_SIZE = new IntegerProperty("imagery.wms.imageSize", 512);
    /** should WMS layer autozoom in default mode */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty("imagery.wms.default_autozoom", true);

    /**
     * Constructs a new {@code WMSLayer}.
     * @param info ImageryInfo description of the layer
     */
    public WMSLayer(ImageryInfo info) {
        super(info);
    }

    @Override
    public void hookUpMapView() {
        super.hookUpMapView();
        final ProjectionChangeListener listener = new ProjectionChangeListener() {
            @Override
            public void projectionChanged(Projection oldValue, Projection newValue) {
                if (!oldValue.equals(newValue) && tileSource instanceof TemplatedWMSTileSource) {
                    ((TemplatedWMSTileSource)tileSource).initProjection(newValue);
                }

            }
        };
        Main.addProjectionChangeListener(listener);

        MapView.addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                // empty
            }

            @Override
            public void layerAdded(Layer newLayer) {
                // empty
            }

            @Override
            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == WMSLayer.this) {
                    Main.removeProjectionChangeListener(listener);
                }
            }
        });
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
    protected TileSource getTileSource(ImageryInfo info) throws IllegalArgumentException {
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


    /**
     * Checks that WMS layer is a grabber-compatible one (HTML or WMS).
     * @throws IllegalStateException if imagery time is neither HTML nor WMS
     * @since 8068
     */
    public void checkGrabberType() {
    }

    private static TileLoaderFactory loaderFactory = new CachedTileLoaderFactory("WMS") {
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
    protected Map<String, String> getHeaders(TileSource tileSource) {
        if (tileSource instanceof TemplatedWMSTileSource) {
            return ((TemplatedWMSTileSource)tileSource).getHeaders();
        }
        return null;
    }
}
