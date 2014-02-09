// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

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
public class MapView extends NavigatableComponent implements PropertyChangeListener, PreferenceChangedListener, OsmDataLayer.LayerStateChangeListener {

    /**
     * Interface to notify listeners of a layer change.
     * @author imi
     */
    public interface LayerChangeListener {

        /**
         * Notifies this listener that the active layer has changed.
         * @param oldLayer The previous active layer
         * @param newLayer The new activer layer
         */
        void activeLayerChange(Layer oldLayer, Layer newLayer);

        /**
         * Notifies this listener that a layer has been added.
         * @param newLayer The new added layer
         */
        void layerAdded(Layer newLayer);

        /**
         * Notifies this listener that a layer has been removed.
         * @param oldLayer The old removed layer
         */
        void layerRemoved(Layer oldLayer);
    }

    public interface EditLayerChangeListener {
        void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer);
    }

    public boolean viewportFollowing = false;

    /**
     * the layer listeners
     */
    private static final CopyOnWriteArrayList<LayerChangeListener> layerChangeListeners = new CopyOnWriteArrayList<LayerChangeListener>();
    private static final CopyOnWriteArrayList<EditLayerChangeListener> editLayerChangeListeners = new CopyOnWriteArrayList<EditLayerChangeListener>();

    /**
     * Removes a layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void removeLayerChangeListener(LayerChangeListener listener) {
        layerChangeListeners.remove(listener);
    }

    public static void removeEditLayerChangeListener(EditLayerChangeListener listener) {
        editLayerChangeListeners.remove(listener);
    }

    /**
     * Adds a layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void addLayerChangeListener(LayerChangeListener listener) {
        if (listener != null) {
            layerChangeListeners.addIfAbsent(listener);
        }
    }

    /**
     * Adds an edit layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @param initialFire Fire an edit-layer-changed-event right after adding
     * the listener in case there is an edit layer present
     */
    public static void addEditLayerChangeListener(EditLayerChangeListener listener, boolean initialFire) {
        addEditLayerChangeListener(listener);
        if (initialFire) {
            if (Main.isDisplayingMapView() && Main.map.mapView.getEditLayer() != null) {
                fireEditLayerChanged(null, Main.map.mapView.getEditLayer());
            }
        }
    }

    /**
     * Adds an edit layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void addEditLayerChangeListener(EditLayerChangeListener listener) {
        if (listener != null) {
            editLayerChangeListeners.addIfAbsent(listener);
        }
    }

    protected static void fireActiveLayerChanged(Layer oldLayer, Layer newLayer) {
        for (LayerChangeListener l : layerChangeListeners) {
            l.activeLayerChange(oldLayer, newLayer);
        }
    }

    protected static void fireLayerAdded(Layer newLayer) {
        for (MapView.LayerChangeListener l : MapView.layerChangeListeners) {
            l.layerAdded(newLayer);
        }
    }

    protected static void fireLayerRemoved(Layer layer) {
        for (MapView.LayerChangeListener l : MapView.layerChangeListeners) {
            l.layerRemoved(layer);
        }
    }

    protected static void fireEditLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        for (EditLayerChangeListener l : editLayerChangeListeners) {
            l.editLayerChanged(oldLayer, newLayer);
        }
    }

    /**
     * A list of all layers currently loaded.
     */
    private final List<Layer> layers = new ArrayList<Layer>();
    /**
     * The play head marker: there is only one of these so it isn't in any specific layer
     */
    public PlayHeadMarker playHeadMarker = null;

    /**
     * The layer from the layers list that is currently active.
     */
    private Layer activeLayer;

    private OsmDataLayer editLayer;

    /**
     * The last event performed by mouse.
     */
    public MouseEvent lastMEvent = new MouseEvent(this, 0, 0, 0, 0, 0, 0, false); // In case somebody reads it before first mouse move

    private final List<MapViewPaintable> temporaryLayers = new LinkedList<MapViewPaintable>();

    private BufferedImage nonChangedLayersBuffer;
    private BufferedImage offscreenBuffer;
    // Layers that wasn't changed since last paint
    private final List<Layer> nonChangedLayers = new ArrayList<Layer>();
    private Layer changedLayer;
    private int lastViewID;
    private boolean paintPreferencesChanged = true;
    private Rectangle lastClipBounds = new Rectangle();
    private MapMover mapMover;

    /**
     * Constructs a new {@code MapView}.
     * @param contentPane The content pane used to register shortcuts in its
     * {@link InputMap} and {@link ActionMap}
     * @param viewportData the initial viewport of the map. Can be null, then
     * the viewport is derived from the layer data.
     */
    public MapView(final JPanel contentPane, final ViewportData viewportData) {
        Main.pref.addPreferenceChangeListener(this);
        final boolean unregisterTab = Shortcut.findShortcut(KeyEvent.VK_TAB, 0)!=null;

        addComponentListener(new ComponentAdapter(){
            @Override public void componentResized(ComponentEvent e) {
                removeComponentListener(this);

                MapSlider zoomSlider = new MapSlider(MapView.this);
                add(zoomSlider);
                zoomSlider.setBounds(3, 0, 114, 30);
                zoomSlider.setFocusTraversalKeysEnabled(!unregisterTab);

                MapScaler scaler = new MapScaler(MapView.this);
                add(scaler);
                scaler.setLocation(10,30);

                mapMover = new MapMover(MapView.this, contentPane);
                if (viewportData != null) {
                    zoomTo(viewportData.getCenter(), viewportData.getScale());
                } else {
                    OsmDataLayer layer = getEditLayer();
                    if (layer != null) {
                        if (!zoomToDataSetBoundingBox(layer.data)) {
                            // no bounding box defined
                            AutoScaleAction.autoScale("data");
                        }
                    } else {
                        AutoScaleAction.autoScale("layer");
                    }
                }
            }
        });

        // listend to selection changes to redraw the map
        DataSet.addSelectionListener(repaintSelectionChangedListener);

        //store the last mouse action
        this.addMouseMotionListener(new MouseMotionListener() {
            @Override public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
            @Override public void mouseMoved(MouseEvent e) {
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

        if (Shortcut.findShortcut(KeyEvent.VK_TAB, 0)!=null) {
            setFocusTraversalKeysEnabled(false);
        }
    }

    // remebered geometry of the component
    private Dimension oldSize = null;
    private Point oldLoc = null;

    /*
     * Call this method to keep map position on screen during next repaint
     */
    public void rememberLastPositionOnScreen() {
        oldSize = getSize();
        oldLoc  = getLocationOnScreen();
    }

    /**
     * Adds a GPX layer. A GPX layer is added below the lowest data layer.
     *
     * @param layer the GPX layer
     */
    protected void addGpxLayer(GpxLayer layer) {
        if (layers.isEmpty()) {
            layers.add(layer);
            return;
        }
        for (int i=layers.size()-1; i>= 0; i--) {
            if (layers.get(i) instanceof OsmDataLayer) {
                if (i == layers.size()-1) {
                    layers.add(layer);
                } else {
                    layers.add(i+1, layer);
                }
                return;
            }
        }
        layers.add(0, layer);
    }

    /**
     * Add a layer to the current MapView. The layer will be added at topmost
     * position.
     * @param layer The layer to add
     */
    public void addLayer(Layer layer) {
        if (layer instanceof MarkerLayer && playHeadMarker == null) {
            playHeadMarker = PlayHeadMarker.create();
        }

        if (layer instanceof GpxLayer) {
            addGpxLayer((GpxLayer)layer);
        } else if (layers.isEmpty()) {
            layers.add(layer);
        } else if (layer.isBackgroundLayer()) {
            int i = 0;
            for (; i < layers.size(); i++) {
                if (layers.get(i).isBackgroundLayer()) {
                    break;
                }
            }
            layers.add(i, layer);
        } else {
            layers.add(0, layer);
        }
        fireLayerAdded(layer);
        boolean isOsmDataLayer = layer instanceof OsmDataLayer;
        if (isOsmDataLayer) {
            ((OsmDataLayer)layer).addLayerStateChangeListener(this);
        }
        boolean callSetActiveLayer = isOsmDataLayer || activeLayer == null;
        if (callSetActiveLayer) {
            // autoselect the new layer
            setActiveLayer(layer); // also repaints this MapView
        }
        layer.addPropertyChangeListener(this);
        Main.addProjectionChangeListener(layer);
        AudioPlayer.reset();
        if (!callSetActiveLayer) {
            repaint();
        }
    }

    @Override
    protected DataSet getCurrentDataSet() {
        if (editLayer != null)
            return editLayer.data;
        else
            return null;
    }

    /**
     * Replies true if the active layer is drawable.
     *
     * @return true if the active layer is drawable, false otherwise
     */
    public boolean isActiveLayerDrawable() {
        return editLayer != null;
    }

    /**
     * Replies true if the active layer is visible.
     *
     * @return true if the active layer is visible, false otherwise
     */
    public boolean isActiveLayerVisible() {
        return isActiveLayerDrawable() && editLayer.isVisible();
    }

    /**
     * Determines the next active data layer according to the following
     * rules:
     * <ul>
     *   <li>if there is at least one {@link OsmDataLayer} the first one
     *     becomes active</li>
     *   <li>otherwise, the top most layer of any type becomes active</li>
     * </ul>
     *
     * @return the next active data layer
     */
    protected Layer determineNextActiveLayer(List<Layer> layersList) {
        // First look for data layer
        for (Layer layer:layersList) {
            if (layer instanceof OsmDataLayer)
                return layer;
        }

        // Then any layer
        if (!layersList.isEmpty())
            return layersList.get(0);

        // and then give up
        return null;

    }

    /**
     * Remove the layer from the mapview. If the layer was in the list before,
     * an LayerChange event is fired.
     * @param layer The layer to remove
     */
    public void removeLayer(Layer layer) {
        List<Layer> layersList = new ArrayList<Layer>(layers);

        if (!layersList.remove(layer))
            return;

        setEditLayer(layersList);

        if (layer == activeLayer) {
            setActiveLayer(determineNextActiveLayer(layersList), false);
        }

        if (layer instanceof OsmDataLayer) {
            ((OsmDataLayer)layer).removeLayerPropertyChangeListener(this);
        }

        layers.remove(layer);
        Main.removeProjectionChangeListener(layer);
        fireLayerRemoved(layer);
        layer.removePropertyChangeListener(this);
        layer.destroy();
        AudioPlayer.reset();
        repaint();
    }

    private boolean virtualNodesEnabled = false;

    public void setVirtualNodesEnabled(boolean enabled) {
        if(virtualNodesEnabled != enabled) {
            virtualNodesEnabled = enabled;
            repaint();
        }
    }
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
        int curLayerPos = layers.indexOf(layer);
        if (curLayerPos == -1)
            throw new IllegalArgumentException(tr("Layer not in list."));
        if (pos == curLayerPos)
            return; // already in place.
        layers.remove(curLayerPos);
        if (pos >= layers.size()) {
            layers.add(layer);
        } else {
            layers.add(pos, layer);
        }
        setEditLayer(layers);
        AudioPlayer.reset();
        repaint();
    }

    public int getLayerPos(Layer layer) {
        int curLayerPos = layers.indexOf(layer);
        if (curLayerPos == -1)
            throw new IllegalArgumentException(tr("Layer not in list."));
        return curLayerPos;
    }

    /**
     * Creates a list of the visible layers in Z-Order, the layer with the lowest Z-Order
     * first, layer with the highest Z-Order last.
     *
     * @return a list of the visible in Z-Order, the layer with the lowest Z-Order
     * first, layer with the highest Z-Order last.
     */
    protected List<Layer> getVisibleLayersInZOrder() {
        List<Layer> ret = new ArrayList<Layer>();
        for (Layer l: layers) {
            if (l.isVisible()) {
                ret.add(l);
            }
        }
        // sort according to position in the list of layers, with one exception:
        // an active data layer always becomes a higher Z-Order than all other
        // data layers
        //
        Collections.sort(
                ret,
                new Comparator<Layer>() {
                    @Override public int compare(Layer l1, Layer l2) {
                        if (l1 instanceof OsmDataLayer && l2 instanceof OsmDataLayer) {
                            if (l1 == getActiveLayer()) return -1;
                            if (l2 == getActiveLayer()) return 1;
                            return Integer.valueOf(layers.indexOf(l1)).compareTo(layers.indexOf(l2));
                        } else
                            return Integer.valueOf(layers.indexOf(l1)).compareTo(layers.indexOf(l2));
                    }
                }
        );
        Collections.reverse(ret);
        return ret;
    }

    private void paintLayer(Layer layer, Graphics2D g, Bounds box) {
        if (layer.getOpacity() < 1) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)layer.getOpacity()));
        }
        layer.paint(g, this, box);
        g.setPaintMode();
    }

    /**
     * Draw the component.
     */
    @Override public void paint(Graphics g) {
        if (BugReportExceptionHandler.exceptionHandlingInProgress())
            return;

        if (center == null)
            return; // no data loaded yet.

        // if the position was remembered, we need to adjust center once before repainting
        if (oldLoc != null && oldSize != null) {
            Point l1  = getLocationOnScreen();
            final EastNorth newCenter = new EastNorth(
                    center.getX()+ (l1.x-oldLoc.x - (oldSize.width-getWidth())/2.0)*getScale(),
                    center.getY()+ (oldLoc.y-l1.y + (oldSize.height-getHeight())/2.0)*getScale()
                    );
            oldLoc = null; oldSize = null;
            zoomTo(newCenter);
        }

        List<Layer> visibleLayers = getVisibleLayersInZOrder();

        int nonChangedLayersCount = 0;
        for (Layer l: visibleLayers) {
            if (l.isChanged() || l == changedLayer) {
                break;
            } else {
                nonChangedLayersCount++;
            }
        }

        boolean canUseBuffer;

        synchronized (this) {
            canUseBuffer = !paintPreferencesChanged;
            paintPreferencesChanged = false;
        }
        canUseBuffer = canUseBuffer && nonChangedLayers.size() <= nonChangedLayersCount &&
        lastViewID == getViewID() && lastClipBounds.contains(g.getClipBounds());
        if (canUseBuffer) {
            for (int i=0; i<nonChangedLayers.size(); i++) {
                if (visibleLayers.get(i) != nonChangedLayers.get(i)) {
                    canUseBuffer = false;
                    break;
                }
            }
        }

        if (null == offscreenBuffer || offscreenBuffer.getWidth() != getWidth() || offscreenBuffer.getHeight() != getHeight()) {
            offscreenBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }

        Graphics2D tempG = offscreenBuffer.createGraphics();
        tempG.setClip(g.getClip());
        Bounds box = getLatLonBounds(g.getClipBounds());

        if (!canUseBuffer || nonChangedLayersBuffer == null) {
            if (null == nonChangedLayersBuffer || nonChangedLayersBuffer.getWidth() != getWidth() || nonChangedLayersBuffer.getHeight() != getHeight()) {
                nonChangedLayersBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            }
            Graphics2D g2 = nonChangedLayersBuffer.createGraphics();
            g2.setClip(g.getClip());
            g2.setColor(PaintColors.getBackgroundColor());
            g2.fillRect(0, 0, getWidth(), getHeight());

            for (int i=0; i<nonChangedLayersCount; i++) {
                paintLayer(visibleLayers.get(i),g2, box);
            }
        } else {
            // Maybe there were more unchanged layers then last time - draw them to buffer
            if (nonChangedLayers.size() != nonChangedLayersCount) {
                Graphics2D g2 = nonChangedLayersBuffer.createGraphics();
                g2.setClip(g.getClip());
                for (int i=nonChangedLayers.size(); i<nonChangedLayersCount; i++) {
                    paintLayer(visibleLayers.get(i),g2, box);
                }
            }
        }

        nonChangedLayers.clear();
        changedLayer = null;
        for (int i=0; i<nonChangedLayersCount; i++) {
            nonChangedLayers.add(visibleLayers.get(i));
        }
        lastViewID = getViewID();
        lastClipBounds = g.getClipBounds();

        tempG.drawImage(nonChangedLayersBuffer, 0, 0, null);

        for (int i=nonChangedLayersCount; i<visibleLayers.size(); i++) {
            paintLayer(visibleLayers.get(i),tempG, box);
        }

        for (MapViewPaintable mvp : temporaryLayers) {
            mvp.paint(tempG, this, box);
        }

        // draw world borders
        tempG.setColor(Color.WHITE);
        Bounds b = getProjection().getWorldBoundsLatLon();
        double lat = b.getMinLat();
        double lon = b.getMinLon();

        Point p = getPoint(b.getMin());

        GeneralPath path = new GeneralPath();

        path.moveTo(p.x, p.y);
        double max = b.getMax().lat();
        for(; lat <= max; lat += 1.0)
        {
            p = getPoint(new LatLon(lat >= max ? max : lat, lon));
            path.lineTo(p.x, p.y);
        }
        lat = max; max = b.getMax().lon();
        for(; lon <= max; lon += 1.0)
        {
            p = getPoint(new LatLon(lat, lon >= max ? max : lon));
            path.lineTo(p.x, p.y);
        }
        lon = max; max = b.getMinLat();
        for(; lat >= max; lat -= 1.0)
        {
            p = getPoint(new LatLon(lat <= max ? max : lat, lon));
            path.lineTo(p.x, p.y);
        }
        lat = max; max = b.getMinLon();
        for(; lon >= max; lon -= 1.0)
        {
            p = getPoint(new LatLon(lat, lon <= max ? max : lon));
            path.lineTo(p.x, p.y);
        }

        int w = getWidth();
        int h = getHeight();

        // Work around OpenJDK having problems when drawing out of bounds
        final Area border = new Area(path);
        // Make the viewport 1px larger in every direction to prevent an
        // additional 1px border when zooming in
        final Area viewport = new Area(new Rectangle(-1, -1, w + 2, h + 2));
        border.intersect(viewport);
        tempG.draw(border);

        if (Main.isDisplayingMapView() && Main.map.filterDialog != null) {
            Main.map.filterDialog.drawOSDText(tempG);
        }

        if (playHeadMarker != null) {
            playHeadMarker.paint(tempG, this);
        }

        g.drawImage(offscreenBuffer, 0, 0, null);
        super.paint(g);
    }

    /**
     * Set the new dimension to the view.
     */
    public void recalculateCenterScale(BoundingXYVisitor box) {
        if (box == null) {
            box = new BoundingXYVisitor();
        }
        if (box.getBounds() == null) {
            box.visit(getProjection().getWorldBoundsLatLon());
        }
        if (!box.hasExtend()) {
            box.enlargeBoundingBox();
        }

        zoomTo(box.getBounds());
    }

    /**
     * @return An unmodifiable collection of all layers
     */
    public Collection<Layer> getAllLayers() {
        return Collections.unmodifiableCollection(new ArrayList<Layer>(layers));
    }

    /**
     * @return An unmodifiable ordered list of all layers
     */
    public List<Layer> getAllLayersAsList() {
        return Collections.unmodifiableList(new ArrayList<Layer>(layers));
    }

    /**
     * Replies an unmodifiable list of layers of a certain type.
     *
     * Example:
     * <pre>
     *     List&lt;WMSLayer&gt; wmsLayers = getLayersOfType(WMSLayer.class);
     * </pre>
     *
     * @return an unmodifiable list of layers of a certain type.
     */
    public <T extends Layer> List<T>  getLayersOfType(Class<T> ofType) {
        return new ArrayList<T>(Utils.filteredCollection(getAllLayers(), ofType));
    }

    /**
     * Replies the number of layers managed by this mav view
     *
     * @return the number of layers managed by this mav view
     */
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * Replies true if there is at least one layer in this map view
     *
     * @return true if there is at least one layer in this map view
     */
    public boolean hasLayers() {
        return getNumLayers() > 0;
    }

    private void setEditLayer(List<Layer> layersList) {
        OsmDataLayer newEditLayer = layersList.contains(editLayer)?editLayer:null;
        OsmDataLayer oldEditLayer = editLayer;

        // Find new edit layer
        if (activeLayer != editLayer || !layersList.contains(editLayer)) {
            if (activeLayer instanceof OsmDataLayer && layersList.contains(activeLayer)) {
                newEditLayer = (OsmDataLayer) activeLayer;
            } else {
                for (Layer layer:layersList) {
                    if (layer instanceof OsmDataLayer) {
                        newEditLayer = (OsmDataLayer) layer;
                        break;
                    }
                }
            }
        }

        // Set new edit layer
        if (newEditLayer != editLayer) {
            if (newEditLayer == null) {
                getCurrentDataSet().setSelected();
            }

            editLayer = newEditLayer;
            fireEditLayerChanged(oldEditLayer, newEditLayer);
            refreshTitle();
        }

    }

    /**
     * Sets the active layer to <code>layer</code>. If <code>layer</code> is an instance
     * of {@link OsmDataLayer} also sets {@link #editLayer} to <code>layer</code>.
     *
     * @param layer the layer to be activate; must be one of the layers in the list of layers
     * @exception IllegalArgumentException thrown if layer is not in the lis of layers
     */
    public void setActiveLayer(Layer layer) {
        setActiveLayer(layer, true);
    }

    private void setActiveLayer(Layer layer, boolean setEditLayer) {
        if (layer != null && !layers.contains(layer))
            throw new IllegalArgumentException(tr("Layer ''{0}'' must be in list of layers", layer.toString()));

        if (layer == activeLayer)
            return;

        Layer old = activeLayer;
        activeLayer = layer;
        if (setEditLayer) {
            setEditLayer(layers);
        }
        fireActiveLayerChanged(old, layer);

        /* This only makes the buttons look disabled. Disabling the actions as well requires
         * the user to re-select the tool after i.e. moving a layer. While testing I found
         * that I switch layers and actions at the same time and it was annoying to mind the
         * order. This way it works as visual clue for new users */
        for (final AbstractButton b: Main.map.allMapModeButtons) {
            MapMode mode = (MapMode)b.getAction();
            if (mode.layerIsSupported(layer)) {
                Main.registerActionShortcut(mode, mode.getShortcut()); //fix #6876
                GuiHelper.runInEDTAndWait(new Runnable() {
                    @Override public void run() {
                        b.setEnabled(true);
                    }
                });
            } else {
                Main.unregisterShortcut(mode.getShortcut());
                GuiHelper.runInEDTAndWait(new Runnable() {
                    @Override public void run() {
                        b.setEnabled(false);
                    }
                });
            }
        }
        AudioPlayer.reset();
        repaint();
    }

    /**
     * Replies the currently active layer
     *
     * @return the currently active layer (may be null)
     */
    public Layer getActiveLayer() {
        return activeLayer;
    }

    /**
     * Replies the current edit layer, if any
     *
     * @return the current edit layer. May be null.
     */
    public OsmDataLayer getEditLayer() {
        return editLayer;
    }

    /**
     * replies true if the list of layers managed by this map view contain layer
     *
     * @param layer the layer
     * @return true if the list of layers managed by this map view contain layer
     */
    public boolean hasLayer(Layer layer) {
        return layers.contains(layer);
    }

    /**
     * Tries to zoom to the download boundingbox[es] of the current edit layer
     * (aka {@link OsmDataLayer}). If the edit layer has multiple download bounding
     * boxes it zooms to a large virtual bounding box containing all smaller ones.
     *
     * @return <code>true</code> if a zoom operation has been performed
     */
    public boolean zoomToDataSetBoundingBox(DataSet ds) {
        // In case we already have an existing data layer ...
        OsmDataLayer layer= getEditLayer();
        if (layer == null)
            return false;
        Collection<DataSource> dataSources = ds.dataSources;
        // ... with bounding box[es] of data loaded from OSM or a file...
        BoundingXYVisitor bbox = new BoundingXYVisitor();
        for (DataSource source : dataSources) {
            bbox.visit(source.bounds);
        }
        if (bbox.hasExtend()) {
            // ... we zoom to it's bounding box
            recalculateCenterScale(bbox);
            return true;
        }
        return false;
    }

    public boolean addTemporaryLayer(MapViewPaintable mvp) {
        if (temporaryLayers.contains(mvp)) return false;
        return temporaryLayers.add(mvp);
    }

    public boolean removeTemporaryLayer(MapViewPaintable mvp) {
        return temporaryLayers.remove(mvp);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Layer.VISIBLE_PROP)) {
            repaint();
        } else if (evt.getPropertyName().equals(Layer.OPACITY_PROP)) {
            Layer l = (Layer)evt.getSource();
            if (l.isVisible()) {
                changedLayer = l;
                repaint();
            }
        } else if (evt.getPropertyName().equals(OsmDataLayer.REQUIRES_SAVE_TO_DISK_PROP)
                || evt.getPropertyName().equals(OsmDataLayer.REQUIRES_UPLOAD_TO_SERVER_PROP)) {
            OsmDataLayer layer = (OsmDataLayer)evt.getSource();
            if (layer == getEditLayer()) {
                refreshTitle();
            }
        }
    }

    protected void refreshTitle() {
        boolean dirty = editLayer != null && (editLayer.requiresSaveToFile() || (editLayer.requiresUploadToServer() && !editLayer.isUploadDiscouraged()));
        if (dirty) {
            JOptionPane.getFrameForComponent(Main.parent).setTitle("* " + tr("Java OpenStreetMap Editor"));
        } else {
            JOptionPane.getFrameForComponent(Main.parent).setTitle(tr("Java OpenStreetMap Editor"));
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        synchronized (this) {
            paintPreferencesChanged = true;
        }
    }

    private SelectionChangedListener repaintSelectionChangedListener = new SelectionChangedListener(){
        @Override public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            repaint();
        }
    };

    public void destroy() {
        Main.pref.removePreferenceChangeListener(this);
        DataSet.removeSelectionListener(repaintSelectionChangedListener);
        MultipolygonCache.getInstance().clear(this);
        if (mapMover != null) {
            mapMover.destroy();
        }
        activeLayer = null;
        changedLayer = null;
        editLayer = null;
        layers.clear();
        nonChangedLayers.clear();
        temporaryLayers.clear();
    }

    @Override
    public void uploadDiscouragedChanged(OsmDataLayer layer, boolean newValue) {
        if (layer == getEditLayer()) {
            refreshTitle();
        }
    }

    /**
     * Get a string representation of all layers suitable for the {@code source} changeset tag.
     */
    public String getLayerInformationForSourceTag() {
        final Collection<String> layerInfo = new ArrayList<String>();
        if (!getLayersOfType(GpxLayer.class).isEmpty()) {
            // no i18n for international values
            layerInfo.add("survey");
        }
        for (final GeoImageLayer i : getLayersOfType(GeoImageLayer.class)) {
            layerInfo.add(i.getName());
        }
        for (final ImageryLayer i : getLayersOfType(ImageryLayer.class)) {
            layerInfo.add(ImageryInfo.ImageryType.BING.equals(i.getInfo().getImageryType()) ? "Bing" : i.getName());
        }
        return Utils.join("; ", layerInfo);
    }
}
