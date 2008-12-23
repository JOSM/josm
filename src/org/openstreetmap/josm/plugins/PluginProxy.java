// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import java.util.List;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;


/**
 * Helper class for the JOSM system to communicate with the plugin.
 *
 * This class should be of no interest for sole plugin writer.
 *
 * @author Immanuel.Scholz
 */
public class PluginProxy extends Plugin {

    public final Object plugin;
    public final PluginInformation info;

    public PluginProxy(Object plugin, PluginInformation info) {
        this.plugin = plugin;
        this.info = info;
    }

    @Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        try {
            plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            throw new PluginException(this, info.name, e);
        }
    }

    @Override public PreferenceSetting getPreferenceSetting() {
        try {
            return (PreferenceSetting)plugin.getClass().getMethod("getPreferenceSetting").invoke(plugin);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new PluginException(this, info.name, e);
        }
    }

    @Override public void addDownloadSelection(List<DownloadSelection> list) {
        try {
            plugin.getClass().getMethod("addDownloadSelection", List.class).invoke(plugin, list);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (Exception e) {
            throw new PluginException(this, info.name, e);
        }
    }
}
