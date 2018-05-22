// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import java.io.IOException;
import java.net.URL;

import org.openstreetmap.josm.io.audio.AudioPlayer.Execute;
import org.openstreetmap.josm.io.audio.AudioPlayer.State;

/**
 * Sound player interface. Implementations can be backed up by Java Sound API or Java FX Media API.
 * @since 12328
 */
public interface SoundPlayer {

    /**
     * Ask player to play a new media.
     * @param command Command containing media information
     * @param stateChange the previous state
     * @param playingUrl the currently playing URL, if any
     * @throws AudioException if an audio error occurs
     * @throws IOException if an I/O error occurs
     */
    void play(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException;

    /**
     * Ask player to pause the current playing media.
     * @param command Command containing media information
     * @param stateChange the previous state
     * @param playingUrl the currently playing URL, if any
     * @throws AudioException if an audio error occurs
     * @throws IOException if an I/O error occurs
     */
    void pause(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException;

    /**
     * Method called when a media is being played.
     * @param command Command containing media information
     * @return {@code true} if the playing call was blocking, and the playback is finished when this method returns
     * @throws AudioException if an audio error occurs
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the play is interrupted
     */
    boolean playing(Execute command) throws AudioException, IOException, InterruptedException;

    /**
     * Returns the media playback position, in seconds.
     * @return the media playback position, in seconds
     */
    double position();

    /**
     * Returns the media playback speed ratio.
     * @return the media playback speed ratio
     */
    double speed();

    /**
     * Adds a listener that will be notified of audio playback events.
     * @param listener audio listener
     */
    void addAudioListener(AudioListener listener);
}
