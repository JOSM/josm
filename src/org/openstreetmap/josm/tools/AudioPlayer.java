// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

/**
 * Creates and controls a separate audio player thread.
 *
 * @author David Earl &lt;david@frankieandshadow.com&gt;
 * @since 547
 */
public final class AudioPlayer extends Thread {

    private static AudioPlayer audioPlayer = null;

    private enum State { INITIALIZING, NOTPLAYING, PLAYING, PAUSED, INTERRUPTED }
    private State state;
    private enum Command { PLAY, PAUSE }
    private enum Result { WAITING, OK, FAILED }
    private URL playingUrl;
    private double leadIn; // seconds
    private double calibration; // ratio of purported duration of samples to true duration
    private double position; // seconds
    private double bytesPerSecond;
    private static long chunk = 4000; /* bytes */
    private double speed = 1.0;

    /**
     * Passes information from the control thread to the playing thread
     */
    private class Execute {
        private Command command;
        private Result result;
        private Exception exception;
        private URL url;
        private double offset; // seconds
        private double speed; // ratio

        /*
         * Called to execute the commands in the other thread
         */
        protected void play(URL url, double offset, double speed) throws Exception {
            this.url = url;
            this.offset = offset;
            this.speed = speed;
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
            if (result == Result.FAILED)
                throw exception;
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
        protected double speed() {
            return speed;
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
     * @throws Exception audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void play(URL url) throws Exception {
        AudioPlayer.get().command.play(url, 0.0, 1.0);
    }

    /**
     * Plays a WAV audio file from a specified position.
     * @param url The resource to play, which must be a WAV file or stream
     * @param seconds The number of seconds into the audio to start playing
     * @throws Exception audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void play(URL url, double seconds) throws Exception {
        AudioPlayer.get().command.play(url, seconds, 1.0);
    }

    /**
     * Plays a WAV audio file from a specified position at variable speed.
     * @param url The resource to play, which must be a WAV file or stream
     * @param seconds The number of seconds into the audio to start playing
     * @param speed Rate at which audio playes (1.0 = real time, &gt; 1 is faster)
     * @throws Exception audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void play(URL url, double seconds, double speed) throws Exception {
        AudioPlayer.get().command.play(url, seconds, speed);
    }

    /**
     * Pauses the currently playing audio stream. Does nothing if nothing playing.
     * @throws Exception audio fault exception, e.g. can't open stream,  unhandleable audio format
     */
    public static void pause() throws Exception {
        AudioPlayer.get().command.pause();
    }

    /**
     * To get the Url of the playing or recently played audio.
     * @return url - could be null
     */
    public static URL url() {
        return AudioPlayer.get().playingUrl;
    }

    /**
     * Whether or not we are paused.
     * @return boolean whether or not paused
     */
    public static boolean paused() {
        return AudioPlayer.get().state == State.PAUSED;
    }

    /**
     * Whether or not we are playing.
     * @return boolean whether or not playing
     */
    public static boolean playing() {
        return AudioPlayer.get().state == State.PLAYING;
    }

    /**
     * How far we are through playing, in seconds.
     * @return double seconds
     */
    public static double position() {
        return AudioPlayer.get().position;
    }

    /**
     * Speed at which we will play.
     * @return double, speed multiplier
     */
    public static double speed() {
        return AudioPlayer.get().speed;
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

    /**
     * Resets the audio player.
     */
    public static void reset() {
        if(audioPlayer != null) {
            try {
                pause();
            } catch(Exception e) {
                Main.warn(e);
            }
            audioPlayer.playingUrl = null;
        }
    }

    private AudioPlayer() {
        state = State.INITIALIZING;
        command = new Execute();
        playingUrl = null;
        leadIn = Main.pref.getDouble("audio.leadin", 1.0 /* default, seconds */);
        calibration = Main.pref.getDouble("audio.calibration", 1.0 /* default, ratio */);
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
        SourceDataLine audioOutputLine = null;
        AudioFormat audioFormat = null;
        byte[] abData = new byte[(int)chunk];

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
                        for(;;) {
                            int nBytesRead = 0;
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
                        Utils.close(audioInputStream);
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
                            speed = command.speed();
                            if (playingUrl != command.url() ||
                                    stateChange != State.PAUSED ||
                                    offset != 0.0)
                            {
                                if (audioInputStream != null) {
                                    Utils.close(audioInputStream);
                                    audioInputStream = null;
                                }
                                playingUrl = command.url();
                                audioInputStream = AudioSystem.getAudioInputStream(playingUrl);
                                audioFormat = audioInputStream.getFormat();
                                long nBytesRead = 0;
                                position = 0.0;
                                offset -= leadIn;
                                double calibratedOffset = offset * calibration;
                                bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
                                * audioFormat.getFrameSize() /* bytes per frame */;
                                if (speed * bytesPerSecond > 256000.0) {
                                    speed = 256000 / bytesPerSecond;
                                }
                                if (calibratedOffset > 0.0) {
                                    long bytesToSkip = (long)(
                                            calibratedOffset /* seconds (double) */ * bytesPerSecond);
                                    /* skip doesn't seem to want to skip big chunks, so
                                     * reduce it to smaller ones
                                     */
                                    // audioInputStream.skip(bytesToSkip);
                                    while (bytesToSkip > chunk) {
                                        nBytesRead = audioInputStream.skip(chunk);
                                        if (nBytesRead <= 0)
                                            throw new IOException(tr("This is after the end of the recording"));
                                        bytesToSkip -= nBytesRead;
                                    }
                                    while (bytesToSkip > 0) {
                                        long skippedBytes = audioInputStream.skip(bytesToSkip);
                                        bytesToSkip -= skippedBytes;
                                        if (skippedBytes == 0) {
                                            // Avoid inifinite loop
                                            Main.warn("Unable to skip bytes from audio input stream");
                                            bytesToSkip = 0;
                                        }
                                    }
                                    position = offset;
                                }
                                if (audioOutputLine != null) {
                                    audioOutputLine.close();
                                }
                                audioFormat = new AudioFormat(audioFormat.getEncoding(),
                                        audioFormat.getSampleRate() * (float) (speed * calibration),
                                        audioFormat.getSampleSizeInBits(),
                                        audioFormat.getChannels(),
                                        audioFormat.getFrameSize(),
                                        audioFormat.getFrameRate() * (float) (speed * calibration),
                                        audioFormat.isBigEndian());
                                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                                audioOutputLine = (SourceDataLine) AudioSystem.getLine(info);
                                audioOutputLine.open(audioFormat);
                                audioOutputLine.start();
                            }
                            stateChange = State.PLAYING;
                            break;
                        case PAUSE:
                            stateChange = State.PAUSED;
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

    /**
     * Shows a popup audio error message for the given exception.
     * @param ex The exception used as error reason. Cannot be {@code null}.
     */
    public static void audioMalfunction(Exception ex) {
        String msg = ex.getMessage();
        if(msg == null)
            msg = tr("unspecified reason");
        else
            msg = tr(msg);
        JOptionPane.showMessageDialog(Main.parent,
                "<html><p>" + msg + "</p></html>",
                tr("Error playing sound"), JOptionPane.ERROR_MESSAGE);
    }
}
