// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Logging;

/**
 * Utils functions for audio.
 *
 * @author David Earl &lt;david@frankieandshadow.com&gt;
 * @since 12326 (move to new package)
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
    public static double getCalibratedDuration(File wavFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                new URL("file:".concat(wavFile.getAbsolutePath())))) {
            AudioFormat audioFormat = audioInputStream.getFormat();
            long filesize = wavFile.length();
            double bytesPerSecond = audioFormat.getFrameRate() /* frames per second */
                * audioFormat.getFrameSize() /* bytes per frame */;
            double naturalLength = filesize / bytesPerSecond;
            double calibration = Main.pref.getDouble("audio.calibration", 1.0 /* default, ratio */);
            return naturalLength / calibration;
        } catch (UnsupportedAudioFileException | IOException e) {
            Logging.debug(e);
            return 0.0;
        }
    }

    /**
     * Shows a popup audio error message for the given exception.
     * @param ex The exception used as error reason. Cannot be {@code null}.
     * @since 12328
     */
    public static void audioMalfunction(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null)
            msg = tr("unspecified reason");
        else
            msg = tr(msg);
        Logging.error(msg);
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(Main.parent,
                    "<html><p>" + msg + "</p></html>",
                    tr("Error playing sound"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
