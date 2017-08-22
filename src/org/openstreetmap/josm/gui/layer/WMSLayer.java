// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.imagery.AbstractWMSTileSource;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.TemplatedWMSTileSource;
import org.openstreetmap.josm.data.imagery.WMSCachedTileLoader;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 *
 */
public class WMSLayer extends AbstractCachedTileSourceLayer<AbstractWMSTileSource> {
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

    private final List<String> serverProjections;

    /**
     * Constructs a new {@code WMSLayer}.
     * @param info ImageryInfo description of the layer
     */
    public WMSLayer(ImageryInfo info) {
        super(info);
        CheckParameterUtil.ensureThat(info.getImageryType() == ImageryType.WMS, "ImageryType is WMS");
        CheckParameterUtil.ensureParameterNotNull(info.getUrl(), "info.url");
        TemplatedWMSTileSource.checkUrl(info.getUrl());
        this.serverProjections = new ArrayList<>(info.getServerProjections());
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
        return ret.toArray(new Action[ret.size()]);
    }

    @Override
    protected AbstractWMSTileSource getTileSource() {
        AbstractWMSTileSource tileSource = new TemplatedWMSTileSource(
                info, chooseProjection(Main.getProjection()));
        info.setAttribution(tileSource);
        return tileSource;
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
    public Collection<String> getNativeProjections() {
        return serverProjections;
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);
        Projection tileProjection = chooseProjection(newValue);
        if (!Objects.equals(tileSource.getTileProjection(), tileProjection)) {
            tileSource.setTileProjection(tileProjection);
        }
    }

    private Projection chooseProjection(Projection requested) {
        if (serverProjections.contains(requested.toCode())) {
            return requested;
        } else {
            for (String code : serverProjections) {
                Projection proj = Projections.getProjectionByCode(code);
                if (proj != null) {
                    Logging.info(tr("Reprojecting layer {0} from {1} to {2}. For best image quality and performance,"
                            + " switch to one of the supported projections: {3}",
                            getName(), proj.toCode(), Main.getProjection().toCode(), Utils.join(", ", getNativeProjections())));
                    return proj;
                }
            }
            Logging.warn(tr("Unable to find supported projection for layer {0}. Using {1}.", getName(), requested.toCode()));
            return requested;
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
