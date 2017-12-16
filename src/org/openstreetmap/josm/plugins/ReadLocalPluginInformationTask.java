// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for reading plugin information from the files
 * in the local plugin repositories.
 *
 * It scans the files in the local plugins repository (see {@link org.openstreetmap.josm.data.Preferences#getPluginsDirectory()}
 * and extracts plugin information from three kind of files:
 * <ul>
 *   <li>.jar files, assuming that they represent plugin jars</li>
 *   <li>.jar.new files, assuming that these are downloaded but not yet installed plugins</li>
 *   <li>cached lists of available plugins, downloaded for instance from
 *   <a href="https://josm.openstreetmap.de/pluginicons">https://josm.openstreetmap.de/pluginicons</a></li>
 * </ul>
 *
 */
public class ReadLocalPluginInformationTask extends PleaseWaitRunnable {
    private final Map<String, PluginInformation> availablePlugins;
    private boolean canceled;

    /**
     * Constructs a new {@code ReadLocalPluginInformationTask}.
     */
    public ReadLocalPluginInformationTask() {
        super(tr("Reading local plugin information.."), false);
        availablePlugins = new HashMap<>();
    }

    /**
     * Constructs a new {@code ReadLocalPluginInformationTask}.
     * @param monitor progress monitor
     */
    public ReadLocalPluginInformationTask(ProgressMonitor monitor) {
        super(tr("Reading local plugin information.."), monitor, false);
        availablePlugins = new HashMap<>();
    }

    @Override
    protected void cancel() {
        canceled = true;
    }

    @Override
    protected void finish() {
        // Do nothing
    }

    protected void processJarFile(File f, String pluginName) throws PluginException {
        PluginInformation info = new PluginInformation(
                f,
                pluginName
        );
        if (!availablePlugins.containsKey(info.getName())) {
            info.updateLocalInfo(info);
            availablePlugins.put(info.getName(), info);
        } else {
            PluginInformation current = availablePlugins.get(info.getName());
            current.updateFromJar(info);
        }
    }

    private static File[] listFiles(File pluginsDirectory, final String regex) {
        return pluginsDirectory.listFiles((FilenameFilter) (dir, name) -> name.matches(regex));
    }

    protected void scanSiteCacheFiles(ProgressMonitor monitor, File pluginsDirectory) {
        File[] siteCacheFiles = listFiles(pluginsDirectory, "^([0-9]+-)?site.*\\.txt$");
        if (siteCacheFiles == null || siteCacheFiles.length == 0)
            return;
        monitor.subTask(tr("Processing plugin site cache files..."));
        monitor.setTicksCount(siteCacheFiles.length);
        for (File f: siteCacheFiles) {
            String fname = f.getName();
            monitor.setCustomText(tr("Processing file ''{0}''", fname));
            try {
                processLocalPluginInformationFile(f);
            } catch (PluginListParseException e) {
                Logging.warn(tr("Failed to scan file ''{0}'' for plugin information. Skipping.", fname));
                Logging.error(e);
            }
            monitor.worked(1);
        }
    }

    protected void scanPluginFiles(ProgressMonitor monitor, File pluginsDirectory) {
        File[] pluginFiles = pluginsDirectory.listFiles(
                (FilenameFilter) (dir, name) -> name.endsWith(".jar") || name.endsWith(".jar.new")
        );
        if (pluginFiles == null || pluginFiles.length == 0)
            return;
        monitor.subTask(tr("Processing plugin files..."));
        monitor.setTicksCount(pluginFiles.length);
        for (File f: pluginFiles) {
            String fname = f.getName();
            monitor.setCustomText(tr("Processing file ''{0}''", fname));
            try {
                if (fname.endsWith(".jar")) {
                    String pluginName = fname.substring(0, fname.length() - 4);
                    processJarFile(f, pluginName);
                } else if (fname.endsWith(".jar.new")) {
                    String pluginName = fname.substring(0, fname.length() - 8);
                    processJarFile(f, pluginName);
                }
            } catch (PluginException e) {
                Logging.log(Logging.LEVEL_WARN, "PluginException: ", e);
                Logging.warn(tr("Failed to scan file ''{0}'' for plugin information. Skipping.", fname));
            }
            monitor.worked(1);
        }
    }

    protected void scanLocalPluginRepository(ProgressMonitor progressMonitor, File pluginsDirectory) {
        if (pluginsDirectory == null)
            return;
        ProgressMonitor monitor = progressMonitor != null ? progressMonitor : NullProgressMonitor.INSTANCE;
        try {
            monitor.beginTask("");
            scanSiteCacheFiles(monitor, pluginsDirectory);
            scanPluginFiles(monitor, pluginsDirectory);
        } finally {
            monitor.setCustomText("");
            monitor.finishTask();
        }
    }

    protected void processLocalPluginInformationFile(File file) throws PluginListParseException {
        try (InputStream fin = Files.newInputStream(file.toPath())) {
            List<PluginInformation> pis = new PluginListParser().parse(fin);
            for (PluginInformation pi : pis) {
                // we always keep plugin information from a plugin site because it
                // includes information not available in the plugin jars Manifest, i.e.
                // the download link or localized descriptions
                //
                availablePlugins.put(pi.name, pi);
            }
        } catch (IOException | InvalidPathException e) {
            throw new PluginListParseException(e);
        }
    }

    protected void analyseInProcessPlugins() {
        for (PluginProxy proxy : PluginHandler.pluginList) {
            PluginInformation info = proxy.getPluginInformation();
            if (canceled) return;
            if (!availablePlugins.containsKey(info.name)) {
                availablePlugins.put(info.name, info);
            } else {
                availablePlugins.get(info.name).localversion = info.localversion;
            }
        }
    }

    protected void filterOldPlugins() {
        for (PluginHandler.DeprecatedPlugin p : PluginHandler.DEPRECATED_PLUGINS) {
            if (canceled) return;
            if (availablePlugins.containsKey(p.name)) {
                availablePlugins.remove(p.name);
            }
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        Collection<String> pluginLocations = PluginInformation.getPluginLocations();
        getProgressMonitor().setTicksCount(pluginLocations.size() + 2);
        if (canceled) return;
        for (String location : pluginLocations) {
            scanLocalPluginRepository(
                    getProgressMonitor().createSubTaskMonitor(1, false),
                    new File(location)
            );
            getProgressMonitor().worked(1);
            if (canceled) return;
        }
        analyseInProcessPlugins();
        getProgressMonitor().worked(1);
        if (canceled) return;
        filterOldPlugins();
        getProgressMonitor().worked(1);
    }

    /**
     * Replies information about available plugins detected by this task.
     *
     * @return information about available plugins detected by this task.
     */
    public List<PluginInformation> getAvailablePlugins() {
        return new ArrayList<>(availablePlugins.values());
    }

    /**
     * Replies true if the task was canceled by the user
     *
     * @return true if the task was canceled by the user
     */
    public boolean isCanceled() {
        return canceled;
    }
}
