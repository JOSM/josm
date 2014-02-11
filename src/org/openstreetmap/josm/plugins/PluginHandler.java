// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.jar.JarFile;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * PluginHandler is basically a collection of static utility functions used to bootstrap
 * and manage the loaded plugins.
 * @since 1326
 */
public final class PluginHandler {

    /**
     * Deprecated plugins that are removed on start
     */
    public final static Collection<DeprecatedPlugin> DEPRECATED_PLUGINS;
    static {
        String IN_CORE = tr("integrated into main program");

        DEPRECATED_PLUGINS = Arrays.asList(new DeprecatedPlugin[] {
            new DeprecatedPlugin("mappaint", IN_CORE),
            new DeprecatedPlugin("unglueplugin", IN_CORE),
            new DeprecatedPlugin("lang-de", IN_CORE),
            new DeprecatedPlugin("lang-en_GB", IN_CORE),
            new DeprecatedPlugin("lang-fr", IN_CORE),
            new DeprecatedPlugin("lang-it", IN_CORE),
            new DeprecatedPlugin("lang-pl", IN_CORE),
            new DeprecatedPlugin("lang-ro", IN_CORE),
            new DeprecatedPlugin("lang-ru", IN_CORE),
            new DeprecatedPlugin("ewmsplugin", IN_CORE),
            new DeprecatedPlugin("ywms", IN_CORE),
            new DeprecatedPlugin("tways-0.2", IN_CORE),
            new DeprecatedPlugin("geotagged", IN_CORE),
            new DeprecatedPlugin("landsat", tr("replaced by new {0} plugin","lakewalker")),
            new DeprecatedPlugin("namefinder", IN_CORE),
            new DeprecatedPlugin("waypoints", IN_CORE),
            new DeprecatedPlugin("slippy_map_chooser", IN_CORE),
            new DeprecatedPlugin("tcx-support", tr("replaced by new {0} plugin","dataimport")),
            new DeprecatedPlugin("usertools", IN_CORE),
            new DeprecatedPlugin("AgPifoJ", IN_CORE),
            new DeprecatedPlugin("utilsplugin", IN_CORE),
            new DeprecatedPlugin("ghost", IN_CORE),
            new DeprecatedPlugin("validator", IN_CORE),
            new DeprecatedPlugin("multipoly", IN_CORE),
            new DeprecatedPlugin("multipoly-convert", IN_CORE),
            new DeprecatedPlugin("remotecontrol", IN_CORE),
            new DeprecatedPlugin("imagery", IN_CORE),
            new DeprecatedPlugin("slippymap", IN_CORE),
            new DeprecatedPlugin("wmsplugin", IN_CORE),
            new DeprecatedPlugin("ParallelWay", IN_CORE),
            new DeprecatedPlugin("dumbutils", tr("replaced by new {0} plugin","utilsplugin2")),
            new DeprecatedPlugin("ImproveWayAccuracy", IN_CORE),
            new DeprecatedPlugin("Curves", tr("replaced by new {0} plugin","utilsplugin2")),
            new DeprecatedPlugin("epsg31287", tr("replaced by new {0} plugin", "proj4j")),
            new DeprecatedPlugin("licensechange", tr("no longer required")),
            new DeprecatedPlugin("restart", IN_CORE),
            new DeprecatedPlugin("wayselector", IN_CORE),
        });
    }

    private PluginHandler() {
        // Hide default constructor for utils classes
    }

    /**
     * Description of a deprecated plugin
     */
    public static class DeprecatedPlugin implements Comparable<DeprecatedPlugin> {
        /** Plugin name */
        public final String name;
        /** Short explanation about deprecation, can be {@code null} */
        public final String reason;
        /** Code to run to perform migration, can be {@code null} */
        private final Runnable migration;

        /**
         * Constructs a new {@code DeprecatedPlugin}.
         * @param name The plugin name
         */
        public DeprecatedPlugin(String name) {
            this(name, null, null);
        }

        /**
         * Constructs a new {@code DeprecatedPlugin} with a given reason.
         * @param name The plugin name
         * @param reason The reason about deprecation
         */
        public DeprecatedPlugin(String name, String reason) {
            this(name, reason, null);
        }

        /**
         * Constructs a new {@code DeprecatedPlugin}.
         * @param name The plugin name
         * @param reason The reason about deprecation
         * @param migration The code to run to perform migration
         */
        public DeprecatedPlugin(String name, String reason, Runnable migration) {
            this.name = name;
            this.reason = reason;
            this.migration = migration;
        }

        /**
         * Performs migration.
         */
        public void migrate() {
            if (migration != null) {
                migration.run();
            }
        }

        @Override
        public int compareTo(DeprecatedPlugin o) {
            return name.compareTo(o.name);
        }
    }

    /**
     * List of unmaintained plugins. Not really up-to-date as the vast majority of plugins are not really maintained after a few months, sadly...
     */
    public static final String [] UNMAINTAINED_PLUGINS = new String[] {"gpsbabelgui", "Intersect_way"};

    /**
     * Default time-based update interval, in days (pluginmanager.time-based-update.interval)
     */
    public static final int DEFAULT_TIME_BASED_UPDATE_INTERVAL = 30;

    /**
     * All installed and loaded plugins (resp. their main classes)
     */
    public final static Collection<PluginProxy> pluginList = new LinkedList<PluginProxy>();

    /**
     * Add here all ClassLoader whose resource should be searched.
     */
    private static final List<ClassLoader> sources = new LinkedList<ClassLoader>();

    static {
        try {
            sources.add(ClassLoader.getSystemClassLoader());
            sources.add(org.openstreetmap.josm.gui.MainApplication.class.getClassLoader());
        } catch (SecurityException ex) {
            sources.add(ImageProvider.class.getClassLoader());
        }
    }

    private static PluginDownloadTask pluginDownloadTask = null;

    public static Collection<ClassLoader> getResourceClassLoaders() {
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * Removes deprecated plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>.
     *
     * Also notifies the user about removed deprecated plugins
     *
     * @param parent The parent Component used to display warning popup
     * @param plugins the collection of plugins
     */
    private static void filterDeprecatedPlugins(Component parent, Collection<String> plugins) {
        Set<DeprecatedPlugin> removedPlugins = new TreeSet<DeprecatedPlugin>();
        for (DeprecatedPlugin depr : DEPRECATED_PLUGINS) {
            if (plugins.contains(depr.name)) {
                plugins.remove(depr.name);
                Main.pref.removeFromCollection("plugins", depr.name);
                removedPlugins.add(depr);
                depr.migrate();
            }
        }
        if (removedPlugins.isEmpty())
            return;

        // notify user about removed deprecated plugins
        //
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn(
                "The following plugin is no longer necessary and has been deactivated:",
                "The following plugins are no longer necessary and have been deactivated:",
                removedPlugins.size()
        ));
        sb.append("<ul>");
        for (DeprecatedPlugin depr: removedPlugins) {
            sb.append("<li>").append(depr.name);
            if (depr.reason != null) {
                sb.append(" (").append(depr.reason).append(")");
            }
            sb.append("</li>");
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
    private static void filterUnmaintainedPlugins(Component parent, Collection<String> plugins) {
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
     * @param parent the parent component relative to which the confirmation dialog
     * is to be displayed
     * @return true if a plugin update should be run; false, otherwise
     */
    public static boolean checkAndConfirmPluginUpdate(Component parent) {
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
            Integer maxTime = Main.pref.getInteger("pluginmanager.time-based-update.interval", DEFAULT_TIME_BASED_UPDATE_INTERVAL);
            long d = (tim - last) / (24 * 60 * 60 * 1000L);
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
                Main.info(tr("Skipping plugin update after JOSM upgrade. Automatic update at startup is disabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Skipping plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return false;
        }

        if (policy.equals("always")) {
            if ("pluginmanager.version-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Running plugin update after JOSM upgrade. Automatic update at startup is enabled."));
            } else if ("pluginmanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Main.info(tr("Running plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return true;
        }

        if (!policy.equals("ask")) {
            Main.warn(tr("Unexpected value ''{0}'' for preference ''{1}''. Assuming value ''ask''.", policy, togglePreferenceKey));
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
     * @param parent The parent Component used to display error popup
     * @param plugin the plugin
     * @param missingRequiredPlugin the missing required plugin
     */
    private static void alertMissingRequiredPlugin(Component parent, String plugin, Set<String> missingRequiredPlugin) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("Plugin {0} requires a plugin which was not found. The missing plugin is:",
                "Plugin {0} requires {1} plugins which were not found. The missing plugins are:",
                missingRequiredPlugin.size(),
                plugin,
                missingRequiredPlugin.size()
        ));
        sb.append(Utils.joinAsHtmlUnorderedList(missingRequiredPlugin));
        sb.append("</html>");
        JOptionPane.showMessageDialog(
                parent,
                sb.toString(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static void alertJOSMUpdateRequired(Component parent, String plugin, int requiredVersion) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>Plugin {0} requires JOSM version {1}. The current JOSM version is {2}.<br>"
                        +"You have to update JOSM in order to use this plugin.</html>",
                        plugin, Integer.toString(requiredVersion), Version.getInstance().getVersionString()
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
     * @param parent The parent Component used to display error popup
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @return true, if the preconditions are met; false otherwise
     */
    public static boolean checkLoadPreconditions(Component parent, Collection<PluginInformation> plugins, PluginInformation plugin) {

        // make sure the plugin is compatible with the current JOSM version
        //
        int josmVersion = Version.getInstance().getVersion();
        if (plugin.localmainversion > josmVersion && josmVersion != Version.JOSM_UNKNOWN_VERSION) {
            alertJOSMUpdateRequired(parent, plugin.name, plugin.localmainversion);
            return false;
        }

        // Add all plugins already loaded (to include early plugins when checking late ones)
        Collection<PluginInformation> allPlugins = new HashSet<PluginInformation>(plugins);
        for (PluginProxy proxy : pluginList) {
            allPlugins.add(proxy.getPluginInformation());
        }

        return checkRequiredPluginsPreconditions(parent, allPlugins, plugin, true);
    }

    /**
     * Checks if required plugins preconditions for loading the plugin <code>plugin</code> are met.
     * No other plugins this plugin depends on should be missing.
     *
     * @param parent The parent Component used to display error popup
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @param local Determines if the local or up-to-date plugin dependencies are to be checked.
     * @return true, if the preconditions are met; false otherwise
     * @since 5601
     */
    public static boolean checkRequiredPluginsPreconditions(Component parent, Collection<PluginInformation> plugins, PluginInformation plugin, boolean local) {

        String requires = local ? plugin.localrequires : plugin.requires;

        // make sure the dependencies to other plugins are not broken
        //
        if (requires != null) {
            Set<String> pluginNames = new HashSet<String>();
            for (PluginInformation pi: plugins) {
                pluginNames.add(pi.name);
            }
            Set<String> missingPlugins = new HashSet<String>();
            List<String> requiredPlugins = local ? plugin.getLocalRequiredPlugins() : plugin.getRequiredPlugins();
            for (String requiredPlugin : requiredPlugins) {
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

        // Add all plugins already loaded (to include early plugins in the classloader, allowing late plugins to rely on early ones)
        Collection<PluginInformation> allPlugins = new HashSet<PluginInformation>(plugins);
        for (PluginProxy proxy : pluginList) {
            allPlugins.add(proxy.getPluginInformation());
        }

        for (PluginInformation info : allPlugins) {
            if (info.libraries == null) {
                continue;
            }
            allPluginLibraries.addAll(info.libraries);
            File pluginJar = new File(pluginDir, info.name + ".jar");
            I18n.addTexts(pluginJar);
            URL pluginJarUrl = Utils.fileToURL(pluginJar);
            allPluginLibraries.add(pluginJarUrl);
        }

        // create a classloader for all plugins:
        final URL[] jarUrls = allPluginLibraries.toArray(new URL[allPluginLibraries.size()]);
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return new URLClassLoader(jarUrls, Main.class.getClassLoader());
            }
      });
    }

    /**
     * Loads and instantiates the plugin described by <code>plugin</code> using
     * the class loader <code>pluginClassLoader</code>.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugin the plugin
     * @param pluginClassLoader the plugin class loader
     */
    public static void loadPlugin(Component parent, PluginInformation plugin, ClassLoader pluginClassLoader) {
        String msg = tr("Could not load plugin {0}. Delete from preferences?", plugin.name);
        try {
            Class<?> klass = plugin.loadClass(pluginClassLoader);
            if (klass != null) {
                Main.info(tr("loading plugin ''{0}'' (version {1})", plugin.name, plugin.localversion));
                PluginProxy pluginProxy = plugin.load(klass);
                pluginList.add(pluginProxy);
                Main.addMapFrameListener(pluginProxy);
            }
            msg = null;
        } catch (PluginException e) {
            Main.error(e);
            if (e.getCause() instanceof ClassNotFoundException) {
                msg = tr("<html>Could not load plugin {0} because the plugin<br>main class ''{1}'' was not found.<br>"
                        + "Delete from preferences?</html>", plugin.name, plugin.className);
            }
        }  catch (Throwable e) {
            Main.error(e);
        }
        if (msg != null && confirmDisablePlugin(parent, msg, plugin.name)) {
            Main.pref.removeFromCollection("plugins", plugin.name);
        }
    }

    /**
     * Loads the plugin in <code>plugins</code> from locally available jar files into
     * memory.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the list of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadPlugins(Component parent,Collection<PluginInformation> plugins, ProgressMonitor monitor) {
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
                        @Override
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
            sources.add(0, pluginClassLoader);
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
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early}
     * set to true.
     *
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadEarlyPlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> earlyPlugins = new ArrayList<PluginInformation>(plugins.size());
        for (PluginInformation pi: plugins) {
            if (pi.early) {
                earlyPlugins.add(pi);
            }
        }
        loadPlugins(parent, earlyPlugins, monitor);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early}
     * set to false.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadLatePlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
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
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
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
                Main.error(e);
                return null;
            } catch(InterruptedException e) {
                Main.warn("InterruptedException in "+PluginHandler.class.getSimpleName()+" while loading locally available plugin information");
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

    private static void alertMissingPluginInformation(Component parent, Collection<String> plugins) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("JOSM could not find information about the following plugin:",
                "JOSM could not find information about the following plugins:",
                plugins.size()));
        sb.append(Utils.joinAsHtmlUnorderedList(plugins));
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
     * @param parent The parent component to be used for the displayed dialog
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the set of plugins to load (as set of plugin names)
     */
    public static List<PluginInformation> buildListOfPluginsToLoad(Component parent, ProgressMonitor monitor) {
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

    private static void alertFailedPluginUpdate(Component parent, Collection<PluginInformation> plugins) {
        StringBuilder sb = new StringBuilder();
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

    private static Set<PluginInformation> findRequiredPluginsToDownload(
            Collection<PluginInformation> pluginsToUpdate, List<PluginInformation> allPlugins, Set<PluginInformation> pluginsToDownload) {
        Set<PluginInformation> result = new HashSet<PluginInformation>();
        for (PluginInformation pi : pluginsToUpdate) {
            for (String name : pi.getRequiredPlugins()) {
                try {
                    PluginInformation installedPlugin = PluginInformation.findPlugin(name);
                    if (installedPlugin == null) {
                        // New required plugin is not installed, find its PluginInformation
                        PluginInformation reqPlugin = null;
                        for (PluginInformation pi2 : allPlugins) {
                            if (pi2.getName().equals(name)) {
                                reqPlugin = pi2;
                                break;
                            }
                        }
                        // Required plugin is known but not already on download list
                        if (reqPlugin != null && !pluginsToDownload.contains(reqPlugin)) {
                            result.add(reqPlugin);
                        }
                    }
                } catch (PluginException e) {
                    Main.warn(tr("Failed to find plugin {0}", name));
                    Main.error(e);
                }
            }
        }
        return result;
    }

    /**
     * Updates the plugins in <code>plugins</code>.
     *
     * @param parent the parent component for message boxes
     * @param pluginsWanted the collection of plugins to update. Updates all plugins if {@code null}
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     * @throws IllegalArgumentException thrown if plugins is null
     */
    public static Collection<PluginInformation> updatePlugins(Component parent,
            Collection<PluginInformation> pluginsWanted, ProgressMonitor monitor, boolean displayErrMsg)
            throws IllegalArgumentException {
        Collection<PluginInformation> plugins = null;
        pluginDownloadTask = null;
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
                    Main.pref.getPluginSites(), displayErrMsg
            );
            Future<?> future = service.submit(task1);
            List<PluginInformation> allPlugins = null;

            try {
                future.get();
                allPlugins = task1.getAvailablePlugins();
                plugins = buildListOfPluginsToLoad(parent,monitor.createSubTaskMonitor(1, false));
                // If only some plugins have to be updated, filter the list
                if (pluginsWanted != null && !pluginsWanted.isEmpty()) {
                    for (Iterator<PluginInformation> it = plugins.iterator(); it.hasNext();) {
                        PluginInformation pi = it.next();
                        boolean found = false;
                        for (PluginInformation piw : pluginsWanted) {
                            if (pi.name.equals(piw.name)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            it.remove();
                        }
                    }
                }
            } catch (ExecutionException e) {
                Main.warn(tr("Failed to download plugin information list")+": ExecutionException");
                Main.error(e);
                // don't abort in case of error, continue with downloading plugins below
            } catch (InterruptedException e) {
                Main.warn(tr("Failed to download plugin information list")+": InterruptedException");
                // don't abort in case of error, continue with downloading plugins below
            }

            // filter plugins which actually have to be updated
            //
            Collection<PluginInformation> pluginsToUpdate = new ArrayList<PluginInformation>();
            for (PluginInformation pi: plugins) {
                if (pi.isUpdateRequired()) {
                    pluginsToUpdate.add(pi);
                }
            }

            if (!pluginsToUpdate.isEmpty()) {

                Set<PluginInformation> pluginsToDownload = new HashSet<PluginInformation>(pluginsToUpdate);

                if (allPlugins != null) {
                    // Updated plugins may need additional plugin dependencies currently not installed
                    //
                    Set<PluginInformation> additionalPlugins = findRequiredPluginsToDownload(pluginsToUpdate, allPlugins, pluginsToDownload);
                    pluginsToDownload.addAll(additionalPlugins);

                    // Iterate on required plugins, if they need themselves another plugins (i.e A needs B, but B needs C)
                    while (!additionalPlugins.isEmpty()) {
                        // Install the additional plugins to load them later
                        plugins.addAll(additionalPlugins);
                        additionalPlugins = findRequiredPluginsToDownload(additionalPlugins, allPlugins, pluginsToDownload);
                        pluginsToDownload.addAll(additionalPlugins);
                    }
                }

                // try to update the locally installed plugins
                //
                pluginDownloadTask = new PluginDownloadTask(
                        monitor.createSubTaskMonitor(1,false),
                        pluginsToDownload,
                        tr("Update plugins")
                );

                future = service.submit(pluginDownloadTask);
                try {
                    future.get();
                } catch(ExecutionException e) {
                    Main.error(e);
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return plugins;
                } catch(InterruptedException e) {
                    Main.warn("InterruptedException in "+PluginHandler.class.getSimpleName()+" while updating plugins");
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return plugins;
                }

                // Update Plugin info for downloaded plugins
                //
                refreshLocalUpdatedPluginInfo(pluginDownloadTask.getDownloadedPlugins());

                // notify user if downloading a locally installed plugin failed
                //
                if (! pluginDownloadTask.getFailedPlugins().isEmpty()) {
                    alertFailedPluginUpdate(parent, pluginDownloadTask.getFailedPlugins());
                    return plugins;
                }
            }
        } finally {
            monitor.finishTask();
        }
        if (pluginsWanted == null) {
            // if all plugins updated, remember the update because it was successful
            //
            Main.pref.putInteger("pluginmanager.version", Version.getInstance().getVersion());
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
        }
        return plugins;
    }

    /**
     * Ask the user for confirmation that a plugin shall be disabled.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param reason the reason for disabling the plugin
     * @param name the plugin name
     * @return true, if the plugin shall be disabled; false, otherwise
     */
    public static boolean confirmDisablePlugin(Component parent, String reason, String name) {
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
     * Returns the plugin of the specified name.
     * @param name The plugin name
     * @return The plugin of the specified name, if installed and loaded, or {@code null} otherwise.
     */
    public static Object getPlugin(String name) {
        for (PluginProxy plugin : pluginList)
            if (plugin.getPluginInformation().name.equals(name))
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
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar.new");
            }});

        for (File updatedPlugin : files) {
            final String filePath = updatedPlugin.getPath();
            File plugin = new File(filePath.substring(0, filePath.length() - 4));
            String pluginName = updatedPlugin.getName().substring(0, updatedPlugin.getName().length() - 8);
            if (plugin.exists()) {
                if (!plugin.delete() && dowarn) {
                    Main.warn(tr("Failed to delete outdated plugin ''{0}''.", plugin.toString()));
                    Main.warn(tr("Failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM is still going to load the old plugin version.", pluginName));
                    continue;
                }
            }
            try {
                // Check the plugin is a valid and accessible JAR file before installing it (fix #7754)
                new JarFile(updatedPlugin).close();
            } catch (Exception e) {
                if (dowarn) {
                    Main.warn(tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. {2}", plugin.toString(), updatedPlugin.toString(), e.getLocalizedMessage()));
                }
                continue;
            }
            // Install plugin
            if (!updatedPlugin.renameTo(plugin) && dowarn) {
                Main.warn(tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. Renaming failed.", plugin.toString(), updatedPlugin.toString()));
                Main.warn(tr("Failed to install already downloaded plugin ''{0}''. Skipping installation. JOSM is still going to load the old plugin version.", pluginName));
            }
        }
        return;
    }

    /**
     * Determines if the specified file is a valid and accessible JAR file.
     * @param jar The fil to check
     * @return true if file can be opened as a JAR file.
     * @since 5723
     */
    public static boolean isValidJar(File jar) {
        if (jar != null && jar.exists() && jar.canRead()) {
            try {
                new JarFile(jar).close();
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Replies the updated jar file for the given plugin name.
     * @param name The plugin name to find.
     * @return the updated jar file for the given plugin name. null if not found or not readable.
     * @since 5601
     */
    public static File findUpdatedJar(String name) {
        File pluginDir = Main.pref.getPluginsDirectory();
        // Find the downloaded file. We have tried to install the downloaded plugins
        // (PluginHandler.installDownloadedPlugins). This succeeds depending on the platform.
        File downloadedPluginFile = new File(pluginDir, name + ".jar.new");
        if (!isValidJar(downloadedPluginFile)) {
            downloadedPluginFile = new File(pluginDir, name + ".jar");
            if (!isValidJar(downloadedPluginFile)) {
                return null;
            }
        }
        return downloadedPluginFile;
    }

    /**
     * Refreshes the given PluginInformation objects with new contents read from their corresponding jar file.
     * @param updatedPlugins The PluginInformation objects to update.
     * @since 5601
     */
    public static void refreshLocalUpdatedPluginInfo(Collection<PluginInformation> updatedPlugins) {
        if (updatedPlugins == null) return;
        for (PluginInformation pi : updatedPlugins) {
            File downloadedPluginFile = findUpdatedJar(pi.name);
            if (downloadedPluginFile == null) {
                continue;
            }
            try {
                pi.updateFromJar(new PluginInformation(downloadedPluginFile, pi.name));
            } catch(PluginException e) {
                Main.error(e);
            }
        }
    }

    private static int askUpdateDisableKeepPluginAfterException(PluginProxy plugin) {
        final ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Update plugin"),
                        ImageProvider.get("dialogs", "refresh"),
                        tr("Click to update the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
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

        final StringBuilder msg = new StringBuilder();
        msg.append("<html>");
        msg.append(tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.", plugin.getPluginInformation().name));
        msg.append("<br>");
        if (plugin.getPluginInformation().author != null) {
            msg.append(tr("According to the information within the plugin, the author is {0}.", plugin.getPluginInformation().author));
            msg.append("<br>");
        }
        msg.append(tr("Try updating to the newest version of this plugin before reporting a bug."));
        msg.append("</html>");

        try {
            FutureTask<Integer> task = new FutureTask<Integer>(new Callable<Integer>() {
                @Override
                public Integer call() {
                    return HelpAwareOptionPane.showOptionDialog(
                            Main.parent,
                            msg.toString(),
                            tr("Update plugins"),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0],
                            ht("/ErrorMessages#ErrorInPlugin")
                    );
                }
            });
            GuiHelper.runInEDT(task);
            return task.get();
        } catch (InterruptedException e) {
            Main.warn(e);
        } catch (ExecutionException e) {
            Main.warn(e);
        }
        return -1;
    }

    /**
     * Replies the plugin which most likely threw the exception <code>ex</code>.
     *
     * @param ex the exception
     * @return the plugin; null, if the exception probably wasn't thrown from a plugin
     */
    private static PluginProxy getPluginCausingException(Throwable ex) {
        PluginProxy err = null;
        StackTraceElement[] stack = ex.getStackTrace();
        /* remember the error position, as multiple plugins may be involved,
           we search the topmost one */
        int pos = stack.length;
        for (PluginProxy p : pluginList) {
            String baseClass = p.getPluginInformation().className;
            baseClass = baseClass.substring(0, baseClass.lastIndexOf('.'));
            for (int elpos = 0; elpos < pos; ++elpos) {
                if (stack[elpos].getClassName().startsWith(baseClass)) {
                    pos = elpos;
                    err = p;
                }
            }
        }
        return err;
    }

    /**
     * Checks whether the exception <code>e</code> was thrown by a plugin. If so,
     * conditionally updates or deactivates the plugin, but asks the user first.
     *
     * @param e the exception
     * @return plugin download task if the plugin has been updated to a newer version, {@code null} if it has been disabled or kept as it
     */
    public static PluginDownloadTask updateOrdisablePluginAfterException(Throwable e) {
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
            return null;

        Set<String> plugins = new HashSet<String>(
                Main.pref.getCollection("plugins",Collections.<String> emptySet())
        );
        final PluginInformation pluginInfo = plugin.getPluginInformation();
        if (! plugins.contains(pluginInfo.name))
            // plugin not activated ? strange in this context but anyway, don't bother
            // the user with dialogs, skip conditional deactivation
            return null;

        switch (askUpdateDisableKeepPluginAfterException(plugin)) {
        case 0:
            // update the plugin
            updatePlugins(Main.parent, Collections.singleton(pluginInfo), null, true);
            return pluginDownloadTask;
        case 1:
            // deactivate the plugin
            plugins.remove(plugin.getPluginInformation().name);
            Main.pref.putCollection("plugins", plugins);
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
            return null;
        default:
            // user doesn't want to deactivate the plugin
            return null;
        }
    }

    /**
     * Returns the list of loaded plugins as a {@code String} to be displayed in status report. Useful for bug reports.
     * @return The list of loaded plugins (one plugin per line)
     */
    public static String getBugReportText() {
        StringBuilder text = new StringBuilder();
        LinkedList <String> pl = new LinkedList<String>(Main.pref.getCollection("plugins", new LinkedList<String>()));
        for (final PluginProxy pp : pluginList) {
            PluginInformation pi = pp.getPluginInformation();
            pl.remove(pi.name);
            pl.add(pi.name + " (" + (pi.localversion != null && !pi.localversion.isEmpty()
                    ? pi.localversion : "unknown") + ")");
        }
        Collections.sort(pl);
        for (String s : pl) {
            text.append("Plugin: ").append(s).append("\n");
        }
        return text.toString();
    }

    /**
     * Returns the list of loaded plugins as a {@code JPanel} to be displayed in About dialog.
     * @return The list of loaded plugins (one "line" of Swing components per plugin)
     */
    public static JPanel getInfoPanel() {
        JPanel pluginTab = new JPanel(new GridBagLayout());
        for (final PluginProxy p : pluginList) {
            final PluginInformation info = p.getPluginInformation();
            String name = info.name
            + (info.version != null && !info.version.isEmpty() ? " Version: " + info.version : "");
            pluginTab.add(new JLabel(name), GBC.std());
            pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
            pluginTab.add(new JButton(new AbstractAction(tr("Information")) {
                @Override
                public void actionPerformed(ActionEvent event) {
                    StringBuilder b = new StringBuilder();
                    for (Entry<String, String> e : info.attr.entrySet()) {
                        b.append(e.getKey());
                        b.append(": ");
                        b.append(e.getValue());
                        b.append("\n");
                    }
                    JosmTextArea a = new JosmTextArea(10, 40);
                    a.setEditable(false);
                    a.setText(b.toString());
                    a.setCaretPosition(0);
                    JOptionPane.showMessageDialog(Main.parent, new JScrollPane(a), tr("Plugin information"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }), GBC.eol());

            JosmTextArea description = new JosmTextArea((info.description == null ? tr("no description available")
                    : info.description));
            description.setEditable(false);
            description.setFont(new JLabel().getFont().deriveFont(Font.ITALIC));
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            description.setBackground(UIManager.getColor("Panel.background"));
            description.setCaretPosition(0);

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
