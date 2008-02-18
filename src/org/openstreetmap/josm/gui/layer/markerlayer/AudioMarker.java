// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * Marker class with audio playback capability.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class AudioMarker extends ButtonMarker {

	private URL audioUrl;
	private double syncOffset;
	private static AudioMarker recentlyPlayedMarker = null;

	/**
	 * Verifies the parameter whether a new AudioMarker can be created and return
	 * one or return <code>null</code>.
	 */
	public static AudioMarker create(LatLon ll, String text, String url, double offset) {
		try {
			return new AudioMarker(ll, text, new URL(url), offset);
		} catch (Exception ex) {
			return null;
		}
	}

	private AudioMarker(LatLon ll, String text, URL audioUrl, double offset) {
		super(ll, text, "speech.png", offset);
		this.audioUrl = audioUrl;
		this.syncOffset = 0.0;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		play();
	}

	public static AudioMarker recentlyPlayedMarker() {
		return recentlyPlayedMarker;
	}
	
	public URL url() {
		return audioUrl;
	}
	
	/**
	 * Starts playing the audio associated with the marker: used in response to pressing
	 * the marker as well as indirectly 
	 *
	 */
	public void play() {
		try {
			AudioPlayer.play(audioUrl, offset + syncOffset);
			recentlyPlayedMarker = this;
		} catch (Exception e) {
			AudioPlayer.audioMalfunction(e);
		}
	}
	
	public void adjustOffset(double adjustment) {
		syncOffset = adjustment; // added to offset may turn out negative, but that's ok
	}
}
