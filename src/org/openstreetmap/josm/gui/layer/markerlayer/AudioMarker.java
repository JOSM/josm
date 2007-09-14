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

/**
 * Marker class with audio playback capability.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class AudioMarker extends ButtonMarker {

	private URL audioUrl;

	/**
	 * Verifies the parameter whether a new AudioMarker can be created and return
	 * one or return <code>null</code>.
	 */
	public static AudioMarker create(LatLon ll, String url) {
		try {
			return new AudioMarker(ll, new URL(url));
		} catch (Exception ex) {
			return null;
		}
	}

	private AudioMarker(LatLon ll, URL audioUrl) {
		super(ll, "speech.png");
		this.audioUrl = audioUrl;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(audioUrl);
		} catch (Exception e) {
			audioMalfunction(e);
			return;
		}
		AudioFormat	audioFormat = audioInputStream.getFormat();
		SourceDataLine line = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		} catch (Exception e)	{
			audioMalfunction(e);
			return;
		}
		line.start();

		int	nBytesRead = 0;
		byte[]	abData = new byte[16384];
		while (nBytesRead != -1) {
			try {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			} catch (IOException e) {
				audioMalfunction(e);
				return;
			}
			if (nBytesRead >= 0) {
				/* int	nBytesWritten = */ line.write(abData, 0, nBytesRead);
			}
		}
		line.drain();
		line.close();
	}

	void audioMalfunction(Exception ex) {
		JOptionPane.showMessageDialog(Main.parent, 
				"<html><b>" + 
				tr("There was an error while trying to play the sound file for this marker.") +
				"</b><br>" + ex.getClass().getName() + ":<br><i>" + ex.getMessage() + "</i></html>",
				tr("Error playing sound"), JOptionPane.ERROR_MESSAGE);
	}
}
