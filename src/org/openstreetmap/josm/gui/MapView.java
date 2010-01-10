// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MoveAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * This is a component used in the MapFrame for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate.
 *
 * MapView hold meta-data about the data set currently displayed, as scale level,
 * center point viewed, what scrolling mode or editing mode is selected or with
 * what projection the map is viewed etc..
 *
 * MapView is able to administrate several layers.
 *
 * @author imi
 */
public class MapView extends NavigatableComponent implements PropertyChangeListener {

    /**
     * Interface to notify listeners of the change of the active layer.
     * @author imi
     */
    public interface LayerChangeListener {
        void activeLayerChange(Layer oldLayer, Layer newLayer);
        void layerAdded(Layer newLayer);
        void layerRemoved(Layer oldLayer);
    }

    public interface EditLayerChangeListener {
        void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer);
    }

    /**
     * the layer listeners
     */
    private static final CopyOnWriteArrayList<MapView.LayerChangeListener> layerChangeListeners = new CopyOnWriteArrayList<MapView.LayerChangeListener>();
    private static final CopyOnWriteArrayList<EditLayerChangeListener> editLayerChangeListeners = new CopyOnWriteArrayList<EditLayerChangeListener>();

    /**
     * Removes a layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void removeLayerChangeListener(MapView.LayerChangeListener listener) {
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
    public static void addLayerChangeListener(MapView.LayerChangeListener listener) {
        if (listener != null) {
            layerChangeListeners.addIfAbsent(listener);
        }
    }

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
    private ArrayList<Layer> layers = new ArrayList<Layer>();
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
    public MouseEvent lastMEvent;

    private LinkedList<MapViewPaintable> temporaryLayers = new LinkedList<MapViewPaintable>();

    private BufferedImage offscreenBuffer;

    public MapView() {
        addComponentListener(new ComponentAdapter(){
            @Override public void componentResized(ComponentEvent e) {
                removeComponentListener(this);

                MapSlider zoomSlider = new MapSlider(MapView.this);
                add(zoomSlider);
                zoomSlider.setBounds(3, 0, 114, 30);

                MapScaler scaler = new MapScaler(MapView.this);
                add(scaler);
                scaler.setLocation(10,30);

                if (!zoomToEditLayerBoundingBox()) {
                    new AutoScaleAction("data").actionPerformed(null);
                }

                new MapMover(MapView.this, Main.contentPane);
                JosmAction mv;
                mv = new MoveAction(MoveAction.Direction.UP);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "UP");
                    Main.contentPane.getActionMap().put("UP", mv);
                }
                mv = new MoveAction(MoveAction.Direction.DOWN);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "DOWN");
                    Main.contentPane.getActionMap().put("DOWN", mv);
                }
                mv = new MoveAction(MoveAction.Direction.LEFT);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "LEFT");
                    Main.contentPane.getActionMap().put("LEFT", mv);
                }
                mv = new MoveAction(MoveAction.Direction.RIGHT);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "RIGHT");
                    Main.contentPane.getActionMap().put("RIGHT", mv);
                }
            }
        });

        // listend to selection changes to redraw the map
        DataSet.selListeners.add(new SelectionChangedListener(){
            public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
                repaint();
            }
        });

        //store the last mouse action
        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
            public void mouseMoved(MouseEvent e) {
                lastMEvent = e;
            }
        });
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
        layers.add(layer);
    }

    /**
     * Add a layer to the current MapView. The layer will be added at topmost
     * position.
     */
    public void addLayer(Layer layer) {
        if (layer instanceof MarkerLayer && playHeadMarker == null) {
            playHeadMarker = PlayHeadMarker.create();
        }

        if (layer instanceof GpxLayer) {
            addGpxLayer((GpxLayer)layer);
        } else if (layer.isBackgroundLayer() || layers.isEmpty()) {
            layers.add(layer);
        } else {
            layers.add(0, layer);
        }
        fireLayerAdded(layer);
        if (layer instanceof OsmDataLayer || activeLayer == null) {
            // autoselect the new layer
            setActiveLayer(layer);
        }
        layer.addPropertyChangeListener(this);
        AudioPlayer.reset();
        repaint();
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
     *   <li>if there is at least one {@see OsmDataLayer} the first one
     *     becomes active</li>
     *   <li>otherwise, the top most layer of any type becomes active</li>
     * </ul>
     *
     * @return the next active data layer
     */
    protected Layer determineNextActiveLayer(Layer ignoredLayer) {
        // First look for data layer
        for (Layer layer:layers) {
            if (layer instanceof OsmDataLayer && layer != ignoredLayer)
                return layer;
        }

        // Then any layer
        for (Layer layer:layers) {
            if (layer != ignoredLayer)
                return layer;
        }

        // and then give up
        return null;

    }

    /**
     * Remove the layer from the mapview. If the layer was in the list before,
     * an LayerChange event is fired.
     */
    public void removeLayer(Layer layer) {
        if (layer == activeLayer) {
            setActiveLayer(determineNextActiveLayer(activeLayer));
        }
        if (layers.remove(layer)) {
            fireLayerRemoved(layer);
        }
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
        setEditLayer();
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
        ArrayList<Layer> ret = new ArrayList<Layer>();
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
                    public int compare(Layer l1, Layer l2) {
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

    /**
     * Draw the component.
     */
    @Override public void paint(Graphics g) {
        if (center == null)
            return; // no data loaded yet.

        // re-create offscreen-buffer if we've been resized, otherwise
        // just re-use it.
        if (null == offscreenBuffer || offscreenBuffer.getWidth() != getWidth()
                || offscreenBuffer.getHeight() != getHeight()) {
            offscreenBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }

        Graphics2D tempG = offscreenBuffer.createGraphics();
        tempG.setClip(g.getClip());
        tempG.setColor(PaintColors.BACKGROUND.get());
        tempG.fillRect(0, 0, getWidth(), getHeight());

        Bounds box = getLatLonBounds(g.getClipBounds());

        for (Layer l: getVisibleLayersInZOrder()) {
            l.paint(tempG, this, box);
        }
        for (MapViewPaintable mvp : temporaryLayers) {
            mvp.paint(tempG, this, box);
        }

        // draw world borders
        tempG.setColor(Color.WHITE);
        Bounds b = getProjection().getWorldBoundsLatLon();
        double lat = b.getMin().lat();
        double lon = b.getMin().lon();

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
        lon = max; max = b.getMin().lat();
        for(; lat >= max; lat -= 1.0)
        {
            p = getPoint(new LatLon(lat <= max ? max : lat, lon));
            path.lineTo(p.x, p.y);
        }
        lat = max; max = b.getMin().lon();
        for(; lon >= max; lon -= 1.0)
        {
            p = getPoint(new LatLon(lat, lon <= max ? max : lon));
            path.lineTo(p.x, p.y);
        }

        int w = offscreenBuffer.getWidth();
        int h = offscreenBuffer.getHeight();

        // Work around OpenJDK having problems when drawing out of bounds
        final Area border = new Area(path);
        // Make the viewport 1px larger in every direction to prevent an
        // additional 1px border when zooming in
        final Area viewport = new Area(new Rectangle(-1, -1, w + 2, h + 2));
        border.intersect(viewport);
        tempG.draw(border);

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
        return Collections.unmodifiableCollection(layers);
    }

    /**
     * @return An unmodifiable ordered list of all layers
     */
    public List<Layer> getAllLayersAsList() {
        return Collections.unmodifiableList(layers);
    }

    /**
     * Replies an unmodifiable list of layers of a certain type.
     *
     * Example:
     * <pre>
     *     List<WMSLayer> wmsLayers = getLayersOfType(WMSLayer.class);
     * </pre>
     *
     * @return an unmodifiable list of layers of a certain type.
     */
    public <T> List<T>  getLayersOfType(Class<T> ofType) {
        ArrayList<T> ret = new ArrayList<T>();
        for (Layer layer : getAllLayersAsList()) {
            if (ofType.isInstance(layer)) {
                ret.add(ofType.cast(layer));
            }
        }
        return ret;
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

    private void setEditLayer() {
        OsmDataLayer newEditLayer = editLayer;
        OsmDataLayer oldEditLayer = editLayer;

        // Find new edit layer
        if (activeLayer != editLayer) {
            if (activeLayer instanceof OsmDataLayer) {
                newEditLayer = (OsmDataLayer) activeLayer;
            } else {
                for (Layer layer:layers) {
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
     * of {@see OsmDataLayer} also sets {@see #editLayer} to <code>layer</code>.
     *
     * @param layer the layer to be activate; must be one of the layers in the list of layers
     * @exception IllegalArgumentException thrown if layer is not in the lis of layers
     */
    public void setActiveLayer(Layer layer) {
        if (layer != null && !layers.contains(layer))
            throw new IllegalArgumentException(tr("Layer ''{0}'' must be in list of layers", layer.toString()));

        if (layer == activeLayer)
            return;

        Layer old = activeLayer;
        activeLayer = layer;
        setEditLayer();
        fireActiveLayerChanged(old, layer);

        /* This only makes the buttons look disabled. Disabling the actions as well requires
         * the user to re-select the tool after i.e. moving a layer. While testing I found
         * that I switch layers and actions at the same time and it was annoying to mind the
         * order. This way it works as visual clue for new users */
        for (Enumeration<AbstractButton> e = Main.map.toolGroup.getElements() ; e.hasMoreElements() ;) {
            AbstractButton x=e.nextElement();
            x.setEnabled(((MapMode)x.getAction()).layerIsSupported(layer));
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
     * This implementation can be used for resolving ticket #1461.
     *
     * @return <code>true</code> if a zoom operation has been performed
     */
    public boolean zoomToEditLayerBoundingBox() {
        // workaround for #1461 (zoom to download bounding box instead of all data)
        // In case we already have an existing data layer ...
        OsmDataLayer layer= getEditLayer();
        if (layer == null)
            return false;
        Collection<DataSource> dataSources = layer.data.dataSources;
        // ... with bounding box[es] of data loaded from OSM or a file...
        BoundingXYVisitor bbox = new BoundingXYVisitor();
        for (DataSource ds : dataSources) {
            bbox.visit(ds.bounds);
            if (bbox.hasExtend()) {
                // ... we zoom to it's bounding box
                recalculateCenterScale(bbox);
                return true;
            }
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

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Layer.VISIBLE_PROP)) {
            repaint();
        } else if (evt.getPropertyName().equals(OsmDataLayer.REQUIRES_SAVE_TO_DISK_PROP)
                || evt.getPropertyName().equals(OsmDataLayer.REQUIRES_UPLOAD_TO_SERVER_PROP)) {
            OsmDataLayer layer = (OsmDataLayer)evt.getSource();
            if (layer == getEditLayer()) {
                refreshTitle();
            }
        }
    }

    protected void refreshTitle() {
        boolean dirty = editLayer != null && (editLayer.requiresSaveToFile() || editLayer.requiresUploadToServer());
        if (dirty) {
            JOptionPane.getFrameForComponent(Main.parent).setTitle("* " + tr("Java OpenStreetMap Editor"));
        } else {
            JOptionPane.getFrameForComponent(Main.parent).setTitle(tr("Java OpenStreetMap Editor"));
        }
    }

}
