// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

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
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.WMSLayerImporter;
import org.openstreetmap.josm.tools.GBC;

/**
 * Base abstract class that supports displaying images provided by TileSource. It might be TMS source, WMS or WMTS
 *
 * It implements all standard functions of tilesource based layers: autozoom,  tile reloads, layer saving, loading,etc.
 *
 * @author Upliner
 * @author Wiktor Niesiobędzki
 * @since 3715
 * @since 8526 (copied from TMSLayer)
 */
public abstract class AbstractTileSourceLayer extends ImageryLayer implements ImageObserver, TileLoaderListener, ZoomChangeListener {
    private static final String PREFERENCE_PREFIX   = "imagery.generic";

    /** maximum zoom level supported */
    public static final int MAX_ZOOM = 30;
    /** minium zoom level supported */
    public static final int MIN_ZOOM = 2;
    private static final Font InfoFont = new Font("sansserif", Font.BOLD, 13);

    /** do set autozoom when creating a new layer */
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty(PREFERENCE_PREFIX + ".default_autozoom", true);
    /** do set autoload when creating a new layer */
    public static final BooleanProperty PROP_DEFAULT_AUTOLOAD = new BooleanProperty(PREFERENCE_PREFIX + ".default_autoload", true);
    /** do show errors per default */
    public static final BooleanProperty PROP_DEFAULT_SHOWERRORS = new BooleanProperty(PREFERENCE_PREFIX + ".default_showerrors", true);
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
    private boolean needRedraw;

    private AttributionSupport attribution = new AttributionSupport();

    // needed public access for session exporter
    /** if layers changes automatically, when user zooms in */
    public boolean autoZoom;
    /** if layer automatically loads new tiles */
    public boolean autoLoad;
    /** if layer should show errors on tiles */
    public boolean showErrors;

    /**
     * use fairly small memory cache, as cached objects are quite big, as they contain BufferedImages
     */
    public static final IntegerProperty MEMORY_CACHE_SIZE = new IntegerProperty(PREFERENCE_PREFIX + "cache.max_objects_ram", 200);

    /*
     *  use MemoryTileCache instead of tileLoader JCS cache, as tileLoader caches only content (byte[] of image)
     *  and MemoryTileCache caches whole Tile. This gives huge performance improvement when a lot of tiles are visible
     *  in MapView (for example - when limiting min zoom in imagery)
     *
     *  Use static instance so memory is shared between layers to prevent out of memory exceptions, when user is working with many layers
     */
    protected static TileCache tileCache = new MemoryTileCache(MEMORY_CACHE_SIZE.get());
    protected AbstractTMSTileSource tileSource;
    protected TileLoader tileLoader;

    /**
     * Creates Tile Source based Imagery Layer based on Imagery Info
     * @param info imagery info
     */
    public AbstractTileSourceLayer(ImageryInfo info) {
        super(info);
        setBackgroundLayer(true);
        this.setVisible(true);
        MapView.addZoomChangeListener(this);
    }

    protected abstract TileLoaderFactory getTileLoaderFactory();

    /**
     *
     * @param info imagery info
     * @return TileSource for specified ImageryInfo
     * @throws IllegalArgumentException when Imagery is not supported by layer
     */
    protected abstract AbstractTMSTileSource getTileSource(ImageryInfo info) throws IllegalArgumentException;

    protected Map<String, String> getHeaders(TileSource tileSource) {
        if (tileSource instanceof TemplatedTileSource) {
            return ((TemplatedTileSource) tileSource).getHeaders();
        }
        return null;
    }

    protected void initTileSource(AbstractTMSTileSource tileSource) {
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
    }

    @Override
    public synchronized void tileLoadingFinished(Tile tile, boolean success) {
        if (tile.hasError()) {
            success = false;
            tile.setImage(null);
        }
        if (sharpenLevel != 0 && success) {
            tile.setImage(sharpenImage(tile.getImage()));
        }
        tile.setLoaded(success);
        needRedraw = true;
        if (Main.map != null) {
            Main.map.repaint(100);
        }
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
        // if we use TMSCachedTileLoader, we already cleared by tile source, this is needed
        // to prevent removal of additional objects
        if (!(tileLoader instanceof TMSCachedTileLoader)) {
            tileCache.clear();
        }

    }

    /**
     * Initiates a repaint of Main.map
     *
     * @see Main#map
     * @see MapFrame#repaint()
     */
    protected void redraw() {
        needRedraw = true;
        Main.map.repaint();
    }

    /**
     * Marks layer as needing redraw on offset change
     */
    @Override
    public void setOffset(double dx, double dy) {
        super.setOffset(dx, dy);
        needRedraw = true;
    }


    /**
     * Returns average number of screen pixels per tile pixel for current mapview
     */
    private double getScaleFactor(int zoom) {
        if (!Main.isDisplayingMapView()) return 1;
        MapView mv = Main.map.mapView;
        LatLon topLeft = mv.getLatLon(0, 0);
        LatLon botRight = mv.getLatLon(mv.getWidth(), mv.getHeight());
        TileXY t1 = tileSource.latLonToTileXY(topLeft.toCoordinate(), zoom);
        TileXY t2 = tileSource.latLonToTileXY(botRight.toCoordinate(), zoom);

        int screenPixels = mv.getWidth()*mv.getHeight();
        double tilePixels = Math.abs((t2.getY()-t1.getY())*(t2.getX()-t1.getX())*tileSource.getTileSize()*tileSource.getTileSize());
        if (screenPixels == 0 || tilePixels == 0) return 1;
        return screenPixels/tilePixels;
    }

    protected int getBestZoom() {
        double factor = getScaleFactor(1); // check the ratio between area of tilesize at zoom 1 to current view
        double result = Math.log(factor)/Math.log(2)/2+1;
        /*
         * Math.log(factor)/Math.log(2) - gives log base 2 of factor
         * We divide result by 2, as factor contains ratio between areas. We could do Math.sqrt before log, or just divide log by 2
         * In general, smaller zoom levels are more readable.  We prefer big,
         * block, pixelated (but readable) map text to small, smeared,
         * unreadable underzoomed text.  So, use .floor() instead of rounding
         * to skew things a bit toward the lower zooms.
         * Remember, that result here, should correspond to TMSLayer.paint(...)
         * getScaleFactor(...) is supposed to be between 0.75 and 3
         */
        int intResult = (int) Math.floor(result);
        if (intResult > getMaxZoomLvl())
            return getMaxZoomLvl();
        if (intResult < getMinZoomLvl())
            return getMinZoomLvl();
        return intResult;
    }

    private static boolean actionSupportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof TMSLayer;
    }

    private final class ShowTileInfoAction extends AbstractAction {
        private final transient TileHolder clickedTileHolder;

        private ShowTileInfoAction(TileHolder clickedTileHolder) {
            super(tr("Show Tile Info"));
            this.clickedTileHolder = clickedTileHolder;
        }

        private String getSizeString(int size) {
            StringBuilder ret = new StringBuilder();
            return ret.append(size).append("x").append(size).toString();
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
                Rectangle displaySize = tileToRect(clickedTile);
                String url = "";
                try {
                    url = clickedTile.getUrl();
                } catch (IOException e) {
                    // silence exceptions
                    if (Main.isTraceEnabled()) {
                        Main.trace(e.getMessage());
                    }
                }

                String[][] content = {
                        {"Tile name", clickedTile.getKey()},
                        {"Tile url", url},
                        {"Tile size", getSizeString(clickedTile.getTileSource().getTileSize()) },
                        {"Tile display size", new StringBuilder().append(displaySize.width).append("x").append(displaySize.height).toString()},
                };

                for (String[] entry: content) {
                    panel.add(new JLabel(tr(entry[0]) + ":"), GBC.std());
                    panel.add(GBC.glue(5, 0), GBC.std());
                    panel.add(createTextField(entry[1]), GBC.eol().fill(GBC.HORIZONTAL));
                }

                for (Entry<String, String> e: clickedTile.getMetadata().entrySet()) {
                    panel.add(new JLabel(tr("Metadata ") + tr(e.getKey()) + ":"), GBC.std());
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

    private class AutoZoomAction extends AbstractAction implements LayerAction {
        public AutoZoomAction() {
            super(tr("Auto Zoom"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            autoZoom = !autoZoom;
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(autoZoom);
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return actionSupportLayers(layers);
        }
    }

    private class AutoLoadTilesAction extends AbstractAction implements LayerAction {
        public AutoLoadTilesAction() {
            super(tr("Auto load tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            autoLoad = !autoLoad;
        }

        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(autoLoad);
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return actionSupportLayers(layers);
        }
    }

    private class LoadAllTilesAction extends AbstractAction {
        public LoadAllTilesAction() {
            super(tr("Load All Tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            loadAllTiles(true);
            redraw();
        }
    }

    private class LoadErroneusTilesAction extends AbstractAction {
        public LoadErroneusTilesAction() {
            super(tr("Load All Error Tiles"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            loadAllErrorTiles(true);
            redraw();
        }
    }

    private class ZoomToNativeLevelAction extends AbstractAction {
        public ZoomToNativeLevelAction() {
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
        public ZoomToBestAction() {
            super(tr("Change resolution"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            setZoomLevel(getBestZoom());
        }
    }

    /**
     * Simple class to keep clickedTile within hookUpMapView
     */
    private static final class TileHolder {
        private Tile t = null;

        public Tile getTile() {
            return t;
        }

        public void setTile(Tile t) {
            this.t = t;
        }
    }

    private class BooleanButtonModel extends DefaultButtonModel {
        private Field field;

        public BooleanButtonModel(Field field) {
            this.field = field;
        }

        @Override
        public boolean isSelected() {
            try {
                return field.getBoolean(AbstractTileSourceLayer.this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }
    /**
     * Creates popup menu items and binds to mouse actions
     */
    @Override
    public void hookUpMapView() {
        this.tileSource = getTileSource(info);
        if (this.tileSource == null) {
            throw new IllegalArgumentException(tr("Failed to create tile source"));
        }

        projectionChanged(null, Main.getProjection()); // check if projection is supported
        initTileSource(this.tileSource);

        ;
        // keep them final here, so we avoid namespace clutter in the class
        final JPopupMenu tileOptionMenu = new JPopupMenu();
        final TileHolder clickedTileHolder = new TileHolder();
        Field autoZoomField;
        Field autoLoadField;
        Field showErrorsField;
        try {
            autoZoomField = AbstractTileSourceLayer.class.getField("autoZoom");
            autoLoadField = AbstractTileSourceLayer.class.getDeclaredField("autoLoad");
            showErrorsField = AbstractTileSourceLayer.class.getDeclaredField("showErrors");
        } catch (NoSuchFieldException | SecurityException e) {
            // shoud not happen
            throw new RuntimeException(e);
        }

        autoZoom = PROP_DEFAULT_AUTOZOOM.get();
        JCheckBoxMenuItem autoZoomPopup = new JCheckBoxMenuItem();
        autoZoomPopup.setModel(new BooleanButtonModel(autoZoomField));
        autoZoomPopup.setAction(new AutoZoomAction());
        tileOptionMenu.add(autoZoomPopup);

        autoLoad = PROP_DEFAULT_AUTOLOAD.get();
        JCheckBoxMenuItem autoLoadPopup = new JCheckBoxMenuItem();
        autoLoadPopup.setAction(new AutoLoadTilesAction());
        autoLoadPopup.setModel(new BooleanButtonModel(autoLoadField));
        tileOptionMenu.add(autoLoadPopup);

        showErrors = PROP_DEFAULT_SHOWERRORS.get();
        JCheckBoxMenuItem showErrorsPopup = new JCheckBoxMenuItem();
        showErrorsPopup.setAction(new AbstractAction(tr("Show Errors")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                showErrors = !showErrors;
            }
        });
        showErrorsPopup.setModel(new BooleanButtonModel(showErrorsField));
        tileOptionMenu.add(showErrorsPopup);

        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Load Tile")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Tile clickedTile = clickedTileHolder.getTile();
                if (clickedTile != null) {
                    loadTile(clickedTile, true);
                    redraw();
                }
            }
        }));

        tileOptionMenu.add(new JMenuItem(new ShowTileInfoAction(clickedTileHolder)));

        tileOptionMenu.add(new JMenuItem(new LoadAllTilesAction()));
        tileOptionMenu.add(new JMenuItem(new LoadErroneusTilesAction()));

        // increase and decrease commands
        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Increase zoom")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                increaseZoomLevel();
                redraw();
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Decrease zoom")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                decreaseZoomLevel();
                redraw();
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Snap to tile size")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                double newFactor = Math.sqrt(getScaleFactor(currentZoomLevel));
                Main.map.mapView.zoomToFactor(newFactor);
                redraw();
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Flush Tile Cache")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new PleaseWaitRunnable(tr("Flush Tile Cache")) {
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
        }));

        final MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isVisible()) return;
                if (e.getButton() == MouseEvent.BUTTON3) {
                    clickedTileHolder.setTile(getTileForPixelpos(e.getX(), e.getY()));
                    tileOptionMenu.show(e.getComponent(), e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    attribution.handleAttribution(e.getPoint(), true);
                }
            }
        };
        Main.map.mapView.addMouseListener(adapter);

        MapView.addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                //
            }

            @Override
            public void layerAdded(Layer newLayer) {
                //
            }

            @Override
            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == AbstractTileSourceLayer.this) {
                    Main.map.mapView.removeMouseListener(adapter);
                    MapView.removeLayerChangeListener(this);
                    MapView.removeZoomChangeListener(AbstractTileSourceLayer.this);
                }
            }
        });

        // FIXME: why do we need this? Without this, if you add a WMS layer and do not move the mouse, sometimes, tiles do not
        // start loading.
        Main.map.repaint(500);
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
        needRedraw = true;
    }

    protected int getMaxZoomLvl() {
        if (info.getMaxZoom() != 0)
            return checkMaxZoomLvl(info.getMaxZoom(), tileSource);
        else
            return getMaxZoomLvl(tileSource);
    }

    protected int getMinZoomLvl() {
        return getMinZoomLvl(tileSource);
    }

    /**
     *
     * @return if its allowed to zoom in
     */
    public boolean zoomIncreaseAllowed() {
        boolean zia = currentZoomLevel < this.getMaxZoomLvl();
        if (Main.isDebugEnabled()) {
            Main.debug("zoomIncreaseAllowed(): " + zia + " " + currentZoomLevel + " vs. " + this.getMaxZoomLvl());
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
        return currentZoomLevel > this.getMinZoomLvl();
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
            tile.loadPlaceholderFromCache(tileCache);
        }
        return tile;
    }

    /*
     * This can and will return null for tiles that are not
     * already in the cache.
     */
    private Tile getTile(int x, int y, int zoom) {
        if (x < 0 || x >= tileSource.getTileXMax(zoom) || y < 0 || y >= tileSource.getTileYMax(zoom))
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
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());
        return new TileSet(topLeft, botRight, currentZoomLevel);
    }

    protected void loadAllTiles(boolean force) {
        TileSet ts = getVisibleTileSet();

        // if there is more than 18 tiles on screen in any direction, do not load all tiles!
        if (ts.tooLarge()) {
            Main.warn("Not downloading all tiles because there is more than 18 tiles on an axis!");
            return;
        }
        ts.loadAllTiles(force);
    }

    protected void loadAllErrorTiles(boolean force) {
        TileSet ts = getVisibleTileSet();
        ts.loadAllErrorTiles(force);
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = (infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0;
        needRedraw = true;
        if (Main.isDebugEnabled()) {
            Main.debug("imageUpdate() done: " + done + " calling repaint");
        }
        Main.map.repaint(done ? 0 : 100);
        return !done;
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
     * Returns the image for the given tile if both tile and image are loaded.
     * Otherwise returns  null.
     *
     * @param tile the Tile for which the image should be returned
     * @return  the image of the tile or null.
     */
    private Image getLoadedTileImage(Tile tile) {
        if (!tile.isLoaded())
            return null;
        Image img = tile.getImage();
        if (!imageLoaded(img))
            return null;
        return img;
    }

    private Rectangle tileToRect(Tile t1) {
        /*
         * We need to get a box in which to draw, so advance by one tile in
         * each direction to find the other corner of the box.
         * Note: this somewhat pollutes the tile cache
         */
        Tile t2 = tempCornerTile(t1);
        Rectangle rect = new Rectangle(pixelPos(t1));
        rect.add(pixelPos(t2));
        return rect;
    }

    // 'source' is the pixel coordinates for the area that
    // the img is capable of filling in.  However, we probably
    // only want a portion of it.
    //
    // 'border' is the screen cordinates that need to be drawn.
    //  We must not draw outside of it.
    private void drawImageInside(Graphics g, Image sourceImg, Rectangle source, Rectangle border) {
        Rectangle target = source;

        // If a border is specified, only draw the intersection
        // if what we have combined with what we are supposed to draw.
        if (border != null) {
            target = source.intersection(border);
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
        int screen_x_offset = target.x - source.x;
        int screen_y_offset = target.y - source.y;
        // And how many pixels into the image itself does that correlate to?
        int img_x_offset = (int) (screen_x_offset * imageXScaling + 0.5);
        int img_y_offset = (int) (screen_y_offset * imageYScaling + 0.5);
        // Now calculate the other corner of the image that we need
        // by scaling the 'target' rectangle's dimensions.
        int img_x_end = img_x_offset + (int) (target.getWidth() * imageXScaling + 0.5);
        int img_y_end = img_y_offset + (int) (target.getHeight() * imageYScaling + 0.5);

        if (Main.isDebugEnabled()) {
            Main.debug("drawing image into target rect: " + target);
        }
        g.drawImage(sourceImg,
                target.x, target.y,
                target.x + target.width, target.y + target.height,
                img_x_offset, img_y_offset,
                img_x_end, img_y_end,
                this);
        if (PROP_FADE_AMOUNT.get() != 0) {
            // dimm by painting opaque rect...
            g.setColor(getFadeColorWithAlpha());
            g.fillRect(target.x, target.y,
                    target.width, target.height);
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
        Rectangle borderRect = null;
        if (border != null) {
            borderRect = tileToRect(border);
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

            Rectangle sourceRect = tileToRect(tile);
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
                    ret.append(line).append("\n");
                    line.setLength(0);
                }
                line.append(s).append(" ");
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
        int fontHeight = g.getFontMetrics().getHeight();
        if (tile == null)
            return;
        Point p = pixelPos(t);
        int texty = p.y + 2 + fontHeight;

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

        if (tile.hasError() && showErrors) {
            myDrawString(g, tr("Error") + ": " + tr(tile.getErrorMessage()), p.x + 2, texty);
            //texty += 1 + fontHeight;
        }

        int xCursor = -1;
        int yCursor = -1;
        if (Main.isDebugEnabled()) {
            if (yCursor < t.getYtile()) {
                if (t.getYtile() % 32 == 31) {
                    g.fillRect(0, p.y - 1, mv.getWidth(), 3);
                } else {
                    g.drawLine(0, p.y, mv.getWidth(), p.y);
                }
                //yCursor = t.getYtile();
            }
            // This draws the vertical lines for the entire column. Only draw them for the top tile in the column.
            if (xCursor < t.getXtile()) {
                if (t.getXtile() % 32 == 0) {
                    // level 7 tile boundary
                    g.fillRect(p.x - 1, 0, 3, mv.getHeight());
                } else {
                    g.drawLine(p.x, 0, p.x, mv.getHeight());
                }
                //xCursor = t.getXtile();
            }
        }
    }

    private Point pixelPos(LatLon ll) {
        return Main.map.mapView.getPoint(Main.getProjection().latlon2eastNorth(ll).add(getDx(), getDy()));
    }

    private Point pixelPos(Tile t) {
        ICoordinate coord = tileSource.tileXYToLatLon(t);
        return pixelPos(new LatLon(coord));
    }

    private LatLon getShiftedLatLon(EastNorth en) {
        return Main.getProjection().eastNorth2latlon(en.add(-getDx(), -getDy()));
    }

    private ICoordinate getShiftedCoord(EastNorth en) {
        return getShiftedLatLon(en).toCoordinate();
    }

    private final TileSet nullTileSet = new TileSet((LatLon) null, (LatLon) null, 0);
    private final class TileSet {
        int x0, x1, y0, y1;
        int zoom;

        /**
         * Create a TileSet by EastNorth bbox taking a layer shift in account
         */
        private TileSet(EastNorth topLeft, EastNorth botRight, int zoom) {
            this(getShiftedLatLon(topLeft), getShiftedLatLon(botRight), zoom);
        }

        /**
         * Create a TileSet by known LatLon bbox without layer shift correction
         */
        private TileSet(LatLon topLeft, LatLon botRight, int zoom) {
            this.zoom = zoom;
            if (zoom == 0)
                return;

            TileXY t1 = tileSource.latLonToTileXY(topLeft.toCoordinate(), zoom);
            TileXY t2 = tileSource.latLonToTileXY(botRight.toCoordinate(), zoom);

            x0 = t1.getXIndex();
            y0 = t1.getYIndex();
            x1 = t2.getXIndex();
            y1 = t2.getYIndex();

            if (x0 > x1) {
                int tmp = x0;
                x0 = x1;
                x1 = tmp;
            }
            if (y0 > y1) {
                int tmp = y0;
                y0 = y1;
                y1 = tmp;
            }

            if (x0 < tileSource.getTileXMin(zoom)) {
                x0 = tileSource.getTileXMin(zoom);
            }
            if (y0 < tileSource.getTileYMin(zoom)) {
                y0 = tileSource.getTileYMin(zoom);
            }
            if (x1 > tileSource.getTileXMax(zoom)) {
                x1 = tileSource.getTileXMax(zoom);
            }
            if (y1 > tileSource.getTileYMax(zoom)) {
                y1 = tileSource.getTileYMax(zoom);
            }
        }

        private boolean tooSmall() {
            return this.tilesSpanned() < 2.1;
        }

        private boolean tooLarge() {
            return this.tilesSpanned() > 10;
        }

        private boolean insane() {
            return this.tilesSpanned() > 100;
        }

        private double tilesSpanned() {
            return Math.sqrt(1.0 * this.size());
        }

        private int size() {
            int xSpan = x1 - x0 + 1;
            int ySpan = y1 - y0 + 1;
            return xSpan * ySpan;
        }

        /*
         * Get all tiles represented by this TileSet that are
         * already in the tileCache.
         */
        private List<Tile> allExistingTiles() {
            return this.__allTiles(false);
        }

        private List<Tile> allTilesCreate() {
            return this.__allTiles(true);
        }

        private List<Tile> __allTiles(boolean create) {
            // Tileset is either empty or too large
            if (zoom == 0 || this.insane())
                return Collections.emptyList();
            List<Tile> ret = new ArrayList<>();
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    Tile t;
                    if (create) {
                        t = getOrCreateTile(x, y , zoom);
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
            final int centerX = (int) Math.ceil((x0 + x1) / 2d);
            final int centerY = (int) Math.ceil((y0 + y1) / 2d);
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
            if (!autoLoad && !force)
                return;
            List<Tile> allTiles = allTilesCreate();
            Collections.sort(allTiles, getTileDistanceComparator());
            for (Tile t : allTiles) {
                loadTile(t, force);
            }
        }

        private void loadAllErrorTiles(boolean force) {
            if (!autoLoad && !force)
                return;
            for (Tile t : this.allTilesCreate()) {
                if (t.hasError()) {
                    loadTile(t, true);
                }
            }
        }
    }

    private static class TileSetInfo {
        public boolean hasVisibleTiles = false;
        public boolean hasOverzoomedTiles = false;
        public boolean hasLoadingTiles = false;
    }

    private static TileSetInfo getTileSetInfo(TileSet ts) {
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
        private final EastNorth topLeft, botRight;
        private final int minZoom, maxZoom;
        private final TileSet[] tileSets;
        private final TileSetInfo[] tileSetInfos;
        public DeepTileSet(EastNorth topLeft, EastNorth botRight, int minZoom, int maxZoom) {
            this.topLeft = topLeft;
            this.botRight = botRight;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
            this.tileSets = new TileSet[maxZoom - minZoom + 1];
            this.tileSetInfos = new TileSetInfo[maxZoom - minZoom + 1];
        }

        public TileSet getTileSet(int zoom) {
            if (zoom < minZoom)
                return nullTileSet;
            synchronized (tileSets) {
                TileSet ts = tileSets[zoom-minZoom];
                if (ts == null) {
                    ts = new TileSet(topLeft, botRight, zoom);
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
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());

        if (botRight.east() == 0 || botRight.north() == 0) {
            /*Main.debug("still initializing??");*/
            // probably still initializing
            return;
        }

        needRedraw = false;

        int zoom = currentZoomLevel;
        if (autoZoom) {
            double pixelScaling = getScaleFactor(zoom);
            if (pixelScaling > 3 || pixelScaling < 0.7) {
                zoom = getBestZoom();
            }
        }

        DeepTileSet dts = new DeepTileSet(topLeft, botRight, getMinZoomLvl(), zoom);
        TileSet ts = dts.getTileSet(zoom);

        int displayZoomLevel = zoom;

        boolean noTilesAtZoom = false;
        if (autoZoom && autoLoad) {
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
        } else if (autoZoom) {
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
            if (!autoZoom) {
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
                LatLon topLeft2  = new LatLon(tileSource.tileXYToLatLon(missed));
                LatLon botRight2 = new LatLon(tileSource.tileXYToLatLon(t2));
                TileSet ts2 = new TileSet(topLeft2, botRight2, newzoom);
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

        attribution.paintAttribution(g, mv.getWidth(), mv.getHeight(), getShiftedCoord(topLeft), getShiftedCoord(botRight),
                displayZoomLevel, this);

        //g.drawString("currentZoomLevel=" + currentZoomLevel, 120, 120);
        g.setColor(Color.lightGray);

        if (ts.insane()) {
            myDrawString(g, tr("zoom in to load any tiles"), 120, 120);
        } else if (ts.tooLarge()) {
            myDrawString(g, tr("zoom in to load more tiles"), 120, 120);
        } else if (!autoZoom && ts.tooSmall()) {
            myDrawString(g, tr("increase zoom level to see more detail"), 120, 120);
        }

        if (noTilesAtZoom) {
            myDrawString(g, tr("No tiles at this zoom level"), 120, 120);
        }
        if (Main.isDebugEnabled()) {
            myDrawString(g, tr("Current zoom: {0}", currentZoomLevel), 50, 140);
            myDrawString(g, tr("Display zoom: {0}", displayZoomLevel), 50, 155);
            myDrawString(g, tr("Pixel scale: {0}", getScaleFactor(currentZoomLevel)), 50, 170);
            myDrawString(g, tr("Best zoom: {0}", getBestZoom()), 50, 185);
            if (tileLoader instanceof TMSCachedTileLoader) {
                TMSCachedTileLoader cachedTileLoader = (TMSCachedTileLoader) tileLoader;
                int offset = 185;
                for (String part: cachedTileLoader.getStats().split("\n")) {
                    myDrawString(g, tr("Cache stats: {0}", part), 50, offset += 15);
                }

            }
        }
    }

    /**
     * This isn't very efficient, but it is only used when the
     * user right-clicks on the map.
     */
    private Tile getTileForPixelpos(int px, int py) {
        if (Main.isDebugEnabled()) {
            Main.debug("getTileForPixelpos("+px+", "+py+")");
        }
        MapView mv = Main.map.mapView;
        Point clicked = new Point(px, py);
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());
        int z = currentZoomLevel;
        TileSet ts = new TileSet(topLeft, botRight, z);

        if (!ts.tooLarge()) {
            ts.loadAllTiles(false); // make sure there are tile objects for all tiles
        }
        Tile clickedTile = null;
        for (Tile t1 : ts.allExistingTiles()) {
            Tile t2 = tempCornerTile(t1);
            Rectangle r = new Rectangle(pixelPos(t1));
            r.add(pixelPos(t2));
            if (Main.isDebugEnabled()) {
                Main.debug("r: " + r + " clicked: " + clicked);
            }
            if (!r.contains(clicked)) {
                continue;
            }
            clickedTile  = t1;
            break;
        }
        if (clickedTile == null)
            return null;
        /*Main.debug("Clicked on tile: " + clickedTile.getXtile() + " " + clickedTile.getYtile() +
                " currentZoomLevel: " + currentZoomLevel);*/
        return clickedTile;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[] {
                LayerListDialog.getInstance().createActivateLayerAction(this),
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                // color,
                new OffsetAction(),
                new RenameLayerAction(this.getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new AutoLoadTilesAction(),
                new AutoZoomAction(),
                new ZoomToBestAction(),
                new ZoomToNativeLevelAction(),
                new LoadErroneusTilesAction(),
                new LoadAllTilesAction(),
                new LayerListPopup.InfoAction(this)
        };
    }

    @Override
    public String getToolTipText() {
        if (autoLoad) {
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
        return needRedraw;
    }

    /**
     * Task responsible for precaching imagery along the gpx track
     *
     */
    public class PrecacheTask implements TileLoaderListener {
        private final ProgressMonitor progressMonitor;
        private int totalCount;
        private AtomicInteger processedCount = new AtomicInteger(0);
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
            if (success) {
                int processed = this.processedCount.incrementAndGet();
                this.progressMonitor.worked(1);
                this.progressMonitor.setCustomText(tr("Downloaded {0}/{1} tiles", processed, totalCount));
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
     * @param precacheTask Task responsible for precaching imagery
     * @param points lat/lon coordinates to download
     * @param bufferX how many units in current Coordinate Reference System to cover in X axis in both sides
     * @param bufferY how many units in current Coordinate Reference System to cover in Y axis in both sides
     */
    public void downloadAreaToCache(final PrecacheTask precacheTask, List<LatLon> points, double bufferX, double bufferY) {
        final Set<Tile> requestedTiles = new ConcurrentSkipListSet<>(new Comparator<Tile>() {
            public int compare(Tile o1, Tile o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey());
            }
        });
        for (LatLon point: points) {

            TileXY minTile = tileSource.latLonToTileXY(point.lat() - bufferY, point.lon() - bufferX, currentZoomLevel);
            TileXY curTile = tileSource.latLonToTileXY(point.toCoordinate(), currentZoomLevel);
            TileXY maxTile = tileSource.latLonToTileXY(point.lat() + bufferY, point.lon() + bufferX, currentZoomLevel);

            // take at least one tile of buffer
            int minY = Math.min(curTile.getYIndex() - 1, minTile.getYIndex());
            int maxY = Math.max(curTile.getYIndex() + 1, maxTile.getYIndex());
            int minX = Math.min(curTile.getXIndex() - 1, minTile.getXIndex());
            int maxX = Math.min(curTile.getXIndex() + 1, minTile.getXIndex());

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
    }

    @Override
    public boolean isSavable() {
        return true; // With WMSLayerExporter
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save WMS file"), WMSLayerImporter.FILE_FILTER);
    }
}
