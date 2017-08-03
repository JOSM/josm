// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.io.audio.AudioUtil;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Jump the audio backward 10 seconds and start playing if paused.
 * @since 547
 */
public class AudioBackAction extends AbstractAudioAction {

    /**
     * Constructs a new {@code AudioBackAction}.
     */
    public AudioBackAction() {
        super(trc("audio", "Back"), "audio-back", trc("audio", "Jump back."),
        Shortcut.registerShortcut("audio:back", tr("Audio: {0}", trc("audio", "Back")), KeyEvent.VK_F6, Shortcut.DIRECT), true);
        this.putValue("help", ht("/Action/AudioBack"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (AudioPlayer.playing() || AudioPlayer.paused())
                AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position()
                - Main.pref.getDouble("audio.forwardbackamount", 10.0));
            else
                MarkerLayer.playAudio();
        } catch (IOException | InterruptedException ex) {
            AudioUtil.audioMalfunction(ex);
        }
    }
}
