// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioPlayer;

public class AudioBackAction extends JosmAction {

	private double amount; // note, normally negative, i.e. jump backwards in time
	
	public AudioBackAction() {
		super(tr("Back"), "audio-back", tr("Jump back."), KeyEvent.VK_F6, 0, true);
		try {
			amount = - Double.parseDouble(Main.pref.get("audio.forwardbackamount","10.0"));
		} catch (NumberFormatException e) {
			amount = 10.0;
		}
		this.putValue("help", "Action/Back");
	}

	public void actionPerformed(ActionEvent e) {
		try {
			if (AudioPlayer.playing() || AudioPlayer.paused())
				AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position() + amount);
			else
				MarkerLayer.playAudio();
		} catch (Exception ex) {
			AudioPlayer.audioMalfunction(ex);
		}
	}
}
