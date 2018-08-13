// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.data.preferences.AbstractProperty.ValueChangeListener;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProcessor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

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
public abstract class Layer extends AbstractMapViewPaintable implements Destroyable, ProjectionChangeListener {

    /**
     * Action related to a single layer.
     */
    public interface LayerAction {

        /**
         * Determines if this action supports a given list of layers.
         * @param layers list of layers
         * @return {@code true} if this action supports the given list of layers, {@code false} otherwise
         */
        boolean supportLayers(List<Layer> layers);

        /**
         * Creates and return the menu component.
         * @return the menu component
         */
        Component createMenuComponent();
    }

    /**
     * Action related to several layers.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface MultiLayerAction {

        /**
         * Returns the action for a given list of layers.
         * @param layers list of layers
         * @return the action for the given list of layers
         */
        Action getMultiLayerAction(List<Layer> layers);
    }

    /**
     * Special class that can be returned by getMenuEntries when JSeparator needs to be created
     */
    public static class SeparatorLayerAction extends AbstractAction implements LayerAction {
        /** Unique instance */
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

    /**
     * The visibility property for this layer. May be <code>true</code> (visible) or <code>false</code> (hidden).
     */
    public static final String VISIBLE_PROP = Layer.class.getName() + ".visible";
    /**
     * The opacity of this layer. A number between 0 and 1
     */
    public static final String OPACITY_PROP = Layer.class.getName() + ".opacity";
    /**
     * The name property of the layer.
     * You can listen to name changes by listening to changes to this property.
     */
    public static final String NAME_PROP = Layer.class.getName() + ".name";
    /**
     * Property that defines the filter state.
     * This is currently not used.
     */
    public static final String FILTER_STATE_PROP = Layer.class.getName() + ".filterstate";

    /**
     * keeps track of property change listeners
     */
    protected PropertyChangeSupport propertyChangeSupport;

    /**
     * The visibility state of the layer.
     */
    private boolean visible = true;

    /**
     * The opacity of the layer.
     */
    private double opacity = 1;

    /**
     * The layer should be handled as a background layer in automatic handling
     */
    private boolean background;

    /**
     * The name of this layer.
     */
    private String name;

    /**
     * This is set if user renamed this layer.
     */
    private boolean renamed;

    /**
     * If a file is associated with this layer, this variable should be set to it.
     */
    private File associatedFile;

    private final ValueChangeListener<Object> invalidateListener = change -> invalidate();
    private boolean isDestroyed;

    /**
     * Create the layer and fill in the necessary components.
     * @param name Layer name
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
     * not null.
     *
     * If you need to execute code when this layer is added to the map view, use
     * {@link #attachToMapView(org.openstreetmap.josm.gui.layer.MapViewPaintable.MapViewEvent)}
     */
    public void hookUpMapView() {
    }

    /**
     * Return a representative small image for this layer. The image must not
     * be larger than 64 pixel in any dimension.
     * @return layer icon
     */
    public abstract Icon getIcon();

    /**
     * Gets the color property to use for this layer.
     * @return The color property.
     * @since 10824
     */
    public AbstractProperty<Color> getColorProperty() {
        NamedColorProperty base = getBaseColorProperty();
        if (base != null) {
            return base.getChildColor(NamedColorProperty.COLOR_CATEGORY_LAYER, getName(), base.getName());
        } else {
            return null;
        }
    }

    /**
     * Gets the color property that stores the default color for this layer.
     * @return The property or <code>null</code> if this layer is not colored.
     * @since 10824
     */
    protected NamedColorProperty getBaseColorProperty() {
        return null;
    }

    private void addColorPropertyListener() {
        AbstractProperty<Color> colorProperty = getColorProperty();
        if (colorProperty != null) {
            colorProperty.addListener(invalidateListener);
        }
    }

    private void removeColorPropertyListener() {
        AbstractProperty<Color> colorProperty = getColorProperty();
        if (colorProperty != null) {
            colorProperty.removeListener(invalidateListener);
        }
    }

    /**
     * @return A small tooltip hint about some statistics for this layer.
     */
    public abstract String getToolTipText();

    /**
     * Merges the given layer into this layer. Throws if the layer types are
     * incompatible.
     * @param from The layer that get merged into this one. After the merge,
     *      the other layer is not usable anymore and passing to one others
     *      mergeFrom should be one of the last things to do with a layer.
     */
    public abstract void mergeFrom(Layer from);

    /**
     * @param other The other layer that is tested to be mergable with this.
     * @return Whether the other layer can be merged into this layer.
     */
    public abstract boolean isMergable(Layer other);

    /**
     * Visits the content bounds of this layer. The behavior of this method depends on the layer,
     * but each implementation should attempt to cover the relevant content of the layer in this method.
     * @param v The visitor that gets notified about the contents of this layer.
     */
    public abstract void visitBoundingBox(BoundingXYVisitor v);

    /**
     * Gets the layer information to display to the user.
     * This is used if the user requests information about this layer.
     * It should display a description of the layer content.
     * @return Either a String or a {@link Component} describing the layer.
     */
    public abstract Object getInfoComponent();

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
     * Use {@link SeparatorLayerAction#INSTANCE} instead of new JSeparator
     * @return menu actions for this layer
     */
    public abstract Action[] getMenuEntries();

    /**
     * Called, when the layer is removed from the mapview and is going to be destroyed.
     *
     * This is because the Layer constructor can not add itself safely as listener
     * to the layerlist dialog, because there may be no such dialog yet (loaded
     * via command line parameter).
     */
    @Override
    public synchronized void destroy() {
        if (isDestroyed) {
            throw new IllegalStateException("The layer has already been destroyed: " + this);
        }
        isDestroyed = true;
        // Override in subclasses if needed
        removeColorPropertyListener();
    }

    /**
     * Gets the associated file for this layer.
     * @return The file or <code>null</code> if it is unset.
     * @see #setAssociatedFile(File)
     */
    public File getAssociatedFile() {
        return associatedFile;
    }

    /**
     * Sets the associated file for this layer.
     *
     * The associated file might be the one that the user opened.
     * @param file The file, may be <code>null</code>
     */
    public void setAssociatedFile(File file) {
        associatedFile = file;
    }

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
     * @param name the name. If null, the name is set to the empty string.
     */
    public void setName(String name) {
        if (this.name != null) {
            removeColorPropertyListener();
        }
        String oldValue = this.name;
        this.name = Optional.ofNullable(name).orElse("");
        if (!this.name.equals(oldValue)) {
            propertyChangeSupport.firePropertyChange(NAME_PROP, oldValue, this.name);
        }

        // re-add listener
        addColorPropertyListener();
        invalidate();
    }

    /**
     * Rename layer and set renamed flag to mark it as renamed (has user given name).
     *
     * @param name the name. If null, the name is set to the empty string.
     */
    public final void rename(String name) {
        renamed = true;
        setName(name);
    }

    /**
     * Replies true if this layer was renamed by user
     *
     * @return true if this layer was renamed by user
     */
    public boolean isRenamed() {
        return renamed;
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
        this.visible = visible;
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

    /**
     * Gets the opacity of the layer, in range 0...1
     * @return The opacity
     */
    public double getOpacity() {
        return opacity;
    }

    /**
     * Sets the opacity of the layer, in range 0...1
     * @param opacity The opacity
     * @throws IllegalArgumentException if the opacity is out of range
     */
    public void setOpacity(double opacity) {
        if (!(opacity >= 0 && opacity <= 1))
            throw new IllegalArgumentException("Opacity value must be between 0 and 1");
        double oldOpacity = getOpacity();
        boolean oldVisible = isVisible();
        this.opacity = opacity;
        if (!Utils.equalsEpsilon(oldOpacity, getOpacity())) {
            fireOpacityChanged(oldOpacity, getOpacity());
        }
        if (oldVisible != isVisible()) {
            fireVisibleChanged(oldVisible, isVisible());
        }
    }

    /**
     * Sets new state to the layer after applying {@link ImageProcessor}.
     */
    public void setFilterStateChanged() {
        fireFilterStateChanged();
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
     * fires a property change for the property {@link #FILTER_STATE_PROP}.
     */
    protected void fireFilterStateChanged() {
        propertyChangeSupport.firePropertyChange(FILTER_STATE_PROP, null, null);
    }

    /**
     * allows to check whether a projection is supported or not
     * @param proj projection
     *
     * @return True if projection is supported for this layer
     */
    public boolean isProjectionSupported(Projection proj) {
        return proj != null;
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
     */
    public static class LayerSaveAction extends AbstractAction {
        private final transient Layer layer;

        /**
         * Create a new action that saves the layer
         * @param layer The layer to save.
         */
        public LayerSaveAction(Layer layer) {
            new ImageProvider("save").getResource().attachImageIcon(this, true);
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

    /**
     * Action to save the layer in a new file
     */
    public static class LayerSaveAsAction extends AbstractAction {
        private final transient Layer layer;

        /**
         * Create a new save as action
         * @param layer The layer that should be saved.
         */
        public LayerSaveAsAction(Layer layer) {
            new ImageProvider("save_as").getResource().attachImageIcon(this, true);
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

    /**
     * Action that exports the layer as gpx file
     */
    public static class LayerGpxExportAction extends AbstractAction {
        private final transient Layer layer;

        /**
         * Create a new gpx export action for the given layer.
         * @param layer The layer
         */
        public LayerGpxExportAction(Layer layer) {
            new ImageProvider("exportgpx").getResource().attachImageIcon(this, true);
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
        if (!isProjectionSupported(newValue)) {
            final String message = "<html><body><p>" +
                    tr("The layer {0} does not support the new projection {1}.",
                            Utils.escapeReservedCharactersHTML(getName()), newValue.toCode()) + "</p>" +
                    "<p style='width: 450px;'>" + tr("Supported projections are: {0}", nameSupportedProjections()) + "</p>" +
                    tr("Change the projection again or remove the layer.");

            // run later to not block loading the UI.
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    message,
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE));
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
     * @see SaveActionBase#createAndOpenSaveFileChooser
     * @since 5459
     */
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save Layer"), "lay");
    }

    /**
     * Gets the strategy that specifies where this layer should be inserted in a layer list.
     * @return That strategy.
     * @since 10008
     */
    public LayerPositionStrategy getDefaultLayerPosition() {
        if (isBackgroundLayer()) {
            return LayerPositionStrategy.BEFORE_FIRST_BACKGROUND_LAYER;
        } else {
            return LayerPositionStrategy.AFTER_LAST_VALIDATION_LAYER;
        }
    }

    /**
     * Gets the {@link ProjectionBounds} for this layer to be visible to the user. This can be the exact bounds, the UI handles padding. Return
     * <code>null</code> if you cannot provide this information. The default implementation uses the bounds from
     * {@link #visitBoundingBox(BoundingXYVisitor)}.
     * @return The bounds for this layer.
     * @since 10371
     */
    public ProjectionBounds getViewProjectionBounds() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        visitBoundingBox(v);
        return v.getBounds();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", associatedFile=" + associatedFile + ']';
    }
}
