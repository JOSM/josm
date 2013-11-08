// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Jump the audio forward 10 seconds.
 * @since 547
 */
public class AudioFwdAction extends JosmAction {

    /**
     * Constructs a new {@code AudioFwdAction}.
     */
    public AudioFwdAction() {
        super(trc("audio", "Forward"), "audio-fwd", trc("audio", "Jump forward"),
        Shortcut.registerShortcut("audio:forward", tr("Audio: {0}", trc("audio", "Forward")), KeyEvent.VK_F7, Shortcut.DIRECT), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (AudioPlayer.playing() || AudioPlayer.paused())
                AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position()
                + Main.pref.getDouble("audio.forwardbackamount", 10.0));
            else
                MarkerLayer.playAudio();
        } catch (Exception ex) {
            AudioPlayer.audioMalfunction(ex);
        }
    }
}
