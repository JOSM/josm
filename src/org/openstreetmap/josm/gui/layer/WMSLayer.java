// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.gui.jmapviewer.AttributionSupport;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.GeorefImage;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.WmsCache;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.io.imagery.Grabber;
import org.openstreetmap.josm.io.imagery.HTMLGrabber;
import org.openstreetmap.josm.io.imagery.WMSGrabber;
import org.openstreetmap.josm.io.imagery.WMSRequest;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and managed to the disc to reduce server load.
 */
public class WMSLayer extends ImageryLayer implements ImageObserver, PreferenceChangedListener {
    public static final BooleanProperty PROP_ALPHA_CHANNEL = new BooleanProperty("imagery.wms.alpha_channel", true);
    public static final IntegerProperty PROP_SIMULTANEOUS_CONNECTIONS = new IntegerProperty("imagery.wms.simultaneousConnections", 3);
    public static final BooleanProperty PROP_OVERLAP = new BooleanProperty("imagery.wms.overlap", false);
    public static final IntegerProperty PROP_OVERLAP_EAST = new IntegerProperty("imagery.wms.overlapEast", 14);
    public static final IntegerProperty PROP_OVERLAP_NORTH = new IntegerProperty("imagery.wms.overlapNorth", 4);

    public int messageNum = 5; //limit for messages per layer
    protected String resolution;
    protected int imageSize = 500;
    protected int dax = 10;
    protected int day = 10;
    protected int daStep = 5;
    protected int minZoom = 3;

    protected GeorefImage[][] images;
    protected final int serializeFormatVersion = 5;
    protected boolean autoDownloadEnabled = true;
    protected boolean settingsChanged;
    protected ImageryInfo info;
    protected final MapView mv;
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
    private int threadCount;
    private int workingThreadCount;
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
        mv = Main.map.mapView;
        setBackgroundLayer(true); /* set global background variable */
        initializeImages();
        if (info.getUrl() != null) {
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
        this.info = new ImageryInfo(info);
        if(this.info.getPixelPerDegree() == 0.0) {
            this.info.setPixelPerDegree(getPPD());
        }
        resolution = mv.getDist100PixelText();

        attribution.initialize(this.info);

        if(info.getUrl() != null)
            startGrabberThreads();

        Main.pref.addPreferenceChangeListener(this);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void doSetName(String name) {
        setName(name);
        info.setName(name);
    }

    public boolean hasAutoDownload(){
        return autoDownloadEnabled;
    }

    @Override
    public void destroy() {
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
            for (int i=0; i<old.length; i++) {
                for (int k=0; k<old[i].length; k++) {
                    GeorefImage o = old[i][k];
                    images[modulo(o.getXIndex(),dax)][modulo(o.getYIndex(),day)] = old[i][k];
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
            return tr("WMS layer ({0}), automatically downloading in zoom {1}", getName(), resolution);
        else
            return tr("WMS layer ({0}), downloading in zoom {1}", getName(), resolution);
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
        cache.setAreaToCache(areaToCache);
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
                new LoadWmsAction(),
                new SaveWmsAction(),
                new BookmarkWmsAction(),
                SeparatorLayerAction.INSTANCE,
                new ZoomToNativeResolution(),
                new StartStopAction(),
                new ToggleAlphaAction(),
                new ChangeResolutionAction(),
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
     * @return -1 if request is no longer needed, otherwise priority of request (lower number <=> more important request)
     */
    private int getRequestPriority(WMSRequest request) {
        if (request.getPixelPerDegree() != info.getPixelPerDegree())
            return -1;
        if (bminx > request.getXIndex()
                || bmaxx < request.getXIndex()
                || bminy > request.getYIndex()
                || bmaxy < request.getYIndex())
            return -1;

        EastNorth cursorEastNorth = mv.getEastNorth(mv.lastMEvent.getX(), mv.lastMEvent.getY());
        int mouseX = getImageXIndex(cursorEastNorth.east());
        int mouseY = getImageYIndex(cursorEastNorth.north());
        int dx = request.getXIndex() - mouseX;
        int dy = request.getYIndex() - mouseY;

        return dx * dx + dy * dy;
    }

    public WMSRequest getRequest() {
        requestQueueLock.lock();
        try {
            workingThreadCount--;
            Iterator<WMSRequest> it = requestQueue.iterator();
            while (it.hasNext()) {
                WMSRequest item = it.next();
                int priority = getRequestPriority(item);
                if (priority == -1 || finishedRequests.contains(item) || processingRequests.contains(item)) {
                    it.remove();
                } else {
                    item.setPriority(priority);
                }
            }
            Collections.sort(requestQueue);

            EastNorth cursorEastNorth = mv.getEastNorth(mv.lastMEvent.getX(), mv.lastMEvent.getY());
            int mouseX = getImageXIndex(cursorEastNorth.east());
            int mouseY = getImageYIndex(cursorEastNorth.north());
            boolean isOnMouse = requestQueue.size() > 0 && requestQueue.get(0).getXIndex() == mouseX && requestQueue.get(0).getYIndex() == mouseY;

            // If there is only one thread left then keep it in case we need to download other tile urgently
            while (!canceled &&
                    (requestQueue.isEmpty() || (!isOnMouse && threadCount - workingThreadCount == 0 && threadCount > 1))) {
                try {
                    queueEmpty.await();
                } catch (InterruptedException e) {
                    // Shouldn't happen
                }
            }

            workingThreadCount++;
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
            processingRequests.remove(request);
            if (request.getState() != null) {
                finishedRequests.add(request);
                mv.repaint();
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    public void addRequest(WMSRequest request) {
        requestQueueLock.lock();
        try {
            if (!requestQueue.contains(request) && !finishedRequests.contains(request) && !processingRequests.contains(request)) {
                requestQueue.add(request);
                queueEmpty.signalAll();
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    public boolean requestIsValid(WMSRequest request) {
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
        private static final long serialVersionUID = -7183852461015284020L;
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
                downloadAndPaintVisible(mv.getGraphics(), mv, true);
            }
        }
    }

    public static class ChangeResolutionAction extends AbstractAction implements LayerAction {
        public ChangeResolutionAction() {
            super(tr("Change resolution"));
        }

        private void changeResolution(WMSLayer layer) {
            layer.resolution = layer.mv.getDist100PixelText();
            layer.info.setPixelPerDegree(layer.getPPD());
            layer.settingsChanged = true;
            for(int x = 0; x<layer.dax; ++x) {
                for(int y = 0; y<layer.day; ++y) {
                    layer.images[x][y].changePosition(-1, -1);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent ev) {

            if (LayerListDialog.getInstance() == null)
                return;

            List<Layer> layers = LayerListDialog.getInstance().getModel().getSelectedLayers();
            for (Layer l: layers) {
                changeResolution((WMSLayer) l);
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
            // See https://josm.openstreetmap.de/ticket/2307
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
            mv.repaint();
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

    public class SaveWmsAction extends AbstractAction {
        public SaveWmsAction() {
            super(tr("Save WMS layer to file"), ImageProvider.get("save"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            File f = SaveActionBase.createAndOpenSaveFileChooser(
                    tr("Save WMS layer"), ".wms");
            try {
                if (f != null) {
                    ObjectOutputStream oos = new ObjectOutputStream(
                            new FileOutputStream(f)
                    );
                    oos.writeInt(serializeFormatVersion);
                    oos.writeInt(dax);
                    oos.writeInt(day);
                    oos.writeInt(imageSize);
                    oos.writeDouble(info.getPixelPerDegree());
                    oos.writeObject(info.getName());
                    oos.writeObject(info.getExtendedUrl());
                    oos.writeObject(images);
                    oos.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    public class LoadWmsAction extends AbstractAction {
        public LoadWmsAction() {
            super(tr("Load WMS layer from file"), ImageProvider.get("open"));
        }
        @Override
        public void actionPerformed(ActionEvent ev) {
            JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true,
                    false, tr("Load WMS layer"), "wms");
            if(fc == null) return;
            File f = fc.getSelectedFile();
            if (f == null) return;
            try
            {
                FileInputStream fis = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fis);
                int sfv = ois.readInt();
                if (sfv != serializeFormatVersion) {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("Unsupported WMS file version; found {0}, expected {1}", sfv, serializeFormatVersion),
                            tr("File Format Error"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                autoDownloadEnabled = false;
                dax = ois.readInt();
                day = ois.readInt();
                imageSize = ois.readInt();
                info.setPixelPerDegree(ois.readDouble());
                doSetName((String)ois.readObject());
                info.setExtendedUrl((String) ois.readObject());
                images = (GeorefImage[][])ois.readObject();
                ois.close();
                fis.close();
                for (GeorefImage[] imgs : images) {
                    for (GeorefImage img : imgs) {
                        if (img != null) {
                            img.setLayer(WMSLayer.this);
                        }
                    }
                }
                settingsChanged = true;
                mv.repaint();
                if (cache != null) {
                    cache.saveIndex();
                    cache = null;
                }
                if(info.getUrl() != null)
                {
                    cache = new WmsCache(info.getUrl(), imageSize);
                    startGrabberThreads();
                }
            }
            catch (Exception ex) {
                // FIXME be more specific
                ex.printStackTrace(System.out);
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Error loading file"),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
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
                mv.repaint();
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
                    // Shouldn't happen
                    e.printStackTrace();
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
                Grabber grabber = getGrabber();
                grabbers.add(grabber);
                Thread t = new Thread(grabber, "WMS " + getName() + " " + i);
                t.setDaemon(true);
                t.start();
                grabberThreads.add(t);
            }
            this.workingThreadCount = grabbers.size();
            this.threadCount = grabbers.size();
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
        if (event.getKey().equals(PROP_SIMULTANEOUS_CONNECTIONS.getKey())) {
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

    protected Grabber getGrabber(){
        if(getInfo().getImageryType() == ImageryType.HTML)
            return new HTMLGrabber(mv, this);
        else if(getInfo().getImageryType() == ImageryType.WMS)
            return new WMSGrabber(mv, this);
        else throw new IllegalStateException("getGrabber() called for non-WMS layer type");
    }

    @Override
    public boolean isProjectionSupported(Projection proj) {
        List<String> serverProjections = info.getServerProjections();
        return serverProjections.contains(proj.toCode().toUpperCase())
        || (proj instanceof Mercator && serverProjections.contains("EPSG:4326"));
    }

    @Override
    public String nameSupportedProjections() {
        String res = "";
        for(String p : info.getServerProjections()) {
            if(!res.isEmpty())
                res += ", ";
            res += p;
        }
        return tr("Supported projections are: {0}", res);
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        boolean done = ((infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0);
        Main.map.repaint(done ? 0 : 100);
        return !done;
    }

}
