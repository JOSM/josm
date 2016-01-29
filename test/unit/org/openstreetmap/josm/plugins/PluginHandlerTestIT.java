// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * Integration tests of {@link PluginHandler} class.
 */
public class PluginHandlerTestIT {

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test that available plugins rules can be loaded.
     */
    @Test
    public void testValidityOfAvailablePlugins() {
        // Download complete list of plugins
        ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                Main.pref.getOnlinePluginSites());
        pluginInfoDownloadTask.run();
        List<PluginInformation> plugins = pluginInfoDownloadTask.getAvailablePlugins();
        System.out.println("Original plugin list contains " + plugins.size() + " plugins");
        assertFalse(plugins.isEmpty());
        PluginInformation info = plugins.get(0);
        assertFalse(info.getName().isEmpty());
        assertFalse(info.getClass().getName().isEmpty());

        // Filter deprecated and unmaintained ones
        List<String> uncooperatingPlugins = Arrays.asList("ebdirigo");
        Set<String> deprecatedPlugins = PluginHandler.getDeprecatedAndUnmaintainedPlugins();
        for (Iterator<PluginInformation> it = plugins.iterator(); it.hasNext();) {
            PluginInformation pi = it.next();
            if (deprecatedPlugins.contains(pi.name) || uncooperatingPlugins.contains(pi.name)) {
                System.out.println("Ignoring " + pi.name + " (deprecated, unmaintained, or uncooperative)");
                it.remove();
            }
        }
        System.out.println("Filtered plugin list contains " + plugins.size() + " plugins");

        // Update the locally installed plugins
        PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(NullProgressMonitor.INSTANCE, plugins, null);
        pluginDownloadTask.run();
        assertTrue(pluginDownloadTask.getFailedPlugins().toString(), pluginDownloadTask.getFailedPlugins().isEmpty());
        assertEquals(plugins.size(), pluginDownloadTask.getDownloadedPlugins().size());

        // Update Plugin info for downloaded plugins
        PluginHandler.refreshLocalUpdatedPluginInfo(pluginDownloadTask.getDownloadedPlugins());

        // Load early plugins
        PluginHandler.loadEarlyPlugins(null, plugins, null);

        // Load late plugins
        PluginHandler.loadLatePlugins(null, plugins, null);

        assertTrue(PluginHandler.pluginLoadingExceptions.toString(), PluginHandler.pluginLoadingExceptions.isEmpty());
    }
}
