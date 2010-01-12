package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.GridBagLayout;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
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
    private static void filterDeprecatedPlugins(Collection<String> plugins) {
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
                Main.parent,
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
    private static void filterUnmaintainedPlugins(Collection<String> plugins) {
        for (String unmaintained : UNMAINTAINED_PLUGINS) {
            if (!plugins.contains(unmaintained)) {
                continue;
            }
            String msg =  tr("<html>Loading of {0} plugin was requested."
                    + "<br>This plugin is no longer developed and very likely will produce errors."
                    +"<br>It should be disabled.<br>Delete from preferences?</html>", unmaintained);
            if (confirmDisablePlugin(msg,unmaintained)) {
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
     * @return true if a plugin update should be run; false, otherwise
     */
    public static boolean checkAndConfirmPluginUpdate() {
        String message = null;
        String togglePreferenceKey = null;
        int v = Version.getInstance().getVersion();
        if (Main.pref.getInteger("pluginmanager.version", 0) < v) {
            message = tr("<html>You updated your JOSM software.<br>"
                    + "To prevent problems the plugins should be updated as well.<br><br>"
                    + "Update plugins now?"
                    + "</html>"
            );
            togglePreferenceKey = "pluginmanger.version";
        }  else {
            long tim = System.currentTimeMillis();
            long last = Main.pref.getLong("pluginmanager.lastupdate", 0);
            Integer maxTime = Main.pref.getInteger("pluginmanager.warntime", 60);
            long d = (tim - last) / (24 * 60 * 60 * 1000l);
            if ((last <= 0) || (maxTime <= 0)) {
                Main.pref.put("pluginmanager.lastupdate", Long.toString(tim));
            } else if (d > maxTime) {
                message = tr("Last plugin update more than {0} days ago.", d);
                togglePreferenceKey = "pluginmanager.time";
            }
        }
        if (message == null) return false;

        // ask whether update is fine
        //
        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                tr("Update plugins"),
                new String[] {
                    tr("Update plugins"), tr("Skip update")
                }
        );
        dialog.setContent(message);
        dialog.toggleEnable(togglePreferenceKey);
        dialog.setButtonIcons( new String[] {"dialogs/refresh.png", "cancel.png"});
        dialog.configureContextsensitiveHelp(ht("/Plugin/AutomaticUpdate"), true /* show help button */);
        dialog.showDialog();
        return dialog.getValue() == 1;
    }

    /**
     * Alerts the user if a plugin required by another plugin is missing
     * 
     * @param plugin the the plugin
     * @param missingRequiredPlugin the missing required plugin
     */
    private static void alertMissingRequiredPlugin(String plugin, Set<String> missingRequiredPlugin) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("A required plugin for plugin {0} was not found. The required plugin is:",
                "{1} required plugins for plugin {0} were not found. The required plugins are:",
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
                Main.parent,
                sb.toString(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
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
    public static boolean checkLoadPreconditions(Collection<PluginInformation> plugins, PluginInformation plugin) {

        // make sure the plugin is compatible with the current JOSM version
        //
        int josmVersion = Version.getInstance().getVersion();
        if (plugin.mainversion > josmVersion && josmVersion != Version.JOSM_UNKNOWN_VERSION) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Plugin {0} requires JOSM update to version {1}.", plugin.name,
                            plugin.mainversion),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
            );
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
                alertMissingRequiredPlugin(plugin.name, missingPlugins);
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
    public static void loadPlugin(PluginInformation plugin, ClassLoader pluginClassLoader) {
        try {
            Class<?> klass = plugin.loadClass(pluginClassLoader);
            if (klass != null) {
                System.out.println(tr("loading plugin ''{0}''", plugin.name));
                pluginList.add(plugin.load(klass));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            String msg = tr("Could not load plugin {0}. Delete from preferences?", plugin.name);
            if (confirmDisablePlugin(msg, plugin.name)) {
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
    public static void loadPlugins(Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Loading plugins ..."));
            List<PluginInformation> toLoad = new LinkedList<PluginInformation>();
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
            monitor.subTask(tr("Checking plugin preconditions..."));
            for (PluginInformation pi: plugins) {
                if (checkLoadPreconditions(plugins, pi)) {
                    toLoad.add(pi);
                }
            }
            if (toLoad.isEmpty())
                return;

            ClassLoader pluginClassLoader = createClassLoader(toLoad);
            ImageProvider.sources.add(0, pluginClassLoader);
            monitor.setTicksCount(toLoad.size());
            for (PluginInformation info : toLoad) {
                monitor.setExtraText(tr("Loading plugin ''{0}''...", info.name));
                loadPlugin(info, pluginClassLoader);
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
    public static void loadEarlyPlugins(Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> earlyPlugins = new ArrayList<PluginInformation>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (pi.early) {
                earlyPlugins.add(pi);
            }
        }
        loadPlugins(earlyPlugins, monitor);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@see PluginInformation#early}
     * set to false.
     * 
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadLatePlugins(Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> latePlugins = new ArrayList<PluginInformation>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (!pi.early) {
                latePlugins.add(pi);
            }
        }
        loadPlugins(latePlugins, monitor);
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

    private static void alertMissingPluginInformation(Collection<String> plugins) {
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
        JOptionPane.showMessageDialog(
                Main.parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE
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
    public static List<PluginInformation> buildListOfPluginsToLoad(ProgressMonitor monitor) {
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
            filterDeprecatedPlugins(plugins);
            monitor.subTask(tr("Removing umaintained plugins..."));
            filterUnmaintainedPlugins(plugins);
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
                alertMissingPluginInformation(plugins);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertFailedPluginUpdate(Collection<PluginInformation> plugins) {
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
        sb.append(tr("Please open the Preference Dialog after JOSM has started and try to update them manually."));
        sb.append("</html>");
        JOptionPane.showMessageDialog(
                Main.parent,
                sb.toString(),
                tr("Plugin update failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Updates the plugins in <code>plugins</code>.
     * 
     * @param plugins the collection of plugins to update. Must not be null.
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null.
     * @throws IllegalArgumentException thrown if plugins is null
     */
    public static void updatePlugins(Collection<PluginInformation> plugins, ProgressMonitor monitor) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(plugins, "plugins");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            PluginDownloadTask task = new PluginDownloadTask(
                    monitor,
                    plugins,
                    tr("Update plugins")
            );
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<?> future = service.submit(task);
            try {
                future.get();
            } catch(ExecutionException e) {
                e.printStackTrace();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            if (! task.getFailedPlugins().isEmpty()) {
                alertFailedPluginUpdate(task.getFailedPlugins());
                return;
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
    public static boolean confirmDisablePlugin(String reason, String name) {
        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                tr("Disable plugin"),
                new String[] {
                    tr("Disable plugin"), tr("Keep plugin")
                }
        );
        dialog.setContent(reason);
        dialog.setButtonIcons(new String[] { "dialogs/delete.png", "cancel.png" });
        dialog.showDialog();
        return dialog.getValue() == 1;
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
     */
    public static void installDownloadedPlugins() {
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
                if (!plugin.delete()) {
                    System.err.println(tr("Warning: failed to delete outdated plugin ''{0}''.", plugin.toString()));
                    System.err.println(tr("Warning: failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM still going to load the old plugin version.", pluginName));
                    continue;
                }
            }
            if (!updatedPlugin.renameTo(plugin)) {
                System.err.println(tr("Warning: failed to install plugin ''{0}'' from temporary download file ''{1}''. Renaming failed.", plugin.toString(), updatedPlugin.toString()));
                System.err.println(tr("Warning: failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM still going to load the old plugin version.", pluginName));
            }
        }
        return;
    }

    public static boolean checkException(Throwable e)
    {
        PluginProxy plugin = null;

        // Check for an explicit problem when calling a plugin function
        if (e instanceof PluginException) {
            plugin = ((PluginException)e).plugin;
        }

        if (plugin == null)
        {
            /**
             * Analyze the stack of the argument and find a name of a plugin, if
             * some known problem pattern has been found.
             */
            for (PluginProxy p : pluginList)
            {
                String baseClass = p.getPluginInformation().className;
                int i = baseClass.lastIndexOf(".");
                baseClass = baseClass.substring(0, i);
                for (StackTraceElement element : e.getStackTrace())
                {
                    String c = element.getClassName();
                    if(c.startsWith(baseClass))
                    {
                        plugin = p;
                        break;
                    }
                }
                if(plugin != null) {
                    break;
                }
            }
        }

        if (plugin != null) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Disable plugin"),
                    new String[] {tr("Disable plugin"), tr("Cancel")}
            );
            dialog.setButtonIcons(new String[] {"dialogs/delete.png", "cancel.png"});
            dialog.setContent(
                    tr("<html>") +
                    tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.", plugin.getPluginInformation().name)
                    + "<br>"
                    + (plugin.getPluginInformation().author != null
                            ? tr("According to the information within the plugin, the author is {0}.", plugin.getPluginInformation().author)
                                    : "")
                                    + "<br>"
                                    + tr("Try updating to the newest version of this plugin before reporting a bug.")
                                    + "<br>"
                                    + tr("Should the plugin be disabled?")
                                    + "</html>"
            );
            dialog.showDialog();
            int answer = dialog.getValue();

            if (answer == 1) {
                List<String> plugins = new ArrayList<String>(Main.pref.getCollection("plugins", Collections.<String>emptyList()));
                if (plugins.contains(plugin.getPluginInformation().name)) {
                    while (plugins.remove(plugin.getPluginInformation().name)) {}
                    Main.pref.putCollection("plugins", plugins);
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("The plugin could not be removed. Probably it was already disabled"),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                return true;
            }
        }
        return false;
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
}
