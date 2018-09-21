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
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.AttributionSupport;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileRange;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.CachedTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ImageryAdjustAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.io.importexport.WMSLayerImporter;
import org.openstreetmap.josm.gui.layer.imagery.AutoLoadTilesAction;
import org.openstreetmap.josm.gui.layer.imagery.AutoZoomAction;
import org.openstreetmap.josm.gui.layer.imagery.DecreaseZoomAction;
import org.openstreetmap.josm.gui.layer.imagery.FlushTileCacheAction;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings.FilterChangeListener;
import org.openstreetmap.josm.gui.layer.imagery.IncreaseZoomAction;
import org.openstreetmap.josm.gui.layer.imagery.LoadAllTilesAction;
import org.openstreetmap.josm.gui.layer.imagery.LoadErroneousTilesAction;
import org.openstreetmap.josm.gui.layer.imagery.ReprojectionTile;
import org.openstreetmap.josm.gui.layer.imagery.ShowErrorsAction;
import org.openstreetmap.josm.gui.layer.imagery.TileAnchor;
import org.openstreetmap.josm.gui.layer.imagery.TileCoordinateConverter;
import org.openstreetmap.josm.gui.layer.imagery.TilePosition;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings.DisplaySettingsChangeEvent;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings.DisplaySettingsChangeListener;
import org.openstreetmap.josm.gui.layer.imagery.ZoomToBestAction;
import org.openstreetmap.josm.gui.layer.imagery.ZoomToNativeLevelAction;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MemoryManager;
import org.openstreetmap.josm.tools.MemoryManager.MemoryHandle;
import org.openstreetmap.josm.tools.MemoryManager.NotEnoughMemoryException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * Base abstract class that supports displaying images provided by TileSource. It might be TMS source, WMS or WMTS
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
    static { // Registers all setting properties
        new TileSourceDisplaySettings();
    }

    /** maximum zoom level supported */
    public static final int MAX_ZOOM = 30;
    /** minium zoom level supported */
    public static final int MIN_ZOOM = 2;
    private static final Font InfoFont = new Font("sansserif", Font.BOLD, 13);

    /** additional layer menu actions */
    private static List<MenuAddition> menuAdditions = new LinkedList<>();

    /** minimum zoom level to show to user */
    public static final IntegerProperty PROP_MIN_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".min_zoom_lvl", 2);
    /** maximum zoom level to show to user */
    public static final IntegerProperty PROP_MAX_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".max_zoom_lvl", 20);

    //public static final BooleanProperty PROP_DRAW_DEBUG = new BooleanProperty(PREFERENCE_PREFIX + ".draw_debug", false);
    /** Zoomlevel at which tiles is currently downloaded. Initial zoom lvl is set to bestZoom */
    private int currentZoomLevel;

    private final AttributionSupport attribution = new AttributionSupport();

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

    /** A timer that is used to delay invalidation events if required. */
    private final Timer invalidateLaterTimer = new Timer(100, e -> this.invalidate());

    private final MouseAdapter adapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!isVisible()) return;
            if (e.getButton() == MouseEvent.BUTTON3) {
                Component component = e.getComponent();
                if (component.isShowing()) {
                    new TileSourceLayerPopup(e.getX(), e.getY()).show(component, e.getX(), e.getY());
                }
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                attribution.handleAttribution(e.getPoint(), true);
            }
        }
    };

    private final TileSourceDisplaySettings displaySettings = createDisplaySettings();

    private final ImageryAdjustAction adjustAction = new ImageryAdjustAction(this);
    // prepared to be moved to the painter
    protected TileCoordinateConverter coordinateConverter;
    private final long minimumTileExpire;

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
        this.minimumTileExpire = info.getMinimumTileExpire();
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
     * Get projections this imagery layer supports natively.
     *
     * For example projection of tiles that are downloaded from a server. Layer
     * may support even more projections (by reprojecting the tiles), but with a
     * certain loss in image quality and performance.
     * @return projections this imagery layer supports natively; null if layer is projection agnostic.
     */
    public abstract Collection<String> getNativeProjections();

    /**
     * Creates and returns a new {@link TileSource} instance depending on {@link #info} specified in the constructor.
     *
     * @return TileSource for specified ImageryInfo
     * @throws IllegalArgumentException when Imagery is not supported by layer
     */
    protected abstract T getTileSource();

    protected Map<String, String> getHeaders(T tileSource) {
        if (tileSource instanceof TemplatedTileSource) {
            return ((TemplatedTileSource) tileSource).getHeaders();
        }
        return null;
    }

    protected void initTileSource(T tileSource) {
        coordinateConverter = new TileCoordinateConverter(MainApplication.getMap().mapView, tileSource, getDisplaySettings());
        attribution.initialize(tileSource);

        currentZoomLevel = getBestZoom();

        Map<String, String> headers = getHeaders(tileSource);

        tileLoader = getTileLoaderFactory().makeTileLoader(this, headers, minimumTileExpire);

        try {
            if ("file".equalsIgnoreCase(new URL(tileSource.getBaseUrl()).getProtocol())) {
                tileLoader = new OsmTileLoader(this);
            }
        } catch (MalformedURLException e) {
            // ignore, assume that this is not a file
            Logging.log(Logging.LEVEL_DEBUG, e);
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
        invalidateLater();
        Logging.debug("tileLoadingFinished() tile: {0} success: {1}", tile, success);
    }

    /**
     * Clears the tile cache.
     */
    public void clearTileCache() {
        if (tileLoader instanceof CachedTileLoader) {
            ((CachedTileLoader) tileLoader).clearCache(tileSource);
        }
        tileCache.clear();
    }

    @Override
    public Object getInfoComponent() {
        JPanel panel = (JPanel) super.getInfoComponent();
        List<List<String>> content = new ArrayList<>();
        Collection<String> nativeProjections = getNativeProjections();
        if (nativeProjections != null) {
            content.add(Arrays.asList(tr("Native projections"), Utils.join(", ", getNativeProjections())));
        }
        EastNorth offset = getDisplaySettings().getDisplacement();
        if (offset.distanceSq(0, 0) > 1e-10) {
            content.add(Arrays.asList(tr("Offset"), offset.east() + ";" + offset.north()));
        }
        if (coordinateConverter.requiresReprojection()) {
            content.add(Arrays.asList(tr("Tile download projection"), tileSource.getServerCRS()));
            content.add(Arrays.asList(tr("Tile display projection"), ProjectionRegistry.getProjection().toCode()));
        }
        content.add(Arrays.asList(tr("Current zoom"), Integer.toString(currentZoomLevel)));
        for (List<String> entry: content) {
            panel.add(new JLabel(entry.get(0) + ':'), GBC.std());
            panel.add(GBC.glue(5, 0), GBC.std());
            panel.add(createTextField(entry.get(1)), GBC.eol().fill(GBC.HORIZONTAL));
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
    public double getScaleFactor(int zoom) {
        if (coordinateConverter != null) {
            return coordinateConverter.getScaleFactor(zoom);
        } else {
            return 1;
        }
    }

    /**
     * Returns best zoom level.
     * @return best zoom level
     */
    public int getBestZoom() {
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
        int minZoom = getMinZoomLvl();
        int maxZoom = getMaxZoomLvl();
        if (minZoom <= maxZoom) {
            intResult = Utils.clamp(intResult, minZoom, maxZoom);
        } else if (intResult > maxZoom) {
            intResult = maxZoom;
        }
        return intResult;
    }

    /**
     * Default implementation of {@link org.openstreetmap.josm.gui.layer.Layer.LayerAction#supportLayers(List)}.
     * @param layers layers
     * @return {@code true} is layers contains only a {@code TMSLayer}
     */
    public static boolean actionSupportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof TMSLayer;
    }

    private abstract static class AbstractTileAction extends AbstractAction {

        protected final AbstractTileSourceLayer<?> layer;
        protected final Tile tile;

        AbstractTileAction(String name, AbstractTileSourceLayer<?> layer, Tile tile) {
            super(name);
            this.layer = layer;
            this.tile = tile;
        }
    }

    private static final class ShowTileInfoAction extends AbstractTileAction {

        private ShowTileInfoAction(AbstractTileSourceLayer<?> layer, Tile tile) {
            super(tr("Show tile info"), layer, tile);
            setEnabled(tile != null);
        }

        private static String getSizeString(int size) {
            return new StringBuilder().append(size).append('x').append(size).toString();
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (tile != null) {
                ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), tr("Tile Info"), tr("OK"));
                JPanel panel = new JPanel(new GridBagLayout());
                Rectangle2D displaySize = layer.coordinateConverter.getRectangleForTile(tile);
                String url = "";
                try {
                    url = tile.getUrl();
                } catch (IOException e) {
                    // silence exceptions
                    Logging.trace(e);
                }

                List<List<String>> content = new ArrayList<>();
                content.add(Arrays.asList(tr("Tile name"), tile.getKey()));
                content.add(Arrays.asList(tr("Tile URL"), url));
                if (tile.getTileSource() instanceof TemplatedTileSource) {
                    Map<String, String> headers = ((TemplatedTileSource) tile.getTileSource()).getHeaders();
                    for (String key: new TreeSet<>(headers.keySet())) {
                        // iterate over sorted keys
                        content.add(Arrays.asList(tr("Custom header: {0}", key), headers.get(key)));
                    }
                }
                content.add(Arrays.asList(tr("Tile size"),
                        getSizeString(tile.getTileSource().getTileSize())));
                content.add(Arrays.asList(tr("Tile display size"),
                        new StringBuilder().append(displaySize.getWidth())
                                .append('x')
                                .append(displaySize.getHeight()).toString()));
                if (layer.coordinateConverter.requiresReprojection()) {
                    content.add(Arrays.asList(tr("Reprojection"),
                            tile.getTileSource().getServerCRS() +
                            " -> " + ProjectionRegistry.getProjection().toCode()));
                    BufferedImage img = tile.getImage();
                    if (img != null) {
                        content.add(Arrays.asList(tr("Reprojected tile size"),
                            img.getWidth() + "x" + img.getHeight()));

                    }
                }
                content.add(Arrays.asList(tr("Status:"), tr(tile.getStatus())));
                content.add(Arrays.asList(tr("Loaded:"), tr(Boolean.toString(tile.isLoaded()))));
                content.add(Arrays.asList(tr("Loading:"), tr(Boolean.toString(tile.isLoading()))));
                content.add(Arrays.asList(tr("Error:"), tr(Boolean.toString(tile.hasError()))));
                for (List<String> entry: content) {
                    panel.add(new JLabel(entry.get(0) + ':'), GBC.std());
                    panel.add(GBC.glue(5, 0), GBC.std());
                    panel.add(layer.createTextField(entry.get(1)), GBC.eol().fill(GBC.HORIZONTAL));
                }

                for (Entry<String, String> e: tile.getMetadata().entrySet()) {
                    panel.add(new JLabel(tr("Metadata ") + tr(e.getKey()) + ':'), GBC.std());
                    panel.add(GBC.glue(5, 0), GBC.std());
                    String value = e.getValue();
                    if ("lastModification".equals(e.getKey()) || "expirationTime".equals(e.getKey())) {
                        value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(value)));
                    }
                    panel.add(layer.createTextField(value), GBC.eol().fill(GBC.HORIZONTAL));

                }
                ed.setIcon(JOptionPane.INFORMATION_MESSAGE);
                ed.setContent(panel);
                ed.showDialog();
            }
        }
    }

    private static final class LoadTileAction extends AbstractTileAction {

        private LoadTileAction(AbstractTileSourceLayer<?> layer, Tile tile) {
            super(tr("Load tile"), layer, tile);
            setEnabled(tile != null);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (tile != null) {
                layer.loadTile(tile, true);
                layer.invalidate();
            }
        }
    }

    private static void sendOsmTileRequest(Tile tile, String request) {
        if (tile != null) {
            try {
                new Notification(HttpClient.create(new URL(tile.getUrl() + '/' + request))
                        .connect().fetchContent()).show();
            } catch (IOException ex) {
                Logging.error(ex);
            }
        }
    }

    private static final class GetOsmTileStatusAction extends AbstractTileAction {
        private GetOsmTileStatusAction(AbstractTileSourceLayer<?> layer, Tile tile) {
            super(tr("Get tile status"), layer, tile);
            setEnabled(tile != null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            sendOsmTileRequest(tile, "status");
        }
    }

    private static final class MarkOsmTileDirtyAction extends AbstractTileAction {
        private MarkOsmTileDirtyAction(AbstractTileSourceLayer<?> layer, Tile tile) {
            super(tr("Force tile rendering"), layer, tile);
            setEnabled(tile != null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            sendOsmTileRequest(tile, "dirty");
        }
    }

    /**
     * Creates popup menu items and binds to mouse actions
     */
    @Override
    public void hookUpMapView() {
        // this needs to be here and not in constructor to allow empty TileSource class construction using SessionWriter
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

        // FIXME: why do we need this? Without this, if you add a WMS layer and do not move the mouse, sometimes, tiles do not start loading.
        // FIXME: Check if this is still required.
        event.getMapView().repaint(500);

        return super.attachToMapView(event);
    }

    private void initializeIfRequired() {
        if (tileSource == null) {
            tileSource = getTileSource();
            if (tileSource == null) {
                throw new IllegalArgumentException(tr("Failed to create tile source"));
            }
            // check if projection is supported
            projectionChanged(null, ProjectionRegistry.getProjection());
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
         * @param x horizontal dimension where user clicked
         * @param y vertical dimension where user clicked
         */
        public TileSourceLayerPopup(int x, int y) {
            List<JMenu> submenus = new ArrayList<>();
            MainApplication.getLayerManager().getVisibleLayersInZOrder().stream()
            .filter(AbstractTileSourceLayer.class::isInstance)
            .map(AbstractTileSourceLayer.class::cast)
            .forEachOrdered(layer -> {
                JMenu submenu = new JMenu(layer.getName());
                for (Action a : layer.getCommonEntries()) {
                    if (a instanceof LayerAction) {
                        submenu.add(((LayerAction) a).createMenuComponent());
                    } else {
                        submenu.add(new JMenuItem(a));
                    }
                }
                submenu.add(new JSeparator());
                Tile tile = layer.getTileForPixelpos(x, y);
                submenu.add(new JMenuItem(new LoadTileAction(layer, tile)));
                submenu.add(new JMenuItem(new ShowTileInfoAction(layer, tile)));
                if (ExpertToggleAction.isExpert() && tileSource != null && tileSource.isModTileFeatures()) {
                    submenu.add(new JMenuItem(new GetOsmTileStatusAction(layer, tile)));
                    submenu.add(new JMenuItem(new MarkOsmTileDirtyAction(layer, tile)));
                }
                submenus.add(submenu);
            });

            if (submenus.size() == 1) {
                JMenu menu = submenus.get(0);
                Arrays.stream(menu.getMenuComponents()).forEachOrdered(this::add);
            } else if (submenus.size() > 1) {
                submenus.stream().forEachOrdered(this::add);
            }
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
        Logging.info("AbstractTileSourceLayer: estimated visible tiles: {0}, estimated cache size: {1}", visibileTiles, ret);
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
            // e.g. displacement
            // trigger a redraw in every case
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
        zoomChanged(true);
    }

    private void zoomChanged(boolean invalidate) {
        Logging.debug("zoomChanged(): {0}", currentZoomLevel);
        if (tileLoader instanceof TMSCachedTileLoader) {
            ((TMSCachedTileLoader) tileLoader).cancelOutstandingTasks();
        }
        if (invalidate) {
            invalidate();
        }
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
        Logging.debug("zoomIncreaseAllowed(): {0} {1} vs. {2}", zia, currentZoomLevel, this.getMaxZoomLvl());
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
            Logging.debug("increasing zoom level to: {0}", currentZoomLevel);
            zoomChanged();
        } else {
            Logging.warn("Current zoom level ("+currentZoomLevel+") could not be increased. "+
                    "Max.zZoom Level "+this.getMaxZoomLvl()+" reached.");
            return false;
        }
        return true;
    }

    /**
     * Get the current zoom level of the layer
     * @return the current zoom level
     * @since 12603
     */
    public int getZoomLevel() {
        return currentZoomLevel;
    }

    /**
     * Sets the zoom level of the layer
     * @param zoom zoom level
     * @return true, when zoom has changed to desired value, false if it was outside supported zoom levels
     */
    public boolean setZoomLevel(int zoom) {
        return setZoomLevel(zoom, true);
    }

    private boolean setZoomLevel(int zoom, boolean invalidate) {
        if (zoom == currentZoomLevel) return true;
        if (zoom > this.getMaxZoomLvl()) return false;
        if (zoom < this.getMinZoomLvl()) return false;
        currentZoomLevel = zoom;
        zoomChanged(invalidate);
        return true;
    }

    /**
     * Check if zooming out is allowed
     *
     * @return    true, if zooming out is allowed (currentZoomLevel &gt; minZoomLevel)
     */
    public boolean zoomDecreaseAllowed() {
        boolean zda = currentZoomLevel > this.getMinZoomLvl();
        Logging.debug("zoomDecreaseAllowed(): {0} {1} vs. {2}", zda, currentZoomLevel, this.getMinZoomLvl());
        return zda;
    }

    /**
     * Zoom out from map.
     *
     * @return    true, if zoom increasing was successfull, false othervise
     */
    public boolean decreaseZoomLevel() {
        if (zoomDecreaseAllowed()) {
            Logging.debug("decreasing zoom level to: {0}", currentZoomLevel);
            currentZoomLevel--;
            zoomChanged();
        } else {
            return false;
        }
        return true;
    }

    private Tile getOrCreateTile(TilePosition tilePosition) {
        return getOrCreateTile(tilePosition.getX(), tilePosition.getY(), tilePosition.getZoom());
    }

    private Tile getOrCreateTile(int x, int y, int zoom) {
        Tile tile = getTile(x, y, zoom);
        if (tile == null) {
            if (coordinateConverter.requiresReprojection()) {
                tile = new ReprojectionTile(tileSource, x, y, zoom);
            } else {
                tile = new Tile(tileSource, x, y, zoom);
            }
            tileCache.addTile(tile);
        }
        return tile;
    }

    private Tile getTile(TilePosition tilePosition) {
        return getTile(tilePosition.getX(), tilePosition.getY(), tilePosition.getZoom());
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
        if (!force && tile.isLoaded())
            return false;
        if (tile.isLoading())
            return false;
        tileLoader.createTileLoaderJob(tile).submit(force);
        return true;
    }

    private TileSet getVisibleTileSet() {
        ProjectionBounds bounds = MainApplication.getMap().mapView.getState().getViewArea().getProjectionBounds();
        return getTileSet(bounds, currentZoomLevel);
    }

    /**
     * Load all visible tiles.
     * @param force {@code true} to force loading if auto-load is disabled
     * @since 11950
     */
    public void loadAllTiles(boolean force) {
        TileSet ts = getVisibleTileSet();

        // if there is more than 18 tiles on screen in any direction, do not load all tiles!
        if (ts.tooLarge()) {
            Logging.warn("Not downloading all tiles because there is more than 18 tiles on an axis!");
            return;
        }
        ts.loadAllTiles(force);
        invalidate();
    }

    /**
     * Load all visible tiles in error.
     * @param force {@code true} to force loading if auto-load is disabled
     * @since 11950
     */
    public void loadAllErrorTiles(boolean force) {
        TileSet ts = getVisibleTileSet();
        ts.loadAllErrorTiles(force);
        invalidate();
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = (infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0;
        Logging.debug("imageUpdate() done: {0} calling repaint", done);

        if (done) {
            invalidate();
        } else {
            invalidateLater();
        }
        return !done;
    }

    /**
     * Invalidate the layer at a time in the future so that the user still sees the interface responsive.
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
        return (status & ALLBITS) != 0;
    }

    /**
     * Returns the image for the given tile image is loaded.
     * Otherwise returns  null.
     *
     * @param tile the Tile for which the image should be returned
     * @return  the image of the tile or null.
     */
    private BufferedImage getLoadedTileImage(Tile tile) {
        BufferedImage img = tile.getImage();
        if (!imageLoaded(img))
            return null;
        return img;
    }

    /**
     * Draw a tile image on screen.
     * @param g the Graphics2D
     * @param toDrawImg tile image
     * @param anchorImage tile anchor in image coordinates
     * @param anchorScreen tile anchor in screen coordinates
     * @param clip clipping region in screen coordinates (can be null)
     */
    private void drawImageInside(Graphics2D g, BufferedImage toDrawImg, TileAnchor anchorImage, TileAnchor anchorScreen, Shape clip) {
        AffineTransform imageToScreen = anchorImage.convert(anchorScreen);
        Point2D screen0 = imageToScreen.transform(new Point.Double(0, 0), null);
        Point2D screen1 = imageToScreen.transform(new Point.Double(
                toDrawImg.getWidth(), toDrawImg.getHeight()), null);

        Shape oldClip = null;
        if (clip != null) {
            oldClip = g.getClip();
            g.clip(clip);
        }
        g.drawImage(toDrawImg, (int) Math.round(screen0.getX()), (int) Math.round(screen0.getY()),
                (int) Math.round(screen1.getX()) - (int) Math.round(screen0.getX()),
                (int) Math.round(screen1.getY()) - (int) Math.round(screen0.getY()), this);
        if (clip != null) {
            g.setClip(oldClip);
        }
    }

    private List<Tile> paintTileImages(Graphics2D g, TileSet ts) {
        Object paintMutex = new Object();
        List<TilePosition> missed = Collections.synchronizedList(new ArrayList<>());
        ts.visitTiles(tile -> {
            boolean miss = false;
            BufferedImage img = null;
            TileAnchor anchorImage = null;
            if (!tile.isLoaded() || tile.hasError()) {
                miss = true;
            } else {
                synchronized (tile) {
                    img = getLoadedTileImage(tile);
                    anchorImage = getAnchor(tile, img);
                }
                if (img == null || anchorImage == null) {
                    miss = true;
                }
            }
            if (miss) {
                missed.add(new TilePosition(tile));
                return;
            }

            img = applyImageProcessors(img);

            TileAnchor anchorScreen = coordinateConverter.getScreenAnchorForTile(tile);
            synchronized (paintMutex) {
                //cannot paint in parallel
                drawImageInside(g, img, anchorImage, anchorScreen, null);
            }
            MapView mapView = MainApplication.getMap().mapView;
            if (tile instanceof ReprojectionTile && ((ReprojectionTile) tile).needsUpdate(mapView.getScale())) {
                // This means we have a reprojected tile in memory cache, but not at
                // current scale. Generally, the positioning of the tile will still
                // be correct, but for best image quality, the tile should be
                // reprojected to the target scale. The original tile image should
                // still be in disk cache, so this is fairly cheap.
                ((ReprojectionTile) tile).invalidate();
                loadTile(tile, false);
            }

        }, missed::add);

        return missed.stream().map(this::getOrCreateTile).collect(Collectors.toList());
    }

    // This function is called for several zoom levels, not just the current one.
    // It should not trigger any tiles to be downloaded.
    // It should also avoid polluting the tile cache with any tiles since these tiles are not mandatory.
    //
    // The "border" tile tells us the boundaries of where we may drawn.
    // It will not be from the zoom level that is being drawn currently.
    // If drawing the displayZoomLevel, border is null and we draw the entire tile set.
    private List<Tile> paintTileImages(Graphics2D g, TileSet ts, int zoom, Tile border) {
        if (zoom <= 0) return Collections.emptyList();
        Shape borderClip = coordinateConverter.getTileShapeScreen(border);
        List<Tile> missedTiles = new LinkedList<>();
        // The callers of this code *require* that we return any tiles that we do not draw in missedTiles.
        // ts.allExistingTiles() by default will only return already-existing tiles.
        // However, we need to return *all* tiles to the callers, so force creation here.
        for (Tile tile : ts.allTilesCreate()) {
            boolean miss = false;
            BufferedImage img = null;
            TileAnchor anchorImage = null;
            if (!tile.isLoaded() || tile.hasError()) {
                miss = true;
            } else {
                synchronized (tile) {
                    img = getLoadedTileImage(tile);
                    anchorImage = getAnchor(tile, img);
                }

                if (img == null || anchorImage == null) {
                    miss = true;
                }
            }
            if (miss) {
                missedTiles.add(tile);
                continue;
            }

            // applying all filters to this layer
            img = applyImageProcessors(img);

            Shape clip;
            if (tileSource.isInside(tile, border)) {
                clip = null;
            } else if (tileSource.isInside(border, tile)) {
                clip = borderClip;
            } else {
                continue;
            }
            TileAnchor anchorScreen = coordinateConverter.getScreenAnchorForTile(tile);
            drawImageInside(g, img, anchorImage, anchorScreen, clip);
        }
        return missedTiles;
    }

    private static TileAnchor getAnchor(Tile tile, BufferedImage image) {
        if (tile instanceof ReprojectionTile) {
            return ((ReprojectionTile) tile).getAnchor();
        } else if (image != null) {
            return new TileAnchor(new Point.Double(0, 0), new Point.Double(image.getWidth(), image.getHeight()));
        } else {
            return null;
        }
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

    private void paintTileText(Tile tile, Graphics2D g) {
        if (tile == null) {
            return;
        }
        Point2D p = coordinateConverter.getPixelForTile(tile);
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
        }

        String tileStatus = tile.getStatus();
        if (!tile.isLoaded() && PROP_DRAW_DEBUG.get()) {
            myDrawString(g, tr("image " + tileStatus), p.x + 2, texty);
            texty += 1 + fontHeight;
        }*/

        if (tile.hasError() && getDisplaySettings().isShowErrors()) {
            myDrawString(g, tr("Error") + ": " + tr(tile.getErrorMessage()), x + 2, texty);
            //texty += 1 + fontHeight;
        }

        if (Logging.isDebugEnabled()) {
            // draw tile outline in semi-transparent red
            g.setColor(new Color(255, 0, 0, 50));
            g.draw(coordinateConverter.getTileShapeScreen(tile));
        }
    }

    private LatLon getShiftedLatLon(EastNorth en) {
        return coordinateConverter.getProjecting().eastNorth2latlonClamped(en);
    }

    private ICoordinate getShiftedCoord(EastNorth en) {
        return CoordinateConversion.llToCoor(getShiftedLatLon(en));
    }

    private final TileSet nullTileSet = new TileSet();

    protected class TileSet extends TileRange {

        private volatile TileSetInfo info;

        protected TileSet(TileXY t1, TileXY t2, int zoom) {
            super(t1, t2, zoom);
            sanitize();
        }

        protected TileSet(TileRange range) {
            super(range);
            sanitize();
        }

        /**
         * null tile set
         */
        private TileSet() {
            // default
        }

        protected void sanitize() {
            minX = Utils.clamp(minX, tileSource.getTileXMin(zoom), tileSource.getTileXMax(zoom));
            maxX = Utils.clamp(maxX, tileSource.getTileXMin(zoom), tileSource.getTileXMax(zoom));
            minY = Utils.clamp(minY, tileSource.getTileYMin(zoom), tileSource.getTileYMax(zoom));
            maxY = Utils.clamp(maxY, tileSource.getTileYMin(zoom), tileSource.getTileYMax(zoom));
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

        /**
         * Get all tiles represented by this TileSet that are already in the tileCache.
         * @return all tiles represented by this TileSet that are already in the tileCache
         */
        private List<Tile> allExistingTiles() {
            return allTiles(AbstractTileSourceLayer.this::getTile);
        }

        private List<Tile> allTilesCreate() {
            return allTiles(AbstractTileSourceLayer.this::getOrCreateTile);
        }

        private List<Tile> allTiles(Function<TilePosition, Tile> mapper) {
            return tilePositions().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
        }

        /**
         * Gets a stream of all tile positions in this set
         * @return A stream of all positions
         */
        public Stream<TilePosition> tilePositions() {
            if (zoom == 0 || this.insane()) {
                return Stream.empty(); // Tileset is either empty or too large
            } else {
                return IntStream.rangeClosed(minX, maxX).mapToObj(
                        x -> IntStream.rangeClosed(minY, maxY).mapToObj(y -> new TilePosition(x, y, zoom))
                        ).flatMap(Function.identity());
            }
        }

        private List<Tile> allLoadedTiles() {
            return allExistingTiles().stream().filter(Tile::isLoaded).collect(Collectors.toList());
        }

        /**
         * @return comparator, that sorts the tiles from the center to the edge of the current screen
         */
        private Comparator<Tile> getTileDistanceComparator() {
            final int centerX = (int) Math.ceil((minX + maxX) / 2d);
            final int centerY = (int) Math.ceil((minY + maxY) / 2d);
            return Comparator.comparingInt(t -> Math.abs(t.getXtile() - centerX) + Math.abs(t.getYtile() - centerY));
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

        /**
         * Call the given paint method for all tiles in this tile set.<p>
         * Uses a parallel stream.
         * @param visitor A visitor to call for each tile.
         * @param missed a consumer to call for each missed tile.
         */
        public void visitTiles(Consumer<Tile> visitor, Consumer<TilePosition> missed) {
            tilePositions().parallel().forEach(tp -> visitTilePosition(visitor, tp, missed));
        }

        private void visitTilePosition(Consumer<Tile> visitor, TilePosition tp, Consumer<TilePosition> missed) {
            Tile tile = getTile(tp);
            if (tile == null) {
                missed.accept(tp);
            } else {
                visitor.accept(tile);
            }
        }

        /**
         * Check if there is any tile fully loaded without error.
         * @return true if there is any tile fully loaded without error
         */
        public boolean hasVisibleTiles() {
            return getTileSetInfo().hasVisibleTiles;
        }

        /**
         * Check if there there is a tile that is overzoomed.
         * <p>
         * I.e. the server response for one tile was "there is no tile here".
         * This usually happens when zoomed in too much. The limit depends on
         * the region, so at the edge of such a region, some tiles may be
         * available and some not.
         * @return true if there there is a tile that is overzoomed
         */
        public boolean hasOverzoomedTiles() {
            return getTileSetInfo().hasOverzoomedTiles;
        }

        /**
         * Check if there are tiles still loading.
         * <p>
         * This is the case if there is a tile not yet in the cache, or in the
         * cache but marked as loading ({@link Tile#isLoading()}.
         * @return true if there are tiles still loading
         */
        public boolean hasLoadingTiles() {
            return getTileSetInfo().hasLoadingTiles;
        }

        /**
         * Check if all tiles in the range are fully loaded.
         * <p>
         * A tile is considered to be fully loaded even if the result of loading
         * the tile was an error.
         * @return true if all tiles in the range are fully loaded
         */
        public boolean hasAllLoadedTiles() {
            return getTileSetInfo().hasAllLoadedTiles;
        }

        private TileSetInfo getTileSetInfo() {
            if (info == null) {
                synchronized (this) {
                    if (info == null) {
                        List<Tile> allTiles = this.allExistingTiles();
                        TileSetInfo newInfo = new TileSetInfo();
                        newInfo.hasLoadingTiles = allTiles.size() < this.size();
                        newInfo.hasAllLoadedTiles = true;
                        for (Tile t : allTiles) {
                            if ("no-tile".equals(t.getValue("tile-info"))) {
                                newInfo.hasOverzoomedTiles = true;
                            }
                            if (t.isLoaded()) {
                                if (!t.hasError()) {
                                    newInfo.hasVisibleTiles = true;
                                }
                            } else {
                                newInfo.hasAllLoadedTiles = false;
                                if (t.isLoading()) {
                                    newInfo.hasLoadingTiles = true;
                                }
                            }
                        }
                        info = newInfo;
                    }
                }
            }
            return info;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": zoom: " + zoom + " X(" + minX + ", " + maxX + ") Y(" + minY + ", " + maxY + ") size: " + size();
        }
    }

    /**
     * Data container to hold information about a {@code TileSet} class.
     */
    private static class TileSetInfo {
        boolean hasVisibleTiles;
        boolean hasOverzoomedTiles;
        boolean hasLoadingTiles;
        boolean hasAllLoadedTiles;
    }

    /**
     * Create a TileSet by EastNorth bbox taking a layer shift in account
     * @param bounds the EastNorth bounds
     * @param zoom zoom level
     * @return the tile set
     */
    protected TileSet getTileSet(ProjectionBounds bounds, int zoom) {
        if (zoom == 0)
            return new TileSet();
        TileXY t1, t2;
        IProjected topLeftUnshifted = coordinateConverter.shiftDisplayToServer(bounds.getMin());
        IProjected botRightUnshifted = coordinateConverter.shiftDisplayToServer(bounds.getMax());
        if (coordinateConverter.requiresReprojection()) {
            Projection projServer = Projections.getProjectionByCode(tileSource.getServerCRS());
            if (projServer == null) {
                throw new IllegalStateException(tileSource.toString());
            }
            ProjectionBounds projBounds = new ProjectionBounds(
                    CoordinateConversion.projToEn(topLeftUnshifted),
                    CoordinateConversion.projToEn(botRightUnshifted));
            ProjectionBounds bbox = projServer.getEastNorthBoundsBox(projBounds, ProjectionRegistry.getProjection());
            t1 = tileSource.projectedToTileXY(CoordinateConversion.enToProj(bbox.getMin()), zoom);
            t2 = tileSource.projectedToTileXY(CoordinateConversion.enToProj(bbox.getMax()), zoom);
        } else {
            t1 = tileSource.projectedToTileXY(topLeftUnshifted, zoom);
            t2 = tileSource.projectedToTileXY(botRightUnshifted, zoom);
        }
        return new TileSet(t1, t2, zoom);
    }

    private class DeepTileSet {
        private final ProjectionBounds bounds;
        private final int minZoom, maxZoom;
        private final TileSet[] tileSets;

        @SuppressWarnings("unchecked")
        DeepTileSet(ProjectionBounds bounds, int minZoom, int maxZoom) {
            this.bounds = bounds;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
            if (minZoom > maxZoom) {
                throw new IllegalArgumentException(minZoom + " > " + maxZoom);
            }
            this.tileSets = new AbstractTileSourceLayer.TileSet[maxZoom - minZoom + 1];
        }

        public TileSet getTileSet(int zoom) {
            if (zoom < minZoom)
                return nullTileSet;
            synchronized (tileSets) {
                TileSet ts = tileSets[zoom-minZoom];
                if (ts == null) {
                    ts = AbstractTileSourceLayer.this.getTileSet(bounds, zoom);
                    tileSets[zoom-minZoom] = ts;
                }
                return ts;
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

        int displayZoomLevel = zoom;

        boolean noTilesAtZoom = false;
        if (getDisplaySettings().isAutoZoom() && getDisplaySettings().isAutoLoad()) {
            // Auto-detection of tilesource maxzoom (currently fully works only for Bing)
            TileSet ts0 = dts.getTileSet(zoom);
            if (!ts0.hasVisibleTiles() && (!ts0.hasLoadingTiles() || ts0.hasOverzoomedTiles())) {
                noTilesAtZoom = true;
            }
            // Find highest zoom level with at least one visible tile
            for (int tmpZoom = zoom; tmpZoom > dts.minZoom; tmpZoom--) {
                if (dts.getTileSet(tmpZoom).hasVisibleTiles()) {
                    displayZoomLevel = tmpZoom;
                    break;
                }
            }
            // Do binary search between currentZoomLevel and displayZoomLevel
            while (zoom > displayZoomLevel && !ts0.hasVisibleTiles() && ts0.hasOverzoomedTiles()) {
                zoom = (zoom + displayZoomLevel)/2;
                ts0 = dts.getTileSet(zoom);
            }

            setZoomLevel(zoom, false);

            // If all tiles at displayZoomLevel is loaded, load all tiles at next zoom level
            // to make sure there're really no more zoom levels
            // loading is done in the next if section
            if (zoom == displayZoomLevel && !ts0.hasLoadingTiles() && zoom < dts.maxZoom) {
                zoom++;
                ts0 = dts.getTileSet(zoom);
            }
            // When we have overzoomed tiles and all tiles at current zoomlevel is loaded,
            // load tiles at previovus zoomlevels until we have all tiles on screen is loaded.
            // loading is done in the next if section
            while (zoom > dts.minZoom && ts0.hasOverzoomedTiles() && !ts0.hasLoadingTiles()) {
                zoom--;
                ts0 = dts.getTileSet(zoom);
            }
        } else if (getDisplaySettings().isAutoZoom()) {
            setZoomLevel(zoom, false);
        }
        TileSet ts = dts.getTileSet(zoom);

        // Too many tiles... refuse to download
        if (!ts.tooLarge()) {
            // try to load tiles from desired zoom level, no matter what we will show (for example, tiles from previous zoom level
            // on zoom in)
            ts.loadAllTiles(false);
        }

        if (displayZoomLevel != zoom) {
            ts = dts.getTileSet(displayZoomLevel);
            if (!dts.getTileSet(displayZoomLevel).hasAllLoadedTiles() && displayZoomLevel < zoom) {
                // if we are showing tiles from lower zoom level, ensure that all tiles are loaded as they are few,
                // and should not trash the tile cache
                // This is especially needed when dts.getTileSet(zoom).tooLarge() is true and we are not loading tiles
                ts.loadAllTiles(false);
            }
        }

        g.setColor(Color.DARK_GRAY);

        List<Tile> missedTiles = this.paintTileImages(g, ts);
        int[] otherZooms = {1, 2, -1, -2, -3, -4, -5};
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
                if (zoomOffset > 0 && "no-tile".equals(missed.getValue("tile-info"))) {
                    // Don't try to paint from higher zoom levels when tile is overzoomed
                    newlyMissedTiles.add(missed);
                    continue;
                }
                TileSet ts2 = new TileSet(tileSource.getCoveringTileRange(missed, newzoom));
                // Instantiating large TileSets is expensive. If there are no loaded tiles, don't bother even trying.
                if (ts2.allLoadedTiles().isEmpty()) {
                    if (zoomOffset > 0) {
                        newlyMissedTiles.add(missed);
                        continue;
                    } else {
                        /*
                         *  We have negative zoom offset. Try to load tiles from lower zoom levels, as they may be not present
                         *  in tile cache (e.g. when user panned the map or opened layer above zoom level, for which tiles are present.
                         *  This will ensure, that tileCache is populated with tiles from lower zoom levels so it will be possible to
                         *  use them to paint overzoomed tiles.
                         *  See: #14562
                         */
                        if (!ts.hasLoadingTiles()) {
                            ts2.loadAllTiles(false);
                        }
                    }
                }
                if (ts2.tooLarge()) {
                    continue;
                }
                newlyMissedTiles.addAll(this.paintTileImages(g, ts2, newzoom, missed));
            }
            missedTiles = newlyMissedTiles;
        }
        if (Logging.isDebugEnabled() && !missedTiles.isEmpty()) {
            Logging.debug("still missed {0} in the end", missedTiles.size());
        }
        g.setColor(Color.red);
        g.setFont(InfoFont);

        // The current zoom tileset should have all of its tiles due to the loadAllTiles(), unless it to tooLarge()
        for (Tile t : ts.allExistingTiles()) {
            this.paintTileText(t, g);
        }

        EastNorth min = pb.getMin();
        EastNorth max = pb.getMax();
        attribution.paintAttribution(g, mv.getWidth(), mv.getHeight(), getShiftedCoord(min), getShiftedCoord(max),
                displayZoomLevel, this);

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
        if (Logging.isDebugEnabled()) {
            myDrawString(g, tr("Current zoom: {0}", currentZoomLevel), 50, 140);
            myDrawString(g, tr("Display zoom: {0}", displayZoomLevel), 50, 155);
            myDrawString(g, tr("Pixel scale: {0}", getScaleFactor(currentZoomLevel)), 50, 170);
            myDrawString(g, tr("Best zoom: {0}", getBestZoom()), 50, 185);
            myDrawString(g, tr("Estimated cache size: {0}", estimateTileCacheSize()), 50, 200);
            if (tileLoader instanceof TMSCachedTileLoader) {
                int offset = 200;
                for (String part: ((TMSCachedTileLoader) tileLoader).getStats().split("\n")) {
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
        Logging.debug("getTileForPixelpos({0}, {1})", px, py);
        TileXY xy = coordinateConverter.getTileforPixel(px, py, currentZoomLevel);
        return getTile(xy.getXIndex(), xy.getYIndex(), currentZoomLevel);
    }

    /**
     * Class to store a menu action and the class it belongs to.
     */
    private static class MenuAddition {
        final Action addition;
        @SuppressWarnings("rawtypes")
        final Class<? extends AbstractTileSourceLayer> clazz;

        @SuppressWarnings("rawtypes")
        MenuAddition(Action addition, Class<? extends AbstractTileSourceLayer> clazz) {
            this.addition = addition;
            this.clazz = clazz;
        }
    }

    /**
     * Register an additional layer context menu entry.
     *
     * @param addition additional menu action
     * @since 11197
     */
    public static void registerMenuAddition(Action addition) {
        menuAdditions.add(new MenuAddition(addition, AbstractTileSourceLayer.class));
    }

    /**
     * Register an additional layer context menu entry for a imagery layer
     * class.  The menu entry is valid for the specified class and subclasses
     * thereof only.
     * <p>
     * Example:
     * <pre>
     * TMSLayer.registerMenuAddition(new TMSSpecificAction(), TMSLayer.class);
     * </pre>
     *
     * @param addition additional menu action
     * @param clazz class the menu action is registered for
     * @since 11197
     */
    public static void registerMenuAddition(Action addition,
                                            Class<? extends AbstractTileSourceLayer<?>> clazz) {
        menuAdditions.add(new MenuAddition(addition, clazz));
    }

    /**
     * Prepare list of additional layer context menu entries.  The list is
     * empty if there are no additional menu entries.
     *
     * @return list of additional layer context menu entries
     */
    private List<Action> getMenuAdditions() {
        final LinkedList<Action> menuAdds = new LinkedList<>();
        for (MenuAddition menuAdd: menuAdditions) {
            if (menuAdd.clazz.isInstance(this)) {
                menuAdds.add(menuAdd.addition);
            }
        }
        if (!menuAdds.isEmpty()) {
            menuAdds.addFirst(SeparatorLayerAction.INSTANCE);
        }
        return menuAdds;
    }

    @Override
    public Action[] getMenuEntries() {
        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(getLayerListEntries()));
        actions.addAll(Arrays.asList(getCommonEntries()));
        actions.addAll(getMenuAdditions());
        actions.add(SeparatorLayerAction.INSTANCE);
        actions.add(new LayerListPopup.InfoAction(this));
        return actions.toArray(new Action[0]);
    }

    /**
     * Returns the contextual menu entries in layer list dialog.
     * @return the contextual menu entries in layer list dialog
     */
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
            new AutoLoadTilesAction(this),
            new AutoZoomAction(this),
            new ShowErrorsAction(this),
            new IncreaseZoomAction(this),
            new DecreaseZoomAction(this),
            new ZoomToBestAction(this),
            new ZoomToNativeLevelAction(this),
            new FlushTileCacheAction(this),
            new LoadErroneousTilesAction(this),
            new LoadAllTilesAction(this)
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
            this.tileLoader = getTileLoaderFactory().makeTileLoader(this, getHeaders(tileSource), minimumTileExpire);
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
                Logging.warn("Tile loading failure: " + tile + " - " + tile.getErrorMessage());
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
            TileXY curTile = tileSource.latLonToTileXY(CoordinateConversion.llToCoor(point), currentZoomLevel);
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
    public synchronized void destroy() {
        super.destroy();
        adjustAction.destroy();
    }

    private class TileSourcePainter extends CompatibilityModeLayerPainter {
        /** The memory handle that will hold our tile source. */
        private MemoryHandle<?> memory;

        @Override
        public void paint(MapViewGraphics graphics) {
            allocateCacheMemory();
            if (memory != null) {
                doPaint(graphics);
            }
        }

        private void doPaint(MapViewGraphics graphics) {
            try {
                drawInViewArea(graphics.getDefaultGraphics(), graphics.getMapView(), graphics.getClipBounds().getProjectionBounds());
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw BugReport.intercept(e)
                               .put("graphics", graphics).put("tileSource", tileSource).put("currentZoomLevel", currentZoomLevel);
            }
        }

        private void allocateCacheMemory() {
            if (memory == null) {
                MemoryManager manager = MemoryManager.getInstance();
                if (manager.isAvailable(getEstimatedCacheSize())) {
                    try {
                        memory = manager.allocateMemory("tile source layer", getEstimatedCacheSize(), Object::new);
                    } catch (NotEnoughMemoryException e) {
                        Logging.warn("Could not allocate tile source memory", e);
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

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        super.projectionChanged(oldValue, newValue);
        displaySettings.setOffsetBookmark(displaySettings.getOffsetBookmark());
        if (tileCache != null) {
            tileCache.clear();
        }
    }

    @Override
    protected List<OffsetMenuEntry> getOffsetMenuEntries() {
        return OffsetBookmark.getBookmarks()
            .stream()
            .filter(b -> b.isUsable(this))
            .map(OffsetMenuBookmarkEntry::new)
            .collect(Collectors.toList());
    }

    /**
     * An entry for a bookmark in the offset menu.
     * @author Michael Zangl
     */
    private class OffsetMenuBookmarkEntry implements OffsetMenuEntry {
        private final OffsetBookmark bookmark;

        OffsetMenuBookmarkEntry(OffsetBookmark bookmark) {
            this.bookmark = bookmark;

        }

        @Override
        public String getLabel() {
            return bookmark.getName();
        }

        @Override
        public boolean isActive() {
            EastNorth offset = bookmark.getDisplacement(ProjectionRegistry.getProjection());
            EastNorth active = getDisplaySettings().getDisplacement();
            return Utils.equalsEpsilon(offset.east(), active.east()) && Utils.equalsEpsilon(offset.north(), active.north());
        }

        @Override
        public void actionPerformed() {
            getDisplaySettings().setOffsetBookmark(bookmark);
        }
    }
}
