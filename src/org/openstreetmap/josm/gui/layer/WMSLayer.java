// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.gui.jmapviewer.AttributionSupport;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.GeorefImage;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.WmsCache;
import org.openstreetmap.josm.data.imagery.types.ObjectFactory;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.WMSLayerImporter;
import org.openstreetmap.josm.io.imagery.Grabber;
import org.openstreetmap.josm.io.imagery.HTMLGrabber;
import org.openstreetmap.josm.io.imagery.WMSGrabber;
import org.openstreetmap.josm.io.imagery.WMSRequest;


/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 */
public class WMSLayer extends ImageryLayer implements ImageObserver, PreferenceChangedListener, Externalizable {

    public static class PrecacheTask {
        private final ProgressMonitor progressMonitor;
        private volatile int totalCount;
        private volatile int processedCount;
        private volatile boolean isCancelled;

        public PrecacheTask(ProgressMonitor progressMonitor) {
            this.progressMonitor = progressMonitor;
        }

        public boolean isFinished() {
            return totalCount == processedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void cancel() {
            isCancelled = true;
        }
    }

    // Fake reference to keep build scripts from removing ObjectFactory class. This class is not used directly but it's necessary for jaxb to work
    private static final ObjectFactory OBJECT_FACTORY = null;

    // these values correspond to the zoom levels used throughout OSM and are in meters/pixel from zoom level 0 to 18.
    // taken from http://wiki.openstreetmap.org/wiki/Zoom_levels
    private static final Double[] snapLevels = { 156412.0, 78206.0, 39103.0, 19551.0, 9776.0, 4888.0,
        2444.0, 1222.0, 610.984, 305.492, 152.746, 76.373, 38.187, 19.093, 9.547, 4.773, 2.387, 1.193, 0.596 };

    public static final BooleanProperty PROP_ALPHA_CHANNEL = new BooleanProperty("imagery.wms.alpha_channel", true);
    public static final IntegerProperty PROP_SIMULTANEOUS_CONNECTIONS = new IntegerProperty("imagery.wms.simultaneousConnections", 3);
    public static final BooleanProperty PROP_OVERLAP = new BooleanProperty("imagery.wms.overlap", false);
    public static final IntegerProperty PROP_OVERLAP_EAST = new IntegerProperty("imagery.wms.overlapEast", 14);
    public static final IntegerProperty PROP_OVERLAP_NORTH = new IntegerProperty("imagery.wms.overlapNorth", 4);
    public static final IntegerProperty PROP_IMAGE_SIZE = new IntegerProperty("imagery.wms.imageSize", 500);
    public static final BooleanProperty PROP_DEFAULT_AUTOZOOM = new BooleanProperty("imagery.wms.default_autozoom", true);

    public int messageNum = 5; //limit for messages per layer
    protected double resolution;
    protected String resolutionText;
    protected int imageSize;
    protected int dax = 10;
    protected int day = 10;
    protected int daStep = 5;
    protected int minZoom = 3;

    protected GeorefImage[][] images;
    protected static final int serializeFormatVersion = 5;
    protected boolean autoDownloadEnabled = true;
    protected boolean autoResolutionEnabled = PROP_DEFAULT_AUTOZOOM.get();
    protected boolean settingsChanged;
    public WmsCache cache;
    private AttributionSupport attribution = new AttributionSupport();

    // Image index boundary for current view
    private volatile int bminx;
    private volatile int bminy;
    private volatile int bmaxx;
    private volatile int bmaxy;
    private volatile int leftEdge;
    private volatile int bottomEdge;

    // Request queue
    private final List<WMSRequest> requestQueue = new ArrayList<WMSRequest>();
    private final List<WMSRequest> finishedRequests = new ArrayList<WMSRequest>();
    /**
     * List of request currently being processed by download threads
     */
    private final List<WMSRequest> processingRequests = new ArrayList<WMSRequest>();
    private final Lock requestQueueLock = new ReentrantLock();
    private final Condition queueEmpty = requestQueueLock.newCondition();
    private final List<Grabber> grabbers = new ArrayList<Grabber>();
    private final List<Thread> grabberThreads = new ArrayList<Thread>();
    private boolean canceled;

    /** set to true if this layer uses an invalid base url */
    private boolean usesInvalidUrl = false;
    /** set to true if the user confirmed to use an potentially invalid WMS base url */
    private boolean isInvalidUrlConfirmed = false;

    public WMSLayer() {
        this(new ImageryInfo(tr("Blank Layer")));
    }

    public WMSLayer(ImageryInfo info) {
        super(info);
        imageSize = PROP_IMAGE_SIZE.get();
        setBackgroundLayer(true); /* set global background variable */
        initializeImages();

        attribution.initialize(this.info);

        Main.pref.addPreferenceChangeListener(this);
    }

    @Override
    public void hookUpMapView() {
        if (info.getUrl() != null) {
            startGrabberThreads();

            for (WMSLayer layer: Main.map.mapView.getLayersOfType(WMSLayer.class)) {
                if (layer.getInfo().getUrl().equals(info.getUrl())) {
                    cache = layer.cache;
                    break;
                }
            }
            if (cache == null) {
                cache = new WmsCache(info.getUrl(), imageSize);
                cache.loadIndex();
            }
        }

        // if automatic resolution is enabled, ensure that the first zoom level
        // is already snapped. Otherwise it may load tiles that will never get
        // used again when zooming.
        updateResolutionSetting(this, autoResolutionEnabled);

        final MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isVisible()) return;
                if (e.getButton() == MouseEvent.BUTTON1) {
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
                if (oldLayer == WMSLayer.this) {
                    Main.map.mapView.removeMouseListener(adapter);
                    MapView.removeLayerChangeListener(this);
                }
            }
        });
    }

    public void doSetName(String name) {
        setName(name);
        info.setName(name);
    }

    public boolean hasAutoDownload(){
        return autoDownloadEnabled;
    }

    public void downloadAreaToCache(PrecacheTask precacheTask, List<LatLon> points, double bufferX, double bufferY) {
        Set<Point> requestedTiles = new HashSet<Point>();
        for (LatLon point: points) {
            EastNorth minEn = Main.getProjection().latlon2eastNorth(new LatLon(point.lat() - bufferY, point.lon() - bufferX));
            EastNorth maxEn = Main.getProjection().latlon2eastNorth(new LatLon(point.lat() + bufferY, point.lon() + bufferX));
            int minX = getImageXIndex(minEn.east());
            int maxX = getImageXIndex(maxEn.east());
            int minY = getImageYIndex(minEn.north());
            int maxY = getImageYIndex(maxEn.north());

            for (int x=minX; x<=maxX; x++) {
                for (int y=minY; y<=maxY; y++) {
                    requestedTiles.add(new Point(x, y));
                }
            }
        }

        for (Point p: requestedTiles) {
            addRequest(new WMSRequest(p.x, p.y, info.getPixelPerDegree(), true, false, precacheTask));
        }

        precacheTask.progressMonitor.setTicksCount(precacheTask.getTotalCount());
        precacheTask.progressMonitor.setCustomText(tr("Downloaded {0}/{1} tiles", 0, precacheTask.totalCount));
    }

    @Override
    public void destroy() {
        super.destroy();
        cancelGrabberThreads(false);
        Main.pref.removePreferenceChangeListener(this);
        if (cache != null) {
            cache.saveIndex();
        }
    }

    public void initializeImages() {
        GeorefImage[][] old = images;
        images = new GeorefImage[dax][day];
        if (old != null) {
            for (GeorefImage[] row : old) {
                for (GeorefImage image : row) {
                    images[modulo(image.getXIndex(), dax)][modulo(image.getYIndex(), day)] = image;
                }
            }
        }
        for(int x = 0; x<dax; ++x) {
            for(int y = 0; y<day; ++y) {
                if (images[x][y] == null) {
                    images[x][y]= new GeorefImage(this);
                }
            }
        }
    }

    @Override public ImageryInfo getInfo() {
        return info;
    }

    @Override public String getToolTipText() {
        if(autoDownloadEnabled)
            return tr("WMS layer ({0}), automatically downloading in zoom {1}", getName(), resolutionText);
        else
            return tr("WMS layer ({0}), downloading in zoom {1}", getName(), resolutionText);
    }

    private int modulo (int a, int b) {
        return a % b >= 0 ? a%b : a%b+b;
    }

    private boolean zoomIsTooBig() {
        //don't download when it's too outzoomed
        return info.getPixelPerDegree() / getPPD() > minZoom;
    }

    @Override public void paint(Graphics2D g, final MapView mv, Bounds b) {
        if(info.getUrl() == null || (usesInvalidUrl && !isInvalidUrlConfirmed)) return;

        if (autoResolutionEnabled && getBestZoom() != mv.getDist100Pixel()) {
            changeResolution(this, true);
        }

        settingsChanged = false;

        ProjectionBounds bounds = mv.getProjectionBounds();
        bminx= getImageXIndex(bounds.minEast);
        bminy= getImageYIndex(bounds.minNorth);
        bmaxx= getImageXIndex(bounds.maxEast);
        bmaxy= getImageYIndex(bounds.maxNorth);

        leftEdge = (int)(bounds.minEast * getPPD());
        bottomEdge = (int)(bounds.minNorth * getPPD());

        if (zoomIsTooBig()) {
            for(int x = 0; x<images.length; ++x) {
                for(int y = 0; y<images[0].length; ++y) {
                    GeorefImage image = images[x][y];
                    image.paint(g, mv, image.getXIndex(), image.getYIndex(), leftEdge, bottomEdge);
                }
            }
        } else {
            downloadAndPaintVisible(g, mv, false);
        }

        attribution.paintAttribution(g, mv.getWidth(), mv.getHeight(), null, null, 0, this);

    }

    @Override
    public void setOffset(double dx, double dy) {
        super.setOffset(dx, dy);
        settingsChanged = true;
    }

    public int getImageXIndex(double coord) {
        return (int)Math.floor( ((coord - dx) * info.getPixelPerDegree()) / imageSize);
    }

    public int getImageYIndex(double coord) {
        return (int)Math.floor( ((coord - dy) * info.getPixelPerDegree()) / imageSize);
    }

    public int getImageX(int imageIndex) {
        return (int)(imageIndex * imageSize * (getPPD() / info.getPixelPerDegree()) + dx * getPPD());
    }

    public int getImageY(int imageIndex) {
        return (int)(imageIndex * imageSize * (getPPD() / info.getPixelPerDegree()) + dy * getPPD());
    }

    public int getImageWidth(int xIndex) {
        return getImageX(xIndex + 1) - getImageX(xIndex);
    }

    public int getImageHeight(int yIndex) {
        return getImageY(yIndex + 1) - getImageY(yIndex);
    }

    /**
     *
     * @return Size of image in original zoom
     */
    public int getBaseImageWidth() {
        int overlap = PROP_OVERLAP.get() ? (PROP_OVERLAP_EAST.get() * imageSize / 100) : 0;
        return imageSize + overlap;
    }

    /**
     *
     * @return Size of image in original zoom
     */
    public int getBaseImageHeight() {
        int overlap = PROP_OVERLAP.get() ? (PROP_OVERLAP_NORTH.get() * imageSize / 100) : 0;
        return imageSize + overlap;
    }

    public int getImageSize() {
        return imageSize;
    }

    public boolean isOverlapEnabled() {
        return WMSLayer.PROP_OVERLAP.get() && (WMSLayer.PROP_OVERLAP_EAST.get() > 0 || WMSLayer.PROP_OVERLAP_NORTH.get() > 0);
    }

    /**
     *
     * @return When overlapping is enabled, return visible part of tile. Otherwise return original image
     */
    public BufferedImage normalizeImage(BufferedImage img) {
        if (isOverlapEnabled()) {
            BufferedImage copy = img;
            img = new BufferedImage(imageSize, imageSize, copy.getType());
            img.createGraphics().drawImage(copy, 0, 0, imageSize, imageSize,
                    0, copy.getHeight() - imageSize, imageSize, copy.getHeight(), null);
        }
        return img;
    }

    /**
     *
     * @param xIndex
     * @param yIndex
     * @return Real EastNorth of given tile. dx/dy is not counted in
     */
    public EastNorth getEastNorth(int xIndex, int yIndex) {
        return new EastNorth((xIndex * imageSize) / info.getPixelPerDegree(), (yIndex * imageSize) / info.getPixelPerDegree());
    }

    protected void downloadAndPaintVisible(Graphics g, final MapView mv, boolean real){

        int newDax = dax;
        int newDay = day;

        if (bmaxx - bminx >= dax || bmaxx - bminx < dax - 2 * daStep) {
            newDax = ((bmaxx - bminx) / daStep + 1) * daStep;
        }

        if (bmaxy - bminy >= day || bmaxy - bminx < day - 2 * daStep) {
            newDay = ((bmaxy - bminy) / daStep + 1) * daStep;
        }

        if (newDax != dax || newDay != day) {
            dax = newDax;
            day = newDay;
            initializeImages();
        }

        for(int x = bminx; x<=bmaxx; ++x) {
            for(int y = bminy; y<=bmaxy; ++y){
                images[modulo(x,dax)][modulo(y,day)].changePosition(x, y);
            }
        }

        gatherFinishedRequests();
        Set<ProjectionBounds> areaToCache = new HashSet<ProjectionBounds>();

        for(int x = bminx; x<=bmaxx; ++x) {
            for(int y = bminy; y<=bmaxy; ++y){
                GeorefImage img = images[modulo(x,dax)][modulo(y,day)];
                if (!img.paint(g, mv, x, y, leftEdge, bottomEdge)) {
                    WMSRequest request = new WMSRequest(x, y, info.getPixelPerDegree(), real, true);
                    addRequest(request);
                    areaToCache.add(new ProjectionBounds(getEastNorth(x, y), getEastNorth(x + 1, y + 1)));
                } else if (img.getState() == State.PARTLY_IN_CACHE && autoDownloadEnabled) {
                    WMSRequest request = new WMSRequest(x, y, info.getPixelPerDegree(), real, false);
                    addRequest(request);
                    areaToCache.add(new ProjectionBounds(getEastNorth(x, y), getEastNorth(x + 1, y + 1)));
                }
            }
        }
        if (cache != null) {
            cache.setAreaToCache(areaToCache);
        }
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for(int x = 0; x<dax; ++x) {
            for(int y = 0; y<day; ++y)
                if(images[x][y].getImage() != null){
                    v.visit(images[x][y].getMin());
                    v.visit(images[x][y].getMax());
                }
        }
    }

    @Override public Action[] getMenuEntries() {
        return new Action[]{
                LayerListDialog.getInstance().createActivateLayerAction(this),
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new OffsetAction(),
                new LayerSaveAction(this),
                new LayerSaveAsAction(this),
                new BookmarkWmsAction(),
                SeparatorLayerAction.INSTANCE,
                new StartStopAction(),
                new ToggleAlphaAction(),
                new ToggleAutoResolutionAction(),
                new ChangeResolutionAction(),
                new ZoomToNativeResolution(),
                new ReloadErrorTilesAction(),
                new DownloadAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this)
        };
    }

    public GeorefImage findImage(EastNorth eastNorth) {
        int xIndex = getImageXIndex(eastNorth.east());
        int yIndex = getImageYIndex(eastNorth.north());
        GeorefImage result = images[modulo(xIndex, dax)][modulo(yIndex, day)];
        if (result.getXIndex() == xIndex && result.getYIndex() == yIndex)
            return result;
        else
            return null;
    }

    /**
     *
     * @param request
     * @return -1 if request is no longer needed, otherwise priority of request (lower number &lt;=&gt; more important request)
     */
    private int getRequestPriority(WMSRequest request) {
        if (request.getPixelPerDegree() != info.getPixelPerDegree())
            return -1;
        if (bminx > request.getXIndex()
                || bmaxx < request.getXIndex()
                || bminy > request.getYIndex()
                || bmaxy < request.getYIndex())
            return -1;

        MouseEvent lastMEvent = Main.map.mapView.lastMEvent;
        EastNorth cursorEastNorth = Main.map.mapView.getEastNorth(lastMEvent.getX(), lastMEvent.getY());
        int mouseX = getImageXIndex(cursorEastNorth.east());
        int mouseY = getImageYIndex(cursorEastNorth.north());
        int dx = request.getXIndex() - mouseX;
        int dy = request.getYIndex() - mouseY;

        return 1 + dx * dx + dy * dy;
    }

    private void sortRequests(boolean localOnly) {
        Iterator<WMSRequest> it = requestQueue.iterator();
        while (it.hasNext()) {
            WMSRequest item = it.next();

            if (item.getPrecacheTask() != null && item.getPrecacheTask().isCancelled) {
                it.remove();
                continue;
            }

            int priority = getRequestPriority(item);
            if (priority == -1 && item.isPrecacheOnly()) {
                priority = Integer.MAX_VALUE; // Still download, but prefer requests in current view
            }

            if (localOnly && !item.hasExactMatch()) {
                priority = Integer.MAX_VALUE; // Only interested in tiles that can be loaded from file immediately
            }

            if (       priority == -1
                    || finishedRequests.contains(item)
                    || processingRequests.contains(item)) {
                it.remove();
            } else {
                item.setPriority(priority);
            }
        }
        Collections.sort(requestQueue);
    }

    public WMSRequest getRequest(boolean localOnly) {
        requestQueueLock.lock();
        try {
            sortRequests(localOnly);
            while (!canceled && (requestQueue.isEmpty() || (localOnly && !requestQueue.get(0).hasExactMatch()))) {
                try {
                    queueEmpty.await();
                    sortRequests(localOnly);
                } catch (InterruptedException e) {
                    Main.warn("InterruptedException in "+getClass().getSimpleName()+" during WMS request");
                }
            }

            if (canceled)
                return null;
            else {
                WMSRequest request = requestQueue.remove(0);
                processingRequests.add(request);
                return request;
            }

        } finally {
            requestQueueLock.unlock();
        }
    }

    public void finishRequest(WMSRequest request) {
        requestQueueLock.lock();
        try {
            PrecacheTask task = request.getPrecacheTask();
            if (task != null) {
                task.processedCount++;
                if (!task.progressMonitor.isCanceled()) {
                    task.progressMonitor.worked(1);
                    task.progressMonitor.setCustomText(tr("Downloaded {0}/{1} tiles", task.processedCount, task.totalCount));
                }
            }
            processingRequests.remove(request);
            if (request.getState() != null && !request.isPrecacheOnly()) {
                finishedRequests.add(request);
                if (Main.isDisplayingMapView()) {
                    Main.map.mapView.repaint();
                }
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    public void addRequest(WMSRequest request) {
        requestQueueLock.lock();
        try {

            if (cache != null) {
                ProjectionBounds b = getBounds(request);
                // Checking for exact match is fast enough, no need to do it in separated thread
                request.setHasExactMatch(cache.hasExactMatch(Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth));
                if (request.isPrecacheOnly() && request.hasExactMatch())
                    return; // We already have this tile cached
            }

            if (!requestQueue.contains(request) && !finishedRequests.contains(request) && !processingRequests.contains(request)) {
                requestQueue.add(request);
                if (request.getPrecacheTask() != null) {
                    request.getPrecacheTask().totalCount++;
                }
                queueEmpty.signalAll();
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    public boolean requestIsVisible(WMSRequest request) {
        return bminx <= request.getXIndex() && bmaxx >= request.getXIndex() && bminy <= request.getYIndex() && bmaxy >= request.getYIndex();
    }

    private void gatherFinishedRequests() {
        requestQueueLock.lock();
        try {
            for (WMSRequest request: finishedRequests) {
                GeorefImage img = images[modulo(request.getXIndex(),dax)][modulo(request.getYIndex(),day)];
                if (img.equalPosition(request.getXIndex(), request.getYIndex())) {
                    img.changeImage(request.getState(), request.getImage());
                }
            }
        } finally {
            requestQueueLock.unlock();
            finishedRequests.clear();
        }
    }

    public class DownloadAction extends AbstractAction {
        public DownloadAction() {
            super(tr("Download visible tiles"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            if (zoomIsTooBig()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("The requested area is too big. Please zoom in a little, or change resolution"),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            } else {
                downloadAndPaintVisible(Main.map.mapView.getGraphics(), Main.map.mapView, true);
            }
        }
    }

    /**
     * Finds the most suitable resolution for the current zoom level, but prefers
     * higher resolutions. Snaps to values defined in snapLevels.
     * @return
     */
    private static double getBestZoom() {
        // not sure why getDist100Pixel returns values corresponding to
        // the snapLevels, which are in meters per pixel. It works, though.
        double dist = Main.map.mapView.getDist100Pixel();
        for(int i = snapLevels.length-2; i >= 0; i--) {
            if(snapLevels[i+1]/3 + snapLevels[i]*2/3 > dist)
                return snapLevels[i+1];
        }
        return snapLevels[0];
    }

    /**
     * Updates the given layer’s resolution settings to the current zoom level. Does
     * not update existing tiles, only new ones will be subject to the new settings.
     *
     * @param layer
     * @param snap  Set to true if the resolution should snap to certain values instead of
     *              matching the current zoom level perfectly
     */
    private static void updateResolutionSetting(WMSLayer layer, boolean snap) {
        if(snap) {
            layer.resolution = getBestZoom();
            layer.resolutionText = MapView.getDistText(layer.resolution);
        } else {
            layer.resolution = Main.map.mapView.getDist100Pixel();
            layer.resolutionText = Main.map.mapView.getDist100PixelText();
        }
        layer.info.setPixelPerDegree(layer.getPPD());
    }

    /**
     * Updates the given layer’s resolution settings to the current zoom level and
     * updates existing tiles. If round is true, tiles will be updated gradually, if
     * false they will be removed instantly (and redrawn only after the new resolution
     * image has been loaded).
     * @param layer
     * @param snap  Set to true if the resolution should snap to certain values instead of
     *              matching the current zoom level perfectly
     */
    private static void changeResolution(WMSLayer layer, boolean snap) {
        updateResolutionSetting(layer, snap);

        layer.settingsChanged = true;

        // Don’t move tiles off screen when the resolution is rounded. This
        // prevents some flickering when zooming with auto-resolution enabled
        // and instead gradually updates each tile.
        if(!snap) {
            for(int x = 0; x<layer.dax; ++x) {
                for(int y = 0; y<layer.day; ++y) {
                    layer.images[x][y].changePosition(-1, -1);
                }
            }
        }
    }

    public static class ChangeResolutionAction extends AbstractAction implements LayerAction {

        /**
         * Constructs a new {@code ChangeResolutionAction}
         */
        public ChangeResolutionAction() {
            super(tr("Change resolution"));
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            List<Layer> layers = LayerListDialog.getInstance().getModel().getSelectedLayers();
            for (Layer l: layers) {
                changeResolution((WMSLayer) l, false);
            }
            Main.map.mapView.repaint();
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            for (Layer l: layers) {
                if (!(l instanceof WMSLayer))
                    return false;
            }
            return true;
        }

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ChangeResolutionAction;
        }
    }

    public class ReloadErrorTilesAction extends AbstractAction {
        public ReloadErrorTilesAction() {
            super(tr("Reload erroneous tiles"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            // Delete small files, because they're probably blank tiles.
            // See #2307
            cache.cleanSmallFiles(4096);

            for (int x = 0; x < dax; ++x) {
                for (int y = 0; y < day; ++y) {
                    GeorefImage img = images[modulo(x,dax)][modulo(y,day)];
                    if(img.getState() == State.FAILED){
                        addRequest(new WMSRequest(img.getXIndex(), img.getYIndex(), info.getPixelPerDegree(), true, false));
                    }
                }
            }
        }
    }

    public class ToggleAlphaAction extends AbstractAction implements LayerAction {
        public ToggleAlphaAction() {
            super(tr("Alpha channel"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) ev.getSource();
            boolean alphaChannel = checkbox.isSelected();
            PROP_ALPHA_CHANNEL.put(alphaChannel);

            // clear all resized cached instances and repaint the layer
            for (int x = 0; x < dax; ++x) {
                for (int y = 0; y < day; ++y) {
                    GeorefImage img = images[modulo(x, dax)][modulo(y, day)];
                    img.flushedResizedCachedInstance();
                }
            }
            Main.map.mapView.repaint();
        }
        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(PROP_ALPHA_CHANNEL.get());
            return item;
        }
        @Override
        public boolean supportLayers(List<Layer> layers) {
            return layers.size() == 1 && layers.get(0) instanceof WMSLayer;
        }
    }


    public class ToggleAutoResolutionAction extends AbstractAction implements LayerAction {
        public ToggleAutoResolutionAction() {
            super(tr("Automatically change resolution"));
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) ev.getSource();
            autoResolutionEnabled = checkbox.isSelected();
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(autoResolutionEnabled);
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return layers.size() == 1 && layers.get(0) instanceof WMSLayer;
        }
    }

    /**
     * This action will add a WMS layer menu entry with the current WMS layer
     * URL and name extended by the current resolution.
     * When using the menu entry again, the WMS cache will be used properly.
     */
    public class BookmarkWmsAction extends AbstractAction {
        public BookmarkWmsAction() {
            super(tr("Set WMS Bookmark"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            ImageryLayerInfo.addLayer(new ImageryInfo(info));
        }
    }

    private class StartStopAction extends AbstractAction implements LayerAction {

        public StartStopAction() {
            super(tr("Automatic downloading"));
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(autoDownloadEnabled);
            return item;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return layers.size() == 1 && layers.get(0) instanceof WMSLayer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            autoDownloadEnabled = !autoDownloadEnabled;
            if (autoDownloadEnabled) {
                for (int x = 0; x < dax; ++x) {
                    for (int y = 0; y < day; ++y) {
                        GeorefImage img = images[modulo(x,dax)][modulo(y,day)];
                        if(img.getState() == State.NOT_IN_CACHE){
                            addRequest(new WMSRequest(img.getXIndex(), img.getYIndex(), info.getPixelPerDegree(), false, true));
                        }
                    }
                }
                Main.map.mapView.repaint();
            }
        }
    }

    private class ZoomToNativeResolution extends AbstractAction {

        public ZoomToNativeResolution() {
            super(tr("Zoom to native resolution"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Main.map.mapView.zoomTo(Main.map.mapView.getCenter(), 1 / info.getPixelPerDegree());
        }

    }

    private void cancelGrabberThreads(boolean wait) {
        requestQueueLock.lock();
        try {
            canceled = true;
            for (Grabber grabber: grabbers) {
                grabber.cancel();
            }
            queueEmpty.signalAll();
        } finally {
            requestQueueLock.unlock();
        }
        if (wait) {
            for (Thread t: grabberThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Main.warn("InterruptedException in "+getClass().getSimpleName()+" while cancelling grabber threads");
                }
            }
        }
    }

    private void startGrabberThreads() {
        int threadCount = PROP_SIMULTANEOUS_CONNECTIONS.get();
        requestQueueLock.lock();
        try {
            canceled = false;
            grabbers.clear();
            grabberThreads.clear();
            for (int i=0; i<threadCount; i++) {
                Grabber grabber = getGrabber(i == 0 && threadCount > 1);
                grabbers.add(grabber);
                Thread t = new Thread(grabber, "WMS " + getName() + " " + i);
                t.setDaemon(true);
                t.start();
                grabberThreads.add(t);
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    @Override
    public boolean isChanged() {
        requestQueueLock.lock();
        try {
            return !finishedRequests.isEmpty() || settingsChanged;
        } finally {
            requestQueueLock.unlock();
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent event) {
        if (event.getKey().equals(PROP_SIMULTANEOUS_CONNECTIONS.getKey()) && info.getUrl() != null) {
            cancelGrabberThreads(true);
            startGrabberThreads();
        } else if (
                event.getKey().equals(PROP_OVERLAP.getKey())
                || event.getKey().equals(PROP_OVERLAP_EAST.getKey())
                || event.getKey().equals(PROP_OVERLAP_NORTH.getKey())) {
            for (int i=0; i<images.length; i++) {
                for (int k=0; k<images[i].length; k++) {
                    images[i][k] = new GeorefImage(this);
                }
            }

            settingsChanged = true;
        }
    }

    protected Grabber getGrabber(boolean localOnly) {
        if (getInfo().getImageryType() == ImageryType.HTML)
            return new HTMLGrabber(Main.map.mapView, this, localOnly);
        else if (getInfo().getImageryType() == ImageryType.WMS)
            return new WMSGrabber(Main.map.mapView, this, localOnly);
        else throw new IllegalStateException("getGrabber() called for non-WMS layer type");
    }

    public ProjectionBounds getBounds(WMSRequest request) {
        ProjectionBounds result = new ProjectionBounds(
                getEastNorth(request.getXIndex(), request.getYIndex()),
                getEastNorth(request.getXIndex() + 1, request.getYIndex() + 1));

        if (WMSLayer.PROP_OVERLAP.get()) {
            double eastSize =  result.maxEast - result.minEast;
            double northSize =  result.maxNorth - result.minNorth;

            double eastCoef = WMSLayer.PROP_OVERLAP_EAST.get() / 100.0;
            double northCoef = WMSLayer.PROP_OVERLAP_NORTH.get() / 100.0;

            result = new ProjectionBounds(result.getMin(),
                    new EastNorth(result.maxEast + eastCoef * eastSize,
                            result.maxNorth + northCoef * northSize));
        }
        return result;
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        List<String> serverProjections = info.getServerProjections();
        return serverProjections.contains(proj.toCode().toUpperCase())
                || ("EPSG:3857".equals(proj.toCode()) && (serverProjections.contains("EPSG:4326") || serverProjections.contains("CRS:84")))
                || ("EPSG:4326".equals(proj.toCode()) && serverProjections.contains("CRS:84"));
    }

    @Override
    public String nameSupportedProjections() {
        StringBuilder res = new StringBuilder();
        for (String p : info.getServerProjections()) {
            if (res.length() > 0) {
                res.append(", ");
            }
            res.append(p);
        }
        return tr("Supported projections are: {0}", res);
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = ((infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0);
        Main.map.repaint(done ? 0 : 100);
        return !done;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(serializeFormatVersion);
        out.writeInt(dax);
        out.writeInt(day);
        out.writeInt(imageSize);
        out.writeDouble(info.getPixelPerDegree());
        out.writeObject(info.getName());
        out.writeObject(info.getExtendedUrl());
        out.writeObject(images);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int sfv = in.readInt();
        if (sfv != serializeFormatVersion)
            throw new InvalidClassException(tr("Unsupported WMS file version; found {0}, expected {1}", sfv, serializeFormatVersion));
        autoDownloadEnabled = false;
        dax = in.readInt();
        day = in.readInt();
        imageSize = in.readInt();
        info.setPixelPerDegree(in.readDouble());
        doSetName((String)in.readObject());
        info.setExtendedUrl((String)in.readObject());
        images = (GeorefImage[][])in.readObject();

        for (GeorefImage[] imgs : images) {
            for (GeorefImage img : imgs) {
                if (img != null) {
                    img.setLayer(WMSLayer.this);
                }
            }
        }

        settingsChanged = true;
        if (Main.isDisplayingMapView()) {
            Main.map.mapView.repaint();
        }
        if (cache != null) {
            cache.saveIndex();
            cache = null;
        }
    }

    @Override
    public void onPostLoadFromFile() {
        if (info.getUrl() != null) {
            cache = new WmsCache(info.getUrl(), imageSize);
            startGrabberThreads();
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
