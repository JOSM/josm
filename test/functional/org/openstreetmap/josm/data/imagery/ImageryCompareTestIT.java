// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.fail;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.HttpClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Automatic test of imagery synchronization between JOSM and ELI.
 * See <a href="https://josm.openstreetmap.de/wiki/ImageryCompare">JOSM wiki</a>
 */
public class ImageryCompareTestIT {

    private static String BLACK_PREFIX = "<pre style=\"margin:3px;color:black\">";
    private static String RED_PREFIX = "<pre style=\"margin:3px;color:red\">";

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(60000);

    /**
     * Test of imagery entries.
     * @throws Exception if an error occurs
     */
    @Test
    public void testImageryEntries() throws Exception {
        // Increase traditional timeouts to avoid random problems
        Config.getPref().putInt("socket.timeout.connect", 60);
        Config.getPref().putInt("socket.timeout.read", 90);
        System.out.println("Displaying only red entries. The test fails if at least one is found");
        boolean rubricDisplayed = false;
        boolean redFound = false;
        String comparison = HttpClient.create(new URL("https://josm.openstreetmap.de/wiki/ImageryCompare")).connect().fetchContent();
        String rubricLine = null;
        for (String line : comparison.split("\n")) {
            boolean black = line.startsWith(BLACK_PREFIX);
            if (black) {
                rubricLine = line;
                rubricDisplayed = false;
            } else {
                boolean red = line.startsWith(RED_PREFIX);
                if (red) {
                    if (!rubricDisplayed && rubricLine != null) {
                        System.out.println(rubricLine.replace(BLACK_PREFIX, "").replace("</pre>", ""));
                        rubricDisplayed = true;
                    }
                    System.out.println(line.replace(RED_PREFIX, "").replace("</pre>", ""));
                    if (!redFound && red) {
                        redFound = true;
                    }
                }
            }
        }
        if (redFound) {
            fail("Error: at least a red line has been found, see https://josm.openstreetmap.de/wiki/ImageryCompare for details");
        } else {
            System.out.println("No error :)");
        }
    }
}
