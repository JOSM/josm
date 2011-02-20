// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JobDispatcher;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.ScanexTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;

/**
 * Class that displays a slippy map layer.
 *
 * @author Frederik Ramm <frederik@remote.org>
 * @author LuVar <lubomir.varga@freemap.sk>
 * @author Dave Hansen <dave@sr71.net>
 * @author Upliner <upliner@gmail.com>
 *
 */
public class TMSLayer extends ImageryLayer implements ImageObserver, TileLoaderListener {
    public static final String PREFERENCE_PREFIX   = "imagery.tms";

    public static final int MAX_ZOOM = 30;
    public static final int MIN_ZOOM = 2;
    public static final int DEFAULT_MAX_ZOOM = 20;
    public static final int DEFAULT_MIN_ZOOM = 2;

    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty(PREFERENCE_PREFIX + ".default_autozoom", true);
    public static final BooleanProperty PROP_DEFAULT_AUTOLOAD = new BooleanProperty(PREFERENCE_PREFIX + ".default_autoload", true);
    public static final IntegerProperty PROP_MIN_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".min_zoom_lvl", DEFAULT_MIN_ZOOM);
    public static final IntegerProperty PROP_MAX_ZOOM_LVL = new IntegerProperty(PREFERENCE_PREFIX + ".max_zoom_lvl", DEFAULT_MAX_ZOOM);
    public static final BooleanProperty PROP_DRAW_DEBUG = new BooleanProperty(PREFERENCE_PREFIX + ".draw_debug", false);
    public static final BooleanProperty PROP_ADD_TO_SLIPPYMAP_CHOOSER = new BooleanProperty(PREFERENCE_PREFIX + ".add_to_slippymap_chooser", true);
    public static final StringProperty PROP_TILECACHE_DIR;

    static {
        String defPath = null;
        try {
            defPath = OsmFileCacheTileLoader.getDefaultCacheDir().getAbsolutePath();
        } catch (SecurityException e) {
        }
        PROP_TILECACHE_DIR = new StringProperty(PREFERENCE_PREFIX + ".tilecache_path", defPath);
    }

    boolean debug = false;
    void out(String s)
    {
        Main.debug(s);
    }

    protected MemoryTileCache tileCache;
    protected TileSource tileSource;
    protected TileLoader tileLoader;
    JobDispatcher jobDispatcher = JobDispatcher.getInstance();

    HashSet<Tile> tileRequestsOutstanding = new HashSet<Tile>();
    @Override
    public synchronized void tileLoadingFinished(Tile tile, boolean success)
    {
        if (tile.hasError()) {
            success = false;
            tile.setImage(null);
        }
        if (sharpenLevel != 0 && success) {
            tile.setImage(sharpenImage(tile.getImage()));
        }
        tile.setLoaded(true);
        needRedraw = true;
        Main.map.repaint(100);
        tileRequestsOutstanding.remove(tile);
        if (debug) {
            out("tileLoadingFinished() tile: " + tile + " success: " + success);
        }
    }
    @Override
    public TileCache getTileCache()
    {
        return tileCache;
    }
    void clearTileCache()
    {
        if (debug) {
            out("clearing tile storage");
        }
        tileCache = new MemoryTileCache();
        tileCache.setCacheSize(200);
    }

    /**
     * Zoomlevel at which tiles is currently downloaded.
     * Initial zoom lvl is set to bestZoom
     */
    public int currentZoomLevel;

    private Tile clickedTile;
    private boolean needRedraw;
    private JPopupMenu tileOptionMenu;
    JCheckBoxMenuItem autoZoomPopup;
    JCheckBoxMenuItem autoLoadPopup;
    Tile showMetadataTile;
    private Image attrImage;
    private String attrTermsUrl;
    private Rectangle attrImageBounds, attrToUBounds;
    private static final Font InfoFont = new Font("sansserif", Font.BOLD, 13);
    private static final Font ATTR_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Font ATTR_LINK_FONT;
    static {
        HashMap<TextAttribute, Integer> aUnderline = new HashMap<TextAttribute, Integer>();
        aUnderline.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        ATTR_LINK_FONT = ATTR_FONT.deriveFont(aUnderline);
    }

    protected boolean autoZoom;
    protected boolean autoLoad;

    void redraw()
    {
        needRedraw = true;
        Main.map.repaint();
    }

    static int checkMaxZoomLvl(int maxZoomLvl, TileSource ts)
    {
        if(maxZoomLvl > MAX_ZOOM) {
            System.err.println("MaxZoomLvl shouldnt be more than 30! Setting to 30.");
            maxZoomLvl = MAX_ZOOM;
        }
        if(maxZoomLvl < PROP_MIN_ZOOM_LVL.get()) {
            System.err.println("maxZoomLvl shouldnt be more than minZoomLvl! Setting to minZoomLvl.");
            maxZoomLvl = PROP_MIN_ZOOM_LVL.get();
        }
        if (ts != null && ts.getMaxZoom() != 0 && ts.getMaxZoom() < maxZoomLvl) {
            maxZoomLvl = ts.getMaxZoom();
        }
        return maxZoomLvl;
    }

    public static int getMaxZoomLvl(TileSource ts)
    {
        return checkMaxZoomLvl(PROP_MAX_ZOOM_LVL.get(), ts);
    }

    public static void setMaxZoomLvl(int maxZoomLvl) {
        maxZoomLvl = checkMaxZoomLvl(maxZoomLvl, null);
        PROP_MAX_ZOOM_LVL.put(maxZoomLvl);
    }

    static int checkMinZoomLvl(int minZoomLvl, TileSource ts)
    {
        if(minZoomLvl < MIN_ZOOM) {
            System.err.println("minZoomLvl shouldnt be lees than "+MIN_ZOOM+"! Setting to that.");
            minZoomLvl = MIN_ZOOM;
        }
        if(minZoomLvl > PROP_MAX_ZOOM_LVL.get()) {
            System.err.println("minZoomLvl shouldnt be more than maxZoomLvl! Setting to maxZoomLvl.");
            minZoomLvl = getMaxZoomLvl(ts);
        }
        if (ts != null && ts.getMinZoom() > minZoomLvl) {
            System.err.println("increasomg minZoomLvl to match tile source");
            minZoomLvl = ts.getMinZoom();
        }
        return minZoomLvl;
    }

    public static int getMinZoomLvl(TileSource ts)
    {
        return checkMinZoomLvl(PROP_MIN_ZOOM_LVL.get(), ts);
    }

    public static void setMinZoomLvl(int minZoomLvl) {
        minZoomLvl = checkMinZoomLvl(minZoomLvl, null);
        PROP_MIN_ZOOM_LVL.put(minZoomLvl);
    }

    public static TileSource getTileSource(ImageryInfo info) {
        if (info.getImageryType() == ImageryType.TMS) {
            if(ImageryInfo.isUrlWithPatterns(info.getUrl()))
                return new TemplatedTMSTileSource(info.getName(), info.getUrl(), info.getMaxZoom());
            else
                return new TMSTileSource(info.getName(),info.getUrl(), info.getMaxZoom());
        } else if (info.getImageryType() == ImageryType.BING)
            return new BingAerialTileSource();
        else if (info.getImageryType() == ImageryType.SCANEX)
            return new ScanexTileSource(info.getUrl());
        return null;
    }

    private void initTileSource(TileSource tileSource)
    {
        this.tileSource = tileSource;
        boolean requireAttr = tileSource.requiresAttribution();
        if(requireAttr) {
            attrImage = tileSource.getAttributionImage();
            if(attrImage == null) {
                System.out.println("Attribution image was null.");
            } else {
                System.out.println("Got an attribution image " + attrImage.getHeight(this) + "x" + attrImage.getWidth(this));
            }

            attrTermsUrl = tileSource.getTermsOfUseURL();
        }

        currentZoomLevel = getBestZoom();

        clearTileCache();
        String cachePath = TMSLayer.PROP_TILECACHE_DIR.get();
        tileLoader = null;
        if (cachePath != null && !cachePath.isEmpty()) {
            try {
                tileLoader = new OsmFileCacheTileLoader(this, new File(cachePath));
            } catch (IOException e) {
            }
        }
        if (tileLoader == null) {
            tileLoader = new OsmTileLoader(this);
        }
    }

    @Override
    public void setOffset(double dx, double dy) {
        super.setOffset(dx, dy);
        needRedraw = true;
    }

    /**
     * Returns average number of screen pixels per tile pixel for current mapview
     */
    private double getScaleFactor(int zoom) {
        if (Main.map == null || Main.map.mapView == null) return 1;
        MapView mv = Main.map.mapView;
        LatLon topLeft = mv.getLatLon(0, 0);
        LatLon botRight = mv.getLatLon(mv.getWidth(), mv.getHeight());
        double x1 = tileSource.lonToTileX(topLeft.lon(), zoom);
        double y1 = tileSource.latToTileY(topLeft.lat(), zoom);
        double x2 = tileSource.lonToTileX(botRight.lon(), zoom);
        double y2 = tileSource.latToTileY(botRight.lat(), zoom);

        int screenPixels = mv.getWidth()*mv.getHeight();
        double tilePixels = Math.abs((y2-y1)*(x2-x1)*tileSource.getTileSize()*tileSource.getTileSize());
        if (screenPixels == 0 || tilePixels == 0) return 1;
        return screenPixels/tilePixels;
    }

    private int getBestZoom() {
        double factor = getScaleFactor(1);
        double result = Math.log(factor)/Math.log(2)/2+1;
        int intResult = (int)Math.round(result);
        if (intResult > getMaxZoomLvl())
            return getMaxZoomLvl();
        if (intResult < getMinZoomLvl())
            return getMinZoomLvl();
        return intResult;
    }

    @SuppressWarnings("serial")
    public TMSLayer(ImageryInfo info) {
        super(info);

        setBackgroundLayer(true);
        this.setVisible(true);

        TileSource source = getTileSource(info);
        if (source == null)
            throw new IllegalStateException("cannot create TMSLayer with non-TMS ImageryInfo");
        initTileSource(source);

        tileOptionMenu = new JPopupMenu();

        autoZoom = PROP_DEFAULT_AUTOZOOM.get();
        autoZoomPopup = new JCheckBoxMenuItem();
        autoZoomPopup.setAction(new AbstractAction(tr("Auto Zoom")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                autoZoom = !autoZoom;
            }
        });
        autoZoomPopup.setSelected(autoZoom);
        tileOptionMenu.add(autoZoomPopup);

        autoLoad = PROP_DEFAULT_AUTOLOAD.get();
        autoLoadPopup = new JCheckBoxMenuItem();
        autoLoadPopup.setAction(new AbstractAction(tr("Auto load tiles")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                autoLoad= !autoLoad;
            }
        });
        autoLoadPopup.setSelected(autoLoad);
        tileOptionMenu.add(autoLoadPopup);

        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Load Tile")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (clickedTile != null) {
                    loadTile(clickedTile);
                    redraw();
                }
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Show Tile Info")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                out("info tile: " + clickedTile);
                if (clickedTile != null) {
                    showMetadataTile = clickedTile;
                    redraw();
                }
            }
        }));

        /* FIXME
        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Request Update")) {
            public void actionPerformed(ActionEvent ae) {
                if (clickedTile != null) {
                    clickedTile.requestUpdate();
                    redraw();
                }
            }
        }));*/

        tileOptionMenu.add(new JMenuItem(new AbstractAction(
                tr("Load All Tiles")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                loadAllTiles(true);
                redraw();
            }
        }));

        // increase and decrease commands
        tileOptionMenu.add(new JMenuItem(
                new AbstractAction(tr("Increase zoom")) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        increaseZoomLevel();
                        redraw();
                    }
                }));

        tileOptionMenu.add(new JMenuItem(
                new AbstractAction(tr("Decrease zoom")) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        decreaseZoomLevel();
                        redraw();
                    }
                }));

        // FIXME: currently ran in errors

        tileOptionMenu.add(new JMenuItem(
                new AbstractAction(tr("Snap to tile size")) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        double new_factor = Math.sqrt(getScaleFactor(currentZoomLevel));
                        Main.map.mapView.zoomToFactor(new_factor);
                        redraw();
                    }
                }));
        // end of adding menu commands

        tileOptionMenu.add(new JMenuItem(
                new AbstractAction(tr("Flush Tile Cache")) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        System.out.print("flushing all tiles...");
                        clearTileCache();
                        System.out.println("done");
                    }
                }));
        // end of adding menu commands

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main.map.mapView.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            clickedTile = getTileForPixelpos(e.getX(), e.getY());
                            tileOptionMenu.show(e.getComponent(), e.getX(), e.getY());
                        } else if (e.getButton() == MouseEvent.BUTTON1) {
                            if(!tileSource.requiresAttribution())
                                return;

                            if(attrImageBounds != null && attrImageBounds.contains(e.getPoint())) {
                                try {
                                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                                    desktop.browse(new URI(tileSource.getAttributionLinkURL()));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (URISyntaxException e1) {
                                    e1.printStackTrace();
                                }
                            } else if(attrToUBounds.contains(e.getPoint())) {
                                try {
                                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                                    desktop.browse(new URI(tileSource.getTermsOfUseURL()));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (URISyntaxException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                });

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
                        MapView.removeLayerChangeListener(this);
                    }
                });
            }
        });
    }

    void zoomChanged()
    {
        if (debug) {
            out("zoomChanged(): " + currentZoomLevel);
        }
        needRedraw = true;
        jobDispatcher.cancelOutstandingJobs();
        tileRequestsOutstanding.clear();
    }

    int getMaxZoomLvl()
    {
        if (info.getMaxZoom() != 0)
            return checkMaxZoomLvl(info.getMaxZoom(), tileSource);
        else
            return getMaxZoomLvl(tileSource);
    }

    int getMinZoomLvl()
    {
        return getMinZoomLvl(tileSource);
    }

    /**
     * Zoom in, go closer to map.
     *
     * @return    true, if zoom increasing was successfull, false othervise
     */
    public boolean zoomIncreaseAllowed()
    {
        boolean zia = currentZoomLevel < this.getMaxZoomLvl();
        if (debug) {
            out("zoomIncreaseAllowed(): " + zia + " " + currentZoomLevel + " vs. " + this.getMaxZoomLvl() );
        }
        return zia;
    }
    public boolean increaseZoomLevel()
    {
        if (zoomIncreaseAllowed()) {
            currentZoomLevel++;
            if (debug) {
                out("increasing zoom level to: " + currentZoomLevel);
            }
            zoomChanged();
        } else {
            System.err.println("current zoom lvl ("+currentZoomLevel+") couldnt be increased. "+
                    "MaxZoomLvl ("+this.getMaxZoomLvl()+") reached.");
            return false;
        }
        return true;
    }

    public boolean setZoomLevel(int zoom)
    {
        if (zoom == currentZoomLevel) return true;
        if (zoom > this.getMaxZoomLvl()) return false;
        if (zoom < this.getMinZoomLvl()) return false;
        currentZoomLevel = zoom;
        zoomChanged();
        return true;
    }

    /**
     * Zoom out from map.
     *
     * @return    true, if zoom increasing was successfull, false othervise
     */
    public boolean zoomDecreaseAllowed()
    {
        return currentZoomLevel > this.getMinZoomLvl();
    }
    public boolean decreaseZoomLevel() {
        int minZoom = this.getMinZoomLvl();
        if (zoomDecreaseAllowed()) {
            if (debug) {
                out("decreasing zoom level to: " + currentZoomLevel);
            }
            currentZoomLevel--;
            zoomChanged();
        } else {
            System.err.println("current zoom lvl couldnt be decreased. MinZoomLvl("+minZoom+") reached.");
            return false;
        }
        return true;
    }

    /*
     * We use these for quick, hackish calculations.  They
     * are temporary only and intentionally not inserted
     * into the tileCache.
     */
    synchronized Tile tempCornerTile(Tile t) {
        int x = t.getXtile() + 1;
        int y = t.getYtile() + 1;
        int zoom = t.getZoom();
        Tile tile = getTile(x, y, zoom);
        if (tile != null)
            return tile;
        return new Tile(tileSource, x, y, zoom);
    }
    synchronized Tile getOrCreateTile(int x, int y, int zoom) {
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
    synchronized Tile getTile(int x, int y, int zoom) {
        int max = (1 << zoom);
        if (x < 0 || x >= max || y < 0 || y >= max)
            return null;
        Tile tile = tileCache.getTile(tileSource, x, y, zoom);
        return tile;
    }

    synchronized boolean loadTile(Tile tile)
    {
        if (tile == null)
            return false;
        if (tile.hasError())
            return false;
        if (tile.isLoaded())
            return false;
        if (tile.isLoading())
            return false;
        if (tileRequestsOutstanding.contains(tile))
            return false;
        tileRequestsOutstanding.add(tile);
        jobDispatcher.addJob(tileLoader.createTileLoaderJob(tileSource,
                tile.getXtile(), tile.getYtile(), tile.getZoom()));
        return true;
    }

    void loadAllTiles(boolean force) {
        MapView mv = Main.map.mapView;
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());

        TileSet ts = new TileSet(topLeft, botRight, currentZoomLevel);

        // if there is more than 18 tiles on screen in any direction, do not
        // load all tiles!
        if (ts.tooLarge()) {
            System.out.println("Not downloading all tiles because there is more than 18 tiles on an axis!");
            return;
        }
        ts.loadAllTiles(force);
    }

    /*
     * Attempt to approximate how much the image is being scaled. For instance,
     * a 100x100 image being scaled to 50x50 would return 0.25.
     */
    Image lastScaledImage = null;
    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = ((infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0);
        needRedraw = true;
        if (debug) {
            out("imageUpdate() done: " + done + " calling repaint");
        }
        Main.map.repaint(done ? 0 : 100);
        return !done;
    }
    boolean imageLoaded(Image i) {
        if (i == null)
            return false;
        int status = Toolkit.getDefaultToolkit().checkImage(i, -1, -1, this);
        if ((status & ALLBITS) != 0)
            return true;
        return false;
    }
    Image getLoadedTileImage(Tile tile)
    {
        if (!tile.isLoaded())
            return null;
        Image img = tile.getImage();
        if (!imageLoaded(img))
            return null;
        return img;
    }

    LatLon tileLatLon(Tile t)
    {
        int zoom = t.getZoom();
        return new LatLon(tileSource.tileYToLat(t.getYtile(), zoom),
                tileSource.tileXToLon(t.getXtile(), zoom));
    }

    Rectangle tileToRect(Tile t1)
    {
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
    void drawImageInside(Graphics g, Image sourceImg, Rectangle source, Rectangle border)
    {
        Rectangle target = source;

        // If a border is specified, only draw the intersection
        // if what we have combined with what we are supposed
        // to draw.
        if (border != null) {
            target = source.intersection(border);
            if (debug) {
                out("source: " + source + "\nborder: " + border + "\nintersection: " + target);
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
        // And how many pixels into the image itself does that
        // correlate to?
        int img_x_offset = (int)(screen_x_offset * imageXScaling);
        int img_y_offset = (int)(screen_y_offset * imageYScaling);
        // Now calculate the other corner of the image that we need
        // by scaling the 'target' rectangle's dimensions.
        int img_x_end   = img_x_offset + (int)(target.getWidth() * imageXScaling);
        int img_y_end   = img_y_offset + (int)(target.getHeight() * imageYScaling);

        if (debug) {
            out("drawing image into target rect: " + target);
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
    List<Tile> paintTileImages(Graphics g, TileSet ts, int zoom, Tile border) {
        if (zoom <= 0) return Collections.emptyList();
        Rectangle borderRect = null;
        if (border != null) {
            borderRect = tileToRect(border);
        }
        List<Tile> missedTiles = new LinkedList<Tile>();
        for (Tile tile : ts.allTiles()) {
            Image img = getLoadedTileImage(tile);
            if (img == null || tile.hasError()) {
                if (debug) {
                    out("missed tile: " + tile);
                }
                missedTiles.add(tile);
                continue;
            }
            Rectangle sourceRect = tileToRect(tile);
            if (borderRect != null && !sourceRect.intersects(borderRect)) {
                continue;
            }
            drawImageInside(g, img, sourceRect, borderRect);
        }// end of for
        return missedTiles;
    }

    void myDrawString(Graphics g, String text, int x, int y) {
        Color oldColor = g.getColor();
        g.setColor(Color.black);
        g.drawString(text,x+1,y+1);
        g.setColor(oldColor);
        g.drawString(text,x,y);
    }

    void paintTileText(TileSet ts, Tile tile, Graphics g, MapView mv, int zoom, Tile t) {
        int fontHeight = g.getFontMetrics().getHeight();
        if (tile == null)
            return;
        Point p = pixelPos(t);
        int texty = p.y + 2 + fontHeight;

        if (PROP_DRAW_DEBUG.get()) {
            myDrawString(g, "x=" + t.getXtile() + " y=" + t.getYtile() + " z=" + zoom + "", p.x + 2, texty);
            texty += 1 + fontHeight;
            if ((t.getXtile() % 32 == 0) && (t.getYtile() % 32 == 0)) {
                myDrawString(g, "x=" + t.getXtile() / 32 + " y=" + t.getYtile() / 32 + " z=7", p.x + 2, texty);
                texty += 1 + fontHeight;
            }
        }// end of if draw debug

        if (tile == showMetadataTile) {
            String md = tile.toString();
            if (md != null) {
                myDrawString(g, md, p.x + 2, texty);
                texty += 1 + fontHeight;
            }
            Map<String, String> meta = tile.getMetadata();
            if (meta != null) {
                for (Map.Entry<String, String> entry : meta.entrySet()) {
                    myDrawString(g, entry.getKey() + ": " + entry.getValue(), p.x + 2, texty);
                    texty += 1 + fontHeight;
                }
            }
        }

        String tileStatus = tile.getStatus();
        if (!tile.isLoaded() && PROP_DRAW_DEBUG.get()) {
            myDrawString(g, tr("image " + tileStatus), p.x + 2, texty);
            texty += 1 + fontHeight;
        }

        if (tile.hasError()) {
            myDrawString(g, tr("Error") + ": " + tr(tile.getErrorMessage()), p.x + 2, texty);
            texty += 1 + fontHeight;
        }

        int xCursor = -1;
        int yCursor = -1;
        if (PROP_DRAW_DEBUG.get()) {
            if (yCursor < t.getYtile()) {
                if (t.getYtile() % 32 == 31) {
                    g.fillRect(0, p.y - 1, mv.getWidth(), 3);
                } else {
                    g.drawLine(0, p.y, mv.getWidth(), p.y);
                }
                yCursor = t.getYtile();
            }
            // This draws the vertical lines for the entire
            // column. Only draw them for the top tile in
            // the column.
            if (xCursor < t.getXtile()) {
                if (t.getXtile() % 32 == 0) {
                    // level 7 tile boundary
                    g.fillRect(p.x - 1, 0, 3, mv.getHeight());
                } else {
                    g.drawLine(p.x, 0, p.x, mv.getHeight());
                }
                xCursor = t.getXtile();
            }
        }
    }

    private Point pixelPos(LatLon ll) {
        return Main.map.mapView.getPoint(Main.proj.latlon2eastNorth(ll).add(getDx(), getDy()));
    }
    private Point pixelPos(Tile t) {
        double lon = tileSource.tileXToLon(t.getXtile(), t.getZoom());
        LatLon tmpLL = new LatLon(tileSource.tileYToLat(t.getYtile(), t.getZoom()), lon);
        return pixelPos(tmpLL);
    }
    private LatLon getShiftedLatLon(EastNorth en) {
        return Main.proj.eastNorth2latlon(en.add(-getDx(), -getDy()));
    }
    private Coordinate getShiftedCoord(EastNorth en) {
        LatLon ll = getShiftedLatLon(en);
        return new Coordinate(ll.lat(),ll.lon());
    }
    private final TileSet nullTileSet = new TileSet((LatLon)null, (LatLon)null, 0);
    private class TileSet {
        int x0, x1, y0, y1;
        int zoom;
        int tileMax = -1;

        /**
         * Create a TileSet by EastNorth bbox taking a layer shift in account
         */
        TileSet(EastNorth topLeft, EastNorth botRight, int zoom) {
            this(getShiftedLatLon(topLeft), getShiftedLatLon(botRight),zoom);
        }

        /**
         * Create a TileSet by known LatLon bbox without layer shift correction
         */
        TileSet(LatLon topLeft, LatLon botRight, int zoom) {
            this.zoom = zoom;
            if (zoom == 0)
                return;

            x0 = (int)tileSource.lonToTileX(topLeft.lon(),  zoom);
            y0 = (int)tileSource.latToTileY(topLeft.lat(),  zoom);
            x1 = (int)tileSource.lonToTileX(botRight.lon(), zoom);
            y1 = (int)tileSource.latToTileY(botRight.lat(), zoom);
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
            tileMax = (int)Math.pow(2.0, zoom);
            if (x0 < 0) {
                x0 = 0;
            }
            if (y0 < 0) {
                y0 = 0;
            }
            if (x1 > tileMax) {
                x1 = tileMax;
            }
            if (y1 > tileMax) {
                y1 = tileMax;
            }
        }
        boolean tooSmall() {
            return this.tilesSpanned() < 2.1;
        }
        boolean tooLarge() {
            return this.tilesSpanned() > 10;
        }
        boolean insane() {
            return this.tilesSpanned() > 100;
        }
        double tilesSpanned() {
            return Math.sqrt(1.0 * this.size());
        }

        int size() {
            int x_span = x1 - x0 + 1;
            int y_span = y1 - y0 + 1;
            return x_span * y_span;
        }

        /*
         * Get all tiles represented by this TileSet that are
         * already in the tileCache.
         */
        List<Tile> allTiles()
        {
            return this.allTiles(false);
        }
        private List<Tile> allTiles(boolean create)
        {
            // Tileset is either empty or too large
            if (zoom == 0 || this.insane())
                return Collections.emptyList();
            List<Tile> ret = new ArrayList<Tile>();
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    Tile t;
                    if (create) {
                        t = getOrCreateTile(x % tileMax, y % tileMax, zoom);
                    } else {
                        t = getTile(x % tileMax, y % tileMax, zoom);
                    }
                    if (t != null) {
                        ret.add(t);
                    }
                }
            }
            return ret;
        }

        void loadAllTiles(boolean force)
        {
            List<Tile> tiles = this.allTiles(true);
            if (!autoLoad && !force)
                return;
            int nr_queued = 0;
            for (Tile t : tiles) {
                if (loadTile(t)) {
                    nr_queued++;
                }
            }
            if (debug)
                if (nr_queued > 0) {
                    out("queued to load: " + nr_queued + "/" + tiles.size() + " tiles at zoom: " + zoom);
                }
        }
    }


    private static class TileSetInfo {
        public boolean hasVisibleTiles = false;
        public boolean hasOverzoomedTiles = false;
        public boolean hasLoadingTiles = false;
    }

    private static TileSetInfo getTileSetInfo(TileSet ts) {
        List<Tile> allTiles = ts.allTiles();
        TileSetInfo result = new TileSetInfo();
        result.hasLoadingTiles = allTiles.size() < ts.size();
        for (Tile t : allTiles) {
            if (t.isLoaded()) {
                if (!t.hasError()) {
                    result.hasVisibleTiles = true;
                }
                if ("no-tile".equals(t.getValue("tile-info"))) {
                    result.hasOverzoomedTiles = true;
                }
            } else {
                result.hasLoadingTiles = true;
            }
        }
        return result;
    }

    private class DeepTileSet {
        final EastNorth topLeft, botRight;
        final int minZoom, maxZoom;
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
            TileSet ts = tileSets[zoom-minZoom];
            if (ts == null) {
                ts = new TileSet(topLeft, botRight, zoom);
                tileSets[zoom-minZoom] = ts;
            }
            return ts;
        }
        public TileSetInfo getTileSetInfo(int zoom) {
            if (zoom < minZoom)
                return new TileSetInfo();
            TileSetInfo tsi = tileSetInfos[zoom-minZoom];
            if (tsi == null) {
                tsi = TMSLayer.getTileSetInfo(getTileSet(zoom));
                tileSetInfos[zoom-minZoom] = tsi;
            }
            return tsi;
        }
    }

    /**
     */
    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bounds) {
        //long start = System.currentTimeMillis();
        EastNorth topLeft = mv.getEastNorth(0, 0);
        EastNorth botRight = mv.getEastNorth(mv.getWidth(), mv.getHeight());

        if (botRight.east() == 0.0 || botRight.north() == 0) {
            Main.debug("still initializing??");
            // probably still initializing
            return;
        }

        needRedraw = false;

        int zoom = currentZoomLevel;
        if (autoZoom) {
            double pixelScaling = getScaleFactor(zoom);
            if (pixelScaling > 3 || pixelScaling < 0.45) {
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
            while (displayZoomLevel > dts.minZoom &&
                    !dts.getTileSetInfo(displayZoomLevel).hasVisibleTiles) {
                displayZoomLevel--;
            }
            // Do binary search between currentZoomLevel and displayZoomLevel
            while (zoom > displayZoomLevel && !tsi.hasVisibleTiles && tsi.hasOverzoomedTiles){
                zoom = (zoom + displayZoomLevel)/2;
                tsi = dts.getTileSetInfo(zoom);
            }

            setZoomLevel(zoom);

            // If all tiles at displayZoomLevel is loaded, load all tiles at next zoom level
            // to make sure there're really no more zoom levels
            if (zoom == displayZoomLevel && !tsi.hasLoadingTiles && zoom < dts.maxZoom) {
                zoom++;
                tsi = dts.getTileSetInfo(zoom);
            }
            // When we have overzoomed tiles and all tiles at current zoomlevel is loaded,
            // load tiles at previovus zoomlevels until we have all tiles on screen is loaded.
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
            //out("size: " + ts.size() + " spanned: " + ts.tilesSpanned());
            ts.loadAllTiles(false);
        }

        if (displayZoomLevel != zoom) {
            ts = dts.getTileSet(displayZoomLevel);
        }

        g.setColor(Color.DARK_GRAY);

        List<Tile> missedTiles = this.paintTileImages(g, ts, displayZoomLevel, null);
        int otherZooms[] = { -1, 1, -2, 2, -3, -4, -5};
        for (int zoomOffset : otherZooms) {
            if (!autoZoom) {
                break;
            }
            if (!autoLoad) {
                break;
            }
            int newzoom = displayZoomLevel + zoomOffset;
            if (missedTiles.size() <= 0) {
                break;
            }
            List<Tile> newlyMissedTiles = new LinkedList<Tile>();
            for (Tile missed : missedTiles) {
                if ("no-tile".equals(missed.getValue("tile-info")) && zoomOffset > 0) {
                    // Don't try to paint from higher zoom levels when tile is overzoomed
                    newlyMissedTiles.add(missed);
                    continue;
                }
                Tile t2 = tempCornerTile(missed);
                LatLon topLeft2  = tileLatLon(missed);
                LatLon botRight2 = tileLatLon(t2);
                TileSet ts2 = new TileSet(topLeft2, botRight2, newzoom);
                if (ts2.tooLarge()) {
                    continue;
                }
                newlyMissedTiles.addAll(this.paintTileImages(g, ts2, newzoom, missed));
            }
            missedTiles = newlyMissedTiles;
        }
        if (debug && missedTiles.size() > 0) {
            out("still missed "+missedTiles.size()+" in the end");
        }
        g.setColor(Color.red);
        g.setFont(InfoFont);

        // The current zoom tileset is guaranteed to have all of
        // its tiles
        for (Tile t : ts.allTiles()) {
            this.paintTileText(ts, t, g, mv, displayZoomLevel, t);
        }

        if (tileSource.requiresAttribution()) {
            // Draw attribution
            Font font = g.getFont();
            g.setFont(ATTR_LINK_FONT);
            g.setColor(Color.white);

            // Draw terms of use text
            Rectangle2D termsStringBounds = g.getFontMetrics().getStringBounds("Background Terms of Use", g);
            int textHeight = (int) termsStringBounds.getHeight() - 5;
            int textWidth = (int) termsStringBounds.getWidth();
            int termsTextY = mv.getHeight() - textHeight;
            if(attrTermsUrl != null) {
                int x = 2;
                int y = mv.getHeight() - textHeight;
                attrToUBounds = new Rectangle(x, y, textWidth, textHeight);
                myDrawString(g, "Background Terms of Use", x, y);
            }

            // Draw attribution logo
            if(attrImage != null) {
                int x = 2;
                int imgWidth = attrImage.getWidth(this);
                int height = attrImage.getHeight(this);
                int y = termsTextY - height - textHeight - 5;
                attrImageBounds = new Rectangle(x, y, imgWidth, height);
                g.drawImage(attrImage, x, y, this);
            }

            g.setFont(ATTR_FONT);
            String attributionText = tileSource.getAttributionText(displayZoomLevel,
                    getShiftedCoord(topLeft), getShiftedCoord(botRight));
            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(attributionText, g);
            {
                int x = mv.getWidth() - (int) stringBounds.getWidth();
                int y = mv.getHeight() - textHeight;
                myDrawString(g, attributionText, x, y);
            }

            g.setFont(font);
        }

        //g.drawString("currentZoomLevel=" + currentZoomLevel, 120, 120);
        g.setColor(Color.lightGray);
        if (!autoZoom) {
            if (ts.insane()) {
                myDrawString(g, tr("zoom in to load any tiles"), 120, 120);
            } else if (ts.tooLarge()) {
                myDrawString(g, tr("zoom in to load more tiles"), 120, 120);
            } else if (ts.tooSmall()) {
                myDrawString(g, tr("increase zoom level to see more detail"), 120, 120);
            }
        }
        if (noTilesAtZoom) {
            myDrawString(g, tr("No tiles at this zoom level"), 120, 120);
        }
        if (debug) {
            myDrawString(g, tr("Current zoom: {0}", currentZoomLevel), 50, 140);
            myDrawString(g, tr("Display zoom: {0}", displayZoomLevel), 50, 155);
            myDrawString(g, tr("Pixel scale: {0}", getScaleFactor(currentZoomLevel)), 50, 170);
            myDrawString(g, tr("Best zoom: {0}", Math.log(getScaleFactor(1))/Math.log(2)/2+1), 50, 185);
        }
    }// end of paint method

    /**
     * This isn't very efficient, but it is only used when the
     * user right-clicks on the map.
     */
    Tile getTileForPixelpos(int px, int py) {
        if (debug) {
            out("getTileForPixelpos("+px+", "+py+")");
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
        for (Tile t1 : ts.allTiles()) {
            Tile t2 = tempCornerTile(t1);
            Rectangle r = new Rectangle(pixelPos(t1));
            r.add(pixelPos(t2));
            if (debug) {
                out("r: " + r + " clicked: " + clicked);
            }
            if (!r.contains(clicked)) {
                continue;
            }
            clickedTile  = t1;
            break;
        }
        if (clickedTile == null)
            return null;
        System.out.println("clicked on tile: " + clickedTile.getXtile() + " " + clickedTile.getYtile() +
                " currentZoomLevel: " + currentZoomLevel);
        return clickedTile;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                // color,
                new OffsetAction(),
                new RenameLayerAction(this.getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
    }

    @Override
    public String getToolTipText() {
        return null;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
    }

    @Override
    public boolean isChanged() {
        return needRedraw;
    }
}
