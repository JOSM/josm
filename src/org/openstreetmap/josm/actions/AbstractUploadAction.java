// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.beans.PropertyChangeListener;

import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abstract super-class of all upload actions.
 * Listens to layer change events to update its enabled state.
 * @since xxx
 */
public abstract class AbstractUploadAction extends JosmAction {

    /**
     * Constructs a new {@code AbstractUploadAction}.
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
    public AbstractUploadAction(String name, String iconName, String tooltip, Shortcut shortcut,
            boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
    }

    private final PropertyChangeListener updateOnRequireUploadChange = evt -> {
        if (OsmDataLayer.REQUIRES_UPLOAD_TO_SERVER_PROP.equals(evt.getPropertyName())) {
            updateEnabledState();
        }
    };

    @Override
    protected LayerChangeAdapter buildLayerChangeAdapter() {
        return new LayerChangeAdapter() {
            @Override
            public void layerAdded(LayerAddEvent e) {
                if (e.getAddedLayer() instanceof OsmDataLayer) {
                    e.getAddedLayer().addPropertyChangeListener(updateOnRequireUploadChange);
                }
                super.layerAdded(e);
            }

            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                if (e.getRemovedLayer() instanceof OsmDataLayer) {
                    e.getRemovedLayer().removePropertyChangeListener(updateOnRequireUploadChange);
                }
                super.layerRemoving(e);
            }
        };
    }
}
