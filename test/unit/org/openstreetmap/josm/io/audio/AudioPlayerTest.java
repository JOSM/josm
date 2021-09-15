// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Stopwatch;

/**
 * Unit tests of {@link AudioPlayer} class.
 */
@BasicPreferences
//@Disabled("unresolved sporadic deadlock - see #13809")
class AudioPlayerTest {

    // We play wav files of about 4 seconds + pause, so define timeout at 16 seconds
    private static final long MAX_DURATION = 16000;

    /**
     * Test method for {@code AudioPlayer#play(URL)}
     * @throws Exception audio fault exception, e.g. can't open stream, unhandleable audio format
     * @throws MalformedURLException wrong URL
     */
    @Test
    void testPlay() throws MalformedURLException, Exception {
        assertTimeout(Duration.ofMillis(4*MAX_DURATION), () -> {
            File wav1 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121226.wav"));
            File wav2 = new File(TestUtils.getRegressionDataFile(6851, "20111003_121557.wav"));

            for (File w : new File[] {wav1, wav2}) {
                System.out.println("Playing " + w.toPath());
                URL url = w.toURI().toURL();
                final Stopwatch stopwatch = Stopwatch.createStarted();
                AudioPlayer.play(url);
                assertTrue(AudioPlayer.playing());
                assertFalse(AudioPlayer.paused());
                AudioPlayer.pause();
                assertFalse(AudioPlayer.playing());
                assertTrue(AudioPlayer.paused());
                AudioPlayer.play(url, AudioPlayer.position());
                while (AudioPlayer.playing() && stopwatch.elapsed() < MAX_DURATION) {
                    Thread.sleep(500);
                }
                System.out.println("Play finished after " + stopwatch);
                assertTrue(stopwatch.elapsed() < MAX_DURATION);
                AudioPlayer.reset();
                Thread.sleep(1000); // precaution, see #13809
            }
        });
    }
}
