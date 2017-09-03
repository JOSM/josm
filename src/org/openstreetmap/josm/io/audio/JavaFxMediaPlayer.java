// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.openstreetmap.josm.io.audio.AudioPlayer.Execute;
import org.openstreetmap.josm.io.audio.AudioPlayer.State;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.ListenerList;

import com.sun.javafx.application.PlatformImpl;

import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

/**
 * Default sound player based on the Java FX Media API.
 * Used on platforms where Java FX is available. It supports the following audio codecs:<ul>
 * <li>MP3</li>
 * <li>AIFF containing uncompressed PCM</li>
 * <li>WAV containing uncompressed PCM</li>
 * <li>MPEG-4 multimedia container with Advanced Audio Coding (AAC) audio</li>
 * </ul>
 * @since 12328
 */
class JavaFxMediaPlayer implements SoundPlayer {

    private final ListenerList<AudioListener> listeners = ListenerList.create();

    private MediaPlayer mediaPlayer;

    JavaFxMediaPlayer() throws JosmRuntimeException {
        try {
            initFxPlatform();
        } catch (InterruptedException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Initializes the JavaFX platform runtime.
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void initFxPlatform() throws InterruptedException {
        final CountDownLatch startupLatch = new CountDownLatch(1);

        // Note, this method is called on the FX Application Thread
        PlatformImpl.startup(startupLatch::countDown);

        // Wait for FX platform to start
        startupLatch.await();
    }

    @Override
    public synchronized void play(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException {
        try {
            final URL url = command.url();
            if (playingUrl != url) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                // Fail fast in case of invalid local URI (JavaFX Media locator retries 5 times with a 1 second delay)
                if ("file".equals(url.getProtocol()) && !new File(url.toURI()).exists()) {
                    throw new FileNotFoundException(url.toString());
                }
                mediaPlayer = new MediaPlayer(new Media(url.toString()));
                mediaPlayer.setOnPlaying(() ->
                    listeners.fireEvent(l -> l.playing(url))
                );
            }
            mediaPlayer.setRate(command.speed());
            if (Status.PLAYING == mediaPlayer.getStatus()) {
                Duration seekTime = Duration.seconds(command.offset());
                if (!seekTime.equals(mediaPlayer.getCurrentTime())) {
                    mediaPlayer.seek(seekTime);
                }
            }
            mediaPlayer.play();
        } catch (MediaException | URISyntaxException e) {
            throw new AudioException(e);
        }
    }

    @Override
    public synchronized void pause(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (MediaException e) {
                throw new AudioException(e);
            }
        }
    }

    @Override
    public boolean playing(Execute command) throws AudioException, IOException, InterruptedException {
        // Not used: JavaFX handles the low-level audio playback
        return false;
    }

    @Override
    public synchronized double position() {
        return mediaPlayer != null ? mediaPlayer.getCurrentTime().toSeconds() : -1;
    }

    @Override
    public synchronized double speed() {
        return mediaPlayer != null ? mediaPlayer.getCurrentRate() : -1;
    }

    @Override
    public void addAudioListener(AudioListener listener) {
        listeners.addWeakListener(listener);
    }
}
