// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Base class for every action related to audio content.
 * @since 12565
 */
public abstract class AbstractAudioAction extends JosmAction {

    /**
     * Constructs a new {@code BaseAudioAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip a longer description of the action that will be displayed in the tooltip
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut
     * @param registerInToolbar register this action for the toolbar preferences?
     */
    public AbstractAudioAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
        updateEnabledState();
    }

    /**
     * Checks if there is at least one {@link AudioMarker} is present in the current layout.
     * @return {@code true} if at least one {@link AudioMarker} is present in the current
     * layout, {@code false} otherwise.
     */
    protected static boolean isAudioMarkerPresent() {
        return Main.getLayerManager().getLayersOfType(MarkerLayer.class).stream()
                .flatMap(ml -> ml.data.stream())
                .anyMatch(m -> m instanceof AudioMarker);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(isAudioMarkerPresent());
    }
}
