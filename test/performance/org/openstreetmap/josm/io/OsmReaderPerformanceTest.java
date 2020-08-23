// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.osm.DataSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This test tests how fast we are at reading an OSM file.
 * <p>
 * For this, we use the neubrandenburg-file, which is a good real world example of an OSM file. We ignore disk access times.
 *
 * @author Michael Zangl
 */
public class OsmReaderPerformanceTest {
    private static final int TIMES = 4;
    private static final String DATA_FILE = "nodist/data/neubrandenburg.osm.bz2";

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    /**
     * Prepare the test.
     */
    @BeforeClass
    public static void createJOSMFixture() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    /**
     * Simulates a plain read of a .osm.bz2 file (from memory)
     * @throws Exception if an error occurs
     */
    @Test
    public void testCompressed() throws Exception {
        runTest("compressed (.osm.bz2)", false);
    }

    /**
     * Simulates a plain read of a .osm file (from memory)
     * @throws Exception if an error occurs
     */
    @Test
    public void testPlain() throws Exception {
        runTest(".osm-file", true);
    }

    private void runTest(String what, boolean decompressBeforeRead) throws IllegalDataException, IOException {
        InputStream is = loadFile(decompressBeforeRead);
        PerformanceTestTimer timer = PerformanceTestUtils.startTimer("load " + what + " " + TIMES + " times");
        DataSet ds = null;
        for (int i = 0; i < TIMES; i++) {
            is.reset();

            ds = OsmReader.parseDataSet(decompressBeforeRead ? is : Compression.byExtension(DATA_FILE)
                    .getUncompressedInputStream(is), null);
        }
        timer.done();
        assertNotNull(ds);
    }

    private InputStream loadFile(boolean decompressBeforeRead) throws IOException {
        File file = new File(DATA_FILE);
        try (InputStream is = decompressBeforeRead ? Compression.getUncompressedFileInputStream(file) : new FileInputStream(file)) {
            ByteArrayOutputStream temporary = new ByteArrayOutputStream();
            byte[] readBuffer = new byte[4096];
            int readBytes = 0;
            while (readBytes != -1) {
                temporary.write(readBuffer, 0, readBytes);
                readBytes = is.read(readBuffer);
            }
            return new ByteArrayInputStream(temporary.toByteArray());
        }
    }
}
