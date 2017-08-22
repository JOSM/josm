// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 *
 * This action allows you to set up an icon, a tooltip text, a globally registered shortcut, register it in the main toolbar and set up
 * layer/selection listeners that call {@link #updateEnabledState()} whenever the global context is changed.
 *
 * A JosmAction can register a {@link LayerChangeListener} and a {@link SelectionChangedListener}. Upon
 * a layer change event or a selection change event it invokes {@link #updateEnabledState()}.
 * Subclasses can override {@link #updateEnabledState()} in order to update the {@link #isEnabled()}-state
 * of a JosmAction depending on the {@link #getLayerManager()} state.
 *
 * destroy() from interface Destroyable is called e.g. for MapModes, when the last layer has
 * been removed and so the mapframe will be destroyed. For other JosmActions, destroy() may never
 * be called (currently).
 *
 * @author imi
 */
public abstract class JosmAction extends AbstractAction implements Destroyable {

    protected transient Shortcut sc;
    private transient LayerChangeAdapter layerChangeAdapter;
    private transient ActiveLayerChangeAdapter activeLayerChangeAdapter;
    private transient SelectionChangeAdapter selectionChangeAdapter;

    /**
     * Constructs a {@code JosmAction}.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param icon the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public JosmAction(String name, ImageProvider icon, String tooltip, Shortcut shortcut, boolean registerInToolbar,
            String toolbarId, boolean installAdapters) {
        super(name);
        if (icon != null)
            icon.getResource().attachImageIcon(this, true);
        setHelpId();
        sc = shortcut;
        if (sc != null && !sc.isAutomatic()) {
            Main.registerActionShortcut(this, sc);
        }
        setTooltip(tooltip);
        if (getValue("toolbar") == null) {
            putValue("toolbar", toolbarId);
        }
        if (registerInToolbar && Main.toolbar != null) {
            Main.toolbar.register(this);
        }
        if (installAdapters) {
            installAdapters();
        }
    }

    /**
     * The new super for all actions.
     *
     * Use this super constructor to setup your action.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar,
            String toolbarId, boolean installAdapters) {
        this(name, iconName == null ? null : new ImageProvider(iconName), tooltip, shortcut, registerInToolbar,
                toolbarId == null ? iconName : toolbarId, installAdapters);
    }

    /**
     * Constructs a new {@code JosmAction}.
     *
     * Use this super constructor to setup your action.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar, boolean installAdapters) {
        this(name, iconName, tooltip, shortcut, registerInToolbar, null, installAdapters);
    }

    /**
     * Constructs a new {@code JosmAction}.
     *
     * Use this super constructor to setup your action.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     */
    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        this(name, iconName, tooltip, shortcut, registerInToolbar, null, true);
    }

    /**
     * Constructs a new {@code JosmAction}.
     */
    public JosmAction() {
        this(true);
    }

    /**
     * Constructs a new {@code JosmAction}.
     *
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public JosmAction(boolean installAdapters) {
        setHelpId();
        if (installAdapters) {
            installAdapters();
        }
    }

    /**
     * Installs the listeners to this action.
     * <p>
     * This should either never be called or only called in the constructor of this action.
     * <p>
     * All registered adapters should be removed in {@link #destroy()}
     */
    protected void installAdapters() {
        // make this action listen to layer change and selection change events
        if (listenToLayerChange()) {
            layerChangeAdapter = new LayerChangeAdapter();
            activeLayerChangeAdapter = new ActiveLayerChangeAdapter();
            getLayerManager().addLayerChangeListener(layerChangeAdapter);
            getLayerManager().addActiveLayerChangeListener(activeLayerChangeAdapter);
        }
        if (listenToSelectionChange()) {
            selectionChangeAdapter = new SelectionChangeAdapter();
            SelectionEventManager.getInstance()
                .addSelectionListener(selectionChangeAdapter, FireMode.IN_EDT_CONSOLIDATED);
        }
        initEnabledState();
    }

    /**
     * Overwrite this if {@link #updateEnabledState()} should be called when the active / availabe layers change. Default is true.
     * @return <code>true</code> if a {@link LayerChangeListener} and a {@link ActiveLayerChangeListener} should be registered.
     * @since 10353
     */
    protected boolean listenToLayerChange() {
        return true;
    }

    /**
     * Overwrite this if {@link #updateEnabledState()} should be called when the selection changed. Default is true.
     * @return <code>true</code> if a {@link SelectionChangedListener} should be registered.
     * @since 10353
     */
    protected boolean listenToSelectionChange() {
        return true;
    }

    @Override
    public void destroy() {
        if (sc != null && !sc.isAutomatic()) {
            Main.unregisterActionShortcut(this);
        }
        if (layerChangeAdapter != null) {
            getLayerManager().removeLayerChangeListener(layerChangeAdapter);
            getLayerManager().removeActiveLayerChangeListener(activeLayerChangeAdapter);
        }
        if (selectionChangeAdapter != null) {
            DataSet.removeSelectionListener(selectionChangeAdapter);
        }
    }

    private void setHelpId() {
        String helpId = "Action/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        if (helpId.endsWith("Action")) {
            helpId = helpId.substring(0, helpId.length()-6);
        }
        putValue("help", helpId);
    }

    /**
     * Returns the shortcut for this action.
     * @return the shortcut for this action, or "No shortcut" if none is defined
     */
    public Shortcut getShortcut() {
        if (sc == null) {
            sc = Shortcut.registerShortcut("core:none", tr("No Shortcut"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
            // as this shortcut is shared by all action that don't want to have a shortcut,
            // we shouldn't allow the user to change it...
            // this is handled by special name "core:none"
        }
        return sc;
    }

    /**
     * Sets the tooltip text of this action.
     * @param tooltip The text to display in tooltip. Can be {@code null}
     */
    public final void setTooltip(String tooltip) {
        if (tooltip != null) {
            putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tooltip, sc));
        }
    }

    /**
     * Gets the layer manager used for this action. Defaults to the main layer manager but you can overwrite this.
     * <p>
     * The layer manager must be available when {@link #installAdapters()} is called and must not change.
     *
     * @return The layer manager.
     * @since 10353
     */
    public MainLayerManager getLayerManager() {
        return Main.getLayerManager();
    }

    protected static void waitFuture(final Future<?> future, final PleaseWaitProgressMonitor monitor) {
        Main.worker.submit(() -> {
                        try {
                            future.get();
                        } catch (InterruptedException | ExecutionException | CancellationException e) {
                            Logging.error(e);
                            return;
                        }
                        monitor.close();
                    });
    }

    /**
     * Override in subclasses to init the enabled state of an action when it is
     * created. Default behaviour is to call {@link #updateEnabledState()}
     *
     * @see #updateEnabledState()
     * @see #updateEnabledState(Collection)
     */
    protected void initEnabledState() {
        updateEnabledState();
    }

    /**
     * Override in subclasses to update the enabled state of the action when
     * something in the JOSM state changes, i.e. when a layer is removed or added.
     *
     * See {@link #updateEnabledState(Collection)} to respond to changes in the collection
     * of selected primitives.
     *
     * Default behavior is empty.
     *
     * @see #updateEnabledState(Collection)
     * @see #initEnabledState()
     * @see #listenToLayerChange()
     */
    protected void updateEnabledState() {
    }

    /**
     * Override in subclasses to update the enabled state of the action if the
     * collection of selected primitives changes. This method is called with the
     * new selection.
     *
     * @param selection the collection of selected primitives; may be empty, but not null
     *
     * @see #updateEnabledState()
     * @see #initEnabledState()
     * @see #listenToSelectionChange()
     */
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
    }

    /**
     * Updates enabled state according to primitives currently selected in edit data set, if any.
     * Can be called in {@link #updateEnabledState()} implementations.
     * @since 10409
     */
    protected final void updateEnabledStateOnCurrentSelection() {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            setEnabled(false);
        } else {
            updateEnabledState(ds.getSelected());
        }
    }

    /**
     * Adapter for layer change events. Runs updateEnabledState() whenever the active layer changed.
     */
    protected class LayerChangeAdapter implements LayerChangeListener {
        @Override
        public void layerAdded(LayerAddEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            updateEnabledState();
        }

        @Override
        public String toString() {
            return "LayerChangeAdapter [" + JosmAction.this + ']';
        }
    }

    /**
     * Adapter for layer change events. Runs updateEnabledState() whenever the active layer changed.
     */
    protected class ActiveLayerChangeAdapter implements ActiveLayerChangeListener {
        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }

        @Override
        public String toString() {
            return "ActiveLayerChangeAdapter [" + JosmAction.this + ']';
        }
    }

    /**
     * Adapter for selection change events. Runs updateEnabledState() whenever the selection changed.
     */
    protected class SelectionChangeAdapter implements SelectionChangedListener {
        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            updateEnabledState(newSelection);
        }

        @Override
        public String toString() {
            return "SelectionChangeAdapter [" + JosmAction.this + ']';
        }
    }
}
