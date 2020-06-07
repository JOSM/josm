// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import java.awt.event.ActionEvent;
import java.io.IOException;

import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.io.audio.AudioUtil;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abstract superclass of {@link AudioFasterAction} and {@link AudioSlowerAction}.
 * @since 563
 */
public abstract class AudioFastSlowAction extends AbstractAudioAction {

    private double multiplier;

    /**
     * Constructs a new {@code AudioFastSlowAction}.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip.
     * @param shortcut a ready-created shortcut object.
     * @param fast {@code true} to increase speed (faster audio), {@code false} to decrease it (slower audio).
     */
    protected AudioFastSlowAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean fast) {
        super(name, iconName, tooltip, shortcut, true);
        multiplier = Config.getPref().getDouble("audio.fastfwdmultiplier", 1.3);
        if (!fast)
            multiplier = 1.0 / multiplier;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        double speed = AudioPlayer.speed();
        if (speed * multiplier <= 0.1)
            return;
        try {
            if (AudioPlayer.playing() || AudioPlayer.paused())
                AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position(), speed * multiplier);
        } catch (IOException | InterruptedException ex) {
            AudioUtil.audioMalfunction(ex);
        }
    }
}
