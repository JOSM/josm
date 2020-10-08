// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link PluginHandler} class.
 */
public class PluginHandlerTestIT {

    private static final List<String> errorsToIgnore = new ArrayList<>();
    /**
     * Setup test.
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().main().projection().preferences().https()
            .timeout(10 * 60 * 1000);

    /**
     * Setup test
     *
     * @throws IOException in case of I/O error
     */
    @BeforeClass
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(PluginHandlerTestIT.class));
    }

    /**
     * Test that available plugins rules can be loaded.
     */
    @Test
    public void testValidityOfAvailablePlugins() {
        loadAllPlugins();

        Map<String, Throwable> loadingExceptions = PluginHandler.pluginLoadingExceptions.entrySet().stream()
                .filter(e -> !(Utils.getRootCause(e.getValue()) instanceof HeadlessException))
                .collect(Collectors.toMap(e -> e.getKey(), e -> Utils.getRootCause(e.getValue())));

        List<PluginInformation> loadedPlugins = PluginHandler.getPlugins();
        Map<String, List<String>> invalidManifestEntries = loadedPlugins.stream().filter(pi -> !pi.invalidManifestEntries.isEmpty())
                .collect(Collectors.toMap(pi -> pi.name, pi -> pi.invalidManifestEntries));

        // Add/remove layers twice to test basic plugin good behaviour
        Map<String, Throwable> layerExceptions = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            OsmDataLayer layer = new OsmDataLayer(new DataSet(), "Layer "+i, null);
            testPlugin(MainApplication.getLayerManager()::addLayer, layer, layerExceptions, loadedPlugins);
            testPlugin(MainApplication.getLayerManager()::removeLayer, layer, layerExceptions, loadedPlugins);
        }
        for (int i = 0; i < 2; i++) {
            GpxLayer layer = new GpxLayer(new GpxData(), "Layer "+i);
            testPlugin(MainApplication.getLayerManager()::addLayer, layer, layerExceptions, loadedPlugins);
            testPlugin(MainApplication.getLayerManager()::removeLayer, layer, layerExceptions, loadedPlugins);
        }

        Map<String, Throwable> noRestartExceptions = new HashMap<>();
        testCompletelyRestartlessPlugins(loadedPlugins, noRestartExceptions);

        debugPrint(invalidManifestEntries);
        debugPrint(loadingExceptions);
        debugPrint(layerExceptions);
        debugPrint(noRestartExceptions);

        invalidManifestEntries = filterKnownErrors(invalidManifestEntries);
        loadingExceptions = filterKnownErrors(loadingExceptions);
        layerExceptions = filterKnownErrors(layerExceptions);
        noRestartExceptions = filterKnownErrors(noRestartExceptions);

        String msg = Arrays.toString(invalidManifestEntries.entrySet().toArray()) + '\n' +
                     Arrays.toString(loadingExceptions.entrySet().toArray()) + '\n' +
                Arrays.toString(layerExceptions.entrySet().toArray()) + '\n'
                + Arrays.toString(noRestartExceptions.entrySet().toArray());
        assertTrue(msg, invalidManifestEntries.isEmpty() && loadingExceptions.isEmpty() && layerExceptions.isEmpty());
    }

    private static void testCompletelyRestartlessPlugins(List<PluginInformation> loadedPlugins,
            Map<String, Throwable> noRestartExceptions) {
        try {
            List<PluginInformation> restartable = loadedPlugins.parallelStream()
                    .filter(info -> PluginHandler.getPlugin(info.name) instanceof Destroyable)
                    .collect(Collectors.toList());
            // ensure good plugin behavior with regards to Destroyable (i.e., they can be
            // removed and readded)
            for (int i = 0; i < 2; i++) {
                assertFalse(PluginHandler.removePlugins(restartable));
                assertTrue(restartable.stream().noneMatch(info -> PluginHandler.getPlugins().contains(info)));
                loadPlugins(restartable);
            }

            assertTrue(PluginHandler.removePlugins(loadedPlugins));
            assertTrue(restartable.parallelStream().noneMatch(info -> PluginHandler.getPlugins().contains(info)));
        } catch (Exception | LinkageError t) {
            Throwable root = Utils.getRootCause(t);
            root.printStackTrace();
            noRestartExceptions.put(findFaultyPlugin(loadedPlugins, root), root);
        }
    }

    private static <T> Map<String, T> filterKnownErrors(Map<String, T> errorMap) {
        return errorMap.entrySet().parallelStream()
                .filter(entry -> !errorsToIgnore.contains(convertEntryToString(entry)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private static void debugPrint(Map<String, ?> invalidManifestEntries) {
        System.out.println(invalidManifestEntries.entrySet()
                .stream()
                .map(e -> convertEntryToString(e))
                .collect(Collectors.joining(", ")));
    }

    private static String convertEntryToString(Entry<String, ?> entry) {
        return entry.getKey() + "=\"" + entry.getValue() + "\"";
    }

    /**
     * Downloads and loads all JOSM plugins.
     */
    public static void loadAllPlugins() {
        // Download complete list of plugins
        ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                Preferences.main().getOnlinePluginSites());
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

        // On Java < 11 and headless mode, filter plugins requiring JavaFX as Monocle is not available
        int javaVersion = Utils.getJavaVersion();
        if (GraphicsEnvironment.isHeadless() && javaVersion < 11) {
            for (Iterator<PluginInformation> it = plugins.iterator(); it.hasNext();) {
                PluginInformation pi = it.next();
                if (pi.getRequiredPlugins().contains("javafx")) {
                    System.out.println("Ignoring " + pi.name + " (requiring JavaFX and we're using Java < 11 in headless mode)");
                    it.remove();
                }
            }
        }

        System.out.println("Filtered plugin list contains " + plugins.size() + " plugins");

        // Download plugins
        downloadPlugins(plugins);

        loadPlugins(plugins);
    }

    static void loadPlugins(List<PluginInformation> plugins) {
        // Load early plugins
        PluginHandler.loadEarlyPlugins(null, plugins, null);

        // Load late plugins
        PluginHandler.loadLatePlugins(null, plugins, null);
    }

    void testPlugin(Consumer<Layer> consumer, Layer layer,
            Map<String, Throwable> layerExceptions, Collection<PluginInformation> loadedPlugins) {
        try {
            consumer.accept(layer);
        } catch (Exception | LinkageError t) {
            Throwable root = Utils.getRootCause(t);
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
