// License: GPL. Copyright 2008 by David Earl and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;

public class AudioPlayPauseAction extends JosmAction {

	public AudioPlayPauseAction() {
		super(tr("Play/pause"), "audio-playpause", tr("Play/pause audio."), KeyEvent.VK_PERIOD, 0, true);
	}

	public void actionPerformed(ActionEvent e) {
		URL url = AudioPlayer.url();
		try {
			if (AudioPlayer.paused() && url != null) {
				AudioPlayer.play(url);
			} else if (AudioPlayer.playing()){
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
