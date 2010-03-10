package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * PluginHandler is basically a collection of static utility functions used to bootstrap
 * and manage the loaded plugins.
 * 
 */
public class PluginHandler {

    final public static String [] DEPRECATED_PLUGINS = new String[] {"mappaint", "unglueplugin",
        "lang-de", "lang-en_GB", "lang-fr", "lang-it", "lang-pl", "lang-ro",
        "lang-ru", "ewmsplugin", "ywms", "tways-0.2", "geotagged", "landsat",
        "namefinder", "waypoints", "slippy_map_chooser", "tcx-support", "usertools",
        "AgPifoJ", "utilsplugin"};

    final public static String [] UNMAINTAINED_PLUGINS = new String[] {"gpsbabelgui", "Intersect_way"};

    /**
     * All installed and loaded plugins (resp. their main classes)
     */
    public final static Collection<PluginProxy> pluginList = new LinkedList<PluginProxy>();


    /**
     * Removes deprecated plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>.
     * 
     * Also notifies the user about removed deprecated plugins
     * 
     * @param plugins the collection of plugins
     */
    private static void filterDeprecatedPlugins(Window parent, Collection<String> plugins) {
        Set<String> removedPlugins = new HashSet<String>();
        for (String p : DEPRECATED_PLUGINS) {
            if (plugins.contains(p)) {
                plugins.remove(p);
                Main.pref.removeFromCollection("plugins", p);
                removedPlugins.add(p);
            }
        }
        if (removedPlugins.isEmpty())
            return;

        // notify user about removed deprecated plugins
        //
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append(trn(
                "The following plugin is no longer necessary and has been deactivated:",
                "The following plugins are no longer necessary and have been deactivated:",
                removedPlugins.size()
        ));
        sb.append("<ul>");
        for (String name: removedPlugins) {
            sb.append("<li>").append(name).append("</li>");
        }
        sb.append("</ul>");
        sb.append("</html>");
        JOptionPane.showMessageDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Removes unmaintained plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>. Also removes the plugin from the list
     * of plugins in the preferences, if necessary.
     * 
     * Asks the user for every unmaintained plugin whether it should be removed.
     * 
     * @param plugins the collection of plugins
     */
    private static void filterUnmaintainedPlugins(Window parent, Collection<String> plugins) {
        for (String unmaintained : UNMAINTAINED_PLUGINS) {
            if (!plugins.contains(unmaintained)) {
                continue;
            }
            String msg =  tr("<html>Loading of the plugin \"{0}\" was requested."
                    + "<br>This plugin is no longer developed and very likely will produce errors."
                    +"<br>It should be disabled.<br>Delete from preferences?</html>", unmaintained);
            if (confirmDisablePlugin(parent, msg,unmaintained)) {
                Main.pref.removeFromCollection("plugins", unmaintained);
                plugins.remove(unmaintained);
            }
        }
    }

    /**
     * Checks whether the locally available plugins should be updated and
     * asks the user if running an update is OK. An update is advised if
     * JOSM was updated to a new version since the last plugin updates or
     * if the plugins were last updated a long time ago.
     * 
     * @param parent the parent window relative to which the confirmation dialog
     * is to be displayed
     * @return true if a plugin update should be run; false, otherwise
     */
    public static boolean checkAndConfirmPluginUpdate(Window parent) {
        String message = null;
        String togglePreferenceKey = null;
        int v = Version.getInstance().getVersion();
        if (Main.pref.getInteger("pluginmanager.version", 0) < v) {
            message =
                "<html>"
                + tr("You updated your JOSM software.<br>"
                        + "To prevent problems the plugins should be updated as well.<br><br>"
                        + "Update plugins now?"
                )
                + "</html>";
            togglePreferenceKey = "pluginmanager.version-based-update.policy";
        }  else {
            long tim = System.currentTimeMillis();
            long last = Main.pref.getLong("pluginmanager.lastupdate", 0);
            Integer maxTime = Main.pref.getInteger("pluginmanager.time-based-update.interval", 60);
            long d = (tim - last) / (24 * 60 * 60 * 1000l);
            if ((last <= 0) || (maxTime <= 0)) {
                Main.pref.put("pluginmanager.lastupdate", Long.toString(tim));
            } else if (d > maxTime) {
                message =
                    "<html>"
                    + tr("Last plugin update more than {0} days ago.", d)
                    + "</html>";
                togglePreferenceKey = "pluginmanager.time-based-update.policy";
            }
        }
        if (message == null) return false;

        ButtonSpec [] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Update plugins"),
                        ImageProvider.get("dialogs", "refresh"),
                        tr("Click to update the activated plugins"),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Skip update"),
                        ImageProvider.get("cancel"),
                        tr("Click to skip updating the activated plugins"),
                        null /* no specific help context */
                )
        };

        UpdatePluginsMessagePanel pnlMessage = new UpdatePluginsMessagePanel();
        pnlMessage.setMessage(message);
        pnlMessage.initDontShowAgain(togglePreferenceKey);

        // check whether automatic update at startup was disabled
        //
        String policy = Main.pref.get(togglePreferenceKey, "ask");
        policy = policy.trim().toLowerCase();
        if (policy.equals("never")) {
            if ("pluginmanager.version-based-update.policy".equals(togglePreferenceKey)) {
                System.out.println(tr("Skipping plugin update after JOSM upgrade. Automatic update at startup is disabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                System.out.println(tr("Skipping plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return false;
        }

        if (policy.equals("always")) {
            if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                System.out.println(tr("Running plugin update after JOSM upgrade. Automatic update at startup is enabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                System.out.println(tr("Running plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return true;
        }

        if (!policy.equals("ask")) {
            System.err.println(tr("Unexpected value ''{0}'' for preference ''{1}''. Assuming value ''ask''.", policy, togglePreferenceKey));
        }
        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                pnlMessage,
                tr("Update plugins"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                ht("/Preferences/Plugins#AutomaticUpdate")
        );

        if (pnlMessage.isRememberDecision()) {
            switch(ret) {
            case 0:
                Main.pref.put(togglePreferenceKey, "always");
                break;
            case JOptionPane.CLOSED_OPTION:
            case 1:
                Main.pref.put(togglePreferenceKey, "never");
                break;
            }
        } else {
            Main.pref.put(togglePreferenceKey, "ask");
        }
        return ret == 0;
    }

    /**
     * Alerts the user if a plugin required by another plugin is missing
     * 
     * @param plugin the the plugin
     * @param missingRequiredPlugin the missing required plugin
     */
    private static void alertMissingRequiredPlugin(Window parent, String plugin, Set<String> missingRequiredPlugin) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("Plugin {0} requires a plugin which was not found. The missing plugin is:",
                "Plugin {0} requires {1} plugins which were not found. The missing plugins are:",
                missingRequiredPlugin.size(),
                plugin,
                missingRequiredPlugin.size()
        ));
        sb.append("<ul>");
        for (String p: missingRequiredPlugin) {
            sb.append("<li>").append(p).append("</li>");
        }
        sb.append("</ul>").append("</html>");
        JOptionPane.showMessageDialog(
                parent,
                sb.toString(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static void alertJOSMUpdateRequired(Window parent, String plugin, int requiredVersion) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>Plugin {0} requires JOSM version {1}. The current JOSM version is {2}.<br>"
                        +"You have to update JOSM in order to use this plugin.</html>",
                        plugin, requiredVersion, Version.getInstance().getVersion()
                ),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Plugin/Loading#JOSMUpdateRequired")
        );
    }

    /**
     * Checks whether all preconditions for loading the plugin <code>plugin</code> are met. The
     * current JOSM version must be compatible with the plugin and no other plugins this plugin
     * depends on should be missing.
     * 
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @return true, if the preconditions are met; false otherwise
     */
    public static boolean checkLoadPreconditions(Window parent, Collection<PluginInformation> plugins, PluginInformation plugin) {

        // make sure the plugin is compatible with the current JOSM version
        //
        int josmVersion = Version.getInstance().getVersion();
        if (plugin.mainversion > josmVersion && josmVersion != Version.JOSM_UNKNOWN_VERSION) {
            alertJOSMUpdateRequired(parent, plugin.name, plugin.mainversion);
            return false;
        }

        // make sure the dependencies to other plugins are not broken
        //
        if(plugin.requires != null){
            Set<String> pluginNames = new HashSet<String>();
            for (PluginInformation pi: plugins) {
                pluginNames.add(pi.name);
            }
            Set<String> missingPlugins = new HashSet<String>();
            for (String requiredPlugin : plugin.requires.split(";")) {
                if (!pluginNames.contains(requiredPlugin)) {
                    missingPlugins.add(requiredPlugin);
                }
            }
            if (!missingPlugins.isEmpty()) {
                alertMissingRequiredPlugin(parent, plugin.name, missingPlugins);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a class loader for loading plugin code.
     * 
     * @param plugins the collection of plugins which are going to be loaded with this
     * class loader
     * @return the class loader
     */
    public static ClassLoader createClassLoader(Collection<PluginInformation> plugins) {
        // iterate all plugins and collect all libraries of all plugins:
        List<URL> allPluginLibraries = new LinkedList<URL>();
        File pluginDir = Main.pref.getPluginsDirectory();
        for (PluginInformation info : plugins) {
            if (info.libraries == null) {
                continue;
            }
            allPluginLibraries.addAll(info.libraries);
            File pluginJar = new File(pluginDir, info.name + ".jar");
            URL pluginJarUrl = PluginInformation.fileToURL(pluginJar);
            allPluginLibraries.add(pluginJarUrl);
        }

        // create a classloader for all plugins:
        URL[] jarUrls = new URL[allPluginLibraries.size()];
        jarUrls = allPluginLibraries.toArray(jarUrls);
        URLClassLoader pluginClassLoader = new URLClassLoader(jarUrls, Main.class.getClassLoader());
        return pluginClassLoader;
    }

    /**
     * Loads and instantiates the plugin described by <code>plugin</code> using
     * the class loader <code>pluginClassLoader</code>.
     * 
     * @param plugin the plugin
     * @param pluginClassLoader the plugin class loader
     */
    public static void loadPlugin(Window parent, PluginInformation plugin, ClassLoader pluginClassLoader) {
        try {
            Class<?> klass = plugin.loadClass(pluginClassLoader);
            if (klass != null) {
                System.out.println(tr("loading plugin ''{0}'' ({1})", plugin.name, plugin.localversion));
                pluginList.add(plugin.load(klass));
            }
        } catch(PluginException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                e.printStackTrace();
                String msg = tr("<html>Could not load plugin {0} because the plugin<br>main class ''{1}'' was not found.<br>"
                        + "Delete from preferences?", plugin.name, plugin.className);
                if (confirmDisablePlugin(parent, msg, plugin.name)) {
                    Main.pref.removeFromCollection("plugins", plugin.name);
                }
            }
        }  catch (Throwable e) {
            e.printStackTrace();
            String msg = tr("Could not load plugin {0}. Delete from preferences?", plugin.name);
            if (confirmDisablePlugin(parent, msg, plugin.name)) {
                Main.pref.removeFromCollection("plugins", plugin.name);
            }
        }
    }

    /**
     * Loads the plugin in <code>plugins</code> from locally available jar files into
     * memory.
     * 
     * @param plugins the list of plugins
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadPlugins(Window parent,Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Loading plugins ..."));
            monitor.subTask(tr("Checking plugin preconditions..."));
            List<PluginInformation> toLoad = new LinkedList<PluginInformation>();
            for (PluginInformation pi: plugins) {
                if (checkLoadPreconditions(parent, plugins, pi)) {
                    toLoad.add(pi);
                }
            }
            // sort the plugins according to their "staging" equivalence class. The
            // lower the value of "stage" the earlier the plugin should be loaded.
            //
            Collections.sort(
                    toLoad,
                    new Comparator<PluginInformation>() {
                        public int compare(PluginInformation o1, PluginInformation o2) {
                            if (o1.stage < o2.stage) return -1;
                            if (o1.stage == o2.stage) return 0;
                            return 1;
                        }
                    }
            );
            if (toLoad.isEmpty())
                return;

            ClassLoader pluginClassLoader = createClassLoader(toLoad);
            ImageProvider.sources.add(0, pluginClassLoader);
            monitor.setTicksCount(toLoad.size());
            for (PluginInformation info : toLoad) {
                monitor.setExtraText(tr("Loading plugin ''{0}''...", info.name));
                loadPlugin(parent, info, pluginClassLoader);
                monitor.worked(1);
            }
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@see PluginInformation#early}
     * set to true.
     * 
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadEarlyPlugins(Window parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> earlyPlugins = new ArrayList<PluginInformation>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (pi.early) {
                earlyPlugins.add(pi);
            }
        }
        loadPlugins(parent, earlyPlugins, monitor);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@see PluginInformation#early}
     * set to false.
     * 
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadLatePlugins(Window parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> latePlugins = new ArrayList<PluginInformation>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (!pi.early) {
                latePlugins.add(pi);
            }
        }
        loadPlugins(parent, latePlugins, monitor);
    }

    /**
     * Loads locally available plugin information from local plugin jars and from cached
     * plugin lists.
     *
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     * @return the list of locally available plugin information
     * 
     */
    private static Map<String, PluginInformation> loadLocallyAvailablePluginInformation(ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            ReadLocalPluginInformationTask task = new ReadLocalPluginInformationTask(monitor);
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<?> future = service.submit(task);
            try {
                future.get();
            } catch(ExecutionException e) {
                e.printStackTrace();
                return null;
            } catch(InterruptedException e) {
                e.printStackTrace();
                return null;
            }
            HashMap<String, PluginInformation> ret = new HashMap<String, PluginInformation>();
            for (PluginInformation pi: task.getAvailablePlugins()) {
                ret.put(pi.name, pi);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertMissingPluginInformation(Window parent, Collection<String> plugins) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("JOSM could not find information about the following plugin:",
                "JOSM could not find information about the following plugins:",
                plugins.size()));
        sb.append("<ul>");
        for (String plugin: plugins) {
            sb.append("<li>").append(plugin).append("</li>");
        }
        sb.append("</ul>");
        sb.append(trn("The plugin is not going to be loaded.",
                "The plugins are not going to be loaded.",
                plugins.size()));
        sb.append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Plugin/Loading#MissingPluginInfos")
        );
    }

    /**
     * Builds the set of plugins to load. Deprecated and unmaintained plugins are filtered
     * out. This involves user interaction. This method displays alert and confirmation
     * messages.
     *
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     * @return the set of plugins to load (as set of plugin names)
     */
    public static List<PluginInformation> buildListOfPluginsToLoad(Window parent, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Determine plugins to load..."));
            Set<String> plugins = new HashSet<String>();
            plugins.addAll(Main.pref.getCollection("plugins",  new LinkedList<String>()));
            if (System.getProperty("josm.plugins") != null) {
                plugins.addAll(Arrays.asList(System.getProperty("josm.plugins").split(",")));
            }
            monitor.subTask(tr("Removing deprecated plugins..."));
            filterDeprecatedPlugins(parent, plugins);
            monitor.subTask(tr("Removing unmaintained plugins..."));
            filterUnmaintainedPlugins(parent, plugins);
            Map<String, PluginInformation> infos = loadLocallyAvailablePluginInformation(monitor.createSubTaskMonitor(1,false));
            List<PluginInformation> ret = new LinkedList<PluginInformation>();
            for (Iterator<String> it = plugins.iterator(); it.hasNext();) {
                String plugin = it.next();
                if (infos.containsKey(plugin)) {
                    ret.add(infos.get(plugin));
                    it.remove();
                }
            }
            if (!plugins.isEmpty()) {
                alertMissingPluginInformation(parent, plugins);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertFailedPluginUpdate(Window parent, Collection<PluginInformation> plugins) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append(trn(
                "Updating the following plugin has failed:",
                "Updating the following plugins has failed:",
                plugins.size()
        )
        );
        sb.append("<ul>");
        for (PluginInformation pi: plugins) {
            sb.append("<li>").append(pi.name).append("</li>");
        }
        sb.append("</ul>");
        sb.append(trn(
                "Please open the Preference Dialog after JOSM has started and try to update it manually.",
                "Please open the Preference Dialog after JOSM has started and try to update them manually.",
                plugins.size()
        ));
        sb.append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Plugin update failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Plugin/Loading#FailedPluginUpdated")
        );
    }

    /**
     * Updates the plugins in <code>plugins</code>.
     * 
     * @param parent the parent window for message boxes
     * @param plugins the collection of plugins to update. Must not be null.
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     * @throws IllegalArgumentException thrown if plugins is null
     */
    public static void updatePlugins(Window parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(plugins, "plugins");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            ExecutorService service = Executors.newSingleThreadExecutor();

            // try to download the plugin lists
            //
            ReadRemotePluginInformationTask task1 = new ReadRemotePluginInformationTask(
                    monitor.createSubTaskMonitor(1,false),
                    Main.pref.getPluginSites()
            );
            Future<?> future = service.submit(task1);
            try {
                future.get();
            } catch(ExecutionException e) {
                System.out.println(tr("Warning: failed to download plugin information list"));
                e.printStackTrace();
                // don't abort in case of error, continue with downloading plugins below
            } catch(InterruptedException e) {
                System.out.println(tr("Warning: failed to download plugin information list"));
                e.printStackTrace();
                // don't abort in case of error, continue with downloading plugins below
            }

            // filter plugins which actually have to be updated
            //
            Collection<PluginInformation> pluginsToUpdate = new ArrayList<PluginInformation>();
            for(PluginInformation pi: plugins) {
                if (pi.isUpdateRequired()) {
                    pluginsToUpdate.add(pi);
                }
            }

            if (!pluginsToUpdate.isEmpty()) {
                // try to update the locally installed plugins
                //
                PluginDownloadTask task2 = new PluginDownloadTask(
                        monitor.createSubTaskMonitor(1,false),
                        pluginsToUpdate,
                        tr("Update plugins")
                );

                future = service.submit(task2);
                try {
                    future.get();
                } catch(ExecutionException e) {
                    e.printStackTrace();
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return;
                } catch(InterruptedException e) {
                    e.printStackTrace();
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return;
                }
                // notify user if downloading a locally installed plugin failed
                //
                if (! task2.getFailedPlugins().isEmpty()) {
                    alertFailedPluginUpdate(parent, task2.getFailedPlugins());
                    return;
                }
            }
        } finally {
            monitor.finishTask();
        }
        // remember the update because it was successful
        //
        Main.pref.putInteger("pluginmanager.version", Version.getInstance().getVersion());
        Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
    }

    /**
     * Ask the user for confirmation that a plugin shall be disabled.
     * 
     * @param reason the reason for disabling the plugin
     * @param name the plugin name
     * @return true, if the plugin shall be disabled; false, otherwise
     */
    public static boolean confirmDisablePlugin(Window parent, String reason, String name) {
        ButtonSpec [] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Disable plugin"),
                        ImageProvider.get("dialogs", "delete"),
                        tr("Click to delete the plugin ''{0}''", name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        ImageProvider.get("cancel"),
                        tr("Click to keep the plugin ''{0}''", name),
                        null /* no specific help context */
                )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                reason,
                tr("Disable plugin"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                null // FIXME: add help topic
        );
        return ret == 0;
    }

    /**
     * Notified loaded plugins about a new map frame
     * 
     * @param old the old map frame
     * @param map the new map frame
     */
    public static void notifyMapFrameChanged(MapFrame old, MapFrame map) {
        for (PluginProxy plugin : pluginList) {
            plugin.mapFrameInitialized(old, map);
        }
    }

    public static Object getPlugin(String name) {
        for (PluginProxy plugin : pluginList)
            if(plugin.getPluginInformation().name.equals(name))
                return plugin.plugin;
        return null;
    }

    public static void addDownloadSelection(List<DownloadSelection> downloadSelections) {
        for (PluginProxy p : pluginList) {
            p.addDownloadSelection(downloadSelections);
        }
    }

    public static void getPreferenceSetting(Collection<PreferenceSettingFactory> settings) {
        for (PluginProxy plugin : pluginList) {
            settings.add(new PluginPreferenceFactory(plugin));
        }
    }

    /**
     * Installs downloaded plugins. Moves files with the suffix ".jar.new" to the corresponding
     * ".jar" files.
     * 
     * If {@code dowarn} is true, this methods emits warning messages on the console if a downloaded
     * but not yet installed plugin .jar can't be be installed. If {@code dowarn} is false, the
     * installation of the respective plugin is sillently skipped.
     * 
     * @param dowarn if true, warning messages are displayed; false otherwise
     */
    public static void installDownloadedPlugins(boolean dowarn) {
        File pluginDir = Main.pref.getPluginsDirectory();
        if (! pluginDir.exists() || ! pluginDir.isDirectory() || ! pluginDir.canWrite())
            return;

        final File[] files = pluginDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar.new");
            }});

        for (File updatedPlugin : files) {
            final String filePath = updatedPlugin.getPath();
            File plugin = new File(filePath.substring(0, filePath.length() - 4));
            String pluginName = updatedPlugin.getName().substring(0, updatedPlugin.getName().length() - 8);
            if (plugin.exists()) {
                if (!plugin.delete() && dowarn) {
                    System.err.println(tr("Warning: failed to delete outdated plugin ''{0}''.", plugin.toString()));
                    System.err.println(tr("Warning: failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM is still going to load the old plugin version.", pluginName));
                    continue;
                }
            }
            if (!updatedPlugin.renameTo(plugin) && dowarn) {
                System.err.println(tr("Warning: failed to install plugin ''{0}'' from temporary download file ''{1}''. Renaming failed.", plugin.toString(), updatedPlugin.toString()));
                System.err.println(tr("Warning: failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM is still going to load the old plugin version.", pluginName));
            }
        }
        return;
    }

    private static boolean confirmDeactivatingPluginAfterException(PluginProxy plugin) {
        ButtonSpec [] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Disable plugin"),
                        ImageProvider.get("dialogs", "delete"),
                        tr("Click to disable the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        ImageProvider.get("cancel"),
                        tr("Click to keep the plugin ''{0}''",plugin.getPluginInformation().name),
                        null /* no specific help context */
                )
        };

        StringBuffer msg = new StringBuffer();
        msg.append("<html>");
        msg.append(tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.", plugin.getPluginInformation().name));
        msg.append("<br>");
        if(plugin.getPluginInformation().author != null) {
            msg.append(tr("According to the information within the plugin, the author is {0}.", plugin.getPluginInformation().author));
            msg.append("<br>");
        }
        msg.append(tr("Try updating to the newest version of this plugin before reporting a bug."));
        msg.append("<br>");
        msg.append(tr("Should the plugin be disabled?"));
        msg.append("</html>");

        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg.toString(),
                tr("Update plugins"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0],
                ht("/ErrorMessages#ErrorInPlugin")
        );
        return ret == 0;
    }

    /**
     * Replies the plugin which most likely threw the exception <code>ex</code>.
     * 
     * @param ex the exception
     * @return the plugin; null, if the exception proably wasn't thrown from a plugin
     */
    private static PluginProxy getPluginCausingException(Throwable ex) {
        for (PluginProxy p : pluginList) {
            String baseClass = p.getPluginInformation().className;
            int i = baseClass.lastIndexOf(".");
            baseClass = baseClass.substring(0, i);
            for (StackTraceElement element : ex.getStackTrace()) {
                String c = element.getClassName();
                if (c.startsWith(baseClass))
                    return p;
            }
        }
        return null;
    }

    /**
     * Checks whether the exception <code>e</code> was thrown by a plugin. If so,
     * conditionally deactivates the plugin, but asks the user first.
     * 
     * @param e the exception
     */
    public static void disablePluginAfterException(Throwable e) {
        PluginProxy plugin = null;
        // Check for an explicit problem when calling a plugin function
        if (e instanceof PluginException) {
            plugin = ((PluginException) e).plugin;
        }
        if (plugin == null) {
            plugin = getPluginCausingException(e);
        }
        if (plugin == null)
            // don't know what plugin threw the exception
            return;

        Set<String> plugins = new HashSet<String>(
                Main.pref.getCollection("plugins",Collections.<String> emptySet())
        );
        if (! plugins.contains(plugin.getPluginInformation().name))
            // plugin not activated ? strange in this context but anyway, don't bother
            // the user with dialogs, skip conditional deactivation
            return;

        if (!confirmDeactivatingPluginAfterException(plugin))
            // user doesn't want to deactivate the plugin
            return;

        // deactivate the plugin
        plugins.remove(plugin.getPluginInformation().name);
        Main.pref.putCollection("plugins", plugins);
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."),
                tr("Information"),
                JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    public static String getBugReportText() {
        String text = "";
        String pl = Main.pref.getCollectionAsString("plugins");
        if (pl != null && pl.length() != 0) {
            text += "Plugins: " + pl + "\n";
        }
        for (final PluginProxy pp : pluginList) {
            text += "Plugin "
                + pp.getPluginInformation().name
                + (pp.getPluginInformation().version != null && !pp.getPluginInformation().version.equals("") ? " Version: " + pp.getPluginInformation().version + "\n"
                        : "\n");
        }
        return text;
    }

    public static JPanel getInfoPanel() {
        JPanel pluginTab = new JPanel(new GridBagLayout());
        for (final PluginProxy p : pluginList) {
            final PluginInformation info = p.getPluginInformation();
            String name = info.name
            + (info.version != null && !info.version.equals("") ? " Version: " + info.version : "");
            pluginTab.add(new JLabel(name), GBC.std());
            pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
            pluginTab.add(new JButton(new AbstractAction(tr("Information")) {
                public void actionPerformed(ActionEvent event) {
                    StringBuilder b = new StringBuilder();
                    for (Entry<String, String> e : info.attr.entrySet()) {
                        b.append(e.getKey());
                        b.append(": ");
                        b.append(e.getValue());
                        b.append("\n");
                    }
                    JTextArea a = new JTextArea(10, 40);
                    a.setEditable(false);
                    a.setText(b.toString());
                    JOptionPane.showMessageDialog(Main.parent, new JScrollPane(a), tr("Plugin information"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }), GBC.eol());

            JTextArea description = new JTextArea((info.description == null ? tr("no description available")
                    : info.description));
            description.setEditable(false);
            description.setFont(new JLabel().getFont().deriveFont(Font.ITALIC));
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            description.setBackground(UIManager.getColor("Panel.background"));

            pluginTab.add(description, GBC.eop().fill(GBC.HORIZONTAL));
        }
        return pluginTab;
    }

    static private class UpdatePluginsMessagePanel extends JPanel {
        private JMultilineLabel lblMessage;
        private JCheckBox cbDontShowAgain;

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.insets = new Insets(5,5,5,5);
            add(lblMessage = new JMultilineLabel(""), gc);
            lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN));

            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weighty = 0.0;
            add(cbDontShowAgain = new JCheckBox(tr("Do not ask again and remember my decision (go to Preferences->Plugins to change it later)")), gc);
            cbDontShowAgain.setFont(cbDontShowAgain.getFont().deriveFont(Font.PLAIN));
        }

        public UpdatePluginsMessagePanel() {
            build();
        }

        public void setMessage(String message) {
            lblMessage.setText(message);
        }

        public void initDontShowAgain(String preferencesKey) {
            String policy = Main.pref.get(preferencesKey, "ask");
            policy = policy.trim().toLowerCase();
            cbDontShowAgain.setSelected(! policy.equals("ask"));
        }

        public boolean isRememberDecision() {
            return cbDontShowAgain.isSelected();
        }
    }
}
