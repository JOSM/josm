// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
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
 *   <a href="http://josm.openstreetmap.de/plugins">http://josm.openstreetmap.de/plugins</a></li>
 * </ul>
 *
 */
public class ReadLocalPluginInformationTask extends PleaseWaitRunnable {
    private Map<String, PluginInformation> availablePlugins;
    private boolean canceled;

    public ReadLocalPluginInformationTask() {
        super(tr("Reading local plugin information.."), false);
        availablePlugins = new HashMap<String, PluginInformation>();
    }

    public ReadLocalPluginInformationTask(ProgressMonitor monitor) {
        super(tr("Reading local plugin information.."),monitor, false);
        availablePlugins = new HashMap<String, PluginInformation>();
    }

    @Override
    protected void cancel() {
        canceled = true;
    }

    @Override
    protected void finish() {}

    protected void processJarFile(File f, String pluginName) throws PluginException{
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

    protected void scanSiteCacheFiles(ProgressMonitor monitor, File pluginsDirectory) {
        File[] siteCacheFiles = pluginsDirectory.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches("^([0-9]+-)?site.*\\.txt$");
                    }
                }
        );
        if (siteCacheFiles == null || siteCacheFiles.length == 0)
            return;
        monitor.subTask(tr("Processing plugin site cache files..."));
        monitor.setTicksCount(siteCacheFiles.length);
        for (File f: siteCacheFiles) {
            String fname = f.getName();
            monitor.setCustomText(tr("Processing file ''{0}''", fname));
            try {
                processLocalPluginInformationFile(f);
            } catch(PluginListParseException e) {
                Main.warn(tr("Failed to scan file ''{0}'' for plugin information. Skipping.", fname));
                Main.error(e);
            }
            monitor.worked(1);
        }
    }

    protected void scanIconCacheFiles(ProgressMonitor monitor, File pluginsDirectory) {
        File[] siteCacheFiles = pluginsDirectory.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches("^([0-9]+-)?site.*plugin-icons\\.zip$");
                    }
                }
        );
        if (siteCacheFiles == null || siteCacheFiles.length == 0)
            return;
        monitor.subTask(tr("Processing plugin site cache icon files..."));
        monitor.setTicksCount(siteCacheFiles.length);
        for (File f: siteCacheFiles) {
            String fname = f.getName();
            monitor.setCustomText(tr("Processing file ''{0}''", fname));
            for (PluginInformation pi : availablePlugins.values()) {
                if (pi.icon == null && pi.iconPath != null) {
                    pi.icon = new ImageProvider(pi.name+".jar/"+pi.iconPath)
                                    .setArchive(f)
                                    .setMaxWidth(24)
                                    .setMaxHeight(24)
                                    .setOptional(true).get();
                }
            }
            monitor.worked(1);
        }
    }

    protected void scanPluginFiles(ProgressMonitor monitor, File pluginsDirectory) {
        File[] pluginFiles = pluginsDirectory.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar") || name.endsWith(".jar.new");
                    }
                }
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
            } catch (PluginException e){
                Main.warn("PluginException: "+e.getMessage());
                Main.warn(tr("Failed to scan file ''{0}'' for plugin information. Skipping.", fname));
            }
            monitor.worked(1);
        }
    }

    protected void scanLocalPluginRepository(ProgressMonitor monitor, File pluginsDirectory) {
        if (pluginsDirectory == null) return;
        try {
            monitor.beginTask("");
            scanSiteCacheFiles(monitor, pluginsDirectory);
            scanIconCacheFiles(monitor, pluginsDirectory);
            scanPluginFiles(monitor, pluginsDirectory);
        } finally {
            monitor.setCustomText("");
            monitor.finishTask();
        }
    }

    protected void processLocalPluginInformationFile(File file) throws PluginListParseException{
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            List<PluginInformation> pis = new PluginListParser().parse(fin);
            for (PluginInformation pi : pis) {
                // we always keep plugin information from a plugin site because it
                // includes information not available in the plugin jars Manifest, i.e.
                // the download link or localized descriptions
                //
                availablePlugins.put(pi.name, pi);
            }
        } catch(IOException e) {
            throw new PluginListParseException(e);
        } finally {
            Utils.close(fin);
        }
    }

    protected void analyseInProcessPlugins() {
        for (PluginProxy proxy : PluginHandler.pluginList) {
            PluginInformation info = proxy.getPluginInformation();
            if (canceled)return;
            if (!availablePlugins.containsKey(info.name)) {
                availablePlugins.put(info.name, info);
            } else {
                availablePlugins.get(info.name).localversion = info.localversion;
            }
        }
    }

    protected void filterOldPlugins() {
        for (PluginHandler.DeprecatedPlugin p : PluginHandler.DEPRECATED_PLUGINS) {
            if (canceled)return;
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
            if (canceled)return;
        }
        analyseInProcessPlugins();
        getProgressMonitor().worked(1);
        if (canceled)return;
        filterOldPlugins();
        getProgressMonitor().worked(1);
    }

    /**
     * Replies information about available plugins detected by this task.
     *
     * @return information about available plugins detected by this task.
     */
    public List<PluginInformation> getAvailablePlugins() {
        return new ArrayList<PluginInformation>(availablePlugins.values());
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
