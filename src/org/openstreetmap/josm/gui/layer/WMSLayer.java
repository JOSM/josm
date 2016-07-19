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
import javax.swing.JOptionPane;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
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
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 *
 */
public class WMSLayer extends AbstractCachedTileSourceLayer<TemplatedWMSTileSource> {
    private static final String PREFERENCE_PREFIX = "imagery.wms";
    /**
     * Registers all setting properties
     */
    static {
        new TileSourceDisplaySettings(PREFERENCE_PREFIX);
    }

    /** default tile size for WMS Layer */
    public static final IntegerProperty PROP_IMAGE_SIZE = new IntegerProperty(PREFERENCE_PREFIX + ".imageSize", 512);

    /** should WMS layer autozoom in default mode */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty(PREFERENCE_PREFIX + ".default_autozoom", true);

    private static final String CACHE_REGION_NAME = "WMS";

    private final Set<String> supportedProjections;

    /**
     * Constructs a new {@code WMSLayer}.
     * @param info ImageryInfo description of the layer
     */
    public WMSLayer(ImageryInfo info) {
        super(info);
        this.supportedProjections = new TreeSet<>(info.getServerProjections());
    }

    @Override
    protected TileSourceDisplaySettings createDisplaySettings() {
        return new TileSourceDisplaySettings(PREFERENCE_PREFIX);
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
    protected TemplatedWMSTileSource getTileSource(ImageryInfo info) {
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
    protected Map<String, String> getHeaders(TemplatedWMSTileSource tileSource) {
        return tileSource.getHeaders();
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        return supportedProjections == null || supportedProjections.isEmpty() || supportedProjections.contains(proj.toCode()) ||
                (info.isEpsg4326To3857Supported() && supportedProjections.contains("EPSG:4326")
                        && "EPSG:3857".equals(Main.getProjection().toCode()));
    }

    @Override
    public String nameSupportedProjections() {
        StringBuilder ret = new StringBuilder();
        for (String e: supportedProjections) {
            ret.append(e).append(", ");
        }
        String appendix = "";

        if (isReprojectionPossible()) {
            appendix = ". <p>" + tr("JOSM will use EPSG:4326 to query the server, but results may vary "
                    + "depending on the WMS server") + "</p>";
        }
        return ret.substring(0, ret.length()-2) + appendix;
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        // do not call super - we need custom warning dialog

        if (!isProjectionSupported(newValue)) {
            String message =
                    "<html><body><p>" + tr("The layer {0} does not support the new projection {1}.", getName(), newValue.toCode()) +
                    "<p style='width: 450px; position: absolute; margin: 0px;'>" +
                            tr("Supported projections are: {0}", nameSupportedProjections()) + "</p>" +
                    "<p>" + tr("Change the projection again or remove the layer.");

            ExtendedDialog warningDialog = new ExtendedDialog(Main.parent, tr("Warning"), new String[]{tr("OK")}).
                    setContent(message).
                    setIcon(JOptionPane.WARNING_MESSAGE);

            if (isReprojectionPossible()) {
                warningDialog.toggleEnable("imagery.wms.projectionSupportWarnings." + tileSource.getBaseUrl());
            }
            warningDialog.showDialog();
        }

        if (!newValue.equals(oldValue)) {
            tileSource.initProjection(newValue);
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

    private boolean isReprojectionPossible() {
        return supportedProjections.contains("EPSG:4326") && "EPSG:3857".equals(Main.getProjection().toCode());
    }
}
