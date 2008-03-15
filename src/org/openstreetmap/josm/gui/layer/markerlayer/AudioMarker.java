// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.event.ActionEvent;
import java.net.URL;

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
	private static AudioMarker recentlyPlayedMarker = null;
	public  double syncOffset;
	
	/**
	 * Verifies the parameter whether a new AudioMarker can be created and return
	 * one or return <code>null</code>.
	 */
	public static AudioMarker create(LatLon ll, String text, String url, MarkerLayer parentLayer, double time, double offset) {
		try {
			return new AudioMarker(ll, text, new URL(url), parentLayer, time, offset);
		} catch (Exception ex) {
			return null;
		}
	}

	private AudioMarker(LatLon ll, String text, URL audioUrl, MarkerLayer parentLayer, double time, double offset) {
		super(ll, text, "speech.png", parentLayer, time, offset);
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
	 * Starts playing the audio associated with the marker offset by the given amount 
	 * @param after : seconds after marker where playing should start
	 */
	public void play(double after) {
		try {
			// first enable tracing the audio along the track
			Main.map.mapView.playHeadMarker.animate();

			AudioPlayer.play(audioUrl, offset + syncOffset + after);
			recentlyPlayedMarker = this;
		} catch (Exception e) {
			AudioPlayer.audioMalfunction(e);
		}
	}

	/**
	 * Starts playing the audio associated with the marker: used in response to pressing
	 * the marker as well as indirectly 
	 *
	 */
	public void play() { play(0.0); }

	public void adjustOffset(double adjustment) {
		syncOffset = adjustment; // added to offset may turn out negative, but that's ok
	}

	public double syncOffset() {
		return syncOffset;
	}
	
	public static String inventName (double offset) {
		int wholeSeconds = (int)(offset + 0.5);
		if (wholeSeconds < 60)
			return Integer.toString(wholeSeconds);
		else if (wholeSeconds < 3600)
			return String.format("%d:%02d", wholeSeconds / 60, wholeSeconds % 60);
		else
			return String.format("%d:%02d:%02d", wholeSeconds / 3600, (wholeSeconds % 3600)/60, wholeSeconds % 60);
	}
}
