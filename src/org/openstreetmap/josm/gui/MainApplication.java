// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.data.AutosaveTask;
import org.openstreetmap.josm.data.CustomConfigurator;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ProgramArguments.Option;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.MessageNotifier;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.FontsManager;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * Main window class application.
 *
 * @author imi
 */
public class MainApplication extends Main {

    private MainFrame mainFrame;

    /**
     * Constructs a new {@code MainApplication} without a window.
     */
    public MainApplication() {
        // Allow subclassing (see JOSM.java)
        this(null);
    }

    /**
     * Constructs a main frame, ready sized and operating. Does not display the frame.
     * @param mainFrame The main JFrame of the application
     * @since 10340
     */
    public MainApplication(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    protected void initializeMainWindow() {
        mainPanel.reAddListeners();
        if (mainFrame != null) {
            mainFrame.initialize();

            menu = mainFrame.getMenu();
        } else {
            // required for running some tests.
            menu = new MainMenu();
        }
    }

    @Override
    protected void shutdown() {
        mainFrame.storeState();
        super.shutdown();
    }

    /**
     * Displays help on the console
     * @since 2748
     */
    public static void showHelp() {
        // TODO: put in a platformHook for system that have no console by default
        System.out.println(tr("Java OpenStreetMap Editor")+" ["
                +Version.getInstance().getAgentString()+"]\n\n"+
                tr("usage")+":\n"+
                "\tjava -jar josm.jar <options>...\n\n"+
                tr("options")+":\n"+
                "\t--help|-h                                 "+tr("Show this help")+'\n'+
                "\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+'\n'+
                "\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+'\n'+
                "\t[--download=]<URL>                        "+tr("Download the location at the URL (with lat=x&lon=y&zoom=z)")+'\n'+
                "\t[--download=]<filename>                   "+tr("Open a file (any file type that can be opened with File/Open)")+'\n'+
                "\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw GPS")+'\n'+
                "\t--downloadgps=<URL>                       "+tr("Download the location at the URL (with lat=x&lon=y&zoom=z) as raw GPS")+'\n'+
                "\t--selection=<searchstring>                "+tr("Select with the given search")+'\n'+
                "\t--[no-]maximize                           "+tr("Launch in maximized mode")+'\n'+
                "\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
                "\t--load-preferences=<url-to-xml>           "+tr("Changes preferences according to the XML file")+"\n\n"+
                "\t--set=<key>=<value>                       "+tr("Set preference key to value")+"\n\n"+
                "\t--language=<language>                     "+tr("Set the language")+"\n\n"+
                "\t--version                                 "+tr("Displays the JOSM version and exits")+"\n\n"+
                "\t--debug                                   "+tr("Print debugging messages to console")+"\n\n"+
                "\t--skip-plugins                            "+tr("Skip loading plugins")+"\n\n"+
                "\t--offline=<osm_api|josm_website|all>      "+tr("Disable access to the given resource(s), separated by comma")+"\n\n"+
                tr("options provided as Java system properties")+":\n"+
                // CHECKSTYLE.OFF: SingleSpaceSeparator
                "\t-Djosm.pref="    +tr("/PATH/TO/JOSM/PREF    ")+tr("Set the preferences directory")+"\n\n"+
                "\t-Djosm.userdata="+tr("/PATH/TO/JOSM/USERDATA")+tr("Set the user data directory")+"\n\n"+
                "\t-Djosm.cache="   +tr("/PATH/TO/JOSM/CACHE   ")+tr("Set the cache directory")+"\n\n"+
                "\t-Djosm.home="    +tr("/PATH/TO/JOSM/HOMEDIR ")+
                // CHECKSTYLE.ON: SingleSpaceSeparator
                tr("Relocate all 3 directories to homedir. Cache directory will be in homedir/cache")+"\n\n"+
                tr("-Djosm.home has lower precedence, i.e. the specific setting overrides the general one")+"\n\n"+
                tr("note: For some tasks, JOSM needs a lot of memory. It can be necessary to add the following\n" +
                        "      Java option to specify the maximum size of allocated memory in megabytes")+":\n"+
                        "\t-Xmx...m\n\n"+
                        tr("examples")+":\n"+
                        "\tjava -jar josm.jar track1.gpx track2.gpx london.osm\n"+
                        "\tjava -jar josm.jar "+OsmUrlToBounds.getURL(43.2, 11.1, 13)+'\n'+
                        "\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml\n"+
                        "\tjava -jar josm.jar 43.2,11.1,43.4,11.4\n"+
                        "\tjava -Djosm.pref=$XDG_CONFIG_HOME -Djosm.userdata=$XDG_DATA_HOME -Djosm.cache=$XDG_CACHE_HOME -jar josm.jar\n"+
                        "\tjava -Djosm.home=/home/user/.josm_dev -jar josm.jar\n"+
                        "\tjava -Xmx1024m -jar josm.jar\n\n"+
                        tr("Parameters --download, --downloadgps, and --selection are processed in this order.")+'\n'+
                        tr("Make sure you load some data if you use --selection.")+'\n'
                );
    }

    /**
     * Main application Startup
     * @param argArray Command-line arguments
     */
    public static void main(final String[] argArray) {
        I18n.init();

        // construct argument table
        ProgramArguments args = null;
        try {
            args = new ProgramArguments(argArray);
        } catch (IllegalArgumentException e) {
            System.exit(1);
            return;
        }

        Level logLevel = args.getLogLevel();
        Logging.setLogLevel(logLevel);
        Main.info(tr("Log level is at ", logLevel));

        Optional<String> language = args.getSingle(Option.LANGUAGE);
        I18n.set(language.orElse(null));

        Policy.setPolicy(new Policy() {
            // Permissions for plug-ins loaded when josm is started via webstart
            private PermissionCollection pc;

            {
                pc = new Permissions();
                pc.add(new AllPermission());
            }

            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                return pc;
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());

        // initialize the platform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we do anything else
        Main.platform.preStartupHook();

        Main.COMMAND_LINE_ARGS.addAll(Arrays.asList(argArray));

        if (args.showVersion()) {
            System.out.println(Version.getInstance().getAgentString());
            System.exit(0);
        } else if (args.showHelp()) {
            showHelp();
            System.exit(0);
        }

        boolean skipLoadingPlugins = args.hasOption(Option.SKIP_PLUGINS);
        if (skipLoadingPlugins) {
            Main.info(tr("Plugin loading skipped"));
        }

        if (Logging.isLoggingEnabled(Logging.LEVEL_TRACE)) {
            // Enable debug in OAuth signpost via system preference, but only at trace level
            Utils.updateSystemProperty("debug", "true");
            Main.info(tr("Enabled detailed debug level (trace)"));
        }

        Main.pref.init(args.hasOption(Option.RESET_PREFERENCES));

        args.getPreferencesToSet().forEach(Main.pref::put);

        if (!language.isPresent()) {
            I18n.set(Main.pref.get("language", null));
        }
        Main.pref.updateSystemProperties();

        checkIPv6();

        // asking for help? show help and exit
        if (args.hasOption(Option.HELP)) {
            showHelp();
            System.exit(0);
        }

        processOffline(args);

        Main.platform.afterPrefStartupHook();

        FontsManager.initialize();

        I18n.setupLanguageFonts();

        WindowGeometry geometry = WindowGeometry.mainWindow("gui.geometry",
                args.getSingle(Option.GEOMETRY).orElse(null),
                !args.hasOption(Option.NO_MAXIMIZE) && Main.pref.getBoolean("gui.maximized", false));
        final MainFrame mainFrame = new MainFrame(contentPanePrivate, mainPanel, geometry);
        Main.parent = mainFrame;

        if (args.hasOption(Option.LOAD_PREFERENCES)) {
            CustomConfigurator.XMLCommandProcessor config = new CustomConfigurator.XMLCommandProcessor(Main.pref);
            for (String i : args.get(Option.LOAD_PREFERENCES)) {
                info("Reading preferences from " + i);
                try (InputStream is = HttpClient.create(new URL(i)).connect().getContent()) {
                    config.openAndReadXML(is);
                } catch (IOException ex) {
                    throw BugReport.intercept(ex).put("file", i);
                }
            }
        }

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            Main.warn(ex);
            Main.warn(getErrorMessage(Utils.getRootCause(ex)));
        }
        Authenticator.setDefault(DefaultAuthenticator.getInstance());
        DefaultProxySelector proxySelector = new DefaultProxySelector(ProxySelector.getDefault());
        ProxySelector.setDefault(proxySelector);
        OAuthAccessTokenHolder.getInstance().init(Main.pref, CredentialsManager.getInstance());

        final SplashScreen splash = GuiHelper.runInEDTAndWaitAndReturn(SplashScreen::new);
        final SplashScreen.SplashProgressMonitor monitor = splash.getProgressMonitor();
        monitor.beginTask(tr("Initializing"));
        GuiHelper.runInEDT(() -> splash.setVisible(Main.pref.getBoolean("draw.splashscreen", true)));
        Main.setInitStatusListener(new InitStatusListener() {

            @Override
            public Object updateStatus(String event) {
                monitor.beginTask(event);
                return event;
            }

            @Override
            public void finish(Object status) {
                if (status instanceof String) {
                    monitor.finishTask((String) status);
                }
            }
        });

        Collection<PluginInformation> pluginsToLoad = null;

        if (!skipLoadingPlugins) {
            pluginsToLoad = updateAndLoadEarlyPlugins(splash, monitor);
        }

        monitor.indeterminateSubTask(tr("Setting defaults"));
        preConstructorInit(args);

        monitor.indeterminateSubTask(tr("Creating main GUI"));
        final Main main = new MainApplication(mainFrame);
        main.initialize();

        if (!skipLoadingPlugins) {
            loadLatePlugins(splash, monitor, pluginsToLoad);
        }

        // Wait for splash disappearance (fix #9714)
        GuiHelper.runInEDTAndWait(() -> {
            splash.setVisible(false);
            splash.dispose();
            mainFrame.setVisible(true);
        });

        Main.MasterWindowListener.setup();

        boolean maximized = Main.pref.getBoolean("gui.maximized", false);
        if ((!args.hasOption(Option.NO_MAXIMIZE) && maximized) || args.hasOption(Option.MAXIMIZE)) {
            mainFrame.setMaximized(true);
        }
        if (main.menu.fullscreenToggleAction != null) {
            main.menu.fullscreenToggleAction.initial();
        }

        SwingUtilities.invokeLater(new GuiFinalizationWorker(args, proxySelector));

        if (Main.isPlatformWindows()) {
            try {
                // Check for insecure certificates to remove.
                // This is Windows-dependant code but it can't go to preStartupHook (need i18n)
                // neither startupHook (need to be called before remote control)
                PlatformHookWindows.removeInsecureCertificates();
            } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
                error(e);
            }
        }

        if (RemoteControl.PROP_REMOTECONTROL_ENABLED.get()) {
            RemoteControl.start();
        }

        if (MessageNotifier.PROP_NOTIFIER_ENABLED.get()) {
            MessageNotifier.start();
        }

        if (Main.pref.getBoolean("debug.edt-checker.enable", Version.getInstance().isLocalBuild())) {
            // Repaint manager is registered so late for a reason - there is lots of violation during startup process
            // but they don't seem to break anything and are difficult to fix
            info("Enabled EDT checker, wrongful access to gui from non EDT thread will be printed to console");
            RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
        }
    }

    static Collection<PluginInformation> updateAndLoadEarlyPlugins(SplashScreen splash, SplashProgressMonitor monitor) {
        Collection<PluginInformation> pluginsToLoad;
        pluginsToLoad = PluginHandler.buildListOfPluginsToLoad(splash, monitor.createSubTaskMonitor(1, false));
        if (!pluginsToLoad.isEmpty() && PluginHandler.checkAndConfirmPluginUpdate(splash)) {
            monitor.subTask(tr("Updating plugins"));
            pluginsToLoad = PluginHandler.updatePlugins(splash, null, monitor.createSubTaskMonitor(1, false), false);
        }

        monitor.indeterminateSubTask(tr("Installing updated plugins"));
        PluginHandler.installDownloadedPlugins(true);

        monitor.indeterminateSubTask(tr("Loading early plugins"));
        PluginHandler.loadEarlyPlugins(splash, pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        return pluginsToLoad;
    }

    static void loadLatePlugins(SplashScreen splash, SplashProgressMonitor monitor, Collection<PluginInformation> pluginsToLoad) {
        monitor.indeterminateSubTask(tr("Loading plugins"));
        PluginHandler.loadLatePlugins(splash, pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        toolbar.refreshToolbarControl();
    }

    private static void processOffline(ProgramArguments args) {
        for (String offlineNames : args.get(Option.OFFLINE)) {
            for (String s : offlineNames.split(",")) {
                try {
                    Main.setOffline(OnlineResource.valueOf(s.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    Main.error(e, tr("''{0}'' is not a valid value for argument ''{1}''. Possible values are {2}, possibly delimited by commas.",
                            s.toUpperCase(Locale.ENGLISH), Option.OFFLINE.getName(), Arrays.toString(OnlineResource.values())));
                    System.exit(1);
                    return;
                }
            }
        }
        Set<OnlineResource> offline = Main.getOfflineResources();
        if (!offline.isEmpty()) {
            Main.warn(trn("JOSM is running in offline mode. This resource will not be available: {0}",
                    "JOSM is running in offline mode. These resources will not be available: {0}",
                    offline.size(), offline.size() == 1 ? offline.iterator().next() : Arrays.toString(offline.toArray())));
        }
    }

    /**
     * Check if IPv6 can be safely enabled and do so. Because this cannot be done after network activation,
     * disabling or enabling IPV6 may only be done with next start.
     */
    private static void checkIPv6() {
        if ("auto".equals(Main.pref.get("prefer.ipv6", "auto"))) {
            new Thread((Runnable) () -> { /* this may take some time (DNS, Connect) */
                boolean hasv6 = false;
                boolean wasv6 = Main.pref.getBoolean("validated.ipv6", false);
                try {
                    /* Use the check result from last run of the software, as after the test, value
                       changes have no effect anymore */
                    if (wasv6) {
                        Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true");
                    }
                    for (InetAddress a : InetAddress.getAllByName("josm.openstreetmap.de")) {
                        if (a instanceof Inet6Address) {
                            if (a.isReachable(1000)) {
                                /* be sure it REALLY works */
                                Socket s = new Socket();
                                s.connect(new InetSocketAddress(a, 80), 1000);
                                s.close();
                                Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true");
                                if (!wasv6) {
                                    Main.info(tr("Detected useable IPv6 network, prefering IPv6 over IPv4 after next restart."));
                                } else {
                                    Main.info(tr("Detected useable IPv6 network, prefering IPv6 over IPv4."));
                                }
                                hasv6 = true;
                            }
                            break; /* we're done */
                        }
                    }
                } catch (IOException | SecurityException e) {
                    if (Main.isDebugEnabled()) {
                        Main.debug("Exception while checking IPv6 connectivity: "+e);
                    }
                    Main.trace(e);
                }
                if (wasv6 && !hasv6) {
                    Main.info(tr("Detected no useable IPv6 network, prefering IPv4 over IPv6 after next restart."));
                    Main.pref.put("validated.ipv6", hasv6); // be sure it is stored before the restart!
                    new RestartAction().actionPerformed(null);
                }
                Main.pref.put("validated.ipv6", hasv6);
            }, "IPv6-checker").start();
        }
    }

    private static class GuiFinalizationWorker implements Runnable {

        private final ProgramArguments args;
        private final DefaultProxySelector proxySelector;

        GuiFinalizationWorker(ProgramArguments args, DefaultProxySelector proxySelector) {
            this.args = args;
            this.proxySelector = proxySelector;
        }

        @Override
        public void run() {

            // Handle proxy/network errors early to inform user he should change settings to be able to use JOSM correctly
            if (!handleProxyErrors()) {
                handleNetworkErrors();
            }

            // Restore autosave layers after crash and start autosave thread
            handleAutosave();

            // Handle command line instructions
            postConstructorProcessCmdLine(args);

            // Show download dialog if autostart is enabled
            DownloadDialog.autostartIfNeeded();
        }

        private static void handleAutosave() {
            if (AutosaveTask.PROP_AUTOSAVE_ENABLED.get()) {
                AutosaveTask autosaveTask = new AutosaveTask();
                List<File> unsavedLayerFiles = autosaveTask.getUnsavedLayersFiles();
                if (!unsavedLayerFiles.isEmpty()) {
                    ExtendedDialog dialog = new ExtendedDialog(
                            Main.parent,
                            tr("Unsaved osm data"),
                            new String[] {tr("Restore"), tr("Cancel"), tr("Discard")}
                            );
                    dialog.setContent(
                            trn("JOSM found {0} unsaved osm data layer. ",
                                    "JOSM found {0} unsaved osm data layers. ", unsavedLayerFiles.size(), unsavedLayerFiles.size()) +
                                    tr("It looks like JOSM crashed last time. Would you like to restore the data?"));
                    dialog.setButtonIcons(new String[] {"ok", "cancel", "dialogs/delete"});
                    int selection = dialog.showDialog().getValue();
                    if (selection == 1) {
                        autosaveTask.recoverUnsavedLayers();
                    } else if (selection == 3) {
                        autosaveTask.discardUnsavedLayers();
                    }
                }
                autosaveTask.schedule();
            }
        }

        private static boolean handleNetworkOrProxyErrors(boolean hasErrors, String title, String message) {
            if (hasErrors) {
                ExtendedDialog ed = new ExtendedDialog(
                        Main.parent, title,
                        new String[]{tr("Change proxy settings"), tr("Cancel")});
                ed.setButtonIcons(new String[]{"dialogs/settings", "cancel"}).setCancelButton(2);
                ed.setMinimumSize(new Dimension(460, 260));
                ed.setIcon(JOptionPane.WARNING_MESSAGE);
                ed.setContent(message);

                if (ed.showDialog().getValue() == 1) {
                    PreferencesAction.forPreferenceSubTab(null, null, ProxyPreference.class).run();
                }
            }
            return hasErrors;
        }

        private boolean handleProxyErrors() {
            return handleNetworkOrProxyErrors(proxySelector.hasErrors(), tr("Proxy errors occurred"),
                    tr("JOSM tried to access the following resources:<br>" +
                            "{0}" +
                            "but <b>failed</b> to do so, because of the following proxy errors:<br>" +
                            "{1}" +
                            "Would you like to change your proxy settings now?",
                            Utils.joinAsHtmlUnorderedList(proxySelector.getErrorResources()),
                            Utils.joinAsHtmlUnorderedList(proxySelector.getErrorMessages())
                    ));
        }

        private static boolean handleNetworkErrors() {
            boolean condition = !NETWORK_ERRORS.isEmpty();
            if (condition) {
                Set<String> errors = new TreeSet<>();
                for (Throwable t : NETWORK_ERRORS.values()) {
                    errors.add(t.toString());
                }
                return handleNetworkOrProxyErrors(condition, tr("Network errors occurred"),
                        tr("JOSM tried to access the following resources:<br>" +
                                "{0}" +
                                "but <b>failed</b> to do so, because of the following network errors:<br>" +
                                "{1}" +
                                "It may be due to a missing proxy configuration.<br>" +
                                "Would you like to change your proxy settings now?",
                                Utils.joinAsHtmlUnorderedList(NETWORK_ERRORS.keySet()),
                                Utils.joinAsHtmlUnorderedList(errors)
                        ));
            }
            return false;
        }
    }
}
