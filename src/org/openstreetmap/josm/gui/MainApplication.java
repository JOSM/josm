// License: GPL. Copyright 2007 by Immanuel Scholz and others
//Licence: GPL
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.AutosaveTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.auth.CredentialsManagerFactory;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Main window class application.
 *
 * @author imi
 */
public class MainApplication extends Main {
    /**
     * Allow subclassing (see JOSM.java)
     */
    public MainApplication() {}

    /**
     * Construct an main frame, ready sized and operating. Does not
     * display the frame.
     */
    public MainApplication(JFrame mainFrame) {
        super();
        mainFrame.setContentPane(contentPanePrivate);
        mainFrame.setJMenuBar(menu);
        mainFrame.setBounds(bounds);
        mainFrame.setIconImage(ImageProvider.get("logo.png").getImage());
        mainFrame.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(final WindowEvent arg0) {
                Main.exitJosm(true);
            }
        });
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Displays help on the console
     *
     */
    public static void showHelp() {
        // TODO: put in a platformHook for system that have no console by default
        System.out.println(tr("Java OpenStreetMap Editor")+"\n\n"+
                tr("usage")+":\n"+
                "\tjava -jar josm.jar <options>...\n\n"+
                tr("options")+":\n"+
                "\t--help|-?|-h                              "+tr("Show this help")+"\n"+
                "\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+"\n"+
                "\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+"\n"+
                "\t[--download=]<url>                        "+tr("Download the location at the url (with lat=x&lon=y&zoom=z)")+"\n"+
                "\t[--download=]<filename>                   "+tr("Open a file (any file type that can be opened with File/Open)")+"\n"+
                "\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw gps")+"\n"+
                "\t--downloadgps=<url>                       "+tr("Download the location at the url (with lat=x&lon=y&zoom=z) as raw gps")+"\n"+
                "\t--selection=<searchstring>                "+tr("Select with the given search")+"\n"+
                "\t--[no-]maximize                           "+tr("Launch in maximized mode")+"\n"+
                "\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
                "\t--language=<language>                     "+tr("Set the language")+"\n\n"+
                tr("options provided as Java system properties")+":\n"+
                "\t-Djosm.home="+tr("/PATH/TO/JOSM/FOLDER/         ")+tr("Change the folder for all user settings")+"\n\n"+
                tr("note: For some tasks, JOSM needs a lot of memory. It can be necessary to add the following\n" +
                "      Java option to specify the maximum size of allocated memory in megabytes")+":\n"+
                "\t-Xmx...m\n\n"+
                tr("examples")+":\n"+
                "\tjava -jar josm.jar track1.gpx track2.gpx london.osm\n"+
                "\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13\n"+
                "\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml\n"+
                "\tjava -jar josm.jar 43.2,11.1,43.4,11.4\n"+
                "\tjava -Djosm.home=/home/user/.josm_dev -jar josm.jar\n"+
                "\tjava -Xmx400m -jar josm.jar\n\n"+
                tr("Parameters --download, --downloadgps, and --selection are processed in this order.")+"\n"+
                tr("Make sure you load some data if you use --selection.")+"\n"
        );
    }

    private static Map<String, Collection<String>> buildCommandLineArgumentMap(String[] args) {
        Map<String, Collection<String>> argMap = new HashMap<String, Collection<String>>();
        for (String arg : args) {
            if ("-h".equals(arg) || "-?".equals(arg)) {
                arg = "--help";
            }
            // handle simple arguments like file names, URLs, bounds
            if (!arg.startsWith("--")) {
                arg = "--download="+arg;
            }
            int i = arg.indexOf('=');
            String key = i == -1 ? arg.substring(2) : arg.substring(2,i);
            String value = i == -1 ? "" : arg.substring(i+1);
            Collection<String> v = argMap.get(key);
            if (v == null) {
                v = new LinkedList<String>();
            }
            v.add(value);
            argMap.put(key, v);
        }
        return argMap;
    }

    /**
     * Main application Startup
     */
    public static void main(final String[] argArray) {
        I18n.init();

        Policy.setPolicy(new Policy() {
            // Permissions for plug-ins loaded when josm is started via webstart
            private PermissionCollection pc;

            {
                pc = new Permissions();
                pc.add(new AllPermission());
            }

            @Override
            public void refresh() { }

            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                return pc;
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());
        // http://stuffthathappens.com/blog/2007/10/15/one-more-note-on-uncaught-exception-handlers/
        System.setProperty("sun.awt.exception.handler", BugReportExceptionHandler.class.getName());

        // initialize the plaform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we anything else
        Main.platform.preStartupHook();

        // construct argument table
        final Map<String, Collection<String>> args = buildCommandLineArgumentMap(argArray);

        Main.pref.init(args.containsKey("reset-preferences"));

        // Check if passed as parameter
        if (args.containsKey("language")) {
            I18n.set((String)(args.get("language").toArray()[0]));
        } else {
            I18n.set(Main.pref.get("language", null));
        }
        Main.pref.updateSystemProperties();

        DefaultAuthenticator.createInstance(CredentialsManagerFactory.getCredentialManager());
        Authenticator.setDefault(DefaultAuthenticator.getInstance());
        ProxySelector.setDefault(new DefaultProxySelector(ProxySelector.getDefault()));
        OAuthAccessTokenHolder.getInstance().init(Main.pref, CredentialsManagerFactory.getCredentialManager());

        // asking for help? show help and exit
        if (args.containsKey("help")) {
            showHelp();
            System.exit(0);
        }

        SplashScreen splash = new SplashScreen();
        ProgressMonitor monitor = splash.getProgressMonitor();
        monitor.beginTask(tr("Initializing"));
        monitor.setTicksCount(7);
        splash.setVisible(Main.pref.getBoolean("draw.splashscreen", true));

        List<PluginInformation> pluginsToLoad = PluginHandler.buildListOfPluginsToLoad(splash,monitor.createSubTaskMonitor(1, false));
        if (!pluginsToLoad.isEmpty() && PluginHandler.checkAndConfirmPluginUpdate(splash)) {
            monitor.subTask(tr("Updating plugins..."));
            pluginsToLoad = PluginHandler.updatePlugins(splash,pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        }
        monitor.worked(1);

        monitor.subTask(tr("Installing updated plugins"));
        PluginHandler.installDownloadedPlugins(true);
        monitor.worked(1);

        monitor.subTask(tr("Loading early plugins"));
        PluginHandler.loadEarlyPlugins(splash,pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        monitor.worked(1);

        monitor.subTask(tr("Setting defaults"));
        preConstructorInit(args);
        removeObsoletePreferences();
        monitor.worked(1);

        monitor.indeterminateSubTask(tr("Creating main GUI"));
        JFrame mainFrame = new JFrame(tr("Java OpenStreetMap Editor"));
        Main.parent = mainFrame;
        Main.addListener();
        final Main main = new MainApplication(mainFrame);
        monitor.worked(1);

        monitor.subTask(tr("Loading plugins"));
        PluginHandler.loadLatePlugins(splash,pluginsToLoad,  monitor.createSubTaskMonitor(1, false));
        monitor.worked(1);
        toolbar.refreshToolbarControl();
        splash.setVisible(false);
        splash.dispose();
        mainFrame.setVisible(true);

        boolean maximized = Boolean.parseBoolean(Main.pref.get("gui.maximized"));
        if ((!args.containsKey("no-maximize") && maximized) || args.containsKey("maximize")) {
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) {
                // Main.debug("Main window maximized");
                Main.windowState = JFrame.MAXIMIZED_BOTH;
                mainFrame.setExtendedState(Main.windowState);
            } else {
                Main.debug("Main window: maximizing not supported");
            }
        } else {
            // Main.debug("Main window not maximized");
        }

        AutosaveTask autosaveTask = new AutosaveTask();
        List<OsmDataLayer> unsavedLayers = autosaveTask.getUnsavedLayers();
        if (!unsavedLayers.isEmpty()) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Unsaved osm data"),
                    new String[] {tr("Restore"), tr("Cancel")}
            );
            dialog.setContent(tr("JOSM found {0} unsaved osm data layers. It looks like JOSM crashed last time. Do you want to restore data?",
                    unsavedLayers.size()));
            dialog.setButtonIcons(new String[] {"ok.png", "cancel.png"});
            dialog.showDialog();
            if (dialog.getValue() == 1) {
                for (OsmDataLayer layer: unsavedLayers) {
                    Main.main.addLayer(layer);
                }
                AutoScaleAction.autoScale("data");
            }


        }
        autosaveTask.schedule();


        EventQueue.invokeLater(new Runnable() {
            public void run() {
                main.postConstructorProcessCmdLine(args);
            }
        });
    }

    /**
     * Removes obsolete preference settings. If you throw out a once-used preference
     * setting, add it to the list here with an expiry date (written as comment). If you
     * see something with an expiry date in the past, remove it from the list.
     */
    public static void removeObsoletePreferences() {
        String[] obsolete = {
                "proxy.anonymous", // 01/2010 - not needed anymore. Can be removed mid 2010
                "proxy.enable"     // 01/2010 - not needed anymore. Can be removed mid 2010
        };
        for (String key : obsolete) {
            if (Main.pref.hasKey(key)) {
                Main.pref.removeFromCollection(key, Main.pref.get(key));
                System.out.println(tr("Preference setting {0} has been removed since it is no longer used.", key));
            }
        }
    }
}
