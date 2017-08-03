// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginHandlerTestIT;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginListParseException;
import org.openstreetmap.josm.plugins.PluginListParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MainApplication} class.
 */
public class MainApplicationTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().mainMenu();

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING")
    private void testShow(final String arg, String expected) throws InterruptedException, IOException {
        PrintStream old = System.out;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(baos));
            Thread t = new Thread() {
                @Override
                public void run() {
                    MainApplication.main(new String[] {arg});
                }
            };
            t.start();
            t.join();
            System.out.flush();
            assertEquals(expected, baos.toString(StandardCharsets.UTF_8.name()).trim());
        } finally {
            System.setOut(old);
        }
    }

    /**
     * Test of {@link MainApplication#main} with argument {@code --version}.
     * @throws Exception in case of error
     */
    @Test
    public void testShowVersion() throws Exception {
        testShow("--version", Version.getInstance().getAgentString());
    }

    /**
     * Test of {@link MainApplication#main} with argument {@code --help}.
     * @throws Exception in case of error
     */
    @Test
    public void testShowHelp() throws Exception {
        testShow("--help", MainApplication.getHelp().trim());
    }

    /**
     * Test of {@link MainApplication#updateAndLoadEarlyPlugins} and {@link MainApplication#loadLatePlugins} methods.
     * @throws PluginListParseException if an error occurs
     */
    @Test
    public void testUpdateAndLoadPlugins() throws PluginListParseException {
        final String old = System.getProperty("josm.plugins");
        try {
            System.setProperty("josm.plugins", "buildings_tools,plastic_laf");
            SplashProgressMonitor monitor = new SplashProgressMonitor("foo", e -> {
                // Do nothing
            });
            Collection<PluginInformation> plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
            if (plugins.isEmpty()) {
                PluginHandlerTestIT.downloadPlugins(Arrays.asList(
                        newPluginInformation("buildings_tools"),
                        newPluginInformation("plastic_laf")));
                plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
            }
            assertEquals(2, plugins.size());
            assertNotNull(PluginHandler.getPlugin("plastic_laf"));
            assertNull(PluginHandler.getPlugin("buildings_tools"));
            MainApplication.loadLatePlugins(null, monitor, plugins);
            assertNotNull(PluginHandler.getPlugin("buildings_tools"));
        } finally {
            if (old != null) {
                System.setProperty("josm.plugins", old);
            } else {
                System.clearProperty("josm.plugins");
            }
        }
    }

    private static PluginInformation newPluginInformation(String plugin) throws PluginListParseException {
        return PluginListParser.createInfo(plugin+".jar", "https://svn.openstreetmap.org/applications/editors/josm/dist/"+plugin+".jar",
                "");
    }
}
