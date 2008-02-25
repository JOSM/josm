// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioPlayer;

public class AudioFwdAction extends JosmAction {

	private double amount;
	
	public AudioFwdAction() {
		super(tr("Forward"), "audio-fwd", tr("Jump forward"), KeyEvent.VK_F7, 0, true);
		try {
			amount = Double.parseDouble(Main.pref.get("audio.forwardbackamount","10.0"));
		} catch (NumberFormatException e) {
			amount = 10.0;
		}
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
