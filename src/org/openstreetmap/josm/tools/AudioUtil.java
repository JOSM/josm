// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.openstreetmap.josm.Main;

/**
 * Utils functions for audio.
 *
 * @author David Earl &lt;david@frankieandshadow.com&gt;
 * @since 1462
 */
public final class AudioUtil {

    private AudioUtil() {
        // Hide default constructor for utils classes
    }

    /**
     * Returns calibrated length of recording in seconds.
     * @param wavFile the recording file (WAV format)
     * @return the calibrated length of recording in seconds.
     */
    static public double getCalibratedDuration(File wavFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new URL("file:".concat(wavFile.getAbsolutePath())));
            AudioFormat audioFormat = audioInputStream.getFormat();
            long filesize = wavFile.length();
            double bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
                * audioFormat.getFrameSize() /* bytes per frame */;
            double naturalLength = filesize / bytesPerSecond;
            Utils.close(audioInputStream);
            double calibration = Main.pref.getDouble("audio.calibration", 1.0 /* default, ratio */);
            return naturalLength / calibration;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
