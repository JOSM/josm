// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.openstreetmap.josm.io.audio.AudioPlayer.Execute;
import org.openstreetmap.josm.io.audio.AudioPlayer.State;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Legacy sound player based on the Java Sound API.
 * Used on platforms where Java FX is not yet available. It supports only WAV files.
 * @since 12328
 */
class JavaSoundPlayer implements SoundPlayer {

    private static int chunk = 4000; /* bytes */

    private AudioInputStream audioInputStream;
    private SourceDataLine audioOutputLine;

    private final double leadIn; // seconds
    private final double calibration; // ratio of purported duration of samples to true duration

    private double bytesPerSecond;
    private final byte[] abData = new byte[chunk];

    private double position; // seconds
    private double speed = 1.0;

    private final ListenerList<AudioListener> listeners = ListenerList.create();

    JavaSoundPlayer(double leadIn, double calibration) {
        this.leadIn = leadIn;
        this.calibration = calibration;
    }

    @Override
    public void play(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException {
        final URL url = command.url();
        double offset = command.offset();
        speed = command.speed();
        if (playingUrl != url ||
                stateChange != State.PAUSED ||
                offset != 0) {
            if (audioInputStream != null) {
                Utils.close(audioInputStream);
            }
            listeners.fireEvent(l -> l.playing(url));
            try {
                audioInputStream = AudioSystem.getAudioInputStream(url);
            } catch (UnsupportedAudioFileException e) {
                throw new AudioException(e);
            }
            AudioFormat audioFormat = audioInputStream.getFormat();
            long nBytesRead;
            position = 0.0;
            offset -= leadIn;
            double calibratedOffset = offset * calibration;
            bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
            * audioFormat.getFrameSize() /* bytes per frame */;
            if (speed * bytesPerSecond > 256_000.0) {
                speed = 256_000 / bytesPerSecond;
            }
            if (calibratedOffset > 0.0) {
                long bytesToSkip = (long) (calibratedOffset /* seconds (double) */ * bytesPerSecond);
                // skip doesn't seem to want to skip big chunks, so reduce it to smaller ones
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
                        Logging.warn("Unable to skip bytes from audio input stream");
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
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                audioOutputLine = (SourceDataLine) AudioSystem.getLine(info);
                audioOutputLine.open(audioFormat);
                audioOutputLine.start();
            } catch (LineUnavailableException e) {
                throw new AudioException(e);
            }
        }
    }

    @Override
    public void pause(Execute command, State stateChange, URL playingUrl) throws AudioException, IOException {
        // Do nothing. As we are very low level, the playback is paused if we stop writing to audio output line
    }

    @Override
    public boolean playing(Execute command) throws AudioException, IOException, InterruptedException {
        for (;;) {
            int nBytesRead = 0;
            if (audioInputStream != null) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                position += nBytesRead / bytesPerSecond;
            }
            command.possiblyInterrupt();
            if (nBytesRead < 0 || audioInputStream == null || audioOutputLine == null) {
                break;
            }
            audioOutputLine.write(abData, 0, nBytesRead); // => int nBytesWritten
            command.possiblyInterrupt();
        }
        // end of audio, clean up
        if (audioOutputLine != null) {
            audioOutputLine.drain();
            audioOutputLine.close();
        }
        audioOutputLine = null;
        Utils.close(audioInputStream);
        audioInputStream = null;
        speed = 0;
        return true;
    }

    @Override
    public double position() {
        return position;
    }

    @Override
    public double speed() {
        return speed;
    }

    @Override
    public void addAudioListener(AudioListener listener) {
        listeners.addWeakListener(listener);
    }
}
