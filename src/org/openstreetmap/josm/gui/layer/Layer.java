// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer encapsulates the gui component of one dataset and its representation.
 *
 * Some layers may display data directly imported from OSM server. Other only
 * display background images. Some can be edited, some not. Some are static and
 * other changes dynamically (auto-updated).
 *
 * Layers can be visible or not. Most actions the user can do applies only on
 * selected layers. The available actions depend on the selected layers too.
 *
 * All layers are managed by the MapView. They are displayed in a list to the
 * right of the screen.
 *
 * @author imi
 */
abstract public class Layer implements Destroyable, MapViewPaintable {
    static public final String VISIBLE_PROP = Layer.class.getName() + ".visible";
    static public final String NAME_PROP = Layer.class.getName() + ".name";

    /** keeps track of property change listeners */
    protected PropertyChangeSupport propertyChangeSupport;

    /**
     * Interface to notify listeners of the change of the active layer.
     * @author imi
     */
    public interface LayerChangeListener {
        void activeLayerChange(Layer oldLayer, Layer newLayer);
        void layerAdded(Layer newLayer);
        void layerRemoved(Layer oldLayer);
    }

    /**
     * The listener of the active layer changes. You may register/deregister yourself
     * while an LayerChangeListener - action is executed.
     */
    public static final Collection<LayerChangeListener> listeners = new CopyOnWriteArrayList<LayerChangeListener>();

    /**
     * The visibility state of the layer.
     *
     */
    private boolean visible = true;

    /**
     * The layer should be handled as a background layer in automatic handling
     *
     */
    private boolean background = false;

    /**
     * The name of this layer.
     *
     */
    private  String name;

    /**
     * If a file is associated with this layer, this variable should be set to it.
     */
    private File associatedFile;

    /**
     * Create the layer and fill in the necessary components.
     */
    public Layer(String name) {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        setName(name);
    }

    /**
     * Paint the dataset using the engine set.
     * @param mv The object that can translate GeoPoints to screen coordinates.
     */
    abstract public void paint(Graphics2D g, MapView mv, Bounds box);
    /**
     * Return a representative small image for this layer. The image must not
     * be larger than 64 pixel in any dimension.
     */
    abstract public Icon getIcon();

    /**
     * @return A small tooltip hint about some statistics for this layer.
     */
    abstract public String getToolTipText();

    /**
     * Merges the given layer into this layer. Throws if the layer types are
     * incompatible.
     * @param from The layer that get merged into this one. After the merge,
     *      the other layer is not usable anymore and passing to one others
     *      mergeFrom should be one of the last things to do with a layer.
     */
    abstract public void mergeFrom(Layer from);

    /**
     * @param other The other layer that is tested to be mergable with this.
     * @return Whether the other layer can be merged into this layer.
     */
    abstract public boolean isMergable(Layer other);

    abstract public void visitBoundingBox(BoundingXYVisitor v);

    abstract public Object getInfoComponent();

    abstract public Component[] getMenuEntries();

    /**
     * Called, when the layer is removed from the mapview and is going to be
     * destroyed.
     *
     * This is because the Layer constructor can not add itself safely as listener
     * to the layerlist dialog, because there may be no such dialog yet (loaded
     * via command line parameter).
     */
    public void destroy() {}

    public File getAssociatedFile() { return associatedFile; }
    public void setAssociatedFile(File file) { associatedFile = file; }

    /**
     * Replies the name of the layer
     *
     * @return the name of the layer
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the layer
     *
     *@param name the name. If null, the name is set to the empty string.
     *
     */
    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        String oldValue = this.name;
        this.name = name;
        if (!this.name.equals(oldValue)) {
            propertyChangeSupport.firePropertyChange(NAME_PROP, oldValue, this.name);
        }
    }

    /**
     * Replies true if this layer is a background layer
     *
     * @return true if this layer is a background layer
     */
    public boolean isBackgroundLayer() {
        return background;
    }

    /**
     * Sets whether this layer is a background layer
     *
     * @param background true, if this layer is a background layer
     */
    public void setBackgroundLayer(boolean background) {
        this.background = background;
    }

    /**
     * Sets the visibility of this layer. Emits property change event for
     * property {@see #VISIBLE_PROP}.
     *
     * @param visible true, if the layer is visible; false, otherwise.
     */
    public void setVisible(boolean visible) {
        boolean oldValue = this.visible;
        this.visible  = visible;
        if (oldValue != this.visible) {
            fireVisibleChanged(oldValue, this.visible);
        }
    }

    /**
     * Replies true if this layer is visible. False, otherwise.
     * @return  true if this layer is visible. False, otherwise.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Toggles the visibility state of this layer.
     */
    public void toggleVisible() {
        setVisible(!isVisible());
    }

    /**
     * Adds a {@see PropertyChangeListener}
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@see PropertyChangeListener}
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * fires a property change for the property {@see #VISIBLE_PROP}
     *
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void fireVisibleChanged(boolean oldValue, boolean newValue) {
        propertyChangeSupport.firePropertyChange(VISIBLE_PROP, oldValue, newValue);
    }

    /**
     * The action to save a layer
     *
     */
    public static class LayerSaveAction extends AbstractAction {
        private Layer layer;
        public LayerSaveAction(Layer layer) {
            putValue(SMALL_ICON, ImageProvider.get("save"));
            putValue(SHORT_DESCRIPTION, tr("Save the current data."));
            putValue(NAME, tr("Save"));
            setEnabled(true);
            this.layer = layer;
        }

        public void actionPerformed(ActionEvent e) {
            new SaveAction().doSave(layer);
        }
    }

    public static class LayerSaveAsAction extends AbstractAction {
        private Layer layer;
        public LayerSaveAsAction(Layer layer) {
            putValue(SMALL_ICON, ImageProvider.get("save_as"));
            putValue(SHORT_DESCRIPTION, tr("Save the current data to a new file."));
            putValue(NAME, tr("Save As..."));
            setEnabled(true);
            this.layer = layer;
        }

        public void actionPerformed(ActionEvent e) {
            new SaveAsAction().doSave(layer);
        }
    }

    public static class LayerGpxExportAction extends AbstractAction {
        private Layer layer;
        public LayerGpxExportAction(Layer layer) {
            putValue(SMALL_ICON, ImageProvider.get("exportgpx"));
            putValue(SHORT_DESCRIPTION, tr("Export the data to GPX file."));
            putValue(NAME, tr("Export to GPX..."));
            setEnabled(true);
            this.layer = layer;
        }

        public void actionPerformed(ActionEvent e) {
            new GpxExportAction().export(layer);
        }
    }
}
