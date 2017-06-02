// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
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
    public final Object plugin;

    /**
     * Constructs a new {@code PluginProxy}.
     * @param plugin the plugin
     * @param info the associated plugin info
     */
    public PluginProxy(Object plugin, PluginInformation info) {
        super(info);
        this.plugin = plugin;
    }

    private void handlePluginException(Exception e) {
        PluginHandler.pluginLoadingExceptions.put(getPluginInformation().name, e);
        BugReportExceptionHandler.handleException(new PluginException(this, getPluginInformation().name, e));
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        try {
            plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (NoSuchMethodException e) {
            Main.trace(e);
            Main.debug("Plugin "+plugin+" does not define mapFrameInitialized");
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            handlePluginException(e);
        }
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        try {
            return (PreferenceSetting) plugin.getClass().getMethod("getPreferenceSetting").invoke(plugin);
        } catch (NoSuchMethodException e) {
            Main.trace(e);
            Main.debug("Plugin "+plugin+" does not define getPreferenceSetting");
            return null;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            handlePluginException(e);
        }
        return null;
    }

    @Override
    public void addDownloadSelection(List<DownloadSelection> list) {
        try {
            plugin.getClass().getMethod("addDownloadSelection", List.class).invoke(plugin, list);
        } catch (NoSuchMethodException e) {
            Main.trace(e);
            Main.debug("Plugin "+plugin+" does not define addDownloadSelection");
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            handlePluginException(e);
        }
    }
}
