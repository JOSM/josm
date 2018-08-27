// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Creates and controls a separate audio player thread.
 *
 * @author David Earl &lt;david@frankieandshadow.com&gt;
 * @since 12326 (move to new package)
 * @since 547
 */
public final class AudioPlayer extends Thread implements AudioListener {

    private static volatile AudioPlayer audioPlayer;

    /**
     * Audio player state.
     */
    public enum State {
        /** Initializing */
        INITIALIZING,
        /** Not playing */
        NOTPLAYING,
        /** Playing */
        PLAYING,
        /** Paused */
        PAUSED,
        /** Interrupted */
        INTERRUPTED
    }

    /**
     * Audio player command.
     */
    public enum Command { /** Audio play */ PLAY, /** Audio pause */ PAUSE }

    /**
     * Audio player result.
     */
    public enum Result { /** In progress */ WAITING, /** Success */ OK, /** Failure */ FAILED }

    private State state;
    private static Class<? extends SoundPlayer> soundPlayerClass;
    private SoundPlayer soundPlayer;
    private URL playingUrl;

    /**
     * Passes information from the control thread to the playing thread
     */
    public class Execute {
        private Command command;
        private Result result;
        private Exception exception;
        private URL url;
        private double offset; // seconds
        private double speed; // ratio

        /*
         * Called to execute the commands in the other thread
         */
        protected void play(URL url, double offset, double speed) throws InterruptedException, IOException {
            this.url = url;
            this.offset = offset;
            this.speed = speed;
            command = Command.PLAY;
            result = Result.WAITING;
            send();
        }

        protected void pause() throws InterruptedException, IOException {
            command = Command.PAUSE;
            send();
        }

        private void send() throws InterruptedException, IOException {
            result = Result.WAITING;
            interrupt();
            while (result == Result.WAITING) {
                sleep(10);
            }
            if (result == Result.FAILED)
                throw new IOException(exception);
        }

        protected void possiblyInterrupt() throws InterruptedException {
            if (interrupted() || result == Result.WAITING)
                throw new InterruptedException();
        }

        protected void failed(Exception e) {
            exception = e;
            result = Result.FAILED;
            state = State.NOTPLAYING;
        }

        protected void ok(State newState) {
            result = Result.OK;
            state = newState;
        }

        /**
         * Returns the offset.
         * @return the offset, in seconds
         */
        public double offset() {
            return offset;
        }

        /**
         * Returns the speed.
         * @return the speed (ratio)
         */
        public double speed() {
            return speed;
        }

        /**
         * Returns the URL.
         * @return The resource to play, which must be a WAV file or stream
         */
        public URL url() {
            return url;
        }

        /**
         * Returns the command.
         * @return the command
         */
        public Command command() {
            return command;
        }
    }

    private final Execute command;

    /**
     * Plays a WAV audio file from the beginning. See also the variant which doesn't
     * start at the beginning of the stream
     * @param url The resource to play, which must be a WAV file or stream
     * @throws InterruptedException thread interrupted
     * @throws IOException audio fault exception, e.g. can't open stream, unhandleable audio format
     */
    public static void play(URL url) throws InterruptedException, IOException {
        AudioPlayer instance = AudioPlayer.getInstance();
        if (instance != null)
            instance.command.play(url, 0.0, 1.0);
    }

    /**
     * Plays a WAV audio file from a specified position.
     * @param url The resource to play, which must be a WAV file or stream
     * @param seconds The number of seconds into the audio to start playing
     * @throws InterruptedException thread interrupted
     * @throws IOException audio fault exception, e.g. can't open stream, unhandleable audio format
     */
    public static void play(URL url, double seconds) throws InterruptedException, IOException {
        AudioPlayer instance = AudioPlayer.getInstance();
        if (instance != null)
            instance.command.play(url, seconds, 1.0);
    }

    /**
     * Plays a WAV audio file from a specified position at variable speed.
     * @param url The resource to play, which must be a WAV file or stream
     * @param seconds The number of seconds into the audio to start playing
     * @param speed Rate at which audio playes (1.0 = real time, &gt; 1 is faster)
     * @throws InterruptedException thread interrupted
     * @throws IOException audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void play(URL url, double seconds, double speed) throws InterruptedException, IOException {
        AudioPlayer instance = AudioPlayer.getInstance();
        if (instance != null)
            instance.command.play(url, seconds, speed);
    }

    /**
     * Pauses the currently playing audio stream. Does nothing if nothing playing.
     * @throws InterruptedException thread interrupted
     * @throws IOException audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void pause() throws InterruptedException, IOException {
        AudioPlayer instance = AudioPlayer.getInstance();
        if (instance != null)
            instance.command.pause();
    }

    /**
     * To get the Url of the playing or recently played audio.
     * @return url - could be null
     */
    public static URL url() {
        AudioPlayer instance = AudioPlayer.getInstance();
        return instance == null ? null : instance.playingUrl;
    }

    /**
     * Whether or not we are paused.
     * @return boolean whether or not paused
     */
    public static boolean paused() {
        AudioPlayer instance = AudioPlayer.getInstance();
        return instance != null && instance.state == State.PAUSED;
    }

    /**
     * Whether or not we are playing.
     * @return boolean whether or not playing
     */
    public static boolean playing() {
        AudioPlayer instance = AudioPlayer.getInstance();
        return instance != null && instance.state == State.PLAYING;
    }

    /**
     * How far we are through playing, in seconds.
     * @return double seconds
     */
    public static double position() {
        AudioPlayer instance = AudioPlayer.getInstance();
        return instance == null ? -1 : instance.soundPlayer.position();
    }

    /**
     * Speed at which we will play.
     * @return double, speed multiplier
     */
    public static double speed() {
        AudioPlayer instance = AudioPlayer.getInstance();
        return instance == null ? -1 : instance.soundPlayer.speed();
    }

    /**
     * Returns the singleton object, and if this is the first time, creates it along with
     * the thread to support audio
     * @return the unique instance
     */
    private static AudioPlayer getInstance() {
        if (audioPlayer != null)
            return audioPlayer;
        try {
            audioPlayer = new AudioPlayer();
            return audioPlayer;
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException ex) {
            Logging.error(ex);
            return null;
        }
    }

    /**
     * Resets the audio player.
     */
    public static void reset() {
        if (audioPlayer != null) {
            try {
                pause();
            } catch (InterruptedException | IOException e) {
                Logging.warn(e);
            }
            audioPlayer.playingUrl = null;
        }
    }

    @SuppressWarnings("unchecked")
    private AudioPlayer() {
        state = State.INITIALIZING;
        command = new Execute();
        playingUrl = null;
        double leadIn = Config.getPref().getDouble("audio.leadin", 1.0 /* default, seconds */);
        double calibration = Config.getPref().getDouble("audio.calibration", 1.0 /* default, ratio */);
        try {
            if (soundPlayerClass == null) {
                // To remove when switching to Java 11
                soundPlayerClass = (Class<? extends SoundPlayer>) Class.forName(
                        "org.openstreetmap.josm.io.audio.fx.JavaFxMediaPlayer");
            }
            soundPlayer = soundPlayerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            Logging.debug(e);
            Logging.warn("JOSM compiled without Java FX support. Falling back to Java Sound API");
        } catch (NoClassDefFoundError | JosmRuntimeException e) {
            Logging.debug(e);
            Logging.warn("Java FX is unavailable. Falling back to Java Sound API");
        }
        if (soundPlayer == null) {
            soundPlayer = new JavaSoundPlayer(leadIn, calibration);
        }
        soundPlayer.addAudioListener(this);
        start();
        while (state == State.INITIALIZING) {
            yield();
        }
    }

    /**
     * Starts the thread to actually play the audio, per Thread interface
     * Not to be used as public, though Thread interface doesn't allow it to be made private
     */
    @Override
    public void run() {
        /* code running in separate thread */

        playingUrl = null;

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
                        command.possiblyInterrupt();
                        if (soundPlayer.playing(command)) {
                            playingUrl = null;
                            state = State.NOTPLAYING;
                        }
                        command.possiblyInterrupt();
                        break;
                    default: // Do nothing
                }
            } catch (InterruptedException e) {
                interrupted(); // just in case we get an interrupt
                State stateChange = state;
                state = State.INTERRUPTED;
                try {
                    switch (command.command()) {
                        case PLAY:
                            soundPlayer.play(command, stateChange, playingUrl);
                            stateChange = State.PLAYING;
                            break;
                        case PAUSE:
                            soundPlayer.pause(command, stateChange, playingUrl);
                            stateChange = State.PAUSED;
                            break;
                        default: // Do nothing
                    }
                    command.ok(stateChange);
                } catch (AudioException | IOException | SecurityException | IllegalArgumentException startPlayingException) {
                    Logging.error(startPlayingException);
                    command.failed(startPlayingException); // sets state
                }
            } catch (AudioException | IOException e) {
                state = State.NOTPLAYING;
                Logging.error(e);
            }
        }
    }

    @Override
    public void playing(URL playingUrl) {
        this.playingUrl = playingUrl;
    }

    /**
     * Returns the custom sound player class, if any.
     * @return the custom sound player class, or {@code null}
     * @since 14183
     */
    public static Class<? extends SoundPlayer> getSoundPlayerClass() {
        return soundPlayerClass;
    }

    /**
     * Sets the custom sound player class to override default core player.
     * Must be called before the first audio method invocation.
     * @param playerClass custom sound player class to override default core player
     * @since 14183
     */
    public static void setSoundPlayerClass(Class<? extends SoundPlayer> playerClass) {
        if (audioPlayer != null) {
            throw new IllegalStateException("Audio player already initialized");
        }
        soundPlayerClass = Objects.requireNonNull(playerClass);
    }
}
