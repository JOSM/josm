// License: GPL. Copyright 2008 by David Earl and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.Shortcut;

public class AudioPlayPauseAction extends JosmAction {

    public AudioPlayPauseAction() {
        super(trc("audio", "Play/Pause"), "audio-playpause", tr("Play/pause audio."),
        Shortcut.registerShortcut("audio:pause", tr("Audio: {0}", trc("audio", "Play/Pause")), KeyEvent.VK_PERIOD, Shortcut.GROUP_DIRECT), true);
    }

    public void actionPerformed(ActionEvent e) {
        URL url = AudioPlayer.url();
        try {
            if (AudioPlayer.paused() && url != null) {
                AudioPlayer.play(url);
            } else if (AudioPlayer.playing()){
                if (AudioPlayer.speed() != 1.0)
                    AudioPlayer.play(url, AudioPlayer.position());
                else
                    AudioPlayer.pause();
            } else {
                // find first audio marker to play
                MarkerLayer.playAudio();
            }
        } catch (Exception ex) {
            AudioPlayer.audioMalfunction(ex);
        }
    }
}
