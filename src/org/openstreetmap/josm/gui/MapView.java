// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.autofilter.AutoFilterManager;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.LayerPainter;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.MapViewEvent;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationEvent;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is a component used in the {@link MapFrame} for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate.<br><br>
 *
 * {@code MapView} holds meta-data about the data set currently displayed, as scale level,
 * center point viewed, what scrolling mode or editing mode is selected or with
 * what projection the map is viewed etc..<br><br>
 *
 * {@code MapView} is able to administrate several layers.
 *
 * @author imi
 */
public class MapView extends NavigatableComponent
implements PropertyChangeListener, PreferenceChangedListener,
LayerManager.LayerChangeListener, MainLayerManager.ActiveLayerChangeListener {

    static {
        MapPaintStyles.addMapPaintSylesUpdateListener(new MapPaintSylesUpdateListener() {
            @Override
            public void mapPaintStylesUpdated() {
                SwingUtilities.invokeLater(() ->
                    // Trigger a repaint of all data layers
                    MainApplication.getLayerManager().getLayers()
                        .stream()
                        .filter(layer -> layer instanceof OsmDataLayer)
                        .forEach(Layer::invalidate)
                );
            }

            @Override
            public void mapPaintStyleEntryUpdated(int index) {
                mapPaintStylesUpdated();
            }
        });
    }

    /**
     * An invalidation listener that simply calls repaint() for now.
     * @author Michael Zangl
     * @since 10271
     */
    private class LayerInvalidatedListener implements PaintableInvalidationListener {
        private boolean ignoreRepaint;

        private final Set<MapViewPaintable> invalidatedLayers = Collections.newSetFromMap(new IdentityHashMap<MapViewPaintable, Boolean>());

        @Override
        public void paintableInvalidated(PaintableInvalidationEvent event) {
            invalidate(event.getLayer());
        }

        /**
         * Invalidate contents and repaint map view
         * @param mapViewPaintable invalidated layer
         */
        public synchronized void invalidate(MapViewPaintable mapViewPaintable) {
            ignoreRepaint = true;
            invalidatedLayers.add(mapViewPaintable);
            repaint();
        }

        /**
         * Temporary until all {@link MapViewPaintable}s support this.
         * @param p The paintable.
         */
        public synchronized void addTo(MapViewPaintable p) {
            p.addInvalidationListener(this);
        }

        /**
         * Temporary until all {@link MapViewPaintable}s support this.
         * @param p The paintable.
         */
        public synchronized void removeFrom(MapViewPaintable p) {
            p.removeInvalidationListener(this);
            invalidatedLayers.remove(p);
        }

        /**
         * Attempts to trace repaints that did not originate from this listener. Good to find missed {@link MapView#repaint()}s in code.
         */
        protected synchronized void traceRandomRepaint() {
            if (!ignoreRepaint) {
                Logging.trace("Repaint: {0} from {1}", Thread.currentThread().getStackTrace()[3], Thread.currentThread());
            }
            ignoreRepaint = false;
        }

        /**
         * Retrieves a set of all layers that have been marked as invalid since the last call to this method.
         * @return The layers
         */
        protected synchronized Set<MapViewPaintable> collectInvalidatedLayers() {
            Set<MapViewPaintable> layers = Collections.newSetFromMap(new IdentityHashMap<MapViewPaintable, Boolean>());
            layers.addAll(invalidatedLayers);
            invalidatedLayers.clear();
            return layers;
        }
    }

    /**
     * A layer painter that issues a warning when being called.
     * @author Michael Zangl
     * @since 10474
     */
    private static class WarningLayerPainter implements LayerPainter {
        boolean warningPrinted;
        private final Layer layer;

        WarningLayerPainter(Layer layer) {
            this.layer = layer;
        }

        @Override
        public void paint(MapViewGraphics graphics) {
            if (!warningPrinted) {
                Logging.debug("A layer triggered a repaint while being added: " + layer);
                warningPrinted = true;
            }
        }

        @Override
        public void detachFromMapView(MapViewEvent event) {
            // ignored
        }
    }

    /**
     * A list of all layers currently loaded. If we support multiple map views, this list may be different for each of them.
     */
    private final MainLayerManager layerManager;

    /**
     * The play head marker: there is only one of these so it isn't in any specific layer
     */
    public transient PlayHeadMarker playHeadMarker;

    /**
     * The last event performed by mouse.
     */
    public MouseEvent lastMEvent = new MouseEvent(this, 0, 0, 0, 0, 0, 0, false); // In case somebody reads it before first mouse move

    /**
     * Temporary layers (selection rectangle, etc.) that are never cached and
     * drawn on top of regular layers.
     * Access must be synchronized.
     */
    private final transient Set<MapViewPaintable> temporaryLayers = new LinkedHashSet<>();

    private transient BufferedImage nonChangedLayersBuffer;
    private transient BufferedImage offscreenBuffer;
    // Layers that wasn't changed since last paint
    private final transient List<Layer> nonChangedLayers = new ArrayList<>();
    private int lastViewID;
    private final AtomicBoolean paintPreferencesChanged = new AtomicBoolean(true);
    private Rectangle lastClipBounds = new Rectangle();
    private transient MapMover mapMover;

    /**
     * The listener that listens to invalidations of all layers.
     */
    private final LayerInvalidatedListener invalidatedListener = new LayerInvalidatedListener();

    /**
     * This is a map of all Layers that have been added to this view.
     */
    private final HashMap<Layer, LayerPainter> registeredLayers = new HashMap<>();

    /**
     * Constructs a new {@code MapView}.
     * @param layerManager The layers to display.
     * @param viewportData the initial viewport of the map. Can be null, then
     * the viewport is derived from the layer data.
     * @since 11713
     */
    public MapView(MainLayerManager layerManager, final ViewportData viewportData) {
        this.layerManager = layerManager;
        initialViewport = viewportData;
        layerManager.addAndFireLayerChangeListener(this);
        layerManager.addActiveLayerChangeListener(this);
        Config.getPref().addPreferenceChangeListener(this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                removeComponentListener(this);
                mapMover = new MapMover(MapView.this);
            }
        });

        // listens to selection changes to redraw the map
        SelectionEventManager.getInstance().addSelectionListenerForEdt(repaintSelectionChangedListener);

        //store the last mouse action
        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMEvent = e;
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                // focus the MapView component when mouse is pressed inside it
                requestFocus();
            }
        });

        setFocusTraversalKeysEnabled(!Shortcut.findShortcut(KeyEvent.VK_TAB, 0).isPresent());

        for (JComponent c : getMapNavigationComponents(this)) {
            add(c);
        }
        if (AutoFilterManager.PROP_AUTO_FILTER_ENABLED.get()) {
            AutoFilterManager.getInstance().enableAutoFilterRule(AutoFilterManager.PROP_AUTO_FILTER_RULE.get());
        }
        setTransferHandler(new OsmTransferHandler());
    }

    /**
     * Adds the map navigation components to a
     * @param forMapView The map view to get the components for.
     * @return A list containing the correctly positioned map navigation components.
     */
    public static List<? extends JComponent> getMapNavigationComponents(MapView forMapView) {
        MapSlider zoomSlider = new MapSlider(forMapView);
        Dimension size = zoomSlider.getPreferredSize();
        zoomSlider.setSize(size);
        zoomSlider.setLocation(3, 0);
        zoomSlider.setFocusTraversalKeysEnabled(!Shortcut.findShortcut(KeyEvent.VK_TAB, 0).isPresent());

        MapScaler scaler = new MapScaler(forMapView);
        scaler.setPreferredLineLength(size.width - 10);
        scaler.setSize(scaler.getPreferredSize());
        scaler.setLocation(3, size.height);

        return Arrays.asList(zoomSlider, scaler);
    }

    // remebered geometry of the component
    private Dimension oldSize;
    private Point oldLoc;

    /**
     * Call this method to keep map position on screen during next repaint
     */
    public void rememberLastPositionOnScreen() {
        oldSize = getSize();
        oldLoc = getLocationOnScreen();
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        try {
            Layer layer = e.getAddedLayer();
            registeredLayers.put(layer, new WarningLayerPainter(layer));
            // Layers may trigger a redraw during this call if they open dialogs.
            LayerPainter painter = layer.attachToMapView(new MapViewEvent(this, false));
            if (!registeredLayers.containsKey(layer)) {
                // The layer may have removed itself during attachToMapView()
                Logging.warn("Layer was removed during attachToMapView()");
            } else {
                registeredLayers.put(layer, painter);

                if (e.isZoomRequired()) {
                    ProjectionBounds viewProjectionBounds = layer.getViewProjectionBounds();
                    if (viewProjectionBounds != null) {
                        scheduleZoomTo(new ViewportData(viewProjectionBounds));
                    }
                }

                layer.addPropertyChangeListener(this);
                ProjectionRegistry.addProjectionChangeListener(layer);
                invalidatedListener.addTo(layer);
                AudioPlayer.reset();

                repaint();
            }
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException t) {
            throw BugReport.intercept(t).put("layer", e.getAddedLayer());
        }
    }

    /**
     * Replies true if the active data layer (edit layer) is drawable.
     *
     * @return true if the active data layer (edit layer) is drawable, false otherwise
     */
    public boolean isActiveLayerDrawable() {
         return layerManager.getEditLayer() != null;
    }

    /**
     * Replies true if the active data layer is visible.
     *
     * @return true if the active data layer is visible, false otherwise
     */
    public boolean isActiveLayerVisible() {
        OsmDataLayer e = layerManager.getActiveDataLayer();
        return e != null && e.isVisible();
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        Layer layer = e.getRemovedLayer();

        LayerPainter painter = registeredLayers.remove(layer);
        if (painter == null) {
            Logging.error("The painter for layer " + layer + " was not registered.");
            return;
        }
        painter.detachFromMapView(new MapViewEvent(this, false));
        ProjectionRegistry.removeProjectionChangeListener(layer);
        layer.removePropertyChangeListener(this);
        invalidatedListener.removeFrom(layer);
        layer.destroy();
        AudioPlayer.reset();

        repaint();
    }

    private boolean virtualNodesEnabled;

    /**
     * Enables or disables drawing of the virtual nodes.
     * @param enabled if virtual nodes are enabled
     */
    public void setVirtualNodesEnabled(boolean enabled) {
        if (virtualNodesEnabled != enabled) {
            virtualNodesEnabled = enabled;
            repaint();
        }
    }

    /**
     * Checks if virtual nodes should be drawn. Default is <code>false</code>
     * @return The virtual nodes property.
     * @see Rendering#render
     */
    public boolean isVirtualNodesEnabled() {
        return virtualNodesEnabled;
    }

    /**
     * Moves the layer to the given new position. No event is fired, but repaints
     * according to the new Z-Order of the layers.
     *
     * @param layer     The layer to move
     * @param pos       The new position of the layer
     */
    public void moveLayer(Layer layer, int pos) {
        layerManager.moveLayer(layer, pos);
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        AudioPlayer.reset();
        repaint();
    }

    /**
     * Paints the given layer to the graphics object, using the current state of this map view.
     * @param layer The layer to draw.
     * @param g A graphics object. It should have the width and height of this component
     * @throws IllegalArgumentException If the layer is not part of this map view.
     * @since 11226
     */
    public void paintLayer(Layer layer, Graphics2D g) {
        try {
            LayerPainter painter = registeredLayers.get(layer);
            if (painter == null) {
                Logging.warn("Cannot paint layer, it is not registered: {0}", layer);
                return;
            }
            MapViewRectangle clipBounds = getState().getViewArea(g.getClipBounds());
            MapViewGraphics paintGraphics = new MapViewGraphics(this, g, clipBounds);

            if (layer.getOpacity() < 1) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) layer.getOpacity()));
            }
            painter.paint(paintGraphics);
            g.setPaintMode();
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException t) {
            BugReport.intercept(t).put("layer", layer).warn();
        }
    }

    /**
     * Draw the component.
     */
    @Override
    public void paint(Graphics g) {
        try {
            if (!prepareToDraw()) {
                return;
            }
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            BugReport.intercept(e).put("center", this::getCenter).warn();
            return;
        }

        try {
            drawMapContent((Graphics2D) g);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("visibleLayers", layerManager::getVisibleLayersInZOrder)
                    .put("temporaryLayers", temporaryLayers);
        }
        super.paint(g);
    }

    private void drawMapContent(Graphics2D g) {
        // In HiDPI-mode, the Graphics g will have a transform that scales
        // everything by a factor of 2.0 or so. At the same time, the value returned
        // by getWidth()/getHeight will be reduced by that factor.
        //
        // This would work as intended, if we were to draw directly on g. But
        // with a temporary buffer image, we need to move the scale transform to
        // the Graphics of the buffer image and (in the end) transfer the content
        // of the temporary buffer pixel by pixel onto g, without scaling.
        // (Otherwise, we would upscale a small buffer image and the result would be
        // blurry, with 2x2 pixel blocks.)
        AffineTransform trOrig = g.getTransform();
        double uiScaleX = g.getTransform().getScaleX();
        double uiScaleY = g.getTransform().getScaleY();
        // width/height in full-resolution screen pixels
        int width = (int) Math.round(getWidth() * uiScaleX);
        int height = (int) Math.round(getHeight() * uiScaleY);
        // This transformation corresponds to the original transformation of g,
        // except for the translation part. It will be applied to the temporary
        // buffer images.
        AffineTransform trDef = AffineTransform.getScaleInstance(uiScaleX, uiScaleY);
        // The goal is to create the temporary image at full pixel resolution,
        // so scale up the clip shape
        Shape scaledClip = trDef.createTransformedShape(g.getClip());

        List<Layer> visibleLayers = layerManager.getVisibleLayersInZOrder();

        int nonChangedLayersCount = 0;
        Set<MapViewPaintable> invalidated = invalidatedListener.collectInvalidatedLayers();
        for (Layer l: visibleLayers) {
            if (invalidated.contains(l)) {
                break;
            } else {
                nonChangedLayersCount++;
            }
        }

        boolean canUseBuffer = !paintPreferencesChanged.getAndSet(false)
                && nonChangedLayers.size() <= nonChangedLayersCount
                && lastViewID == getViewID()
                && lastClipBounds.contains(g.getClipBounds())
                && nonChangedLayers.equals(visibleLayers.subList(0, nonChangedLayers.size()));

        if (null == offscreenBuffer || offscreenBuffer.getWidth() != width || offscreenBuffer.getHeight() != height) {
            offscreenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        }

        if (!canUseBuffer || nonChangedLayersBuffer == null) {
            if (null == nonChangedLayersBuffer
                    || nonChangedLayersBuffer.getWidth() != width || nonChangedLayersBuffer.getHeight() != height) {
                nonChangedLayersBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            }
            Graphics2D g2 = nonChangedLayersBuffer.createGraphics();
            g2.setClip(scaledClip);
            g2.setTransform(trDef);
            g2.setColor(PaintColors.getBackgroundColor());
            g2.fillRect(0, 0, width, height);

            for (int i = 0; i < nonChangedLayersCount; i++) {
                paintLayer(visibleLayers.get(i), g2);
            }
        } else {
            // Maybe there were more unchanged layers then last time - draw them to buffer
            if (nonChangedLayers.size() != nonChangedLayersCount) {
                Graphics2D g2 = nonChangedLayersBuffer.createGraphics();
                g2.setClip(scaledClip);
                g2.setTransform(trDef);
                for (int i = nonChangedLayers.size(); i < nonChangedLayersCount; i++) {
                    paintLayer(visibleLayers.get(i), g2);
                }
            }
        }

        nonChangedLayers.clear();
        nonChangedLayers.addAll(visibleLayers.subList(0, nonChangedLayersCount));
        lastViewID = getViewID();
        lastClipBounds = g.getClipBounds();

        Graphics2D tempG = offscreenBuffer.createGraphics();
        tempG.setClip(scaledClip);
        tempG.setTransform(new AffineTransform());
        tempG.drawImage(nonChangedLayersBuffer, 0, 0, null);
        tempG.setTransform(trDef);

        for (int i = nonChangedLayersCount; i < visibleLayers.size(); i++) {
            paintLayer(visibleLayers.get(i), tempG);
        }

        try {
            drawTemporaryLayers(tempG, getLatLonBounds(new Rectangle(
                    (int) Math.round(g.getClipBounds().x * uiScaleX),
                    (int) Math.round(g.getClipBounds().y * uiScaleY))));
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            BugReport.intercept(e).put("temporaryLayers", temporaryLayers).warn();
        }

        // draw world borders
        try {
            drawWorldBorders(tempG);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            // getProjection() needs to be inside lambda to catch errors.
            BugReport.intercept(e).put("bounds", () -> getProjection().getWorldBoundsLatLon()).warn();
        }

        MapFrame map = MainApplication.getMap();
        if (AutoFilterManager.getInstance().getCurrentAutoFilter() != null) {
            AutoFilterManager.getInstance().drawOSDText(tempG);
        } else if (MainApplication.isDisplayingMapView() && map.filterDialog != null) {
            map.filterDialog.drawOSDText(tempG);
        }

        if (playHeadMarker != null) {
            playHeadMarker.paint(tempG, this);
        }

        try {
            g.setTransform(new AffineTransform(1, 0, 0, 1, trOrig.getTranslateX(), trOrig.getTranslateY()));
            g.drawImage(offscreenBuffer, 0, 0, null);
        } catch (ClassCastException e) {
            // See #11002 and duplicate tickets. On Linux with Java >= 8 Many users face this error here:
            //
            // java.lang.ClassCastException: sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData
            //   at sun.java2d.xr.XRPMBlitLoops.cacheToTmpSurface(XRPMBlitLoops.java:145)
            //   at sun.java2d.xr.XrSwToPMBlit.Blit(XRPMBlitLoops.java:353)
            //   at sun.java2d.pipe.DrawImage.blitSurfaceData(DrawImage.java:959)
            //   at sun.java2d.pipe.DrawImage.renderImageCopy(DrawImage.java:577)
            //   at sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:67)
            //   at sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:1014)
            //   at sun.java2d.pipe.ValidatePipe.copyImage(ValidatePipe.java:186)
            //   at sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3318)
            //   at sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3296)
            //   at org.openstreetmap.josm.gui.MapView.paint(MapView.java:834)
            //
            // It seems to be this JDK bug, but Oracle does not seem to be fixing it:
            // https://bugs.openjdk.java.net/browse/JDK-7172749
            //
            // According to bug reports it can happen for a variety of reasons such as:
            // - long period of time
            // - change of screen resolution
            // - addition/removal of a secondary monitor
            //
            // But the application seems to work fine after, so let's just log the error
            Logging.error(e);
        } finally {
            g.setTransform(trOrig);
        }
    }

    private void drawTemporaryLayers(Graphics2D tempG, Bounds box) {
        synchronized (temporaryLayers) {
            for (MapViewPaintable mvp : temporaryLayers) {
                try {
                    mvp.paint(tempG, this, box);
                } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    throw BugReport.intercept(e).put("mvp", mvp);
                }
            }
        }
    }

    private void drawWorldBorders(Graphics2D tempG) {
        tempG.setColor(Color.WHITE);
        Bounds b = getProjection().getWorldBoundsLatLon();

        int w = getWidth();
        int h = getHeight();

        // Work around OpenJDK having problems when drawing out of bounds
        final Area border = getState().getArea(b);
        // Make the viewport 1px larger in every direction to prevent an
        // additional 1px border when zooming in
        final Area viewport = new Area(new Rectangle(-1, -1, w + 2, h + 2));
        border.intersect(viewport);
        tempG.draw(border);
    }

    /**
     * Sets up the viewport to prepare for drawing the view.
     * @return <code>true</code> if the view can be drawn, <code>false</code> otherwise.
     */
    public boolean prepareToDraw() {
        updateLocationState();
        if (initialViewport != null) {
            zoomTo(initialViewport);
            initialViewport = null;
        }

        EastNorth oldCenter = getCenter();
        if (oldCenter == null)
            return false; // no data loaded yet.

        // if the position was remembered, we need to adjust center once before repainting
        if (oldLoc != null && oldSize != null) {
            Point l1 = getLocationOnScreen();
            final EastNorth newCenter = new EastNorth(
                    oldCenter.getX()+ (l1.x-oldLoc.x - (oldSize.width-getWidth())/2.0)*getScale(),
                    oldCenter.getY()+ (oldLoc.y-l1.y + (oldSize.height-getHeight())/2.0)*getScale()
                    );
            oldLoc = null; oldSize = null;
            zoomTo(newCenter);
        }

        return true;
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            /* This only makes the buttons look disabled. Disabling the actions as well requires
             * the user to re-select the tool after i.e. moving a layer. While testing I found
             * that I switch layers and actions at the same time and it was annoying to mind the
             * order. This way it works as visual clue for new users */
            // FIXME: This does not belong here.
            for (final AbstractButton b: map.allMapModeButtons) {
                MapMode mode = (MapMode) b.getAction();
                final boolean activeLayerSupported = mode.layerIsSupported(layerManager.getActiveLayer());
                if (activeLayerSupported) {
                    MainApplication.registerActionShortcut(mode, mode.getShortcut()); //fix #6876
                } else {
                    MainApplication.unregisterShortcut(mode.getShortcut());
                }
                b.setEnabled(activeLayerSupported);
            }
        }
        // invalidate repaint cache. The layer order may have changed by this, so we invalidate every layer
        getLayerManager().getLayers().forEach(invalidatedListener::invalidate);
        AudioPlayer.reset();
    }

    /**
     * Adds a new temporary layer.
     * <p>
     * A temporary layer is a layer that is painted above all normal layers. Layers are painted in the order they are added.
     *
     * @param mvp The layer to paint.
     * @return <code>true</code> if the layer was added.
     */
    public boolean addTemporaryLayer(MapViewPaintable mvp) {
        synchronized (temporaryLayers) {
            boolean added = temporaryLayers.add(mvp);
            if (added) {
                invalidatedListener.addTo(mvp);
            }
            repaint();
            return added;
        }
    }

    /**
     * Removes a layer previously added as temporary layer.
     * @param mvp The layer to remove.
     * @return <code>true</code> if that layer was removed.
     */
    public boolean removeTemporaryLayer(MapViewPaintable mvp) {
        synchronized (temporaryLayers) {
            boolean removed = temporaryLayers.remove(mvp);
            if (removed) {
                invalidatedListener.removeFrom(mvp);
            }
            repaint();
            return removed;
        }
    }

    /**
     * Gets a list of temporary layers.
     * @return The layers in the order they are added.
     */
    public List<MapViewPaintable> getTemporaryLayers() {
        synchronized (temporaryLayers) {
            return Collections.unmodifiableList(new ArrayList<>(temporaryLayers));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Layer.VISIBLE_PROP)) {
            repaint();
        } else if (evt.getPropertyName().equals(Layer.OPACITY_PROP) ||
                evt.getPropertyName().equals(Layer.FILTER_STATE_PROP)) {
            Layer l = (Layer) evt.getSource();
            if (l.isVisible()) {
                invalidatedListener.invalidate(l);
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        paintPreferencesChanged.set(true);
    }

    private final transient DataSelectionListener repaintSelectionChangedListener = event -> repaint();

    /**
     * Destroy this map view panel. Should be called once when it is not needed any more.
     */
    public void destroy() {
        layerManager.removeAndFireLayerChangeListener(this);
        layerManager.removeActiveLayerChangeListener(this);
        Config.getPref().removePreferenceChangeListener(this);
        SelectionEventManager.getInstance().removeSelectionListener(repaintSelectionChangedListener);
        MultipolygonCache.getInstance().clear();
        if (mapMover != null) {
            mapMover.destroy();
        }
        nonChangedLayers.clear();
        synchronized (temporaryLayers) {
            temporaryLayers.clear();
        }
        nonChangedLayersBuffer = null;
        offscreenBuffer = null;
        setTransferHandler(null);
        GuiHelper.destroyComponents(this, false);
    }

    /**
     * Get a string representation of all layers suitable for the {@code source} changeset tag.
     * @return A String of sources separated by ';'
     */
    public String getLayerInformationForSourceTag() {
        final Set<String> layerInfo = new TreeSet<>();
        if (!layerManager.getLayersOfType(GpxLayer.class).isEmpty()) {
            // no i18n for international values
            layerInfo.add("survey");
        }
        for (final GeoImageLayer i : layerManager.getLayersOfType(GeoImageLayer.class)) {
            if (i.isVisible()) {
                layerInfo.add(i.getName());
            }
        }
        for (final ImageryLayer i : layerManager.getLayersOfType(ImageryLayer.class)) {
            if (i.isVisible()) {
                layerInfo.add(i.getInfo().getSourceName());
            }
        }
        return Utils.join("; ", layerInfo);
    }

    /**
     * This is a listener that gets informed whenever repaint is called for this MapView.
     * <p>
     * This is the only safe method to find changes to the map view, since many components call MapView.repaint() directly.
     * @author Michael Zangl
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface RepaintListener {
        /**
         * Called when any repaint method is called (using default arguments if required).
         * @param tm see {@link JComponent#repaint(long, int, int, int, int)}
         * @param x see {@link JComponent#repaint(long, int, int, int, int)}
         * @param y see {@link JComponent#repaint(long, int, int, int, int)}
         * @param width see {@link JComponent#repaint(long, int, int, int, int)}
         * @param height see {@link JComponent#repaint(long, int, int, int, int)}
         */
        void repaint(long tm, int x, int y, int width, int height);
    }

    private final transient CopyOnWriteArrayList<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a listener that gets informed whenever repaint() is called for this class.
     * @param l The listener.
     */
    public void addRepaintListener(RepaintListener l) {
        repaintListeners.add(l);
    }

    /**
     * Removes a registered repaint listener.
     * @param l The listener.
     */
    public void removeRepaintListener(RepaintListener l) {
        repaintListeners.remove(l);
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        // This is the main repaint method, all other methods are convenience methods and simply call this method.
        // This is just an observation, not a must, but seems to be true for all implementations I found so far.
        if (repaintListeners != null) {
            // Might get called early in super constructor
            for (RepaintListener l : repaintListeners) {
                l.repaint(tm, x, y, width, height);
            }
        }
        super.repaint(tm, x, y, width, height);
    }

    @Override
    public void repaint() {
        if (Logging.isTraceEnabled()) {
            invalidatedListener.traceRandomRepaint();
        }
        super.repaint();
    }

    /**
     * Returns the layer manager.
     * @return the layer manager
     * @since 10282
     */
    public final MainLayerManager getLayerManager() {
        return layerManager;
    }

    /**
     * Schedule a zoom to the given position on the next redraw.
     * Temporary, may be removed without warning.
     * @param viewportData the viewport to zoom to
     * @since 10394
     */
    public void scheduleZoomTo(ViewportData viewportData) {
        initialViewport = viewportData;
    }

    /**
     * Returns the internal {@link MapMover}.
     * @return the internal {@code MapMover}
     * @since 13126
     */
    public final MapMover getMapMover() {
        return mapMover;
    }
}
