// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.util.List;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * Helper class for the JOSM system to communicate with the plugin.
 *
 * This class should be of no interest for sole plugin writer.
 *
 * @author Immanuel.Scholz
 */
public class PluginProxy extends Plugin {

    /**
     * The plugin.
     */
    private final Object plugin;
    private final PluginClassLoader classLoader;

    /**
     * Constructs a new {@code PluginProxy}.
     * @param plugin the plugin
     * @param info the associated plugin info
     * @param classLoader the class loader for the plugin
     * @since 12322
     */
    public PluginProxy(Object plugin, PluginInformation info, PluginClassLoader classLoader) {
        super(info);
        this.plugin = plugin;
        this.classLoader = classLoader;
    }

    /**
     * Get the plugin object.
     * @return the plugin object
     * @since 12322
     */
    public Object getPlugin() {
        return plugin;
    }

    /**
     * Get the class loader for the plugin.
     * @return the plugin class loader
     * @since 12322
     */
    public PluginClassLoader getClassLoader() {
        return classLoader;
    }

    private void handlePluginException(Throwable e) {
        PluginHandler.pluginLoadingExceptions.put(getPluginInformation().name, e);
        BugReportExceptionHandler.handleException(new PluginException(this, getPluginInformation().name, e));
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        try {
            plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (NoSuchMethodException e) {
            Logging.trace(e);
            Logging.debug("Plugin "+plugin+" does not define mapFrameInitialized");
        } catch (ReflectiveOperationException | IllegalArgumentException | NoClassDefFoundError e) {
            handlePluginException(e);
        }
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        try {
            return (PreferenceSetting) plugin.getClass().getMethod("getPreferenceSetting").invoke(plugin);
        } catch (NoSuchMethodException e) {
            Logging.trace(e);
            Logging.debug("Plugin "+plugin+" does not define getPreferenceSetting");
            return null;
        } catch (ReflectiveOperationException | IllegalArgumentException | NoClassDefFoundError e) {
            handlePluginException(e);
        }
        return null;
    }

    @Override
    public void addDownloadSelection(List<DownloadSelection> list) {
        try {
            plugin.getClass().getMethod("addDownloadSelection", List.class).invoke(plugin, list);
        } catch (NoSuchMethodException e) {
            Logging.trace(e);
            Logging.debug("Plugin "+plugin+" does not define addDownloadSelection");
        } catch (ReflectiveOperationException | IllegalArgumentException | NoClassDefFoundError e) {
            handlePluginException(e);
        }
    }
}
