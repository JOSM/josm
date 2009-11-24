// License: GPL. Copyright 2009 by David Earl and others
package org.openstreetmap.josm.tools;

import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.openstreetmap.josm.Main;

/**
 * Returns calibrated length of recording in seconds.
 *
 * @author David Earl <david@frankieandshadow.com>
 *
 */
public class AudioUtil {
    static public double getCalibratedDuration(File wavFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new URL("file:".concat(wavFile.getAbsolutePath())));
            AudioFormat audioFormat = audioInputStream.getFormat();
            long filesize = wavFile.length();
            double bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
                * audioFormat.getFrameSize() /* bytes per frame */;
            double naturalLength = filesize / bytesPerSecond;
            audioInputStream.close();
            double calibration = Main.pref.getDouble("audio.calibration", "1.0" /* default, ratio */);
            return naturalLength / calibration;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
