// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginHandlerTestIT;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginListParseException;
import org.openstreetmap.josm.plugins.PluginListParser;

/**
 * Unit tests of {@link MainApplication} class.
 */
public class MainApplicationTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
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
            SplashProgressMonitor monitor = new SplashProgressMonitor("foo", new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    // Do nothing
                }
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
        //return new PluginInformation(new File(TestUtils.getTestDataRoot()+File.separator+"plugin"+File.separator+plugin+".jar"));
        return PluginListParser.createInfo(plugin+".jar", "https://svn.openstreetmap.org/applications/editors/josm/dist/"+plugin+".jar",
                "");
    }
}
