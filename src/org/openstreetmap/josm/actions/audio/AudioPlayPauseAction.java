// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;

import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.io.audio.AudioUtil;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * If not playing, play the sound track from the first Audio Marker, or from the point at which it was paused.<br>
 * If playing, pause the sound.<br>
 * If fast forwarding or slow forwarding, resume normal speed.
 * @since 547
 */
public class AudioPlayPauseAction extends AbstractAudioAction {

    /**
     * Constructs a new {@code AudioPlayPauseAction}.
     */
    public AudioPlayPauseAction() {
        super(trc("audio", "Play/Pause"), "audio-playpause", tr("Play/pause audio."),
        Shortcut.registerShortcut("audio:pause", tr("Audio: {0}", trc("audio", "Play/Pause")), KeyEvent.VK_PERIOD, Shortcut.DIRECT), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        URL url = AudioPlayer.url();
        try {
            if (url != null && AudioPlayer.paused()) {
                AudioPlayer.play(url);
            } else if (AudioPlayer.playing()) {
                if (!Utils.equalsEpsilon(AudioPlayer.speed(), 1.0))
                    AudioPlayer.play(url, AudioPlayer.position());
                else
                    AudioPlayer.pause();
            } else {
                // play the last-played marker again, if there is one
                AudioMarker lastPlayed = AudioMarker.recentlyPlayedMarker();
                if (lastPlayed != null) {
                    lastPlayed.play();
                } else {
                    // If no marker was played recently, play the first one
                    MarkerLayer.playAudio();
                }
            }
        } catch (IOException | InterruptedException ex) {
            AudioUtil.audioMalfunction(ex);
        }
    }
}
