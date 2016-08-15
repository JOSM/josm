// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.AttributionSupport;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.CachedTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ImageryAdjustAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings.FilterChangeListener;
import org.openstreetmap.josm.gui.layer.imagery.TileCoordinateConverter;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings.DisplaySettingsChangeEvent;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings.DisplaySettingsChangeListener;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.WMSLayerImporter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.MemoryManager;
import org.openstreetmap.josm.tools.MemoryManager.MemoryHandle;
import org.openstreetmap.josm.tools.MemoryManager.NotEnoughMemoryException;

/**
 * Base abstract class that supports displaying images provided by TileSource. It might be TMS source, WMS or WMTS
 *
 * It implements all standard functions of tilesource based layers: autozoom, tile reloads, layer saving, loading,etc.
 *
 * @author Upliner
 * @author Wiktor Niesiobędzki
 * @param <T> Tile Source class used for this layer
 * @since 3715
 * @since 8526 (copied from TMSLayer)
 */
public abstract class AbstractTileSourceLayer<T extends AbstractTMSTileSource> extends ImageryLayer
implements ImageObserver, TileLoaderListener, ZoomChangeListener, FilterChangeListener, DisplaySettingsChangeListener {
    private static final String PREFERENCE_PREFIX = "imagery.generic";
    /**
     * Registers all setting properties
     */
    static {
        new TileSourceDisplaySettings();
    }

    /** maximum zoom level supported */
    public static final int MAX_ZOOM = 30;
    /** minium zoom level supported */
    public static final int MIN_ZOOM = 2;
    private static final Font InfoFont = new Font("sansserif", Font.BOLD, 13);

    /** minimum zoom level to show to user */
    public static final IntegerProperty PROP_MIN_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".min_zoom_lvl", 2);
    /** maximum zoom level to show to user */
    public static final IntegerProperty PROP_MAX_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".max_zoom_lvl", 20);

    //public static final BooleanProperty PROP_DRAW_DEBUG = new BooleanProperty(PREFERENCE_PREFIX + ".draw_debug", false);
    /**
     * Zoomlevel at which tiles is currently downloaded.
     * Initial zoom lvl is set to bestZoom
     */
    public int currentZoomLevel;

    private final AttributionSupport attribution = new AttributionSupport();
    private final TileHolder clickedTileHolder = new TileHolder();

    /**
     * Offset between calculated zoom level and zoom level used to download and show tiles. Negative values will result in
     * lower resolution of imagery useful in "retina" displays, positive values will result in higher resolution
     */
    public static final IntegerProperty ZOOM_OFFSET = new IntegerProperty(PREFERENCE_PREFIX + ".zoom_offset", 0);

    /*
     *  use MemoryTileCache instead of tileLoader JCS cache, as tileLoader caches only content (byte[] of image)
     *  and MemoryTileCache caches whole Tile. This gives huge performance improvement when a lot of tiles are visible
     *  in MapView (for example - when limiting min zoom in imagery)
     *
     *  Use per-layer tileCache instance, as the more layers there are, the more tiles needs to be cached
     */
    protected TileCache tileCache; // initialized together with tileSource
    protected T tileSource;
    protected TileLoader tileLoader;

    /**
     * A timer that is used to delay invalidation events if required.
     */
    private final Timer invalidateLaterTimer = new Timer(100, e -> this.invalidate());

    private final MouseAdapter adapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!isVisible()) return;
            if (e.getButton() == MouseEvent.BUTTON3) {
                clickedTileHolder.setTile(getTileForPixelpos(e.getX(), e.getY()));
                new TileSourceLayerPopup().show(e.getComponent(), e.getX(), e.getY());
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                attribution.handleAttribution(e.getPoint(), true);
            }
        }
    };

    private final TileSourceDisplaySettings displaySettings = createDisplaySettings();

    private final ImageryAdjustAction adjustAction = new ImageryAdjustAction(this);
    // prepared to be moved to the painter
    private TileCoordinateConverter coordinateConverter;

    /**
     * Creates Tile Source based Imagery Layer based on Imagery Info
     * @param info imagery info
     */
    public AbstractTileSourceLayer(ImageryInfo info) {
        super(info);
        setBackgroundLayer(true);
        this.setVisible(true);
        getFilterSettings().addFilterChangeListener(this);
        getDisplaySettings().addSettingsChangeListener(this);
    }

    /**
     * This method creates the {@link TileSourceDisplaySettings} object. Subclasses may implement it to e.g. change the prefix.
     * @return The object.
     * @since 10568
     */
    protected TileSourceDisplaySettings createDisplaySettings() {
        return new TileSourceDisplaySettings();
    }

    /**
     * Gets the {@link TileSourceDisplaySettings} instance associated with this tile source.
     * @return The tile source display settings
     * @since 10568
     */
    public TileSourceDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    @Override
    public void filterChanged() {
        invalidate();
    }

    protected abstract TileLoaderFactory getTileLoaderFactory();

    /**
     *
     * @param info imagery info
     * @return TileSource for specified ImageryInfo
     * @throws IllegalArgumentException when Imagery is not supported by layer
     */
    protected abstract T getTileSource(ImageryInfo info);

    protected Map<String, String> getHeaders(T tileSource) {
        if (tileSource instanceof TemplatedTileSource) {
            return ((TemplatedTileSource) tileSource).getHeaders();
        }
        return null;
    }

    protected void initTileSource(T tileSource) {
        coordinateConverter = new TileCoordinateConverter(Main.map.mapView, tileSource, getDisplaySettings());
        attribution.initialize(tileSource);

        currentZoomLevel = getBestZoom();

        Map<String, String> headers = getHeaders(tileSource);

        tileLoader = getTileLoaderFactory().makeTileLoader(this, headers);

        try {
            if ("file".equalsIgnoreCase(new URL(tileSource.getBaseUrl()).getProtocol())) {
                tileLoader = new OsmTileLoader(this);
            }
        } catch (MalformedURLException e) {
            // ignore, assume that this is not a file
            if (Main.isDebugEnabled()) {
                Main.debug(e.getMessage());
            }
        }

        if (tileLoader == null)
            tileLoader = new OsmTileLoader(this, headers);

        tileCache = new MemoryTileCache(estimateTileCacheSize());
    }

    @Override
    public synchronized void tileLoadingFinished(Tile tile, boolean success) {
        if (tile.hasError()) {
            success = false;
            tile.setImage(null);
        }
        tile.setLoaded(success);
        invalidateLater();
        if (Main.isDebugEnabled()) {
            Main.debug("tileLoadingFinished() tile: " + tile + " success: " + success);
        }
    }

    /**
     * Clears the tile cache.
     *
     * If the current tileLoader is an instance of OsmTileLoader, a new
     * TmsTileClearController is created and passed to the according clearCache
     * method.
     *
     * @param monitor not used in this implementation - as cache clear is instaneus
     */
    public void clearTileCache(ProgressMonitor monitor) {
        if (tileLoader instanceof CachedTileLoader) {
            ((CachedTileLoader) tileLoader).clearCache(tileSource);
        }
        tileCache.clear();
    }

    /**
     * Initiates a repaint of Main.map
     *
     * @see Main#map
     * @see MapFrame#repaint()
     * @see #invalidate() To trigger a repaint of all places where the layer is displayed.
     */
    protected void redraw() {
        invalidate();
    }

    /**
     * {@inheritDoc}
     * @deprecated Use {@link TileSourceDisplaySettings#getDx()}
     */
    @Override
    @Deprecated
    public double getDx() {
        return getDisplaySettings().getDx();
    }

    /**
     * {@inheritDoc}
     * @deprecated Use {@link TileSourceDisplaySettings#getDy()}
     */
    @Override
    @Deprecated
    public double getDy() {
        return getDisplaySettings().getDy();
    }

    /**
     * {@inheritDoc}
     * @deprecated Use {@link TileSourceDisplaySettings}
     */
    @Override
    @Deprecated
    public void displace(double dx, double dy) {
        getDisplaySettings().addDisplacement(new EastNorth(dx, dy));
    }

    /**
     * {@inheritDoc}
     * @deprecated Use {@link TileSourceDisplaySettings}
     */
    @Override
    @Deprecated
    public void setOffset(double dx, double dy) {
        getDisplaySettings().setDisplacement(new EastNorth(dx, dy));
    }

    @Override
    public Object getInfoComponent() {
        JPanel panel = (JPanel) super.getInfoComponent();
        EastNorth offset = getDisplaySettings().getDisplacement();
        if (offset.distanceSq(0, 0) > 1e-10) {
            panel.add(new JLabel(tr("Offset: ") + offset.east() + ';' + offset.north()), GBC.eol().insets(0, 5, 10, 0));
        }
        return panel;
    }

    @Override
    protected Action getAdjustAction() {
        return adjustAction;
    }

    /**
     * Returns average number of screen pixels per tile pixel for current mapview
     * @param zoom zoom level
     * @return average number of screen pixels per tile pixel
     */
    private double getScaleFactor(int zoom) {
        if (coordinateConverter != null) {
            return coordinateConverter.getScaleFactor(zoom);
        } else {
            return 1;
        }
    }

    protected int getBestZoom() {
        double factor = getScaleFactor(1); // check the ratio between area of tilesize at zoom 1 to current view
        double result = Math.log(factor)/Math.log(2)/2;
        /*
         * Math.log(factor)/Math.log(2) - gives log base 2 of factor
         * We divide result by 2, as factor contains ratio between areas. We could do Math.sqrt before log, or just divide log by 2
         *
         * ZOOM_OFFSET controls, whether we work with overzoomed or underzoomed tiles. Positive ZOOM_OFFSET
         * is for working with underzoomed tiles (higher quality when working with aerial imagery), negative ZOOM_OFFSET
         * is for working with overzoomed tiles (big, pixelated), which is good when working with high-dpi screens and/or
         * maps as a imagery layer
         */

        int intResult = (int) Math.round(result + 1 + ZOOM_OFFSET.get() / 1.9);

        intResult = Math.min(intResult, getMaxZoomLvl());
        intResult = Math.max(intResult, getMinZoomLvl());
        return intResult;
    }

    private static boolean actionSupportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof TMSLayer;
    }

    private final class ShowTileInfoAction extends AbstractAction {

        private ShowTileInfoAction() {
            super(tr("Show tile info"));
        }

        private String getSizeString(int size) {
            StringBuilder ret = new StringBuilder();
            return ret.append(size).append('x').append(size).toString();
        }

        private JTextField createTextField(String text) {
            JTextField ret = new JTextField(text);
            ret.setEditable(false);
            ret.setBorder(BorderFactory.createEmptyBorder());
            return ret;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            Tile clickedTile = clickedTileHolder.getTile();
            if (clickedTile != null) {
                ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Tile Info"), new String[]{tr("OK")});
                JPanel panel = new JPanel(new GridBagLayout());
                Rectangle2D displaySize = coordinateConverter.getRectangleForTile(clickedTile);
                String url = "";
                try {
                    url = clickedTile.getUrl();
                } catch (IOException e) {
                    // silence exceptions
                    Main.trace(e);
                }

                String[][] content = {
                        {"Tile name", clickedTile.getKey()},
                        {"Tile url", url},
                        {"Tile size", getSizeString(clickedTile.getTileSource().getTileSize()) },
                        {"Tile display size", new StringBuilder().append(displaySize.getWidth())
                                                                 .append('x')
                                                                 .append(displaySize.getHeight()).toString()},
                };

                for (String[] entry: content) {
                    panel.add(new JLabel(tr(entry[0]) + ':'), GBC.std());
                    panel.add(GBC.glue(5, 0), GBC.std());
                    panel.add(createTextField(entry[1]), GBC.eol().fill(GBC.HORIZONTAL));
                }

                for (Entry<String, String> e: clickedTile.getMetadata().entrySet()) {
                    panel.add(new JLabel(tr("Metadata ") + tr(e.getKey()) + ':'), GBC.std());
                    panel.add(GBC.glue(5, 0), GBC.std());
                    String value = e.getValue();
                    if ("lastModification".equals(e.getKey()) || "expirationTime".equals(e.getKey())) {
                        value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(value)));
                    }
                    panel.add(createTextField(value), GBC.eol().fill(GBC.HORIZONTAL));

                }
                ed.setIcon(JOptionPane.INFORMATION_MESSAGE);
                ed.setContent(panel);
                ed.showDialog();
            }
        }
    }

    private final class LoadTileAction extends AbstractAction {

        private LoadTileAction() {
            super(tr("Load tile"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            Tile clickedTile = clickedTileHolder.getTile();
            if (clickedTile != null) {
                loadTile(clickedTile, true);
                invalidate();
            }
        }
    }

    private class AutoZoomAction extends AbstractAction implements LayerAction {
        AutoZoomAction() {
            super(tr("Auto zoom"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            getDisplaySettings().setAutoZoom(!getDisplaySettings().isAutoZoom());
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(getDisplaySettings().isAutoZoom());
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return actionSupportLayers(layers);
        }
    }

    private class AutoLoadTilesAction extends AbstractAction implements LayerAction {
        AutoLoadTilesAction() {
            super(tr("Auto load tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            getDisplaySettings().setAutoLoad(!getDisplaySettings().isAutoLoad());
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(getDisplaySettings().isAutoLoad());
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return actionSupportLayers(layers);
        }
    }

    private class ShowErrorsAction extends AbstractAction implements LayerAction {
        ShowErrorsAction() {
            super(tr("Show errors"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            getDisplaySettings().setShowErrors(!getDisplaySettings().isShowErrors());
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(getDisplaySettings().isShowErrors());
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return actionSupportLayers(layers);
        }
    }

    private class LoadAllTilesAction extends AbstractAction {
        LoadAllTilesAction() {
            super(tr("Load all tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            loadAllTiles(true);
        }
    }

    private class LoadErroneusTilesAction extends AbstractAction {
        LoadErroneusTilesAction() {
            super(tr("Load all error tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            loadAllErrorTiles(true);
        }
    }

    private class ZoomToNativeLevelAction extends AbstractAction {
        ZoomToNativeLevelAction() {
            super(tr("Zoom to native resolution"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            double newFactor = Math.sqrt(getScaleFactor(currentZoomLevel));
            Main.map.mapView.zoomToFactor(newFactor);
            redraw();
        }
    }

    private class ZoomToBestAction extends AbstractAction {
        ZoomToBestAction() {
            super(tr("Change resolution"));
            setEnabled(!getDisplaySettings().isAutoZoom() && getBestZoom() != currentZoomLevel);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            setZoomLevel(getBestZoom());
        }
    }

    private class IncreaseZoomAction extends AbstractAction {
        IncreaseZoomAction() {
            super(tr("Increase zoom"));
            setEnabled(!getDisplaySettings().isAutoZoom() && zoomIncreaseAllowed());
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            increaseZoomLevel();
        }
    }

    private class DecreaseZoomAction extends AbstractAction {
        DecreaseZoomAction() {
            super(tr("Decrease zoom"));
            setEnabled(!getDisplaySettings().isAutoZoom() && zoomDecreaseAllowed());
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            decreaseZoomLevel();
        }
    }

    private class FlushTileCacheAction extends AbstractAction {
        FlushTileCacheAction() {
            super(tr("Flush tile cache"));
            setEnabled(tileLoader instanceof CachedTileLoader);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            new PleaseWaitRunnable(tr("Flush tile cache")) {
                @Override
                protected void realRun() {
                    clearTileCache(getProgressMonitor());
                }

                @Override
                protected void finish() {
                    // empty - flush is instaneus
                }

                @Override
                protected void cancel() {
                    // empty - flush is instaneus
                }
            }.run();
        }
    }

    /**
     * Simple class to keep clickedTile within hookUpMapView
     */
    private static final class TileHolder {
        private Tile t;

        public Tile getTile() {
            return t;
        }

        public void setTile(Tile t) {
            this.t = t;
        }
    }

    /**
     * Creates popup menu items and binds to mouse actions
     */
    @Override
    public void hookUpMapView() {
        // this needs to be here and not in constructor to allow empty TileSource class construction
        // using SessionWriter
        initializeIfRequired();

        super.hookUpMapView();
    }

    @Override
    public LayerPainter attachToMapView(MapViewEvent event) {
        initializeIfRequired();

        event.getMapView().addMouseListener(adapter);
        MapView.addZoomChangeListener(this);

        if (this instanceof NativeScaleLayer) {
            event.getMapView().setNativeScaleLayer((NativeScaleLayer) this);
        }

        // FIXME: why do we need this? Without this, if you add a WMS layer and do not move the mouse, sometimes, tiles do not
        // start loading.
        // FIXME: Check if this is still required.
        event.getMapView().repaint(500);

        return super.attachToMapView(event);
    }

    private void initializeIfRequired() {
        if (tileSource == null) {
            tileSource = getTileSource(info);
            if (tileSource == null) {
                throw new IllegalArgumentException(tr("Failed to create tile source"));
            }
            // check if projection is supported
            projectionChanged(null, Main.getProjection());
            initTileSource(this.tileSource);
        }
    }

    @Override
    protected LayerPainter createMapViewPainter(MapViewEvent event) {
        return new TileSourcePainter();
    }

    /**
     * Tile source layer popup menu.
     */
    public class TileSourceLayerPopup extends JPopupMenu {
        /**
         * Constructs a new {@code TileSourceLayerPopup}.
         */
        public TileSourceLayerPopup() {
            for (Action a : getCommonEntries()) {
                if (a instanceof LayerAction) {
                    add(((LayerAction) a).createMenuComponent());
                } else {
                    add(new JMenuItem(a));
                }
            }
            add(new JSeparator());
            add(new JMenuItem(new LoadTileAction()));
            add(new JMenuItem(new ShowTileInfoAction()));
        }
    }

    protected int estimateTileCacheSize() {
        Dimension screenSize = GuiHelper.getMaximumScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        int tileSize = 256; // default tile size
        if (tileSource != null) {
            tileSize = tileSource.getTileSize();
        }
        // as we can see part of the tile at the top and at the bottom, use Math.ceil(...) + 1 to accommodate for that
        int visibileTiles = (int) (Math.ceil((double) height / tileSize + 1) * Math.ceil((double) width / tileSize + 1));
        // add 10% for tiles from different zoom levels
        int ret = (int) Math.ceil(
                Math.pow(2d, ZOOM_OFFSET.get()) * visibileTiles // use offset to decide, how many tiles are visible
                * 4);
        Main.info("AbstractTileSourceLayer: estimated visible tiles: {0}, estimated cache size: {1}", visibileTiles, ret);
        return ret;
    }

    @Override
    public void displaySettingsChanged(DisplaySettingsChangeEvent e) {
        if (tileSource == null) {
            return;
        }
        switch (e.getChangedSetting()) {
        case TileSourceDisplaySettings.AUTO_ZOOM:
            if (getDisplaySettings().isAutoZoom() && getBestZoom() != currentZoomLevel) {
                setZoomLevel(getBestZoom());
                invalidate();
            }
            break;
        case TileSourceDisplaySettings.AUTO_LOAD:
            if (getDisplaySettings().isAutoLoad()) {
                invalidate();
            }
            break;
        default:
            // trigger a redraw just to be sure.
            invalidate();
        }
    }

    /**
     * Checks zoom level against settings
     * @param maxZoomLvl zoom level to check
     * @param ts tile source to crosscheck with
     * @return maximum zoom level, not higher than supported by tilesource nor set by the user
     */
    public static int checkMaxZoomLvl(int maxZoomLvl, TileSource ts) {
        if (maxZoomLvl > MAX_ZOOM) {
            maxZoomLvl = MAX_ZOOM;
        }
        if (maxZoomLvl < PROP_MIN_ZOOM_LVL.get()) {
            maxZoomLvl = PROP_MIN_ZOOM_LVL.get();
        }
        if (ts != null && ts.getMaxZoom() != 0 && ts.getMaxZoom() < maxZoomLvl) {
            maxZoomLvl = ts.getMaxZoom();
        }
        return maxZoomLvl;
    }

    /**
     * Checks zoom level against settings
     * @param minZoomLvl zoom level to check
     * @param ts tile source to crosscheck with
     * @return minimum zoom level, not higher than supported by tilesource nor set by the user
     */
    public static int checkMinZoomLvl(int minZoomLvl, TileSource ts) {
        if (minZoomLvl < MIN_ZOOM) {
            minZoomLvl = MIN_ZOOM;
        }
        if (minZoomLvl > PROP_MAX_ZOOM_LVL.get()) {
            minZoomLvl = getMaxZoomLvl(ts);
        }
        if (ts != null && ts.getMinZoom() > minZoomLvl) {
            minZoomLvl = ts.getMinZoom();
        }
        return minZoomLvl;
    }

    /**
     * @param ts TileSource for which we want to know maximum zoom level
     * @return maximum max zoom level, that will be shown on layer
     */
    public static int getMaxZoomLvl(TileSource ts) {
        return checkMaxZoomLvl(PROP_MAX_ZOOM_LVL.get(), ts);
    }

    /**
     * @param ts TileSource for which we want to know minimum zoom level
     * @return minimum zoom level, that will be shown on layer
     */
    public static int getMinZoomLvl(TileSource ts) {
        return checkMinZoomLvl(PROP_MIN_ZOOM_LVL.get(), ts);
    }

    /**
     * Sets maximum zoom level, that layer will attempt show
     * @param maxZoomLvl maximum zoom level
     */
    public static void setMaxZoomLvl(int maxZoomLvl) {
        PROP_MAX_ZOOM_LVL.put(checkMaxZoomLvl(maxZoomLvl, null));
    }

    /**
     * Sets minimum zoom level, that layer will attempt show
     * @param minZoomLvl minimum zoom level
     */
    public static void setMinZoomLvl(int minZoomLvl) {
        PROP_MIN_ZOOM_LVL.put(checkMinZoomLvl(minZoomLvl, null));
    }

    /**
     * This fires every time the user changes the zoom, but also (due to ZoomChangeListener) - on all
     * changes to visible map (panning/zooming)
     */
    @Override
    public void zoomChanged() {
        if (Main.isDebugEnabled()) {
            Main.debug("zoomChanged(): " + currentZoomLevel);
        }
        if (tileLoader instanceof TMSCachedTileLoader) {
            ((TMSCachedTileLoader) tileLoader).cancelOutstandingTasks();
        }
        invalidate();
    }

    protected int getMaxZoomLvl() {
        if (info.getMaxZoom() != 0)
            return checkMaxZoomLvl(info.getMaxZoom(), tileSource);
        else
            return getMaxZoomLvl(tileSource);
    }

    protected int getMinZoomLvl() {
        if (info.getMinZoom() != 0)
            return checkMinZoomLvl(info.getMinZoom(), tileSource);
        else
            return getMinZoomLvl(tileSource);
    }

    /**
     *
     * @return if its allowed to zoom in
     */
    public boolean zoomIncreaseAllowed() {
        boolean zia = currentZoomLevel < this.getMaxZoomLvl();
        if (Main.isDebugEnabled()) {
            Main.debug("zoomIncreaseAllowed(): " + zia + ' ' + currentZoomLevel + " vs. " + this.getMaxZoomLvl());
        }
        return zia;
    }

    /**
     * Zoom in, go closer to map.
     *
     * @return    true, if zoom increasing was successful, false otherwise
     */
    public boolean increaseZoomLevel() {
        if (zoomIncreaseAllowed()) {
            currentZoomLevel++;
            if (Main.isDebugEnabled()) {
                Main.debug("increasing zoom level to: " + currentZoomLevel);
            }
            zoomChanged();
        } else {
            Main.warn("Current zoom level ("+currentZoomLevel+") could not be increased. "+
                    "Max.zZoom Level "+this.getMaxZoomLvl()+" reached.");
            return false;
        }
        return true;
    }

    /**
     * Sets the zoom level of the layer
     * @param zoom zoom level
     * @return true, when zoom has changed to desired value, false if it was outside supported zoom levels
     */
    public boolean setZoomLevel(int zoom) {
        if (zoom == currentZoomLevel) return true;
        if (zoom > this.getMaxZoomLvl()) return false;
        if (zoom < this.getMinZoomLvl()) return false;
        currentZoomLevel = zoom;
        zoomChanged();
        return true;
    }

    /**
     * Check if zooming out is allowed
     *
     * @return    true, if zooming out is allowed (currentZoomLevel &gt; minZoomLevel)
     */
    public boolean zoomDecreaseAllowed() {
        boolean zda = currentZoomLevel > this.getMinZoomLvl();
        if (Main.isDebugEnabled()) {
            Main.debug("zoomDecreaseAllowed(): " + zda + ' ' + currentZoomLevel + " vs. " + this.getMinZoomLvl());
        }
        return zda;
    }

    /**
     * Zoom out from map.
     *
     * @return    true, if zoom increasing was successfull, false othervise
     */
    public boolean decreaseZoomLevel() {
        if (zoomDecreaseAllowed()) {
            if (Main.isDebugEnabled()) {
                Main.debug("decreasing zoom level to: " + currentZoomLevel);
            }
            currentZoomLevel--;
            zoomChanged();
        } else {
            return false;
        }
        return true;
    }

    /*
     * We use these for quick, hackish calculations.  They
     * are temporary only and intentionally not inserted
     * into the tileCache.
     */
    private Tile tempCornerTile(Tile t) {
        int x = t.getXtile() + 1;
        int y = t.getYtile() + 1;
        int zoom = t.getZoom();
        Tile tile = getTile(x, y, zoom);
        if (tile != null)
            return tile;
        return new Tile(tileSource, x, y, zoom);
    }

    private Tile getOrCreateTile(int x, int y, int zoom) {
        Tile tile = getTile(x, y, zoom);
        if (tile == null) {
            tile = new Tile(tileSource, x, y, zoom);
            tileCache.addTile(tile);
        }

        if (!tile.isLoaded()) {
            tile.loadPlaceholderFromCache(tileCache);
        }
        return tile;
    }

    /**
     * Returns tile at given position.
     * This can and will return null for tiles that are not already in the cache.
     * @param x tile number on the x axis of the tile to be retrieved
     * @param y tile number on the y axis of the tile to be retrieved
     * @param zoom zoom level of the tile to be retrieved
     * @return tile at given position
     */
    private Tile getTile(int x, int y, int zoom) {
        if (x < tileSource.getTileXMin(zoom) || x > tileSource.getTileXMax(zoom)
         || y < tileSource.getTileYMin(zoom) || y > tileSource.getTileYMax(zoom))
            return null;
        return tileCache.getTile(tileSource, x, y, zoom);
    }

    private boolean loadTile(Tile tile, boolean force) {
        if (tile == null)
            return false;
        if (!force && (tile.isLoaded() || tile.hasError()))
            return false;
        if (tile.isLoading())
            return false;
        tileLoader.createTileLoaderJob(tile).submit(force);
        return true;
    }

    private TileSet getVisibleTileSet() {
        MapView mv = Main.map.mapView;
        MapViewRectangle area = mv.getState().getViewArea();
        ProjectionBounds bounds = area.getProjectionBounds();
        return getTileSet(bounds.getMin(), bounds.getMax(), currentZoomLevel);
    }

    protected void loadAllTiles(boolean force) {
        TileSet ts = getVisibleTileSet();

        // if there is more than 18 tiles on screen in any direction, do not load all tiles!
        if (ts.tooLarge()) {
            Main.warn("Not downloading all tiles because there is more than 18 tiles on an axis!");
            return;
        }
        ts.loadAllTiles(force);
        invalidate();
    }

    protected void loadAllErrorTiles(boolean force) {
        TileSet ts = getVisibleTileSet();
        ts.loadAllErrorTiles(force);
        invalidate();
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = (infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0;
        if (Main.isDebugEnabled()) {
            Main.debug("imageUpdate() done: " + done + " calling repaint");
        }

        if (done) {
            invalidate();
        } else {
            invalidateLater();
        }
        return !done;
    }

    /**
     * Invalidate the layer at a time in the future so taht the user still sees the interface responsive.
     */
    private void invalidateLater() {
        GuiHelper.runInEDT(() -> {
            if (!invalidateLaterTimer.isRunning()) {
                invalidateLaterTimer.setRepeats(false);
                invalidateLaterTimer.start();
            }
        });
    }

    private boolean imageLoaded(Image i) {
        if (i == null)
            return false;
        int status = Toolkit.getDefaultToolkit().checkImage(i, -1, -1, this);
        if ((status & ALLBITS) != 0)
            return true;
        return false;
    }

    /**
     * Returns the image for the given tile image is loaded.
     * Otherwise returns  null.
     *
     * @param tile the Tile for which the image should be returned
     * @return  the image of the tile or null.
     */
    private Image getLoadedTileImage(Tile tile) {
        Image img = tile.getImage();
        if (!imageLoaded(img))
            return null;
        return img;
    }

    // 'source' is the pixel coordinates for the area that
    // the img is capable of filling in.  However, we probably
    // only want a portion of it.
    //
    // 'border' is the screen cordinates that need to be drawn.
    //  We must not draw outside of it.
    private void drawImageInside(Graphics g, Image sourceImg, Rectangle2D source, Rectangle2D border) {
        Rectangle2D target = source;

        // If a border is specified, only draw the intersection
        // if what we have combined with what we are supposed to draw.
        if (border != null) {
            target = source.createIntersection(border);
            if (Main.isDebugEnabled()) {
                Main.debug("source: " + source + "\nborder: " + border + "\nintersection: " + target);
            }
        }

        // All of the rectangles are in screen coordinates.  We need
        // to how these correlate to the sourceImg pixels.  We could
        // avoid doing this by scaling the image up to the 'source' size,
        // but this should be cheaper.
        //
        // In some projections, x any y are scaled differently enough to
        // cause a pixel or two of fudge.  Calculate them separately.
        double imageYScaling = sourceImg.getHeight(this) / source.getHeight();
        double imageXScaling = sourceImg.getWidth(this) / source.getWidth();

        // How many pixels into the 'source' rectangle are we drawing?
        double screenXoffset = target.getX() - source.getX();
        double screenYoffset = target.getY() - source.getY();
        // And how many pixels into the image itself does that correlate to?
        int imgXoffset = (int) (screenXoffset * imageXScaling + 0.5);
        int imgYoffset = (int) (screenYoffset * imageYScaling + 0.5);
        // Now calculate the other corner of the image that we need
        // by scaling the 'target' rectangle's dimensions.
        int imgXend = imgXoffset + (int) (target.getWidth() * imageXScaling + 0.5);
        int imgYend = imgYoffset + (int) (target.getHeight() * imageYScaling + 0.5);

        if (Main.isDebugEnabled()) {
            Main.debug("drawing image into target rect: " + target);
        }
        g.drawImage(sourceImg,
                (int) target.getX(), (int) target.getY(),
                (int) target.getMaxX(), (int) target.getMaxY(),
                imgXoffset, imgYoffset,
                imgXend, imgYend,
                this);
        if (PROP_FADE_AMOUNT.get() != 0) {
            // dimm by painting opaque rect...
            g.setColor(getFadeColorWithAlpha());
            ((Graphics2D) g).fill(target);
        }
    }

    // This function is called for several zoom levels, not just
    // the current one.  It should not trigger any tiles to be
    // downloaded.  It should also avoid polluting the tile cache
    // with any tiles since these tiles are not mandatory.
    //
    // The "border" tile tells us the boundaries of where we may
    // draw.  It will not be from the zoom level that is being
    // drawn currently.  If drawing the displayZoomLevel,
    // border is null and we draw the entire tile set.
    private List<Tile> paintTileImages(Graphics g, TileSet ts, int zoom, Tile border) {
        if (zoom <= 0) return Collections.emptyList();
        Rectangle2D borderRect = null;
        if (border != null) {
            borderRect = coordinateConverter.getRectangleForTile(border);
        }
        List<Tile> missedTiles = new LinkedList<>();
        // The callers of this code *require* that we return any tiles
        // that we do not draw in missedTiles.  ts.allExistingTiles() by
        // default will only return already-existing tiles.  However, we
        // need to return *all* tiles to the callers, so force creation here.
        for (Tile tile : ts.allTilesCreate()) {
            Image img = getLoadedTileImage(tile);
            if (img == null || tile.hasError()) {
                if (Main.isDebugEnabled()) {
                    Main.debug("missed tile: " + tile);
                }
                missedTiles.add(tile);
                continue;
            }

            // applying all filters to this layer
            img = applyImageProcessors((BufferedImage) img);

            Rectangle2D sourceRect = coordinateConverter.getRectangleForTile(tile);
            if (borderRect != null && !sourceRect.intersects(borderRect)) {
                continue;
            }
            drawImageInside(g, img, sourceRect, borderRect);
        }
        return missedTiles;
    }

    private void myDrawString(Graphics g, String text, int x, int y) {
        Color oldColor = g.getColor();
        String textToDraw = text;
        if (g.getFontMetrics().stringWidth(text) > tileSource.getTileSize()) {
            // text longer than tile size, split it
            StringBuilder line = new StringBuilder();
            StringBuilder ret = new StringBuilder();
            for (String s: text.split(" ")) {
                if (g.getFontMetrics().stringWidth(line.toString() + s) > tileSource.getTileSize()) {
                    ret.append(line).append('\n');
                    line.setLength(0);
                }
                line.append(s).append(' ');
            }
            ret.append(line);
            textToDraw = ret.toString();
        }
        int offset = 0;
        for (String s: textToDraw.split("\n")) {
            g.setColor(Color.black);
            g.drawString(s, x + 1, y + offset + 1);
            g.setColor(oldColor);
            g.drawString(s, x, y + offset);
            offset += g.getFontMetrics().getHeight() + 3;
        }
    }

    private void paintTileText(TileSet ts, Tile tile, Graphics g, MapView mv, int zoom, Tile t) {
        if (tile == null) {
            return;
        }
        Point2D p = coordinateConverter.getPixelForTile(t);
        int fontHeight = g.getFontMetrics().getHeight();
        int x = (int) p.getX();
        int y = (int) p.getY();
        int texty = y + 2 + fontHeight;

        /*if (PROP_DRAW_DEBUG.get()) {
            myDrawString(g, "x=" + t.getXtile() + " y=" + t.getYtile() + " z=" + zoom + "", p.x + 2, texty);
            texty += 1 + fontHeight;
            if ((t.getXtile() % 32 == 0) && (t.getYtile() % 32 == 0)) {
                myDrawString(g, "x=" + t.getXtile() / 32 + " y=" + t.getYtile() / 32 + " z=7", p.x + 2, texty);
                texty += 1 + fontHeight;
            }
        }*/

        /*String tileStatus = tile.getStatus();
        if (!tile.isLoaded() && PROP_DRAW_DEBUG.get()) {
            myDrawString(g, tr("image " + tileStatus), p.x + 2, texty);
            texty += 1 + fontHeight;
        }*/

        if (tile.hasError() && getDisplaySettings().isShowErrors()) {
            myDrawString(g, tr("Error") + ": " + tr(tile.getErrorMessage()), x + 2, texty);
            //texty += 1 + fontHeight;
        }

        int xCursor = -1;
        int yCursor = -1;
        if (Main.isDebugEnabled()) {
            if (yCursor < t.getYtile()) {
                if (t.getYtile() % 32 == 31) {
                    g.fillRect(0, y - 1, mv.getWidth(), 3);
                } else {
                    g.drawLine(0, y, mv.getWidth(), y);
                }
                //yCursor = t.getYtile();
            }
            // This draws the vertical lines for the entire column. Only draw them for the top tile in the column.
            if (xCursor < t.getXtile()) {
                if (t.getXtile() % 32 == 0) {
                    // level 7 tile boundary
                    g.fillRect(x - 1, 0, 3, mv.getHeight());
                } else {
                    g.drawLine(x, 0, x, mv.getHeight());
                }
                //xCursor = t.getXtile();
            }
        }
    }

    private LatLon getShiftedLatLon(EastNorth en) {
        return coordinateConverter.getProjecting().eastNorth2latlonClamped(en);
    }

    private ICoordinate getShiftedCoord(EastNorth en) {
        return getShiftedLatLon(en).toCoordinate();
    }

    private LatLon getShiftedLatLon(ICoordinate latLon) {
        return getShiftedLatLon(Main.getProjection().latlon2eastNorth(new LatLon(latLon)));
    }


    private final TileSet nullTileSet = new TileSet();

    /**
     * This is a rectangular range of tiles.
     */
    private static class TileRange {
        int minX;
        int maxX;
        int minY;
        int maxY;
        int zoom;

        private TileRange() {
        }

        protected TileRange(TileXY t1, TileXY t2, int zoom) {
            minX = (int) Math.floor(Math.min(t1.getX(), t2.getX()));
            minY = (int) Math.floor(Math.min(t1.getY(), t2.getY()));
            maxX = (int) Math.ceil(Math.max(t1.getX(), t2.getX()));
            maxY = (int) Math.ceil(Math.max(t1.getY(), t2.getY()));
            this.zoom = zoom;
        }

        protected double tilesSpanned() {
            return Math.sqrt(1.0 * this.size());
        }

        protected int size() {
            int xSpan = maxX - minX + 1;
            int ySpan = maxY - minY + 1;
            return xSpan * ySpan;
        }
    }

    private class TileSet extends TileRange {

        protected TileSet(TileXY t1, TileXY t2, int zoom) {
            super(t1, t2, zoom);
            sanitize();
        }

        /**
         * null tile set
         */
        private TileSet() {
            return;
        }

        protected void sanitize() {
            if (minX < tileSource.getTileXMin(zoom)) {
                minX = tileSource.getTileXMin(zoom);
            }
            if (minY < tileSource.getTileYMin(zoom)) {
                minY = tileSource.getTileYMin(zoom);
            }
            if (maxX > tileSource.getTileXMax(zoom)) {
                maxX = tileSource.getTileXMax(zoom);
            }
            if (maxY > tileSource.getTileYMax(zoom)) {
                maxY = tileSource.getTileYMax(zoom);
            }
        }

        private boolean tooSmall() {
            return this.tilesSpanned() < 2.1;
        }

        private boolean tooLarge() {
            return insane() || this.tilesSpanned() > 20;
        }

        private boolean insane() {
            return tileCache == null || size() > tileCache.getCacheSize();
        }

        /*
         * Get all tiles represented by this TileSet that are
         * already in the tileCache.
         */
        private List<Tile> allExistingTiles() {
            return this.findAllTiles(false);
        }

        private List<Tile> allTilesCreate() {
            return this.findAllTiles(true);
        }

        private List<Tile> findAllTiles(boolean create) {
            // Tileset is either empty or too large
            if (zoom == 0 || this.insane())
                return Collections.emptyList();
            List<Tile> ret = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    Tile t;
                    if (create) {
                        t = getOrCreateTile(x, y, zoom);
                    } else {
                        t = getTile(x, y, zoom);
                    }
                    if (t != null) {
                        ret.add(t);
                    }
                }
            }
            return ret;
        }

        private List<Tile> allLoadedTiles() {
            List<Tile> ret = new ArrayList<>();
            for (Tile t : this.allExistingTiles()) {
                if (t.isLoaded())
                    ret.add(t);
            }
            return ret;
        }

        /**
         * @return comparator, that sorts the tiles from the center to the edge of the current screen
         */
        private Comparator<Tile> getTileDistanceComparator() {
            final int centerX = (int) Math.ceil((minX + maxX) / 2d);
            final int centerY = (int) Math.ceil((minY + maxY) / 2d);
            return new Comparator<Tile>() {
                private int getDistance(Tile t) {
                    return Math.abs(t.getXtile() - centerX) + Math.abs(t.getYtile() - centerY);
                }

                @Override
                public int compare(Tile o1, Tile o2) {
                    int distance1 = getDistance(o1);
                    int distance2 = getDistance(o2);
                    return Integer.compare(distance1, distance2);
                }
            };
        }

        private void loadAllTiles(boolean force) {
            if (!getDisplaySettings().isAutoLoad() && !force)
                return;
            List<Tile> allTiles = allTilesCreate();
            allTiles.sort(getTileDistanceComparator());
            for (Tile t : allTiles) {
                loadTile(t, force);
            }
        }

        private void loadAllErrorTiles(boolean force) {
            if (!getDisplaySettings().isAutoLoad() && !force)
                return;
            for (Tile t : this.allTilesCreate()) {
                if (t.hasError()) {
                    tileLoader.createTileLoaderJob(t).submit(force);
                }
            }
        }

        @Override
        public String toString() {
            return getClass().getName() + ": zoom: " + zoom + " X(" + minX + ", " + maxX + ") Y(" + minY + ", " + maxY + ") size: " + size();
        }
    }

    /**
     * Create a TileSet by EastNorth bbox taking a layer shift in account
     * @param topLeft top-left lat/lon
     * @param botRight bottom-right lat/lon
     * @param zoom zoom level
     * @return the tile set
     * @since 10651
     */
    protected TileSet getTileSet(EastNorth topLeft, EastNorth botRight, int zoom) {
        return getTileSet(getShiftedLatLon(topLeft), getShiftedLatLon(botRight), zoom);
    }

    /**
     * Create a TileSet by known LatLon bbox without layer shift correction
     * @param topLeft top-left lat/lon
     * @param botRight bottom-right lat/lon
     * @param zoom zoom level
     * @return the tile set
     * @since 10651
     */
    protected TileSet getTileSet(LatLon topLeft, LatLon botRight, int zoom) {
        if (zoom == 0)
            return new TileSet();

        TileXY t1 = tileSource.latLonToTileXY(topLeft.toCoordinate(), zoom);
        TileXY t2 = tileSource.latLonToTileXY(botRight.toCoordinate(), zoom);
        return new TileSet(t1, t2, zoom);
    }

    private static class TileSetInfo {
        public boolean hasVisibleTiles;
        public boolean hasOverzoomedTiles;
        public boolean hasLoadingTiles;
    }

    private static <S extends AbstractTMSTileSource> TileSetInfo getTileSetInfo(AbstractTileSourceLayer<S>.TileSet ts) {
        List<Tile> allTiles = ts.allExistingTiles();
        TileSetInfo result = new TileSetInfo();
        result.hasLoadingTiles = allTiles.size() < ts.size();
        for (Tile t : allTiles) {
            if ("no-tile".equals(t.getValue("tile-info"))) {
                result.hasOverzoomedTiles = true;
            }

            if (t.isLoaded()) {
                if (!t.hasError()) {
                    result.hasVisibleTiles = true;
                }
            } else if (t.isLoading()) {
                result.hasLoadingTiles = true;
            }
        }
        return result;
    }

    private class DeepTileSet {
        private final ProjectionBounds bounds;
        private final int minZoom, maxZoom;
        private final TileSet[] tileSets;
        private final TileSetInfo[] tileSetInfos;

        @SuppressWarnings("unchecked")
        DeepTileSet(ProjectionBounds bounds, int minZoom, int maxZoom) {
            this.bounds = bounds;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
            this.tileSets = new AbstractTileSourceLayer.TileSet[maxZoom - minZoom + 1];
            this.tileSetInfos = new TileSetInfo[maxZoom - minZoom + 1];
        }

        public TileSet getTileSet(int zoom) {
            if (zoom < minZoom)
                return nullTileSet;
            synchronized (tileSets) {
                TileSet ts = tileSets[zoom-minZoom];
                if (ts == null) {
                    ts = AbstractTileSourceLayer.this.getTileSet(bounds.getMin(), bounds.getMax(), zoom);
                    tileSets[zoom-minZoom] = ts;
                }
                return ts;
            }
        }

        public TileSetInfo getTileSetInfo(int zoom) {
            if (zoom < minZoom)
                return new TileSetInfo();
            synchronized (tileSetInfos) {
                TileSetInfo tsi = tileSetInfos[zoom-minZoom];
                if (tsi == null) {
                    tsi = AbstractTileSourceLayer.getTileSetInfo(getTileSet(zoom));
                    tileSetInfos[zoom-minZoom] = tsi;
                }
                return tsi;
            }
        }
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bounds) {
        // old and unused.
    }

    private void drawInViewArea(Graphics2D g, MapView mv, ProjectionBounds pb) {
        int zoom = currentZoomLevel;
        if (getDisplaySettings().isAutoZoom()) {
            zoom = getBestZoom();
        }

        DeepTileSet dts = new DeepTileSet(pb, getMinZoomLvl(), zoom);
        TileSet ts = dts.getTileSet(zoom);

        int displayZoomLevel = zoom;

        boolean noTilesAtZoom = false;
        if (getDisplaySettings().isAutoZoom() && getDisplaySettings().isAutoLoad()) {
            // Auto-detection of tilesource maxzoom (currently fully works only for Bing)
            TileSetInfo tsi = dts.getTileSetInfo(zoom);
            if (!tsi.hasVisibleTiles && (!tsi.hasLoadingTiles || tsi.hasOverzoomedTiles)) {
                noTilesAtZoom = true;
            }
            // Find highest zoom level with at least one visible tile
            for (int tmpZoom = zoom; tmpZoom > dts.minZoom; tmpZoom--) {
                if (dts.getTileSetInfo(tmpZoom).hasVisibleTiles) {
                    displayZoomLevel = tmpZoom;
                    break;
                }
            }
            // Do binary search between currentZoomLevel and displayZoomLevel
            while (zoom > displayZoomLevel && !tsi.hasVisibleTiles && tsi.hasOverzoomedTiles) {
                zoom = (zoom + displayZoomLevel)/2;
                tsi = dts.getTileSetInfo(zoom);
            }

            setZoomLevel(zoom);

            // If all tiles at displayZoomLevel is loaded, load all tiles at next zoom level
            // to make sure there're really no more zoom levels
            // loading is done in the next if section
            if (zoom == displayZoomLevel && !tsi.hasLoadingTiles && zoom < dts.maxZoom) {
                zoom++;
                tsi = dts.getTileSetInfo(zoom);
            }
            // When we have overzoomed tiles and all tiles at current zoomlevel is loaded,
            // load tiles at previovus zoomlevels until we have all tiles on screen is loaded.
            // loading is done in the next if section
            while (zoom > dts.minZoom && tsi.hasOverzoomedTiles && !tsi.hasLoadingTiles) {
                zoom--;
                tsi = dts.getTileSetInfo(zoom);
            }
            ts = dts.getTileSet(zoom);
        } else if (getDisplaySettings().isAutoZoom()) {
            setZoomLevel(zoom);
        }

        // Too many tiles... refuse to download
        if (!ts.tooLarge()) {
            //Main.debug("size: " + ts.size() + " spanned: " + ts.tilesSpanned());
            ts.loadAllTiles(false);
        }

        if (displayZoomLevel != zoom) {
            ts = dts.getTileSet(displayZoomLevel);
        }

        g.setColor(Color.DARK_GRAY);

        List<Tile> missedTiles = this.paintTileImages(g, ts, displayZoomLevel, null);
        int[] otherZooms = {-1, 1, -2, 2, -3, -4, -5};
        for (int zoomOffset : otherZooms) {
            if (!getDisplaySettings().isAutoZoom()) {
                break;
            }
            int newzoom = displayZoomLevel + zoomOffset;
            if (newzoom < getMinZoomLvl() || newzoom > getMaxZoomLvl()) {
                continue;
            }
            if (missedTiles.isEmpty()) {
                break;
            }
            List<Tile> newlyMissedTiles = new LinkedList<>();
            for (Tile missed : missedTiles) {
                if ("no-tile".equals(missed.getValue("tile-info")) && zoomOffset > 0) {
                    // Don't try to paint from higher zoom levels when tile is overzoomed
                    newlyMissedTiles.add(missed);
                    continue;
                }
                Tile t2 = tempCornerTile(missed);
                TileSet ts2 = getTileSet(
                        getShiftedLatLon(tileSource.tileXYToLatLon(missed)),
                        getShiftedLatLon(tileSource.tileXYToLatLon(t2)),
                        newzoom);
                // Instantiating large TileSets is expensive.  If there
                // are no loaded tiles, don't bother even trying.
                if (ts2.allLoadedTiles().isEmpty()) {
                    newlyMissedTiles.add(missed);
                    continue;
                }
                if (ts2.tooLarge()) {
                    continue;
                }
                newlyMissedTiles.addAll(this.paintTileImages(g, ts2, newzoom, missed));
            }
            missedTiles = newlyMissedTiles;
        }
        if (Main.isDebugEnabled() && !missedTiles.isEmpty()) {
            Main.debug("still missed "+missedTiles.size()+" in the end");
        }
        g.setColor(Color.red);
        g.setFont(InfoFont);

        // The current zoom tileset should have all of its tiles due to the loadAllTiles(), unless it to tooLarge()
        for (Tile t : ts.allExistingTiles()) {
            this.paintTileText(ts, t, g, mv, displayZoomLevel, t);
        }

        EastNorth min = pb.getMin();
        EastNorth max = pb.getMax();
        attribution.paintAttribution(g, mv.getWidth(), mv.getHeight(), getShiftedCoord(min), getShiftedCoord(max),
                displayZoomLevel, this);

        //g.drawString("currentZoomLevel=" + currentZoomLevel, 120, 120);
        g.setColor(Color.lightGray);

        if (ts.insane()) {
            myDrawString(g, tr("zoom in to load any tiles"), 120, 120);
        } else if (ts.tooLarge()) {
            myDrawString(g, tr("zoom in to load more tiles"), 120, 120);
        } else if (!getDisplaySettings().isAutoZoom() && ts.tooSmall()) {
            myDrawString(g, tr("increase tiles zoom level (change resolution) to see more detail"), 120, 120);
        }

        if (noTilesAtZoom) {
            myDrawString(g, tr("No tiles at this zoom level"), 120, 120);
        }
        if (Main.isDebugEnabled()) {
            myDrawString(g, tr("Current zoom: {0}", currentZoomLevel), 50, 140);
            myDrawString(g, tr("Display zoom: {0}", displayZoomLevel), 50, 155);
            myDrawString(g, tr("Pixel scale: {0}", getScaleFactor(currentZoomLevel)), 50, 170);
            myDrawString(g, tr("Best zoom: {0}", getBestZoom()), 50, 185);
            myDrawString(g, tr("Estimated cache size: {0}", estimateTileCacheSize()), 50, 200);
            if (tileLoader instanceof TMSCachedTileLoader) {
                TMSCachedTileLoader cachedTileLoader = (TMSCachedTileLoader) tileLoader;
                int offset = 200;
                for (String part: cachedTileLoader.getStats().split("\n")) {
                    offset += 15;
                    myDrawString(g, tr("Cache stats: {0}", part), 50, offset);
                }
            }
        }
    }

    /**
     * Returns tile for a pixel position.<p>
     * This isn't very efficient, but it is only used when the user right-clicks on the map.
     * @param px pixel X coordinate
     * @param py pixel Y coordinate
     * @return Tile at pixel position
     */
    private Tile getTileForPixelpos(int px, int py) {
        if (Main.isDebugEnabled()) {
            Main.debug("getTileForPixelpos("+px+", "+py+')');
        }
        MapView mv = Main.map.mapView;
        Point clicked = new Point(px, py);
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());
        int z = currentZoomLevel;
        TileSet ts = getTileSet(topLeft, botRight, z);

        if (!ts.tooLarge()) {
            ts.loadAllTiles(false); // make sure there are tile objects for all tiles
        }
        Stream<Tile> clickedTiles = ts.allExistingTiles().stream()
                .filter(t -> coordinateConverter.getRectangleForTile(t).contains(clicked));
        if (Main.isTraceEnabled()) {
            clickedTiles = clickedTiles.peek(t -> Main.trace("Clicked on tile: " + t.getXtile() + ' ' + t.getYtile() +
                    " currentZoomLevel: " + currentZoomLevel));
        }
        return clickedTiles.findAny().orElse(null);
    }

    @Override
    public Action[] getMenuEntries() {
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(getLayerListEntries()));
        actions.addAll(Arrays.asList(getCommonEntries()));
        actions.add(SeparatorLayerAction.INSTANCE);
        actions.add(new LayerListPopup.InfoAction(this));
        return actions.toArray(new Action[actions.size()]);
    }

    public Action[] getLayerListEntries() {
        return new Action[] {
            LayerListDialog.getInstance().createActivateLayerAction(this),
            LayerListDialog.getInstance().createShowHideLayerAction(),
            LayerListDialog.getInstance().createDeleteLayerAction(),
            SeparatorLayerAction.INSTANCE,
            // color,
            new OffsetAction(),
            new RenameLayerAction(this.getAssociatedFile(), this),
            SeparatorLayerAction.INSTANCE
        };
    }

    /**
     * Returns the common menu entries.
     * @return the common menu entries
     */
    public Action[] getCommonEntries() {
        return new Action[] {
            new AutoLoadTilesAction(),
            new AutoZoomAction(),
            new ShowErrorsAction(),
            new IncreaseZoomAction(),
            new DecreaseZoomAction(),
            new ZoomToBestAction(),
            new ZoomToNativeLevelAction(),
            new FlushTileCacheAction(),
            new LoadErroneusTilesAction(),
            new LoadAllTilesAction()
        };
    }

    @Override
    public String getToolTipText() {
        if (getDisplaySettings().isAutoLoad()) {
            return tr("{0} ({1}), automatically downloading in zoom {2}", this.getClass().getSimpleName(), getName(), currentZoomLevel);
        } else {
            return tr("{0} ({1}), downloading in zoom {2}", this.getClass().getSimpleName(), getName(), currentZoomLevel);
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
    }

    @Override
    public boolean isChanged() {
        // we use #invalidate()
        return false;
    }

    /**
     * Task responsible for precaching imagery along the gpx track
     *
     */
    public class PrecacheTask implements TileLoaderListener {
        private final ProgressMonitor progressMonitor;
        private int totalCount;
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private final TileLoader tileLoader;

        /**
         * @param progressMonitor that will be notified about progess of the task
         */
        public PrecacheTask(ProgressMonitor progressMonitor) {
            this.progressMonitor = progressMonitor;
            this.tileLoader = getTileLoaderFactory().makeTileLoader(this, getHeaders(tileSource));
            if (this.tileLoader instanceof TMSCachedTileLoader) {
                ((TMSCachedTileLoader) this.tileLoader).setDownloadExecutor(
                        TMSCachedTileLoader.getNewThreadPoolExecutor("Precache downloader"));
            }
        }

        /**
         * @return true, if all is done
         */
        public boolean isFinished() {
            return processedCount.get() >= totalCount;
        }

        /**
         * @return total number of tiles to download
         */
        public int getTotalCount() {
            return totalCount;
        }

        /**
         * cancel the task
         */
        public void cancel() {
            if (tileLoader instanceof TMSCachedTileLoader) {
                ((TMSCachedTileLoader) tileLoader).cancelOutstandingTasks();
            }
        }

        @Override
        public void tileLoadingFinished(Tile tile, boolean success) {
            int processed = this.processedCount.incrementAndGet();
            if (success) {
                this.progressMonitor.worked(1);
                this.progressMonitor.setCustomText(tr("Downloaded {0}/{1} tiles", processed, totalCount));
            } else {
                Main.warn("Tile loading failure: " + tile + " - " + tile.getErrorMessage());
            }
        }

        /**
         * @return tile loader that is used to load the tiles
         */
        public TileLoader getTileLoader() {
            return tileLoader;
        }
    }

    /**
     * Calculates tiles, that needs to be downloaded to cache, gets a current tile loader and creates a task to download
     * all of the tiles. Buffer contains at least one tile.
     *
     * To prevent accidental clear of the queue, new download executor is created with separate queue
     *
     * @param progressMonitor progress monitor for download task
     * @param points lat/lon coordinates to download
     * @param bufferX how many units in current Coordinate Reference System to cover in X axis in both sides
     * @param bufferY how many units in current Coordinate Reference System to cover in Y axis in both sides
     * @return precache task representing download task
     */
    public AbstractTileSourceLayer<T>.PrecacheTask downloadAreaToCache(final ProgressMonitor progressMonitor, List<LatLon> points,
            double bufferX, double bufferY) {
        PrecacheTask precacheTask = new PrecacheTask(progressMonitor);
        final Set<Tile> requestedTiles = new ConcurrentSkipListSet<>(
                (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey()));
        for (LatLon point: points) {

            TileXY minTile = tileSource.latLonToTileXY(point.lat() - bufferY, point.lon() - bufferX, currentZoomLevel);
            TileXY curTile = tileSource.latLonToTileXY(point.toCoordinate(), currentZoomLevel);
            TileXY maxTile = tileSource.latLonToTileXY(point.lat() + bufferY, point.lon() + bufferX, currentZoomLevel);

            // take at least one tile of buffer
            int minY = Math.min(curTile.getYIndex() - 1, minTile.getYIndex());
            int maxY = Math.max(curTile.getYIndex() + 1, maxTile.getYIndex());
            int minX = Math.min(curTile.getXIndex() - 1, minTile.getXIndex());
            int maxX = Math.max(curTile.getXIndex() + 1, maxTile.getXIndex());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    requestedTiles.add(new Tile(tileSource, x, y, currentZoomLevel));
                }
            }
        }

        precacheTask.totalCount = requestedTiles.size();
        precacheTask.progressMonitor.setTicksCount(requestedTiles.size());

        TileLoader loader = precacheTask.getTileLoader();
        for (Tile t: requestedTiles) {
            loader.createTileLoaderJob(t).submit();
        }
        return precacheTask;
    }

    @Override
    public boolean isSavable() {
        return true; // With WMSLayerExporter
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save WMS file"), WMSLayerImporter.FILE_FILTER);
    }

    @Override
    public void destroy() {
        super.destroy();
        adjustAction.destroy();
    }

    private class TileSourcePainter extends CompatibilityModeLayerPainter {
        /**
         * The memory handle that will hold our tile source.
         */
        private MemoryHandle<?> memory;

        @Override
        public void paint(MapViewGraphics graphics) {
            allocateCacheMemory();
            if (memory != null) {
                doPaint(graphics);
            }
        }

        private void doPaint(MapViewGraphics graphics) {
            ProjectionBounds pb = graphics.getClipBounds().getProjectionBounds();

            drawInViewArea(graphics.getDefaultGraphics(), graphics.getMapView(), pb);
        }

        private void allocateCacheMemory() {
            if (memory == null) {
                MemoryManager manager = MemoryManager.getInstance();
                if (manager.isAvailable(getEstimatedCacheSize())) {
                    try {
                        memory = manager.allocateMemory("tile source layer", getEstimatedCacheSize(), Object::new);
                    } catch (NotEnoughMemoryException e) {
                        Main.warn("Could not allocate tile source memory", e);
                    }
                }
            }
        }

        protected long getEstimatedCacheSize() {
            return 4L * tileSource.getTileSize() * tileSource.getTileSize() * estimateTileCacheSize();
        }

        @Override
        public void detachFromMapView(MapViewEvent event) {
            event.getMapView().removeMouseListener(adapter);
            MapView.removeZoomChangeListener(AbstractTileSourceLayer.this);
            super.detachFromMapView(event);
            if (memory != null) {
                memory.free();
            }
        }
    }
}
