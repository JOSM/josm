// License: GPL. Copyright 2008 by David Earl and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.lang.Thread;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;

/**
 * Creates and controls a separate audio player thread.
 * 
 * @author David Earl <david@frankieandshadow.com>
 *
 */
public class AudioPlayer extends Thread {

	private static AudioPlayer audioPlayer = null;

	private enum State { INITIALIZING, NOTPLAYING, PLAYING, PAUSED, INTERRUPTED } 
	private State state;
    private enum Command { PLAY, PAUSE }
    private enum Result { WAITING, OK, FAILED }
    private URL playingUrl;
    private double leadIn; // seconds
	private double position; // seconds
	private double bytesPerSecond; 

	/**
	 * Passes information from the control thread to the playing thread 
	 */
	private class Execute {
		private Command command;
		private Result result;
		private Exception exception;
		private URL url;
		private double offset; // seconds

		/*
		 * Called to execute the commands in the other thread 
		 */
		protected void play(URL url, double offset) throws Exception {
			this.url = url;
			this.offset = offset;
			command = Command.PLAY;
			result = Result.WAITING;
			send();
		}
		protected void pause() throws Exception {
			command = Command.PAUSE;
			send();
		}
		private void send() throws Exception {
			result = Result.WAITING;
			interrupt();
			while (result == Result.WAITING) { sleep(10); /* yield(); */ }
			if (result == Result.FAILED) { throw exception; }
		}
		private void possiblyInterrupt() throws InterruptedException {
			if (interrupted() || result == Result.WAITING)
				throw new InterruptedException();
		}
		protected void failed (Exception e) {
			exception = e;
			result = Result.FAILED;
			state = State.NOTPLAYING;
		}
		protected void ok (State newState) {
			result = Result.OK;
			state = newState;
		}
		protected double offset() {
			return offset;
		}
		protected URL url() {
			return url;
		}
		protected Command command() {
			return command;
		}
	}
	
	private Execute command;

	/**
	 * Plays a WAV audio file from the beginning. See also the variant which doesn't 
	 * start at the beginning of the stream
	 * @param url The resource to play, which must be a WAV file or stream
	 * @throws audio fault exception, e.g. can't open stream,  unhandleable audio format
	 */
	public static void play(URL url) throws Exception {
		AudioPlayer.play(url, 0.0);
	}
	
	/**
	 * Plays a WAV audio file from a specified position.
	 * @param url The resource to play, which must be a WAV file or stream
	 * @param seconds The number of seconds into the audio to start playing
	 * @throws audio fault exception, e.g. can't open stream,  unhandleable audio format
	 */
	public static void play(URL url, double seconds) throws Exception {
		AudioPlayer.get().command.play(url, seconds);
	}
	
	/**
	 * Pauses the currently playing audio stream. Does nothing if nothing playing.
	 * @throws audio fault exception, e.g. can't open stream,  unhandleable audio format
	 */
	public static void pause() throws Exception {
		AudioPlayer.get().command.pause();
	}
	
	/**
	 * To get the Url of the playing or recently played audio.
	 * @returns url - could be null
	 */
	public static URL url() {
		return AudioPlayer.get().playingUrl;
	}
	
	/**
	 * Whether or not we are paused.
	 * @returns boolean whether or not paused
	 */
	public static boolean paused() {
		return AudioPlayer.get().state == State.PAUSED;
	}

	/**
	 * Whether or not we are playing.
	 * @returns boolean whether or not playing
	 */
	public static boolean playing() {
		return AudioPlayer.get().state == State.PLAYING;
	}

	/**
	 * How far we are through playing, in seconds.
	 * @returns double seconds
	 */
	public static double position() {
		return AudioPlayer.get().position;
	}
	
	/**
	 *  gets the singleton object, and if this is the first time, creates it along with 
	 *  the thread to support audio
	 */
	private static AudioPlayer get() {
		if (audioPlayer != null)
			return audioPlayer;
		try {
			audioPlayer = new AudioPlayer();
			return audioPlayer;
		} catch (Exception ex) {
			return null;
		}
	}

	private AudioPlayer() {
		state = State.INITIALIZING;
		command = new Execute();
		playingUrl = null;
		try {
			leadIn = Double.parseDouble(Main.pref.get("audio.leadin", "1.0" /* default, seconds */));
		} catch (NumberFormatException e) {
			leadIn = 1.0; // failed to parse
		}
		start();
		while (state == State.INITIALIZING) { yield(); }
	}

	/**
	 * Starts the thread to actually play the audio, per Thread interface
	 * Not to be used as public, though Thread interface doesn't allow it to be made private
	 */
	@Override public void run() {
		/* code running in separate thread */

		playingUrl = null;
		AudioInputStream audioInputStream = null;
		int nBytesRead = 0;
		SourceDataLine audioOutputLine = null;
		AudioFormat	audioFormat = null;
		byte[] abData = new byte[8192];
		
		for (;;) {
			try {
				switch (state) {
				case INITIALIZING:
					// we're ready to take interrupts
					state = State.NOTPLAYING;
					break;
				case NOTPLAYING:
				case PAUSED:
					sleep(200);
					break;
				case PLAYING:
					for(;;) {
						nBytesRead = audioInputStream.read(abData, 0, abData.length);
						position += nBytesRead / bytesPerSecond;
						command.possiblyInterrupt();
						if (nBytesRead < 0) { break; }
						audioOutputLine.write(abData, 0, nBytesRead); // => int nBytesWritten
						command.possiblyInterrupt();
					}
					// end of audio, clean up
					audioOutputLine.drain();
					audioOutputLine.close();
					audioOutputLine = null;
					audioInputStream.close();
					audioInputStream = null;
					playingUrl = null;
					state = State.NOTPLAYING;
					command.possiblyInterrupt();
					break;
				}
			} catch (InterruptedException e) {
				interrupted(); // just in case we get an interrupt
				State stateChange = state;
				state = State.INTERRUPTED;
				try {
					switch (command.command()) {
					case PLAY:	
						double offset = command.offset();
						if (playingUrl != command.url() || 
							stateChange != State.PAUSED || 
							offset != 0.0) 
						{
							if (audioInputStream != null) {
								audioInputStream.close();
								audioInputStream = null;
							}
							playingUrl = command.url();
							audioInputStream = AudioSystem.getAudioInputStream(playingUrl);
							audioFormat = audioInputStream.getFormat();
							DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
							nBytesRead = 0;
							position = 0.0;
							double adjustedOffset = offset - leadIn;
							bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
								* audioFormat.getFrameSize() /* bytes per frame */;
							if (offset != 0.0 && adjustedOffset > 0.0) {
								long bytesToSkip = (long)(
									adjustedOffset /* seconds (double) */ * bytesPerSecond);
								/* skip doesn't seem to want to skip big chunks, so 
								 * reduce it to smaller ones 
								 */
								// audioInputStream.skip(bytesToSkip);
								int skipsize = 8192;
								while (bytesToSkip > skipsize) {
									audioInputStream.skip(skipsize);
									bytesToSkip -= skipsize;
								}
								audioInputStream.skip(bytesToSkip);
								position = adjustedOffset;
							}
							if (audioOutputLine == null) {
								audioOutputLine = (SourceDataLine) AudioSystem.getLine(info);
								audioOutputLine.open(audioFormat);
								audioOutputLine.start();
							}
						}
						stateChange = State.PLAYING;
						break;
					case PAUSE:
						stateChange = state.PAUSED;
						break;
					}
					command.ok(stateChange);
				} catch (Exception startPlayingException) {
					command.failed(startPlayingException); // sets state
				}
			} catch (Exception e) {
				state = State.NOTPLAYING;
			}
		}
	}

	public static void audioMalfunction(Exception ex) {
		JOptionPane.showMessageDialog(Main.parent, 
				"<html><b>" + 
				tr("There was an error while trying to play the sound file for this marker.") +
				"</b><br>" + ex.getClass().getName() + ":<br><i>" + ex.getMessage() + "</i></html>",
				tr("Error playing sound"), JOptionPane.ERROR_MESSAGE);
	}
}
