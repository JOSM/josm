// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import java.util.List;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;

/**
 * Helper class for the JOSM system to communicate with the plugin.
 *
 * This class should be of no interest for sole plugin writer.
 *
 * @author Immanuel.Scholz
 */
public class PluginProxy extends Plugin {

    public final Object plugin;

    public PluginProxy(Object plugin, PluginInformation info) {
        super(info);
        this.plugin = plugin;
    }

    @Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        try {
            plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            BugReportExceptionHandler.handleException(new PluginException(this, getPluginInformation().name, e));
        }
    }

    @Override public PreferenceSetting getPreferenceSetting() {
        try {
            return (PreferenceSetting)plugin.getClass().getMethod("getPreferenceSetting").invoke(plugin);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            BugReportExceptionHandler.handleException(new PluginException(this, getPluginInformation().name, e));
        }
        return null;
    }

    @Override public void addDownloadSelection(List<DownloadSelection> list) {
        try {
            plugin.getClass().getMethod("addDownloadSelection", List.class).invoke(plugin, list);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (Exception e) {
            BugReportExceptionHandler.handleException(new PluginException(this, getPluginInformation().name, e));
        }
    }
}
