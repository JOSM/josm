// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.tools.Utils;

/**
 * For all purposes of loading dynamic resources, the Plugin's class loader should be used
 * (or else, the plugin jar will not be within the class path).
 *
 * A plugin may subclass this abstract base class (but it is optional).
 *
 * The actual implementation of this class is optional, as all functions will be called
 * via reflection. This is to be able to change this interface without the need of
 * recompiling or even breaking the plugins. If your class does not provide a
 * function here (or does provide a function with a mismatching signature), it will not
 * be called. That simple.
 *
 * Or in other words: See this base class as an documentation of what automatic callbacks
 * are provided (you can register yourself to more callbacks in your plugin class
 * constructor).
 *
 * Subclassing Plugin and overriding some functions makes it easy for you to keep sync
 * with the correct actual plugin architecture of JOSM.
 *
 * @author Immanuel.Scholz
 */
public abstract class Plugin implements MapFrameListener {

    /**
     * This is the info available for this plugin. You can access this from your
     * constructor.
     *
     * (The actual implementation to request the info from a static variable
     * is a bit hacky, but it works).
     */
    private PluginInformation info = null;

    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public Plugin(PluginInformation info) {
        this.info = info;
    }

    /**
     * Replies the plugin information object for this plugin
     *
     * @return the plugin information object
     */
    public PluginInformation getPluginInformation() {
        return info;
    }

    /**
     * Sets the plugin information object for this plugin
     *
     * @param info the plugin information object
     */
    public void setPluginInformation(PluginInformation info) {
        this.info = info;
    }

    /**
     * @return The directory for the plugin to store all kind of stuff.
     */
    public String getPluginDir() {
        return new File(Main.pref.getPluginsDirectory(), info.name).getPath();
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {}

    /**
     * Called in the preferences dialog to create a preferences page for the plugin,
     * if any available.
     */
    public PreferenceSetting getPreferenceSetting() { return null; }

    /**
     * Called in the download dialog to give the plugin a chance to modify the list
     * of bounding box selectors.
     */
    public void addDownloadSelection(List<DownloadSelection> list) {}

    /**
     * Copies the resource 'from' to the file in the plugin directory named 'to'.
     */
    public void copy(String from, String to) throws FileNotFoundException, IOException {
        String pluginDirName = getPluginDir();
        File pluginDir = new File(pluginDirName);
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        FileOutputStream out = null;
        InputStream in = null;
        try {
            out = new FileOutputStream(new File(pluginDirName, to));
            in = getClass().getResourceAsStream(from);
            if (in == null) {
                throw new IOException("Resource not found: "+from);
            }
            byte[] buffer = new byte[8192];
            for(int len = in.read(buffer); len > 0; len = in.read(buffer)) {
                out.write(buffer, 0, len);
            }
        } finally {
            Utils.close(in);
            Utils.close(out);
        }
    }

    /**
     * Get a class loader for loading resources from the plugin jar.
     *
     * This can be used to avoid getting a file from another plugin that
     * happens to have a file with the same file name and path.
     *
     * Usage: Instead of
     *   getClass().getResource("/resources/pluginProperties.properties");
     * write
     *   getPluginResourceClassLoader().getResource("resources/pluginProperties.properties");
     *
     * (Note the missing leading "/".)
     */
    public ClassLoader getPluginResourceClassLoader() {
        File pluginDir = Main.pref.getPluginsDirectory();
        File pluginJar = new File(pluginDir, info.name + ".jar");
        final URL pluginJarUrl = Utils.fileToURL(pluginJar);
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
              public ClassLoader run() {
                  return new URLClassLoader(new URL[] {pluginJarUrl}, Main.class.getClassLoader());
              }
        });
    }
}
