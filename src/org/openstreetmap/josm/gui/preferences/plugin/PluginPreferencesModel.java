// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;

public class PluginPreferencesModel extends Observable{
    @SuppressWarnings("unused")
    private final static Logger logger = Logger.getLogger(PluginPreferencesModel.class.getName());

    private final ArrayList<PluginInformation> availablePlugins = new ArrayList<PluginInformation>();
    private final ArrayList<PluginInformation> displayedPlugins = new ArrayList<PluginInformation>();
    private final HashMap<PluginInformation, Boolean> selectedPluginsMap = new HashMap<PluginInformation, Boolean>();
    private Set<String> pendingDownloads = new HashSet<String>();
    private String filterExpression;
    private Set<String> currentActivePlugins;

    public PluginPreferencesModel() {
        currentActivePlugins = new HashSet<String>();
        currentActivePlugins.addAll(Main.pref.getCollection("plugins", currentActivePlugins));
    }

    public void filterDisplayedPlugins(String filter) {
        if (filter == null) {
            displayedPlugins.clear();
            displayedPlugins.addAll(availablePlugins);
            this.filterExpression = null;
            return;
        }
        displayedPlugins.clear();
        for (PluginInformation pi: availablePlugins) {
            if (pi.matches(filter)) {
                displayedPlugins.add(pi);
            }
        }
        filterExpression = filter;
        clearChanged();
        notifyObservers();
    }

    public void setAvailablePlugins(Collection<PluginInformation> available) {
        availablePlugins.clear();
        if (available != null) {
            availablePlugins.addAll(available);
        }
        sort();
        filterDisplayedPlugins(filterExpression);
        Set<String> activePlugins = new HashSet<String>();
        activePlugins.addAll(Main.pref.getCollection("plugins", activePlugins));
        for (PluginInformation pi: availablePlugins) {
            if (selectedPluginsMap.get(pi) == null) {
                if (activePlugins.contains(pi.name)) {
                    selectedPluginsMap.put(pi, true);
                }
            }
        }
        clearChanged();
        notifyObservers();
    }

    protected  void updateAvailablePlugin(PluginInformation other) {
        if (other == null) return;
        PluginInformation pi = getPluginInformation(other.name);
        if (pi == null) {
            availablePlugins.add(other);
            return;
        }
        pi.updateFromPluginSite(other);
    }

    /**
     * Updates the list of plugin information objects with new information from
     * plugin update sites.
     * 
     * @param fromPluginSite plugin information read from plugin update sites
     */
    public void updateAvailablePlugins(Collection<PluginInformation> fromPluginSite) {
        for (PluginInformation other: fromPluginSite) {
            updateAvailablePlugin(other);
        }
        sort();
        filterDisplayedPlugins(filterExpression);
        Set<String> activePlugins = new HashSet<String>();
        activePlugins.addAll(Main.pref.getCollection("plugins", activePlugins));
        for (PluginInformation pi: availablePlugins) {
            if (selectedPluginsMap.get(pi) == null) {
                if (activePlugins.contains(pi.name)) {
                    selectedPluginsMap.put(pi, true);
                }
            }
        }
        clearChanged();
        notifyObservers();
    }

    /**
     * Replies the list of selected plugin information objects
     * 
     * @return the list of selected plugin information objects
     */
    public List<PluginInformation> getSelectedPlugins() {
        List<PluginInformation> ret = new LinkedList<PluginInformation>();
        for (PluginInformation pi: availablePlugins) {
            if (selectedPluginsMap.get(pi) == null) {
                continue;
            }
            if (selectedPluginsMap.get(pi)) {
                ret.add(pi);
            }
        }
        return ret;
    }

    /**
     * Replies the list of selected plugin information objects
     * 
     * @return the list of selected plugin information objects
     */
    public Set<String> getSelectedPluginNames() {
        Set<String> ret = new HashSet<String>();
        for (PluginInformation pi: getSelectedPlugins()) {
            ret.add(pi.name);
        }
        return ret;
    }

    /**
     * Sorts the list of available plugins
     */
    protected void sort() {
        Collections.sort(
                availablePlugins,
                new Comparator<PluginInformation>() {
                    public int compare(PluginInformation o1, PluginInformation o2) {
                        String n1 = o1.getName() == null ? "" : o1.getName().toLowerCase();
                        String n2 = o2.getName() == null ? "" : o2.getName().toLowerCase();
                        return n1.compareTo(n2);
                    }
                }
        );
    }

    /**
     * Replies the list of plugin informations to display
     * 
     * @return the list of plugin informations to display
     */
    public List<PluginInformation> getDisplayedPlugins() {
        return displayedPlugins;
    }


    /**
     * Replies the list of plugins waiting for update or download
     * 
     * @return the list of plugins waiting for update or download
     */
    public List<PluginInformation> getPluginsScheduledForUpdateOrDownload() {
        List<PluginInformation> ret = new ArrayList<PluginInformation>();
        for (String plugin: pendingDownloads) {
            PluginInformation pi = getPluginInformation(plugin);
            if (pi == null) {
                continue;
            }
            ret.add(pi);
        }
        return ret;
    }

    /**
     * Sets whether the plugin is selected or not.
     * 
     * @param name the name of the plugin
     * @param selected true, if selected; false, otherwise
     */
    public void setPluginSelected(String name, boolean selected) {
        PluginInformation pi = getPluginInformation(name);
        if (pi != null) {
            selectedPluginsMap.put(pi,selected);
            if (pi.isUpdateRequired()) {
                pendingDownloads.add(pi.name);
            }
        }
        if (!selected) {
            pendingDownloads.remove(name);
        }
    }

    /**
     * Removes all the plugin in {@code plugins} from the list of plugins
     * with a pending download
     * 
     * @param plugins the list of plugins to clear for a pending download
     */
    public void clearPendingPlugins(Collection<PluginInformation> plugins){
        if (plugins == null || plugins.isEmpty()) return;
        for(PluginInformation pi: plugins) {
            pendingDownloads.remove(pi.name);
        }
    }

    /**
     * Replies the plugin info with the name <code>name</code>. null, if no
     * such plugin info exists.
     * 
     * @param name the name. If null, replies null.
     * @return the plugin info.
     */
    public PluginInformation getPluginInformation(String name) {
        for (PluginInformation pi: availablePlugins) {
            if (pi.getName() != null && pi.getName().equals(name))
                return pi;
        }
        return null;
    }

    /**
     * Initializes the model from preferences
     */
    public void initFromPreferences() {
        Collection<String> enabledPlugins = Main.pref.getCollection("plugins", null);
        if (enabledPlugins == null) {
            this.selectedPluginsMap.clear();
            return;
        }
        for (String name: enabledPlugins) {
            PluginInformation pi = getPluginInformation(name);
            if (pi == null) {
                continue;
            }
            setPluginSelected(name, true);
        }
    }

    /**
     * Replies true if the plugin with name <code>name</code> is currently
     * selected in the plugin model
     * 
     * @param name the plugin name
     * @return true if the plugin is selected; false, otherwise
     */
    public boolean isSelectedPlugin(String name) {
        PluginInformation pi = getPluginInformation(name);
        if (pi == null) return false;
        if (selectedPluginsMap.get(pi) == null) return false;
        return selectedPluginsMap.get(pi);
    }

    /**
     * Replies the set of plugins which have been added by the user to
     * the set of activated plugins.
     * 
     * @return the set of newly deactivated plugins
     */
    public List<PluginInformation> getNewlyActivatedPlugins() {
        List<PluginInformation> ret = new LinkedList<PluginInformation>();
        for (Entry<PluginInformation, Boolean> entry: selectedPluginsMap.entrySet()) {
            PluginInformation pi = entry.getKey();
            boolean selected = entry.getValue();
            if (selected && ! currentActivePlugins.contains(pi.name)) {
                ret.add(pi);
            }
        }
        return ret;
    }

    /**
     * Replies the set of plugins which have been removed by the user from
     * the set of activated plugins.
     * 
     * @return the set of newly deactivated plugins
     */
    public List<PluginInformation> getNewlyDeactivatedPlugins() {
        List<PluginInformation> ret = new LinkedList<PluginInformation>();
        for (PluginInformation pi: availablePlugins) {
            if (!currentActivePlugins.contains(pi.name)) {
                continue;
            }
            if (selectedPluginsMap.get(pi) == null || ! selectedPluginsMap.get(pi)) {
                ret.add(pi);
            }
        }
        return ret;
    }

    /**
     * Replies the set of plugin names which have been added by the user to
     * the set of activated plugins.
     * 
     * @return the set of newly activated plugin names
     */
    public Set<String> getNewlyActivatedPluginNames() {
        Set<String> ret = new HashSet<String>();
        List<PluginInformation> plugins = getNewlyActivatedPlugins();
        for (PluginInformation pi: plugins) {
            ret.add(pi.name);
        }
        return ret;
    }

    /**
     * Replies true if the set of active plugins has been changed by the user
     * in this preference model. He has either added plugins or removed plugins
     * being active before.
     * 
     * @return true if the collection of active plugins has changed
     */
    public boolean isActivePluginsChanged() {
        Set<String> newActivePlugins = getSelectedPluginNames();
        return ! newActivePlugins.equals(currentActivePlugins);
    }

    /**
     * Refreshes the local version field on the plugins in <code>plugins</code> with
     * the version in the manifest of the downloaded "jar.new"-file for this plugin.
     * 
     * @param plugins the collections of plugins to refresh
     */
    public void refreshLocalPluginVersion(Collection<PluginInformation> plugins) {
        if (plugins == null) return;
        File pluginDir = Main.pref.getPluginsDirectory();
        for (PluginInformation pi : plugins) {
            // Find the downloaded file. We have tried to install the downloaded plugins
            // (PluginHandler.installDownloadedPlugins). This succeeds depending on the
            // platform.
            File downloadedPluginFile = new File(pluginDir, pi.name + ".jar.new");
            if (!(downloadedPluginFile.exists() && downloadedPluginFile.canRead())) {
                downloadedPluginFile = new File(pluginDir, pi.name + ".jar");
                if (!(downloadedPluginFile.exists() && downloadedPluginFile.canRead())) {
                    continue;
                }
            }
            try {
                PluginInformation newinfo = new PluginInformation(downloadedPluginFile, pi.name);
                PluginInformation oldinfo = getPluginInformation(pi.name);
                if (oldinfo == null) {
                    // should not happen
                    continue;
                }
                oldinfo.localversion = newinfo.version;
            } catch(PluginException e) {
                e.printStackTrace();
            }
        }
    }
}
