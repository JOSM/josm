// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link PluginHandler} class.
 */
public class PluginHandlerTestIT {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main().projection().timeout(10*60*1000);

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

        // Filter deprecated and unmaintained ones, or those not responsive enough to match our continuous integration needs
        List<String> uncooperatingPlugins = Arrays.asList("ebdirigo", "scoutsigns", "josm-config");
        Set<String> deprecatedPlugins = PluginHandler.getDeprecatedAndUnmaintainedPlugins();
        for (Iterator<PluginInformation> it = plugins.iterator(); it.hasNext();) {
            PluginInformation pi = it.next();
            if (deprecatedPlugins.contains(pi.name) || uncooperatingPlugins.contains(pi.name)) {
                System.out.println("Ignoring " + pi.name + " (deprecated, unmaintained, or uncooperative)");
                it.remove();
            }
        }
        System.out.println("Filtered plugin list contains " + plugins.size() + " plugins");

        // Download plugins
        downloadPlugins(plugins);

        // Load early plugins
        PluginHandler.loadEarlyPlugins(null, plugins, null);

        // Load late plugins
        PluginHandler.loadLatePlugins(null, plugins, null);

        Map<String, Throwable> loadingExceptions = PluginHandler.pluginLoadingExceptions.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> ExceptionUtils.getRootCause(e.getValue())));

        // Add/remove layers twice to test basic plugin good behaviour
        Map<String, Throwable> layerExceptions = new HashMap<>();
        List<PluginInformation> loadedPlugins = PluginHandler.getPlugins();
        for (int i = 0; i < 2; i++) {
            OsmDataLayer layer = new OsmDataLayer(new DataSet(), "Layer "+i, null);
            testPlugin(MainApplication.getLayerManager()::addLayer, layer, layerExceptions, loadedPlugins);
            testPlugin(MainApplication.getLayerManager()::removeLayer, layer, layerExceptions, loadedPlugins);
        }
        for (int i = 0; i < 2; i++) {
            GpxLayer layer = new GpxLayer(new GpxData());
            testPlugin(MainApplication.getLayerManager()::addLayer, layer, layerExceptions, loadedPlugins);
            testPlugin(MainApplication.getLayerManager()::removeLayer, layer, layerExceptions, loadedPlugins);
        }

        MapUtils.debugPrint(System.out, null, loadingExceptions);
        MapUtils.debugPrint(System.out, null, layerExceptions);
        String msg = Arrays.toString(loadingExceptions.entrySet().toArray()) + '\n' +
                     Arrays.toString(layerExceptions.entrySet().toArray());
        assertTrue(msg, loadingExceptions.isEmpty() && layerExceptions.isEmpty());
    }

    void testPlugin(Consumer<Layer> consumer, Layer layer,
            Map<String, Throwable> layerExceptions, Collection<PluginInformation> loadedPlugins) {
        try {
            consumer.accept(layer);
        } catch (Exception | LinkageError t) {
            Throwable root = ExceptionUtils.getRootCause(t);
            root.printStackTrace();
            layerExceptions.put(findFaultyPlugin(loadedPlugins, root), root);
        }
    }

    private static String findFaultyPlugin(Collection<PluginInformation> plugins, Throwable root) {
        for (PluginInformation p : plugins) {
            try {
                ClassLoader cl = PluginHandler.getPluginClassLoader(p.getName());
                String pluginPackage = cl.loadClass(p.className).getPackage().getName();
                for (StackTraceElement e : root.getStackTrace()) {
                    try {
                        String stackPackage = cl.loadClass(e.getClassName()).getPackage().getName();
                        if (stackPackage.startsWith(pluginPackage)) {
                            return p.name;
                        }
                    } catch (ClassNotFoundException ex) {
                        System.err.println(ex.getMessage());
                        continue;
                    }
                }
            } catch (ClassNotFoundException ex) {
                System.err.println(ex.getMessage());
                continue;
            }
        }
        return "<unknown>";
    }

    /**
     * Download plugins
     * @param plugins plugins to download
     */
    public static void downloadPlugins(Collection<PluginInformation> plugins) {
        // Update the locally installed plugins
        PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(NullProgressMonitor.INSTANCE, plugins, null);
        // Increase default timeout to avoid random network errors on big jar files
        int defTimeout = Config.getPref().getInt("socket.timeout.read", 30);
        Config.getPref().putInt("socket.timeout.read", 2 * defTimeout);
        pluginDownloadTask.run();
        // Restore default timeout
        Config.getPref().putInt("socket.timeout.read", defTimeout);
        assertTrue(pluginDownloadTask.getFailedPlugins().toString(), pluginDownloadTask.getFailedPlugins().isEmpty());
        assertEquals(plugins.size(), pluginDownloadTask.getDownloadedPlugins().size());

        // Update Plugin info for downloaded plugins
        PluginHandler.refreshLocalUpdatedPluginInfo(pluginDownloadTask.getDownloadedPlugins());
    }
}
