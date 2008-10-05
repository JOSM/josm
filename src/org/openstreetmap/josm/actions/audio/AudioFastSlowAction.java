// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.ShortCut;

abstract public class AudioFastSlowAction extends JosmAction {

	private double multiplier;

	public AudioFastSlowAction(String name, String iconName, String tooltip, ShortCut shortcut, boolean fast) {
		super(name, iconName, tooltip, shortcut, true);
		try {
			multiplier = Double.parseDouble(Main.pref.get("audio.fastfwdmultiplier","1.3"));
		} catch (NumberFormatException e) {
			multiplier = 1.3;
		}
		if (! fast)
			multiplier = 1.0 / multiplier;
	}

	@Deprecated
	public AudioFastSlowAction(String name, String iconName, String tooltip, int shortcut, int modifier, boolean fast) {
		super(name, iconName, tooltip, shortcut, modifier, true);
		try {
			multiplier = Double.parseDouble(Main.pref.get("audio.fastfwdmultiplier","1.3"));
		} catch (NumberFormatException e) {
			multiplier = 1.3;
		}
		if (! fast)
			multiplier = 1.0 / multiplier;
	}

	public void actionPerformed(ActionEvent e) {
		double speed = AudioPlayer.speed();
		if (speed * multiplier <= 0.1)
			return;
		try {
			if (AudioPlayer.playing() || AudioPlayer.paused())
				AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position(), speed * multiplier);
		} catch (Exception ex) {
			AudioPlayer.audioMalfunction(ex);
		}
	}
}
