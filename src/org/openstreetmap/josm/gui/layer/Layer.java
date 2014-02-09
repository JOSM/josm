// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
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
abstract public class Layer implements Destroyable, MapViewPaintable, ProjectionChangeListener {

    public interface LayerAction {
        boolean supportLayers(List<Layer> layers);
        Component createMenuComponent();
    }

    public interface MultiLayerAction {
        Action getMultiLayerAction(List<Layer> layers);
    }


    /**
     * Special class that can be returned by getMenuEntries when JSeparator needs to be created
     *
     */
    public static class SeparatorLayerAction extends AbstractAction implements LayerAction {
        public static final SeparatorLayerAction INSTANCE = new SeparatorLayerAction();
        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Component createMenuComponent() {
            return new JSeparator();
        }
        @Override
        public boolean supportLayers(List<Layer> layers) {
            return false;
        }
    }

    static public final String VISIBLE_PROP = Layer.class.getName() + ".visible";
    static public final String OPACITY_PROP = Layer.class.getName() + ".opacity";
    static public final String NAME_PROP = Layer.class.getName() + ".name";

    static public final int ICON_SIZE = 16;

    /** keeps track of property change listeners */
    protected PropertyChangeSupport propertyChangeSupport;

    /**
     * The visibility state of the layer.
     *
     */
    private boolean visible = true;

    /**
     * The opacity of the layer.
     *
     */
    private double opacity = 1;

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
     * Initialization code, that depends on Main.map.mapView.
     *
     * It is always called in the event dispatching thread.
     * Note that Main.map is null as long as no layer has been added, so do
     * not execute code in the constructor, that assumes Main.map.mapView is
     * not null. Instead override this method.
     */
    public void hookUpMapView() {
    }

    /**
     * Paint the dataset using the engine set.
     * @param mv The object that can translate GeoPoints to screen coordinates.
     */
    @Override
    abstract public void paint(Graphics2D g, MapView mv, Bounds box);
    /**
     * Return a representative small image for this layer. The image must not
     * be larger than 64 pixel in any dimension.
     */
    abstract public Icon getIcon();

    /**
     * Return a Color for this layer. Return null when no color specified.
     * @param ignoreCustom Custom color should return null, as no default color
     *      is used. When this is true, then even for custom coloring the base
     *      color is returned - mainly for layer internal use.
     */
    public Color getColor(boolean ignoreCustom) {
        return null;
    }

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

    /**
     * Determines if info dialog can be resized (false by default).
     * @return {@code true} if the info dialog can be resized, {@code false} otherwise
     * @since 6708
     */
    public boolean isInfoResizable() {
        return false;
    }

    /**
     * Returns list of actions. Action can implement LayerAction interface when it needs to be represented by other
     * menu component than JMenuItem or when it supports multiple layers. Actions that support multiple layers should also
     * have correct equals implementation.
     *
     * Use SeparatorLayerAction.INSTANCE instead of new JSeparator
     *
     */
    abstract public Action[] getMenuEntries();

    /**
     * Called, when the layer is removed from the mapview and is going to be
     * destroyed.
     *
     * This is because the Layer constructor can not add itself safely as listener
     * to the layerlist dialog, because there may be no such dialog yet (loaded
     * via command line parameter).
     */
    @Override
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
     * property {@link #VISIBLE_PROP}.
     *
     * @param visible true, if the layer is visible; false, otherwise.
     */
    public void setVisible(boolean visible) {
        boolean oldValue = isVisible();
        this.visible  = visible;
        if (visible && opacity == 0) {
            setOpacity(1);
        } else if (oldValue != isVisible()) {
            fireVisibleChanged(oldValue, isVisible());
        }
    }

    /**
     * Replies true if this layer is visible. False, otherwise.
     * @return  true if this layer is visible. False, otherwise.
     */
    public boolean isVisible() {
        return visible && opacity != 0;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        if (!(opacity >= 0 && opacity <= 1))
            throw new IllegalArgumentException("Opacity value must be between 0 and 1");
        double oldOpacity = getOpacity();
        boolean oldVisible = isVisible();
        this.opacity = opacity;
        if (oldOpacity != getOpacity()) {
            fireOpacityChanged(oldOpacity, getOpacity());
        }
        if (oldVisible != isVisible()) {
            fireVisibleChanged(oldVisible, isVisible());
        }
    }

    /**
     * Toggles the visibility state of this layer.
     */
    public void toggleVisible() {
        setVisible(!isVisible());
    }

    /**
     * Adds a {@link PropertyChangeListener}
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener}
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * fires a property change for the property {@link #VISIBLE_PROP}
     *
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void fireVisibleChanged(boolean oldValue, boolean newValue) {
        propertyChangeSupport.firePropertyChange(VISIBLE_PROP, oldValue, newValue);
    }

    /**
     * fires a property change for the property {@link #OPACITY_PROP}
     *
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void fireOpacityChanged(double oldValue, double newValue) {
        propertyChangeSupport.firePropertyChange(OPACITY_PROP, oldValue, newValue);
    }

    /**
     * Check changed status of layer
     *
     * @return True if layer was changed since last paint
     */
    public boolean isChanged() {
        return true;
    }

    /**
     * allows to check whether a projection is supported or not
     *
     * @return True if projection is supported for this layer
     */
    public boolean isProjectionSupported(Projection proj) {
        return true;
    }

    /**
     * Specify user information about projections
     *
     * @return User readable text telling about supported projections
     */
    public String nameSupportedProjections() {
        return tr("All projections are supported");
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

        @Override
        public void actionPerformed(ActionEvent e) {
            SaveAction.getInstance().doSave(layer);
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

        @Override
        public void actionPerformed(ActionEvent e) {
            SaveAsAction.getInstance().doSave(layer);
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

        @Override
        public void actionPerformed(ActionEvent e) {
            new GpxExportAction().export(layer);
        }
    }

    /* --------------------------------------------------------------------------------- */
    /* interface ProjectionChangeListener                                                */
    /* --------------------------------------------------------------------------------- */
    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        if(!isProjectionSupported(newValue)) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The layer {0} does not support the new projection {1}.\n{2}\n"
                            + "Change the projection again or remove the layer.",
                            getName(), newValue.toCode(), nameSupportedProjections()),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Initializes the layer after a successful load of data from a file
     * @since 5459
     */
    public void onPostLoadFromFile() {
        // To be overriden if needed
    }

    /**
     * Replies the savable state of this layer (i.e if it can be saved through a "File-&gt;Save" dialog).
     * @return true if this layer can be saved to a file
     * @since 5459
     */
    public boolean isSavable() {
        return false;
    }

    /**
     * Checks whether it is ok to launch a save (whether we have data, there is no conflict etc.)
     * @return <code>true</code>, if it is safe to save.
     * @since 5459
     */
    public boolean checkSaveConditions() {
        return true;
    }

    /**
     * Creates a new "Save" dialog for this layer and makes it visible.<br>
     * When the user has chosen a file, checks the file extension, and confirms overwrite if needed.
     * @return The output {@code File}
     * @since 5459
     * @see SaveActionBase#createAndOpenSaveFileChooser
     */
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save Layer"), "lay");
    }
}
