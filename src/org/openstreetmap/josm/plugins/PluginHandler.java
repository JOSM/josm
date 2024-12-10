// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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

import jakarta.annotation.Nullable;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.ResourceProvider;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * PluginHandler is basically a collection of static utility functions used to bootstrap
 * and manage the loaded plugins.
 * @since 1326
 */
public final class PluginHandler {
    private static final String DIALOGS = "dialogs";
    private static final String WARNING = marktr("Warning");
    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";
    private static final String UPDATE_PLUGINS = marktr("Update plugins");
    private static final String CANCEL = "cancel";
    private static final String PLUGINS = "plugins";
    private static final String DISABLE_PLUGIN = marktr("Disable plugin");
    private static final String PLUGINMANAGER_VERSION_BASED_UPDATE_POLICY = "pluginmanager.version-based-update.policy";
    private static final String PLUGINMANAGER_LASTUPDATE = "pluginmanager.lastupdate";
    private static final String PLUGINMANAGER_TIME_BASED_UPDATE_POLICY = "pluginmanager.time-based-update.policy";

    /**
     * Deprecated plugins that are removed on start
     */
    static final List<DeprecatedPlugin> DEPRECATED_PLUGINS;
    static {
        String inCore = tr("integrated into main program");
        String replacedByPlugin = marktr("replaced by new {0} plugin");
        String noLongerRequired = tr("no longer required");

        DEPRECATED_PLUGINS = Arrays.asList(
            new DeprecatedPlugin("mappaint", inCore),
            new DeprecatedPlugin("unglueplugin", inCore),
            new DeprecatedPlugin("lang-de", inCore),
            new DeprecatedPlugin("lang-en_GB", inCore),
            new DeprecatedPlugin("lang-fr", inCore),
            new DeprecatedPlugin("lang-it", inCore),
            new DeprecatedPlugin("lang-pl", inCore),
            new DeprecatedPlugin("lang-ro", inCore),
            new DeprecatedPlugin("lang-ru", inCore),
            new DeprecatedPlugin("ewmsplugin", inCore),
            new DeprecatedPlugin("ywms", inCore),
            new DeprecatedPlugin("tways-0.2", inCore),
            new DeprecatedPlugin("geotagged", inCore),
            new DeprecatedPlugin("landsat", tr(replacedByPlugin, "scanaerial")),
            new DeprecatedPlugin("namefinder", inCore),
            new DeprecatedPlugin("waypoints", inCore),
            new DeprecatedPlugin("slippy_map_chooser", inCore),
            new DeprecatedPlugin("tcx-support", tr(replacedByPlugin, "dataimport")),
            new DeprecatedPlugin("usertools", inCore),
            new DeprecatedPlugin("AgPifoJ", inCore),
            new DeprecatedPlugin("utilsplugin", inCore),
            new DeprecatedPlugin("ghost", inCore),
            new DeprecatedPlugin("validator", inCore),
            new DeprecatedPlugin("multipoly", inCore),
            new DeprecatedPlugin("multipoly-convert", inCore),
            new DeprecatedPlugin("remotecontrol", inCore),
            new DeprecatedPlugin("imagery", inCore),
            new DeprecatedPlugin("slippymap", inCore),
            new DeprecatedPlugin("wmsplugin", inCore),
            new DeprecatedPlugin("ParallelWay", inCore),
            new DeprecatedPlugin("dumbutils", tr(replacedByPlugin, "utilsplugin2")),
            new DeprecatedPlugin("ImproveWayAccuracy", inCore),
            new DeprecatedPlugin("Curves", tr(replacedByPlugin, "utilsplugin2")),
            new DeprecatedPlugin("epsg31287", inCore),
            new DeprecatedPlugin("licensechange", noLongerRequired),
            new DeprecatedPlugin("restart", inCore),
            new DeprecatedPlugin("wayselector", inCore),
            new DeprecatedPlugin("openstreetbugs", inCore),
            new DeprecatedPlugin("nearclick", noLongerRequired),
            new DeprecatedPlugin("notes", inCore),
            new DeprecatedPlugin("mirrored_download", inCore),
            new DeprecatedPlugin("ImageryCache", inCore),
            new DeprecatedPlugin("commons-imaging", tr(replacedByPlugin, "apache-commons")),
            new DeprecatedPlugin("missingRoads", tr(replacedByPlugin, "ImproveOsm")),
            new DeprecatedPlugin("trafficFlowDirection", tr(replacedByPlugin, "ImproveOsm")),
            new DeprecatedPlugin("kendzi3d-jogl", tr(replacedByPlugin, "jogl")),
            new DeprecatedPlugin("josm-geojson", inCore),
            new DeprecatedPlugin("proj4j", inCore),
            new DeprecatedPlugin("OpenStreetView", tr(replacedByPlugin, "OpenStreetCam")),
            new DeprecatedPlugin("imageryadjust", inCore),
            new DeprecatedPlugin("walkingpapers", tr(replacedByPlugin, "fieldpapers")),
            new DeprecatedPlugin("czechaddress", noLongerRequired),
            new DeprecatedPlugin("kendzi3d_Improved_by_Andrei", noLongerRequired),
            new DeprecatedPlugin("videomapping", noLongerRequired),
            new DeprecatedPlugin("public_transport_layer", tr(replacedByPlugin, "pt_assistant")),
            new DeprecatedPlugin("lakewalker", tr(replacedByPlugin, "scanaerial")),
            new DeprecatedPlugin("download_along", inCore),
            new DeprecatedPlugin("plastic_laf", noLongerRequired),
            new DeprecatedPlugin("osmarender", noLongerRequired),
            new DeprecatedPlugin("geojson", inCore),
            new DeprecatedPlugin("gpxfilter", inCore),
            new DeprecatedPlugin("tag2link", inCore),
            new DeprecatedPlugin("rapid", tr(replacedByPlugin, "MapWithAI")),
            new DeprecatedPlugin("MovementAlert", inCore),
            new DeprecatedPlugin("OpenStreetCam", tr(replacedByPlugin, "KartaView")),
            new DeprecatedPlugin("scoutsigns", tr(replacedByPlugin, "KartaView")),
            new DeprecatedPlugin("javafx-osx", inCore),
            new DeprecatedPlugin("javafx-unixoid", inCore),
            new DeprecatedPlugin("javafx-windows", inCore),
            new DeprecatedPlugin("wikidata", tr(replacedByPlugin, "osmwiki-dataitem")),
            new DeprecatedPlugin("mapdust", noLongerRequired),
            new DeprecatedPlugin("tofix", noLongerRequired)
        );
        Collections.sort(DEPRECATED_PLUGINS);
    }

    private PluginHandler() {
        // Hide default constructor for utils classes
    }

    static final class PluginInformationAction extends AbstractAction {
        private final PluginInformation info;

        PluginInformationAction(PluginInformation info) {
            super(tr("Information"));
            this.info = info;
        }

        /**
         * Returns plugin information text.
         * @return plugin information text
         */
        public String getText() {
            StringBuilder b = new StringBuilder();
            Map<Object, Object> sorted = new TreeMap<>(Comparator.comparing(String::valueOf));
            sorted.putAll(info.attr);
            for (Entry<Object, Object> e : sorted.entrySet()) {
                b.append(e.getKey())
                        .append(": ")
                        .append(e.getValue())
                        .append('\n');
            }
            return b.toString();
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String text = getText();
            JosmTextArea a = new JosmTextArea(10, 40);
            a.setEditable(false);
            a.setText(text);
            a.setCaretPosition(0);
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), new JScrollPane(a), tr("Plugin information"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Description of a deprecated plugin
     */
    public static class DeprecatedPlugin implements Comparable<DeprecatedPlugin> {
        /** Plugin name */
        public final String name;
        /** Short explanation about deprecation, can be {@code null} */
        public final String reason;

        /**
         * Constructs a new {@code DeprecatedPlugin} with a given reason.
         * @param name The plugin name
         * @param reason The reason about deprecation
         */
        public DeprecatedPlugin(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, reason);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DeprecatedPlugin other = (DeprecatedPlugin) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (reason == null) {
                if (other.reason != null)
                    return false;
            } else if (!reason.equals(other.reason))
                return false;
            return true;
        }

        @Override
        public int compareTo(DeprecatedPlugin o) {
            int d = name.compareTo(o.name);
            if (d == 0)
                d = reason.compareTo(o.reason);
            return d;
        }
    }

    /**
     * List of unmaintained plugins. Not really up-to-date as the vast majority of plugins are not maintained after a few months, sadly...
     */
    static final List<String> UNMAINTAINED_PLUGINS = List.of(
        "irsrectify", // See https://josm.openstreetmap.de/changeset/29404/osm/
        "surveyor2", // See https://josm.openstreetmap.de/changeset/29404/osm/
        "gpsbabelgui",
        "Intersect_way",
        "ContourOverlappingMerge", // See #11202, #11518, https://github.com/bularcasergiu/ContourOverlappingMerge/issues/1
        "LaneConnector",           // See #11468, #11518, https://github.com/TrifanAdrian/LanecConnectorPlugin/issues/1
        "Remove.redundant.points"  // See #11468, #11518, https://github.com/bularcasergiu/RemoveRedundantPoints (not even created an issue...)
    );

    /**
     * Default time-based update interval, in days (pluginmanager.time-based-update.interval)
     */
    public static final int DEFAULT_TIME_BASED_UPDATE_INTERVAL = 30;

    /**
     * All installed and loaded plugins (resp. their main classes)
     */
    static final Collection<PluginProxy> pluginList = new CopyOnWriteArrayList<>();

    /**
     * All installed but not loaded plugins
     */
    static final Collection<PluginInformation> pluginListNotLoaded = new LinkedList<>();

    /**
     * All exceptions that occurred during plugin loading
     */
    static final Map<String, Throwable> pluginLoadingExceptions = new HashMap<>();

    /**
     * Class loader to locate resources from plugins.
     * @see #getJoinedPluginResourceCL()
     */
    private static DynamicURLClassLoader joinedPluginResourceCL;

    /**
     * Add here all ClassLoader whose resource should be searched.
     */
    private static final List<ClassLoader> sources = new LinkedList<>();
    static {
        try {
            sources.add(ClassLoader.getSystemClassLoader());
            sources.add(PluginHandler.class.getClassLoader());
        } catch (SecurityException ex) {
            Logging.debug(ex);
            sources.add(ImageProvider.class.getClassLoader());
        }
    }

    /**
     * Plugin class loaders.
     */
    private static final Map<String, PluginClassLoader> classLoaders = new HashMap<>();

    private static PluginDownloadTask pluginDownloadTask;

    /**
     * Returns the list of currently installed and loaded plugins, sorted by name.
     * @return the list of currently installed and loaded plugins, sorted by name
     * @since 10982
     */
    public static List<PluginInformation> getPlugins() {
        return pluginList.stream().map(PluginProxy::getPluginInformation)
                .sorted(Comparator.comparing(PluginInformation::getName)).collect(Collectors.toList());
    }

    /**
     * Returns all ClassLoaders whose resource should be searched.
     * @return all ClassLoaders whose resource should be searched
     */
    public static Collection<ClassLoader> getResourceClassLoaders() {
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * Returns all plugin classloaders.
     * @return all plugin classloaders
     * @since 14978
     */
    public static Collection<PluginClassLoader> getPluginClassLoaders() {
        return Collections.unmodifiableCollection(classLoaders.values());
    }

    /**
     * Get a {@link ServiceLoader} for the specified service. This uses {@link #getJoinedPluginResourceCL()} as the
     * class loader, so that we don't have to iterate through the {@link ClassLoader}s from {@link #getPluginClassLoaders()}.
     * @param <S> The service type
     * @param service The service class to look for
     * @return The service loader
     * @since 18833
     */
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return ServiceLoader.load(service, getJoinedPluginResourceCL());
    }

    /**
     * Removes deprecated plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>.
     * <p>
     * Also notifies the user about removed deprecated plugins
     *
     * @param parent The parent Component used to display warning popup
     * @param plugins the collection of plugins
     */
    static void filterDeprecatedPlugins(Component parent, Collection<String> plugins) {
        Set<DeprecatedPlugin> removedPlugins = new TreeSet<>();
        for (DeprecatedPlugin depr : DEPRECATED_PLUGINS) {
            if (plugins.contains(depr.name)) {
                plugins.remove(depr.name);
                PreferencesUtils.removeFromList(Config.getPref(), PLUGINS, depr.name);
                removedPlugins.add(depr);
            }
        }
        if (removedPlugins.isEmpty())
            return;

        // notify user about removed deprecated plugins
        //
        JOptionPane.showMessageDialog(
                parent,
                getRemovedPluginsMessage(removedPlugins),
                tr(WARNING),
                JOptionPane.WARNING_MESSAGE
        );
    }

    static String getRemovedPluginsMessage(Collection<DeprecatedPlugin> removedPlugins) {
        StringBuilder sb = new StringBuilder(32);
        sb.append(HTML_START)
          .append(trn(
                "The following plugin is no longer necessary and has been deactivated:",
                "The following plugins are no longer necessary and have been deactivated:",
                removedPlugins.size()))
          .append("<ul>");
        for (DeprecatedPlugin depr: removedPlugins) {
            sb.append("<li>").append(depr.name);
            if (depr.reason != null) {
                sb.append(" (").append(depr.reason).append(')');
            }
            sb.append("</li>");
        }
        sb.append("</ul>").append(HTML_END);
        return sb.toString();
    }

    /**
     * Removes unmaintained plugins from a collection of plugins. Modifies the
     * collection <code>plugins</code>. Also removes the plugin from the list
     * of plugins in the preferences, if necessary.
     * <p>
     * Asks the user for every unmaintained plugin whether it should be removed.
     * @param parent The parent Component used to display warning popup
     *
     * @param plugins the collection of plugins
     */
    static void filterUnmaintainedPlugins(Component parent, Collection<String> plugins) {
        for (String unmaintained : UNMAINTAINED_PLUGINS) {
            if (!plugins.contains(unmaintained)) {
                continue;
            }
            if (confirmDisablePlugin(parent, getUnmaintainedPluginMessage(unmaintained), unmaintained)) {
                PreferencesUtils.removeFromList(Config.getPref(), PLUGINS, unmaintained);
                plugins.remove(unmaintained);
            }
        }
    }

    static String getUnmaintainedPluginMessage(String unmaintained) {
        return tr("<html>Loading of the plugin \"{0}\" was requested."
                + "<br>This plugin is no longer developed and very likely will produce errors."
                +"<br>It should be disabled.<br>Delete from preferences?</html>",
                Utils.escapeReservedCharactersHTML(unmaintained));
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
        if (Preferences.main().getPluginSites().stream().anyMatch(NetworkManager::isOffline)) {
            Logging.info(OfflineAccessException.forResource(tr("Plugin update")).getMessage());
            return false;
        }
        String message = null;
        String togglePreferenceKey = null;
        int v = Version.getInstance().getVersion();
        if (Config.getPref().getInt("pluginmanager.version", 0) < v) {
            message =
                HTML_START
                + tr("You updated your JOSM software.<br>"
                        + "To prevent problems the plugins should be updated as well.<br><br>"
                        + "Update plugins now?"
                )
                + HTML_END;
            togglePreferenceKey = PLUGINMANAGER_VERSION_BASED_UPDATE_POLICY;
        } else {
            long tim = System.currentTimeMillis();
            long last = Config.getPref().getLong(PLUGINMANAGER_LASTUPDATE, 0);
            int maxTime = Config.getPref().getInt("pluginmanager.time-based-update.interval", DEFAULT_TIME_BASED_UPDATE_INTERVAL);
            long d = TimeUnit.MILLISECONDS.toDays(tim - last);
            if ((last <= 0) || (maxTime <= 0)) {
                Config.getPref().put(PLUGINMANAGER_LASTUPDATE, Long.toString(tim));
            } else if (d > maxTime) {
                message =
                    HTML_START
                    + tr("Last plugin update more than {0} days ago.", d)
                    + HTML_END;
                togglePreferenceKey = PLUGINMANAGER_TIME_BASED_UPDATE_POLICY;
            }
        }
        if (message == null) return false;

        UpdatePluginsMessagePanel pnlMessage = new UpdatePluginsMessagePanel();
        pnlMessage.setMessage(message);
        pnlMessage.initDontShowAgain(togglePreferenceKey);

        // check whether automatic update at startup was disabled
        //
        String policy = Config.getPref().get(togglePreferenceKey, "ask").trim().toLowerCase(Locale.ENGLISH);
        switch (policy) {
        case "never":
            if (PLUGINMANAGER_VERSION_BASED_UPDATE_POLICY.equals(togglePreferenceKey)) {
                Logging.info(tr("Skipping plugin update after JOSM upgrade. Automatic update at startup is disabled."));
            } else if (PLUGINMANAGER_TIME_BASED_UPDATE_POLICY.equals(togglePreferenceKey)) {
                Logging.info(tr("Skipping plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return false;

        case "always":
            if (PLUGINMANAGER_VERSION_BASED_UPDATE_POLICY.equals(togglePreferenceKey)) {
                Logging.info(tr("Running plugin update after JOSM upgrade. Automatic update at startup is enabled."));
            } else if (PLUGINMANAGER_TIME_BASED_UPDATE_POLICY.equals(togglePreferenceKey)) {
                Logging.info(tr("Running plugin update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return true;

        case "ask":
            break;

        default:
            Logging.warn(tr("Unexpected value ''{0}'' for preference ''{1}''. Assuming value ''ask''.", policy, togglePreferenceKey));
        }

        ButtonSpec[] options = {
                new ButtonSpec(
                        tr(UPDATE_PLUGINS),
                        new ImageProvider(DIALOGS, "refresh"),
                        tr("Click to update the activated plugins"),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Skip update"),
                        new ImageProvider(CANCEL),
                        tr("Click to skip updating the activated plugins"),
                        null /* no specific help context */
                )
        };

        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                pnlMessage,
                tr(UPDATE_PLUGINS),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                ht("/Preferences/Plugins#AutomaticUpdate")
        );

        if (pnlMessage.isRememberDecision()) {
            switch (ret) {
            case 0:
                Config.getPref().put(togglePreferenceKey, "always");
                break;
            case JOptionPane.CLOSED_OPTION:
            case 1:
                Config.getPref().put(togglePreferenceKey, "never");
                break;
            default: // Do nothing
            }
        } else {
            Config.getPref().put(togglePreferenceKey, "ask");
        }
        return ret == 0;
    }

    /**
     * Alerts the user if a plugin required by another plugin is missing, and offer to download them &amp; restart JOSM
     *
     * @param parent The parent Component used to display error popup
     * @param plugin the plugin
     * @param missingRequiredPlugin the missing required plugin
     */
    private static void alertMissingRequiredPlugin(Component parent, String plugin, Set<String> missingRequiredPlugin) {
        StringBuilder sb = new StringBuilder(48);
        sb.append(HTML_START)
          .append(trn("Plugin {0} requires a plugin which was not found. The missing plugin is:",
                "Plugin {0} requires {1} plugins which were not found. The missing plugins are:",
                missingRequiredPlugin.size(),
                Utils.escapeReservedCharactersHTML(plugin),
                missingRequiredPlugin.size()))
          .append(Utils.joinAsHtmlUnorderedList(missingRequiredPlugin))
          .append(HTML_END);
        ButtonSpec[] specs = {
                new ButtonSpec(
                        tr("Download and restart"),
                        new ImageProvider("restart"),
                        trn("Click to download missing plugin and restart JOSM",
                            "Click to download missing plugins and restart JOSM",
                            missingRequiredPlugin.size()),
                        null /* no specific help text */
                ),
                new ButtonSpec(
                        tr("Continue"),
                        new ImageProvider("ok"),
                        trn("Click to continue without this plugin",
                            "Click to continue without these plugins",
                            missingRequiredPlugin.size()),
                        null /* no specific help text */
                )
        };
        if (0 == HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE,
                null, /* no special icon */
                specs,
                specs[0],
                ht("/Plugin/Loading#MissingRequiredPlugin"))) {
            downloadRequiredPluginsAndRestart(parent, missingRequiredPlugin);
        }
    }

    private static void downloadRequiredPluginsAndRestart(final Component parent, final Set<String> missingRequiredPlugin) {
        // Update plugin list
        final ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                Preferences.main().getOnlinePluginSites());
        MainApplication.worker.submit(pluginInfoDownloadTask);

        // Continuation
        MainApplication.worker.submit(() -> {
            // Build list of plugins to download
            Set<PluginInformation> toDownload = new HashSet<>(pluginInfoDownloadTask.getAvailablePlugins());
            toDownload.removeIf(info -> !missingRequiredPlugin.contains(info.getName()));
            // Check if something has still to be downloaded
            if (!toDownload.isEmpty()) {
                // download plugins
                final PluginDownloadTask task = new PluginDownloadTask(parent, toDownload, tr("Download plugins"));
                MainApplication.worker.submit(task);
                MainApplication.worker.submit(() -> {
                    // restart if some plugins have been downloaded
                    if (!task.getDownloadedPlugins().isEmpty()) {
                        // update plugin list in preferences
                        Set<String> plugins = new HashSet<>(Config.getPref().getList(PLUGINS));
                        for (PluginInformation plugin : task.getDownloadedPlugins()) {
                            plugins.add(plugin.name);
                        }
                        Config.getPref().putList(PLUGINS, new ArrayList<>(plugins));
                        // restart
                        RestartAction.restartJOSM();
                    } else {
                        Logging.warn("No plugin downloaded, restart canceled");
                    }
                });
            } else {
                Logging.warn("No plugin to download, operation canceled");
            }
        });
    }

    private static void logWrongPlatform(String plugin, String pluginPlatform) {
        Logging.warn(
                tr("Plugin {0} must be run on a {1} platform.",
                        plugin, pluginPlatform
                ));
    }

    private static void alertJavaUpdateRequired(Component parent, String plugin, int requiredVersion) {
        final ButtonSpec[] options = {
                new ButtonSpec(tr("OK"), ImageProvider.get("ok"), tr("Click to close the dialog"), null),
                new ButtonSpec(tr("Update Java"), ImageProvider.get("java"), tr("Update Java"), null)
        };
        final int selected = HelpAwareOptionPane.showOptionDialog(
                parent,
                HTML_START + tr("Plugin {0} requires Java version {1}. The current Java version is {2}.<br>"
                                + "You have to update Java in order to use this plugin.",
                        plugin, Integer.toString(requiredVersion), Utils.getJavaVersion()
                ) + HTML_END,
                tr(WARNING),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                null
        );
        if (selected == 1 && !Utils.isRunningWebStart()) {
            final String javaUrl = PlatformManager.getPlatform().getJavaUrl();
            OpenBrowser.displayUrl(javaUrl);
        }
    }

    private static void alertJOSMUpdateRequired(Component parent, String plugin, int requiredVersion) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>Plugin {0} requires JOSM version {1}. The current JOSM version is {2}.<br>"
                        +"You have to update JOSM in order to use this plugin.</html>",
                        plugin, Integer.toString(requiredVersion), Version.getInstance().getVersionString()
                ),
                tr(WARNING),
                JOptionPane.WARNING_MESSAGE,
                null
        );
    }

    /**
     * Checks whether all preconditions for loading the plugin <code>plugin</code> are met. The
     * current Java and JOSM versions must be compatible with the plugin and no other plugins this plugin
     * depends on should be missing.
     *
     * @param parent The parent Component used to display error popup
     * @param plugins the collection of all loaded plugins
     * @param plugin the plugin for which preconditions are checked
     * @return true, if the preconditions are met; false otherwise
     */
    public static boolean checkLoadPreconditions(Component parent, Collection<PluginInformation> plugins, PluginInformation plugin) {

        // make sure the plugin is not meant for another platform
        if (!plugin.isForCurrentPlatform()) {
            // Just log a warning, this is unlikely to happen as we display only relevant plugins in HMI
            logWrongPlatform(plugin.name, plugin.platform);
            return false;
        }

        // make sure the plugin is compatible with the current Java version
        if (plugin.localminjavaversion > Utils.getJavaVersion()) {
            alertJavaUpdateRequired(parent, plugin.name, plugin.localminjavaversion);
            return false;
        }

        // make sure the plugin is compatible with the current JOSM version
        int josmVersion = Version.getInstance().getVersion();
        if (plugin.localmainversion > josmVersion && josmVersion != Version.JOSM_UNKNOWN_VERSION) {
            alertJOSMUpdateRequired(parent, plugin.name, plugin.localmainversion);
            return false;
        }

        // Add all plugins already loaded (to include early plugins when checking late ones)
        Collection<PluginInformation> allPlugins = new HashSet<>(plugins);
        for (PluginProxy proxy : pluginList) {
            allPlugins.add(proxy.getPluginInformation());
        }

        // Include plugins that have been processed but not been loaded (for javafx plugin)
        allPlugins.addAll(pluginListNotLoaded);

        return checkRequiredPluginsPreconditions(parent, allPlugins, plugin, true);
    }

    /**
     * Checks if required plugins preconditions for loading the plugin <code>plugin</code> are met.
     * No other plugins this plugin depends on should be missing.
     *
     * @param parent The parent Component used to display error popup. If parent is
     * null, the error popup is suppressed
     * @param plugins the collection of all processed plugins
     * @param plugin the plugin for which preconditions are checked
     * @param local Determines if the local or up-to-date plugin dependencies are to be checked.
     * @return true, if the preconditions are met; false otherwise
     * @since 5601
     */
    public static boolean checkRequiredPluginsPreconditions(Component parent, Collection<PluginInformation> plugins,
            PluginInformation plugin, boolean local) {

        String requires = local ? plugin.localrequires : plugin.requires;

        // make sure the dependencies to other plugins are not broken
        if (requires != null) {
            Set<String> missingPlugins = findMissingPlugins(plugins, plugin, local);
            if (!missingPlugins.isEmpty()) {
                if (parent != null) {
                    alertMissingRequiredPlugin(parent, plugin.name, missingPlugins);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Find the missing plugin(s) for a specified plugin
     * @param plugins The currently loaded plugins
     * @param plugin The plugin to find the missing information for
     * @param local Determines if the local or up-to-date plugin dependencies are to be checked.
     * @return A set of missing plugins for the given plugin
     */
    private static Set<String> findMissingPlugins(Collection<PluginInformation> plugins, PluginInformation plugin, boolean local) {
        Set<String> pluginNames = new HashSet<>();
        for (PluginInformation pi: plugins) {
            pluginNames.add(pi.name);
            if (pi.provides != null) {
                pluginNames.add(pi.provides);
            }
        }
        Set<String> missingPlugins = new HashSet<>();
        List<String> requiredPlugins = local ? plugin.getLocalRequiredPlugins() : plugin.getRequiredPlugins();
        for (String requiredPlugin : requiredPlugins) {
            if (!pluginNames.contains(requiredPlugin)) {
                missingPlugins.add(requiredPlugin);
            }
        }
        return missingPlugins;
    }

    /**
     * Get class loader to locate resources from plugins.
     * <p>
     * It joins URLs of all plugins, to find images, etc.
     * (Not for loading Java classes - each plugin has a separate {@link PluginClassLoader}
     * for that purpose.)
     * @return class loader to locate resources from plugins
     */
    private static synchronized DynamicURLClassLoader getJoinedPluginResourceCL() {
        if (joinedPluginResourceCL == null) {
            joinedPluginResourceCL = AccessController.doPrivileged((PrivilegedAction<DynamicURLClassLoader>)
                    () -> new DynamicURLClassLoader(new URL[0], PluginHandler.class.getClassLoader()));
            sources.add(0, joinedPluginResourceCL);
        }
        return joinedPluginResourceCL;
    }

    /**
     * Add more plugins to the joined plugin resource class loader.
     *
     * @param plugins the plugins to add
     */
    @SuppressWarnings("PMD.CloseResource") // NOSONAR We do *not* want to close class loaders in this method...
    private static void extendJoinedPluginResourceCL(Collection<PluginInformation> plugins) {
        // iterate all plugins and collect all libraries of all plugins:
        File pluginDir = Preferences.main().getPluginsDirectory();
        DynamicURLClassLoader cl = getJoinedPluginResourceCL();

        for (PluginInformation info : plugins) {
            if (info.libraries == null) {
                continue;
            }
            for (URL libUrl : info.libraries) {
                cl.addURL(libUrl);
            }
            File pluginJar = new File(pluginDir, info.name + ".jar");
            I18n.addTexts(pluginJar);
            URL pluginJarUrl = Utils.fileToURL(pluginJar);
            cl.addURL(pluginJarUrl);
        }
    }

    /**
     * Loads and instantiates the plugin described by <code>plugin</code> using
     * the class loader <code>pluginClassLoader</code>.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugin the plugin
     * @param pluginClassLoader the plugin class loader
     */
    private static void loadPlugin(Component parent, PluginInformation plugin, PluginClassLoader pluginClassLoader) {
        String msg = tr("Could not load plugin {0}. Delete from preferences?", "'"+plugin.name+"'");
        try {
            Class<?> klass = plugin.loadClass(pluginClassLoader);
            if (klass != null) {
                Logging.info(tr("loading plugin ''{0}'' (version {1})", plugin.name, plugin.localversion));
                PluginProxy pluginProxy = plugin.load(klass, pluginClassLoader);
                pluginList.add(pluginProxy);
                MainApplication.addAndFireMapFrameListener(pluginProxy);
            }
            msg = null;
        } catch (PluginException e) {
            pluginLoadingExceptions.put(plugin.name, e);
            Logging.error(e);
            if (e.getCause() instanceof ClassNotFoundException) {
                msg = tr("<html>Could not load plugin {0} because the plugin<br>main class ''{1}'' was not found.<br>"
                        + "Delete from preferences?</html>", "'"+Utils.escapeReservedCharactersHTML(plugin.name)+"'", plugin.className);
            }
        } catch (RuntimeException e) { // NOPMD
            pluginLoadingExceptions.put(plugin.name, e);
            Logging.error(e);
        }
        if (msg != null && confirmDisablePlugin(parent, msg, plugin.name)) {
            PreferencesUtils.removeFromList(Config.getPref(), PLUGINS, plugin.name);
        }
    }

    /**
     * Loads the plugin in <code>plugins</code> from locally available jar files into memory.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the list of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadPlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Loading plugins ..."));
            monitor.subTask(tr("Checking plugin preconditions..."));
            List<PluginInformation> toLoad = new LinkedList<>();
            for (PluginInformation pi: plugins) {
                if (checkLoadPreconditions(parent, plugins, pi)) {
                    toLoad.add(pi);
                } else {
                    pluginListNotLoaded.add(pi);
                }
            }
            // sort the plugins according to their "staging" equivalence class. The
            // lower the value of "stage" the earlier the plugin should be loaded.
            //
            toLoad.sort(Comparator.comparingInt(o -> o.stage));
            if (toLoad.isEmpty())
                return;

            generateClassloaders(toLoad);

            // resolve dependencies
            resolveDependencies(toLoad);

            extendJoinedPluginResourceCL(toLoad);
            ResourceProvider.addAdditionalClassLoaders(getResourceClassLoaders());
            monitor.setTicksCount(toLoad.size());
            for (PluginInformation info : toLoad) {
                monitor.setExtraText(tr("Loading plugin ''{0}''...", info.name));
                loadPlugin(parent, info, classLoaders.get(info.name));
                monitor.worked(1);
            }
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Generate classloaders for a list of plugins
     * @param toLoad The plugins to generate the classloaders for
     */
    @SuppressWarnings({"squid:S2095", "PMD.CloseResource"}) // NOSONAR the classloaders and put in a map which we want to keep.
    private static void generateClassloaders(List<PluginInformation> toLoad) {
        for (PluginInformation info : toLoad) {
            PluginClassLoader cl = AccessController.doPrivileged((PrivilegedAction<PluginClassLoader>)
                    () -> new PluginClassLoader(
                            info.libraries.toArray(new URL[0]),
                            PluginHandler.class.getClassLoader(),
                            null));
            classLoaders.put(info.name, cl);
        }
    }

    /**
     * Resolve dependencies for a list of plugins
     * @param toLoad The plugins to resolve dependencies for
     */
    @SuppressWarnings({"squid:S2095", "PMD.CloseResource"}) // NOSONAR the classloaders are from a persistent map
    private static void resolveDependencies(List<PluginInformation> toLoad) {
        for (PluginInformation info : toLoad) {
            PluginClassLoader cl = classLoaders.get(info.name);
            for (String depName : info.getLocalRequiredPlugins()) {
                boolean finished = false;
                for (PluginInformation depInfo : toLoad) {
                    if (isDependency(depInfo, depName)) {
                        cl.addDependency(classLoaders.get(depInfo.name));
                        finished = true;
                        break;
                    }
                }
                if (finished) {
                    continue;
                }
                boolean found = false;
                for (PluginProxy proxy : pluginList) {
                    if (isDependency(proxy.getPluginInformation(), depName)) {
                        found = cl.addDependency(proxy.getClassLoader());
                        break;
                    }
                }
                if (!found) {
                    Logging.error("unable to find dependency " + depName + " for plugin " + info.getName());
                }
            }
        }
    }

    private static boolean isDependency(PluginInformation pi, String depName) {
        return depName.equals(pi.getName()) || depName.equals(pi.provides);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early} set to true
     * <i>and</i> a negative {@link PluginInformation#stage} value.
     * <p>
     * This is meant for plugins that provide additional {@link javax.swing.LookAndFeel}.
     */
    public static void loadVeryEarlyPlugins() {
        List<PluginInformation> veryEarlyPlugins = PluginHandler.buildListOfPluginsToLoad(null, null)
                .stream()
                .filter(pi -> pi.early && pi.stage < 0)
                .collect(Collectors.toList());
        loadPlugins(null, veryEarlyPlugins, null);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early} set to true
     * <i>and</i> a non-negative {@link PluginInformation#stage} value.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadEarlyPlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> earlyPlugins = plugins.stream()
                .filter(pi -> pi.early && pi.stage >= 0)
                .collect(Collectors.toList());
        loadPlugins(parent, earlyPlugins, monitor);
    }

    /**
     * Loads plugins from <code>plugins</code> which have the flag {@link PluginInformation#early} set to false.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param plugins the collection of plugins
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadLatePlugins(Component parent, Collection<PluginInformation> plugins, ProgressMonitor monitor) {
        List<PluginInformation> latePlugins = plugins.stream()
                .filter(pi -> !pi.early)
                .collect(Collectors.toList());
        loadPlugins(parent, latePlugins, monitor);
    }

    /**
     * Loads locally available plugin information from local plugin jars and from cached
     * plugin lists.
     *
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the map of locally available plugin information, null in case of errors
     *
     */
    @Nullable
    @SuppressWarnings("squid:S1168") // The null return is part of the API for this method.
    private static Map<String, PluginInformation> loadLocallyAvailablePluginInformation(ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            ReadLocalPluginInformationTask task = new ReadLocalPluginInformationTask(monitor);
            Future<?> future = MainApplication.worker.submit(task);
            try {
                future.get();
            } catch (ExecutionException e) {
                Logging.error(e);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logging.warn("InterruptedException in " + PluginHandler.class.getSimpleName()
                        + " while loading locally available plugin information");
                return null;
            }
            Map<String, PluginInformation> ret = new HashMap<>();
            for (PluginInformation pi: task.getAvailablePlugins()) {
                ret.put(pi.name, pi);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertMissingPluginInformation(Component parent, Collection<String> plugins) {
        String sb = HTML_START +
                trn("JOSM could not find information about the following plugin:",
                        "JOSM could not find information about the following plugins:",
                        plugins.size()) +
                Utils.joinAsHtmlUnorderedList(plugins) +
                trn("The plugin is not going to be loaded.",
                        "The plugins are not going to be loaded.",
                        plugins.size()) +
                HTML_END;
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb,
                tr(WARNING),
                JOptionPane.WARNING_MESSAGE,
                ht("/Plugin/Loading#MissingPluginInfos")
        );
    }

    /**
     * Builds the list of plugins to load. Deprecated and unmaintained plugins are filtered
     * out. This involves user interaction. This method displays alert and confirmation
     * messages.
     *
     * @param parent The parent component to be used for the displayed dialog
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the list of plugins to load (as set of plugin names)
     */
    public static List<PluginInformation> buildListOfPluginsToLoad(Component parent, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Determining plugins to load..."));
            Set<String> plugins = new HashSet<>(Config.getPref().getList(PLUGINS, new LinkedList<>()));
            Logging.debug("Plugins list initialized to {0}", plugins);
            String systemProp = Utils.getSystemProperty("josm.plugins");
            if (systemProp != null) {
                plugins.addAll(Arrays.asList(systemProp.split(",", -1)));
                Logging.debug("josm.plugins system property set to ''{0}''. Plugins list is now {1}", systemProp, plugins);
            }
            monitor.subTask(tr("Removing deprecated plugins..."));
            filterDeprecatedPlugins(parent, plugins);
            monitor.subTask(tr("Removing unmaintained plugins..."));
            filterUnmaintainedPlugins(parent, plugins);
            Logging.debug("Plugins list is finally set to {0}", plugins);
            Map<String, PluginInformation> infos = loadLocallyAvailablePluginInformation(monitor.createSubTaskMonitor(1, false));
            List<PluginInformation> ret = new LinkedList<>();
            if (infos != null) {
                for (Iterator<String> it = plugins.iterator(); it.hasNext();) {
                    String plugin = it.next();
                    if (infos.containsKey(plugin)) {
                        ret.add(infos.get(plugin));
                        it.remove();
                    }
                }
            }
            if (!plugins.isEmpty() && parent != null) {
                alertMissingPluginInformation(parent, plugins);
            }
            return ret;
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertFailedPluginUpdate(Component parent, Collection<PluginInformation> plugins) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(HTML_START)
          .append(trn(
                "Updating the following plugin has failed:",
                "Updating the following plugins has failed:",
                plugins.size()))
          .append("<ul>");
        for (PluginInformation pi: plugins) {
            sb.append("<li>").append(Utils.escapeReservedCharactersHTML(pi.name)).append("</li>");
        }
        sb.append("</ul>")
          .append(trn(
                "Please open the Preference Dialog after JOSM has started and try to update it manually.",
                "Please open the Preference Dialog after JOSM has started and try to update them manually.",
                plugins.size()))
          .append(HTML_END);
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Plugin update failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/Plugin/Loading#FailedPluginUpdated")
        );
    }

    private static Set<PluginInformation> findRequiredPluginsToDownload(
            Collection<PluginInformation> pluginsToUpdate, List<PluginInformation> allPlugins, Set<PluginInformation> pluginsToDownload) {
        Set<PluginInformation> result = new HashSet<>();
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
                    Logging.warn(tr("Failed to find plugin {0}", name));
                    Logging.error(e);
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
     * @return the list of plugins to load
     * @throws IllegalArgumentException if plugins is null
     */
    public static Collection<PluginInformation> updatePlugins(Component parent,
            Collection<PluginInformation> pluginsWanted, ProgressMonitor monitor, boolean displayErrMsg) {
        Collection<PluginInformation> plugins = null;
        pluginDownloadTask = null;
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");

            // try to download the plugin lists
            ReadRemotePluginInformationTask task1 = new ReadRemotePluginInformationTask(
                    monitor.createSubTaskMonitor(1, false),
                    Preferences.main().getOnlinePluginSites(), displayErrMsg
                    );
            List<PluginInformation> allPlugins = null;
            Future<?> future = MainApplication.worker.submit(task1);

            try {
                future.get();
                allPlugins = task1.getAvailablePlugins();
                plugins = buildListOfPluginsToLoad(parent, monitor.createSubTaskMonitor(1, false));
                // If only some plugins have to be updated, filter the list
                if (!Utils.isEmpty(pluginsWanted)) {
                    final Collection<String> pluginsWantedName = Utils.transform(pluginsWanted, piw -> piw.name);
                    plugins = SubclassFilteredCollection.filter(plugins, pi -> pluginsWantedName.contains(pi.name));
                }
            } catch (ExecutionException e) {
                Logging.warn(tr("Failed to download plugin information list") + ": ExecutionException");
                Logging.error(e);
                // don't abort in case of error, continue with downloading plugins below
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logging.warn(tr("Failed to download plugin information list") + ": InterruptedException");
                // don't abort in case of error, continue with downloading plugins below
            }

            // filter plugins which actually have to be updated
            Collection<PluginInformation> pluginsToUpdate = new ArrayList<>();
            if (plugins != null) {
                for (PluginInformation pi: plugins) {
                    if (pi.isUpdateRequired()) {
                        pluginsToUpdate.add(pi);
                    }
                }
            }

            if (!pluginsToUpdate.isEmpty()) {

                Set<PluginInformation> pluginsToDownload = new HashSet<>(pluginsToUpdate);

                if (allPlugins != null) {
                    // Updated plugins may need additional plugin dependencies currently not installed
                    //
                    Set<PluginInformation> additionalPlugins = findRequiredPluginsToDownload(pluginsToUpdate, allPlugins, pluginsToDownload);
                    pluginsToDownload.addAll(additionalPlugins);

                    // Iterate on required plugins, if they need themselves another plugins (i.e A needs B, but B needs C)
                    while (!additionalPlugins.isEmpty()) {
                        // Install the additional plugins to load them later
                        if (plugins != null)
                            plugins.addAll(additionalPlugins);
                        additionalPlugins = findRequiredPluginsToDownload(additionalPlugins, allPlugins, pluginsToDownload);
                        pluginsToDownload.addAll(additionalPlugins);
                    }
                }

                // try to update the locally installed plugins
                pluginDownloadTask = new PluginDownloadTask(
                        monitor.createSubTaskMonitor(1, false),
                        pluginsToDownload,
                        tr(UPDATE_PLUGINS)
                        );
                future = MainApplication.worker.submit(pluginDownloadTask);

                try {
                    future.get();
                } catch (ExecutionException e) {
                    Logging.error(e);
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return plugins;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logging.warn("InterruptedException in " + PluginHandler.class.getSimpleName()
                            + " while updating plugins");
                    alertFailedPluginUpdate(parent, pluginsToUpdate);
                    return plugins;
                }

                // Update Plugin info for downloaded plugins
                refreshLocalUpdatedPluginInfo(pluginDownloadTask.getDownloadedPlugins());

                // notify user if downloading a locally installed plugin failed
                if (!pluginDownloadTask.getFailedPlugins().isEmpty()) {
                    alertFailedPluginUpdate(parent, pluginDownloadTask.getFailedPlugins());
                    return plugins;
                }
            }
        } finally {
            monitor.finishTask();
        }
        if (pluginsWanted == null) {
            // if all plugins updated, remember the update because it was successful
            Config.getPref().putInt("pluginmanager.version", Version.getInstance().getVersion());
            Config.getPref().put(PLUGINMANAGER_LASTUPDATE, Long.toString(System.currentTimeMillis()));
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
        ButtonSpec[] options = {
                new ButtonSpec(
                        tr(DISABLE_PLUGIN),
                        new ImageProvider(DIALOGS, "delete"),
                        tr("Click to delete the plugin ''{0}''", name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        new ImageProvider(CANCEL),
                        tr("Click to keep the plugin ''{0}''", name),
                        null /* no specific help context */
                )
        };
        return 0 == HelpAwareOptionPane.showOptionDialog(
                    parent,
                    reason,
                    tr(DISABLE_PLUGIN),
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0],
                    null // FIXME: add help topic
            );
    }

    /**
     * Returns the plugin of the specified name.
     * @param name The plugin name
     * @return The plugin of the specified name, if installed and loaded, or {@code null} otherwise.
     */
    public static Object getPlugin(String name) {
        for (PluginProxy plugin : pluginList) {
            if (plugin.getPluginInformation().name.equals(name))
                return plugin.getPlugin();
        }
        return null;
    }

    /**
     * Returns the plugin class loader for the plugin of the specified name.
     * @param name The plugin name
     * @return The plugin class loader for the plugin of the specified name, if
     * installed and loaded, or {@code null} otherwise.
     * @since 12323
     */
    public static PluginClassLoader getPluginClassLoader(String name) {
        for (PluginProxy plugin : pluginList) {
            if (plugin.getPluginInformation().name.equals(name))
                return plugin.getClassLoader();
        }
        return null;
    }

    /**
     * Called in the download dialog to give the plugins a chance to modify the list
     * of bounding box selectors.
     * @param downloadSelections list of bounding box selectors
     */
    public static void addDownloadSelection(List<DownloadSelection> downloadSelections) {
        for (PluginProxy p : pluginList) {
            p.addDownloadSelection(downloadSelections);
        }
    }

    /**
     * Returns the list of plugin preference settings.
     * @return the list of plugin preference settings
     */
    public static Collection<PreferenceSettingFactory> getPreferenceSetting() {
        Collection<PreferenceSettingFactory> settings = new ArrayList<>();
        for (PluginProxy plugin : pluginList) {
            settings.add(new PluginPreferenceFactory(plugin));
        }
        return settings;
    }

    /**
     * Installs downloaded plugins. Moves files with the suffix ".jar.new" to the corresponding ".jar" files.
     * <p>
     * If {@code dowarn} is true, this methods emits warning messages on the console if a downloaded
     * but not yet installed plugin .jar can't be be installed. If {@code dowarn} is false, the
     * installation of the respective plugin is silently skipped.
     *
     * @param pluginsToLoad list of plugin informations to update
     * @param dowarn if true, warning messages are displayed; false otherwise
     * @since 13294
     */
    public static void installDownloadedPlugins(Collection<PluginInformation> pluginsToLoad, boolean dowarn) {
        File pluginDir = Preferences.main().getPluginsDirectory();
        if (!pluginDir.exists() || !pluginDir.isDirectory() || !pluginDir.canWrite())
            return;

        final File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(".jar.new"));
        if (files == null)
            return;

        for (File updatedPlugin : files) {
            final String filePath = updatedPlugin.getPath();
            File plugin = new File(filePath.substring(0, filePath.length() - 4));
            String pluginName = updatedPlugin.getName().substring(0, updatedPlugin.getName().length() - 8);
            try {
                // Check the plugin is a valid and accessible JAR file before installing it (fix #7754)
                new JarFile(updatedPlugin).close();
            } catch (IOException e) {
                if (dowarn) {
                    Logging.log(Logging.LEVEL_WARN, tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. {2}",
                            plugin.toString(), updatedPlugin.toString(), e.getLocalizedMessage()), e);
                }
                continue;
            }
            if (plugin.exists() && !plugin.delete() && dowarn) {
                Logging.warn(tr("Failed to delete outdated plugin ''{0}''.", plugin.toString()));
                Logging.warn(tr("Failed to install already downloaded plugin ''{0}''. " +
                        "Skipping installation. JOSM is still going to load the old plugin version.",
                        pluginName));
                continue;
            }
            // Install plugin
            if (updatedPlugin.renameTo(plugin)) {
                try {
                    // Update plugin URL
                    URL newPluginURL = plugin.toURI().toURL();
                    URL oldPluginURL = updatedPlugin.toURI().toURL();
                    pluginsToLoad.stream().filter(x -> x.libraries.contains(oldPluginURL)).forEach(
                            x -> Collections.replaceAll(x.libraries, oldPluginURL, newPluginURL));

                    // Attempt to update loaded plugin (must implement Destroyable)
                    PluginInformation tInfo = pluginsToLoad.parallelStream()
                            .filter(x -> x.libraries.contains(newPluginURL)).findAny().orElse(null);
                    if (tInfo != null) {
                        Object tUpdatedPlugin = getPlugin(tInfo.name);
                        if (tUpdatedPlugin instanceof Destroyable) {
                            ((Destroyable) tUpdatedPlugin).destroy();
                            PluginHandler.loadPlugins(getInfoPanel(), Collections.singleton(tInfo),
                                    NullProgressMonitor.INSTANCE);
                        }
                    }
                } catch (MalformedURLException e) {
                    Logging.warn(e);
                }
            } else if (dowarn) {
                Logging.warn(tr("Failed to install plugin ''{0}'' from temporary download file ''{1}''. Renaming failed.",
                        plugin.toString(), updatedPlugin.toString()));
                Logging.warn(tr("Failed to install already downloaded plugin ''{0}''. " +
                        "Skipping installation. JOSM is still going to load the old plugin version.",
                        pluginName));
            }
        }
    }

    /**
     * Determines if the specified file is a valid and accessible JAR file.
     * @param jar The file to check
     * @return true if file can be opened as a JAR file.
     * @since 5723
     */
    public static boolean isValidJar(File jar) {
        if (jar != null && jar.exists() && jar.canRead()) {
            try {
                new JarFile(jar).close();
            } catch (IOException e) {
                Logging.warn(e);
                return false;
            }
            return true;
        } else if (jar != null) {
            Logging.debug("Invalid jar file ''"+jar+"'' (exists: "+jar.exists()+", canRead: "+jar.canRead()+')');
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
        File pluginDir = Preferences.main().getPluginsDirectory();
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
            } catch (PluginException e) {
                Logging.error(e);
            }
        }
    }

    private static int askUpdateDisableKeepPluginAfterException(PluginProxy plugin) {
        final ButtonSpec[] options = {
                new ButtonSpec(
                        tr("Update plugin"),
                        new ImageProvider(DIALOGS, "refresh"),
                        tr("Click to update the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr(DISABLE_PLUGIN),
                        new ImageProvider(DIALOGS, "delete"),
                        tr("Click to disable the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Keep plugin"),
                        new ImageProvider(CANCEL),
                        tr("Click to keep the plugin ''{0}''", plugin.getPluginInformation().name),
                        null /* no specific help context */
                )
        };

        final StringBuilder msg = new StringBuilder(256);
        msg.append(HTML_START)
           .append(tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.",
                   Utils.escapeReservedCharactersHTML(plugin.getPluginInformation().name)))
           .append("<br>");
        if (plugin.getPluginInformation().author != null) {
            msg.append(tr("According to the information within the plugin, the author is {0}.",
                    Utils.escapeReservedCharactersHTML(plugin.getPluginInformation().author)))
               .append("<br>");
        }
        msg.append(tr("Try updating to the newest version of this plugin before reporting a bug."))
           .append(HTML_END);

        try {
            FutureTask<Integer> task = new FutureTask<>(() -> HelpAwareOptionPane.showOptionDialog(
                    MainApplication.getMainFrame(),
                    msg.toString(),
                    tr(UPDATE_PLUGINS),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0],
                    ht("/ErrorMessages#ErrorInPlugin")
            ));
            GuiHelper.runInEDT(task);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logging.warn(e);
        } catch (ExecutionException e) {
            Logging.warn(e);
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
        List<StackTraceElement> stack = new ArrayList<>();
        Set<Throwable> seen = new HashSet<>();
        Throwable current = ex;
        while (current != null) {
            seen.add(current);
            stack.addAll(Arrays.asList(current.getStackTrace()));
            Throwable cause = current.getCause();
            if (cause != null && seen.contains(cause)) {
                break; // circular reference
            }
            current = cause;
        }

        // remember the error position, as multiple plugins may be involved, we search the topmost one
        int pos = stack.size();
        for (PluginProxy p : pluginList) {
            String baseClass = p.getPluginInformation().className;
            baseClass = baseClass.substring(0, baseClass.lastIndexOf('.'));
            for (int elpos = 0; elpos < pos; ++elpos) {
                if (stack.get(elpos).getClassName().startsWith(baseClass)) {
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

        Set<String> plugins = new HashSet<>(Config.getPref().getList(PLUGINS));
        final PluginInformation pluginInfo = plugin.getPluginInformation();
        if (!plugins.contains(pluginInfo.name))
            // plugin not activated ? strange in this context but anyway, don't bother
            // the user with dialogs, skip conditional deactivation
            return null;

        switch (askUpdateDisableKeepPluginAfterException(plugin)) {
        case 0:
            // update the plugin
            updatePlugins(MainApplication.getMainFrame(), Collections.singleton(pluginInfo), null, true);
            return pluginDownloadTask;
        case 1:
            // deactivate the plugin
            plugins.remove(plugin.getPluginInformation().name);
            Config.getPref().putList(PLUGINS, new ArrayList<>(plugins));
            GuiHelper.runInEDTAndWait(() -> JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            ));
            return null;
        default:
            // user doesn't want to deactivate the plugin
            return null;
        }
    }

    /**
     * Returns the list of loaded plugins as a {@code String} to be displayed in status report. Useful for bug reports.
     * @return The list of loaded plugins
     */
    public static Collection<String> getBugReportInformation() {
        final Collection<String> pl = new TreeSet<>(Config.getPref().getList(PLUGINS, new LinkedList<>()));
        for (final PluginProxy pp : pluginList) {
            PluginInformation pi = pp.getPluginInformation();
            pl.remove(pi.name);
            pl.add(pi.name + " (" + (!Utils.isEmpty(pi.localversion)
                    ? pi.localversion : "unknown") + ')');
        }
        return pl;
    }

    /**
     * Returns the list of loaded plugins as a {@code JPanel} to be displayed in About dialog.
     * @return The list of loaded plugins (one "line" of Swing components per plugin)
     */
    public static JPanel getInfoPanel() {
        JPanel pluginTab = new JPanel(new GridBagLayout());
        for (final PluginInformation info : getPlugins()) {
            String name = info.name
            + (!Utils.isEmpty(info.localversion) ? " Version: " + info.localversion : "");
            pluginTab.add(new JLabel(name), GBC.std());
            pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GridBagConstraints.HORIZONTAL));
            pluginTab.add(new JButton(new PluginInformationAction(info)), GBC.eol());

            JosmTextArea description = new JosmTextArea(info.description == null ? tr("no description available")
                    : info.description);
            description.setEditable(false);
            description.setFont(new JLabel().getFont().deriveFont(Font.ITALIC));
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            description.setBackground(UIManager.getColor("Panel.background"));
            description.setCaretPosition(0);

            pluginTab.add(description, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        }
        return pluginTab;
    }

    /**
     * Returns the set of deprecated and unmaintained plugins.
     * @return set of deprecated and unmaintained plugins names.
     * @since 8938
     */
    public static Set<String> getDeprecatedAndUnmaintainedPlugins() {
        Set<String> result = new HashSet<>(DEPRECATED_PLUGINS.size() + UNMAINTAINED_PLUGINS.size());
        for (DeprecatedPlugin dp : DEPRECATED_PLUGINS) {
            result.add(dp.name);
        }
        result.addAll(UNMAINTAINED_PLUGINS);
        return result;
    }

    private static class UpdatePluginsMessagePanel extends JPanel {
        private final JMultilineLabel lblMessage = new JMultilineLabel("");
        private final JCheckBox cbDontShowAgain = new JCheckBox(
                tr("Do not ask again and remember my decision (go to Preferences->Plugins to change it later)"));

        UpdatePluginsMessagePanel() {
            build();
        }

        protected final void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.insets = new Insets(5, 5, 5, 5);
            add(lblMessage, gc);
            lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN));

            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weighty = 0.0;
            add(cbDontShowAgain, gc);
            cbDontShowAgain.setFont(cbDontShowAgain.getFont().deriveFont(Font.PLAIN));
        }

        public void setMessage(String message) {
            lblMessage.setText(message);
        }

        /**
         * Returns the text. Useful for logging in {@link HelpAwareOptionPane#showOptionDialog}
         * @return the text
         */
        @Override
        public String toString() {
            return Utils.stripHtml(lblMessage.getText());
        }

        public void initDontShowAgain(String preferencesKey) {
            String policy = Config.getPref().get(preferencesKey, "ask");
            policy = policy.trim().toLowerCase(Locale.ENGLISH);
            cbDontShowAgain.setSelected(!"ask".equals(policy));
        }

        public boolean isRememberDecision() {
            return cbDontShowAgain.isSelected();
        }
    }

    /**
     * Remove deactivated plugins, returning true if JOSM should restart
     *
     * @param deactivatedPlugins The plugins to deactivate
     *
     * @return true if there was a plugin that requires a restart
     * @since 15508
     */
    public static boolean removePlugins(List<PluginInformation> deactivatedPlugins) {
        List<Destroyable> noRestart = deactivatedPlugins.parallelStream()
                .map(info -> PluginHandler.getPlugin(info.name)).filter(Destroyable.class::isInstance)
                .map(Destroyable.class::cast).collect(Collectors.toList());
        boolean restartNeeded;
        try {
            noRestart.forEach(Destroyable::destroy);
            new ArrayList<>(pluginList).stream().filter(proxy -> noRestart.contains(proxy.getPlugin()))
                    .forEach(pluginList::remove);
            restartNeeded = deactivatedPlugins.size() != noRestart.size();
        } catch (Exception e) {
            Logging.error(e);
            restartNeeded = true;
        }
        return restartNeeded;
    }
}
