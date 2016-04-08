// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;

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
     */
    @Test
    public void testUpdateAndLoadPlugins() {
        final String old = System.getProperty("josm.plugins");
        try {
            System.setProperty("josm.plugins", "buildings_tools,plastic_laf");
            assertEquals("buildings_tools,plastic_laf", System.getProperty("josm.plugins"));
            SplashProgressMonitor monitor = new SplashProgressMonitor("foo", new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    // Do nothing
                }
            });
            Collection<PluginInformation> plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
            assertEquals("buildings_tools,plastic_laf", System.getProperty("josm.plugins"));
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
}
