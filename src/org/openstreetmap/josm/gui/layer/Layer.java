// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import java.awt.Component;
import java.awt.Graphics;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.Destroyable;

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
     */
    public boolean visible = true;

    /**
     * The layer should be handled as a background layer in automatic handling
     */
    public boolean background = false;

    /**
     * The name of this layer.
     */
    public String name;
    /**
     * If a file is associated with this layer, this variable should be set to it.
     */
    public File associatedFile;

    /**
     * Create the layer and fill in the necessary components.
     */
    public Layer(String name) {
        this.name = name;
    }

    /**
     * Paint the dataset using the engine set.
     * @param mv The object that can translate GeoPoints to screen coordinates.
     */
    abstract public void paint(Graphics g, MapView mv);
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
}
