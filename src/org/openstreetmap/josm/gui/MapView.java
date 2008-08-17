// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.MoveAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.ModifiedChangedListener;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;

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
public class MapView extends NavigatableComponent {

	/**
	 * Interface to notify listeners of the change of the active layer.
	 * @author imi
	 * @deprecated Use Layer.LayerChangeListener instead
	 */
	@Deprecated public interface LayerChangeListener {
		void activeLayerChange(Layer oldLayer, Layer newLayer);
		void layerAdded(Layer newLayer);
		void layerRemoved(Layer oldLayer);
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
	 * Direct link to the edit layer (if any) in the layers list.
	 */
	public OsmDataLayer editLayer;
	/**
	 * The layer from the layers list that is currently active.
	 */
	private Layer activeLayer;
	
	private LinkedList<MapViewPaintable> temporaryLayers = new LinkedList<MapViewPaintable>();
	
	private BufferedImage offscreenBuffer;
	
	/**
	 * The listener of the active layer changes.
	 * @deprecated Use Layer.listener instead.
	 */
	@Deprecated private Collection<LayerChangeListener> listeners = new LinkedList<LayerChangeListener>();

	public MapView() {
		addComponentListener(new ComponentAdapter(){
			@Override public void componentResized(ComponentEvent e) {
				removeComponentListener(this);

				new AutoScaleAction("data").actionPerformed(null);

				new MapMover(MapView.this, Main.contentPane);
				Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK), "UP");
				Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK), "DOWN");
				Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK), "LEFT");
				Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK), "RIGHT");

				Main.contentPane.getActionMap().put("UP", new MoveAction(MoveAction.Direction.UP));
				Main.contentPane.getActionMap().put("DOWN", new MoveAction(MoveAction.Direction.DOWN));
				Main.contentPane.getActionMap().put("LEFT", new MoveAction(MoveAction.Direction.LEFT));
				Main.contentPane.getActionMap().put("RIGHT", new MoveAction(MoveAction.Direction.RIGHT));
				

				MapSlider zoomSlider = new MapSlider(MapView.this);
				add(zoomSlider);
				zoomSlider.setBounds(3, 0, 114, 30);

				MapScaler scaler = new MapScaler(MapView.this, Main.proj);
				add(scaler);
				scaler.setLocation(10,30);
			}
		});

		// listend to selection changes to redraw the map
		DataSet.selListeners.add(new SelectionChangedListener(){
			public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
				repaint();
			}
		});
	}

	/**
	 * Add a layer to the current MapView. The layer will be added at topmost
	 * position.
	 */
	public void addLayer(Layer layer) {
		if (layer instanceof OsmDataLayer) {
			editLayer = (OsmDataLayer)layer;
			Main.ds = editLayer.data;
			editLayer.listenerModified.add(new ModifiedChangedListener(){
				public void modifiedChanged(boolean value, OsmDataLayer source) {
					JOptionPane.getFrameForComponent(Main.parent).setTitle((value?"*":"")+tr("Java OpenStreetMap - Editor"));
				}
			});
		}
		if (layer instanceof MarkerLayer && playHeadMarker == null)
			playHeadMarker = PlayHeadMarker.create();
		
		layers.add(layers.size(), layer);

		// TODO: Deprecated
		for (LayerChangeListener l : listeners)
			l.layerAdded(layer);
		for (Layer.LayerChangeListener l : Layer.listeners)
			l.layerAdded(layer);
		// autoselect the new layer
		Layer old = activeLayer;
		setActiveLayer(layer);
		for (Layer.LayerChangeListener l : Layer.listeners)
			l.activeLayerChange(old, layer);
		repaint();
	}

	/**
	 * Remove the layer from the mapview. If the layer was in the list before,
	 * an LayerChange event is fired.
	 */
	public void removeLayer(Layer layer) {
		if (layers.remove(layer)) {
			// TODO: Deprecated
			for (LayerChangeListener l : listeners)
				l.layerRemoved(layer);
			for (Layer.LayerChangeListener l : Layer.listeners)
				l.layerRemoved(layer);
		}
		if (layer == editLayer) {
			editLayer = null;
			Main.ds.setSelected();
		}
		layer.destroy();
	}

	private Boolean virtualnodes = false;
	public void enableVirtualNodes(Boolean state)
	{
		if(virtualnodes != state)
		{
			virtualnodes = state;
			repaint();
		}
	}
	public Boolean useVirtualNodes()
	{
		return virtualnodes;
	}

	/**
	 * Moves the layer to the given new position. No event is fired.
	 * @param layer		The layer to move
	 * @param pos		The new position of the layer
	 */
	public void moveLayer(Layer layer, int pos) {
		int curLayerPos = layers.indexOf(layer);
		if (curLayerPos == -1)
			throw new IllegalArgumentException(tr("layer not in list."));
		if (pos == curLayerPos)
			return; // already in place.
		layers.remove(curLayerPos);
		if (pos >= layers.size())
			layers.add(layer);
		else
			layers.add(pos, layer);
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
		        || offscreenBuffer.getHeight() != getHeight())
			offscreenBuffer = new BufferedImage(getWidth(), getHeight(),
			        BufferedImage.TYPE_INT_ARGB);

		Graphics2D tempG = offscreenBuffer.createGraphics();
		tempG.setColor(Main.pref.getColor("background", Color.BLACK));
		tempG.fillRect(0, 0, getWidth(), getHeight());

		for (int i = layers.size()-1; i >= 0; --i) {
			Layer l = layers.get(i);
			if (l.visible && l != getActiveLayer())
				l.paint(tempG, this);
		}
		
		if (getActiveLayer() != null && getActiveLayer().visible)
			getActiveLayer().paint(tempG, this);

		for (MapViewPaintable mvp : temporaryLayers) {
			mvp.paint(tempG, this);
		}
		
		// draw world borders
		tempG.setColor(Color.WHITE);
		Bounds b = new Bounds();
		Point min = getPoint(getProjection().latlon2eastNorth(b.min));
		Point max = getPoint(getProjection().latlon2eastNorth(b.max));
		int x1 = Math.min(min.x, max.x);
		int y1 = Math.min(min.y, max.y);
		int x2 = Math.max(min.x, max.x);
		int y2 = Math.max(min.y, max.y);
		if (x1 > 0 || y1 > 0 || x2 < getWidth() || y2 < getHeight())
			tempG.drawRect(x1, y1, x2-x1+1, y2-y1+1);
		
		if (playHeadMarker != null)
			playHeadMarker.paint(tempG, this);

		g.drawImage(offscreenBuffer, 0, 0, null);
		super.paint(g);
	}

	/**
	 * Set the new dimension to the projection class. Also adjust the components
	 * scale, if in autoScale mode.
	 */
	public void recalculateCenterScale(BoundingXYVisitor box) {
		// -20 to leave some border
		int w = getWidth()-20;
		if (w < 20)
			w = 20;
		int h = getHeight()-20;
		if (h < 20)
			h = 20;

		EastNorth oldCenter = center;
		double oldScale = this.scale;

		if (box == null || box.min == null || box.max == null || box.min.equals(box.max)) {
			// no bounds means whole world
			center = getProjection().latlon2eastNorth(new LatLon(0,0));
			EastNorth world = getProjection().latlon2eastNorth(new LatLon(Projection.MAX_LAT,Projection.MAX_LON));
			double scaleX = world.east()*2/w;
			double scaleY = world.north()*2/h;
			scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
		} else {
			center = new EastNorth(box.min.east()/2+box.max.east()/2, box.min.north()/2+box.max.north()/2);
			double scaleX = (box.max.east()-box.min.east())/w;
			double scaleY = (box.max.north()-box.min.north())/h;
			scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
		}

		if (!center.equals(oldCenter))
			firePropertyChange("center", oldCenter, center);
		if (oldScale != scale)
			firePropertyChange("scale", oldScale, scale);
		repaint();
	}

	/**
	 * Add a listener for changes of active layer.
	 * @param listener The listener that get added.
	 * @deprecated Use Layer.listener.add instead.
	 */
	@Deprecated public void addLayerChangeListener(LayerChangeListener listener) {
		if (listener != null)
			listeners.add(listener);
	}

	/**
	 * Remove the listener.
	 * @param listener The listener that get removed from the list.
	 * @deprecated Use Layer.listener.remove instead
	 */
	@Deprecated public void removeLayerChangeListener(LayerChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return An unmodificable list of all layers
	 */
	public Collection<Layer> getAllLayers() {
		return Collections.unmodifiableCollection(layers);
	}

	/**
	 * Set the active selection to the given value and raise an layerchange event.
	 */
	public void setActiveLayer(Layer layer) {
		if (!layers.contains(layer))
			throw new IllegalArgumentException("Layer must be in layerlist");
		if (layer instanceof OsmDataLayer) {
			editLayer = (OsmDataLayer)layer;
			Main.ds = editLayer.data;
			DataSet.fireSelectionChanged(Main.ds.getSelected());
		}
		Layer old = activeLayer;
		activeLayer = layer;
		if (old != layer) {
			// TODO: Deprecated
			for (LayerChangeListener l : listeners)
				l.activeLayerChange(old, layer);
			for (Layer.LayerChangeListener l : Layer.listeners)
				l.activeLayerChange(old, layer);
		}
		repaint();
	}

	/**
	 * @return The current active layer
	 */
	public Layer getActiveLayer() {
		return activeLayer;
	}

	/**
	 * In addition to the base class funcitonality, this keep trak of the autoscale
	 * feature.
	 */
	@Override public void zoomTo(EastNorth newCenter, double scale) {
		EastNorth oldCenter = center;
		double oldScale = this.scale;
		super.zoomTo(newCenter, scale);
		if ((oldCenter == null && center != null) || !oldCenter.equals(center))
			firePropertyChange("center", oldCenter, center);
		if (oldScale != scale)
			firePropertyChange("scale", oldScale, scale);
	}
	
	public boolean addTemporaryLayer(MapViewPaintable mvp) {
		if (temporaryLayers.contains(mvp)) return false;
		return temporaryLayers.add(mvp);
	}
	
	public boolean removeTemporaryLayer(MapViewPaintable mvp) {
		return temporaryLayers.remove(mvp);
	}
}
