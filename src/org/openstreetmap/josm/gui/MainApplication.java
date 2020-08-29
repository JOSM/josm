// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.AWTError;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.GeneralSecurityException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLSocketFactory;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenFileAction.OpenFileTask;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.cli.CLIModule;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.data.projection.ProjectionBoundsProvider;
import org.openstreetmap.josm.data.projection.ProjectionCLI;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileSource;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.NTV2Proj4DirGridShiftFileSource;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.ProgramArguments.Option;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.gui.bugreport.BugReportDialog;
import org.openstreetmap.josm.gui.bugreport.DefaultBugReportSendingHandler;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.io.CredentialDialog;
import org.openstreetmap.josm.gui.io.CustomConfigurator.XMLCommandProcessor;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.AutosaveTask;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.RenderingCLI;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.display.LafPreference;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.progress.swing.ProgressMonitorExecutor;
import org.openstreetmap.josm.gui.util.CheckThreadViolationRepaintManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.RedirectInputMap;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.ChangesetUpdater;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.MessageNotifier;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.AbstractCredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.io.protocols.data.Handler;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.lifecycle.InitStatusListener;
import org.openstreetmap.josm.spi.lifecycle.Lifecycle;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.FontsManager;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.PlatformHook.NativeOsCallback;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.ReflectionUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;
import org.xml.sax.SAXException;

/**
 * Main window class application.
 *
 * @author imi
 */
public class MainApplication {

    /**
     * Command-line arguments used to run the application.
     */
    private static volatile List<String> commandLineArgs;

    /**
     * The main menu bar at top of screen.
     */
    static MainMenu menu;

    /**
     * The main panel, required to be static for {@link MapFrameListener} handling.
     */
    static MainPanel mainPanel;

    /**
     * The private content pane of {@link MainFrame}, required to be static for shortcut handling.
     */
    static JComponent contentPanePrivate;

    /**
     * The MapFrame.
     */
    static MapFrame map;

    /**
     * The toolbar preference control to register new actions.
     */
    static volatile ToolbarPreferences toolbar;

    private static MainFrame mainFrame;

    /**
     * The worker thread slave. This is for executing all long and intensive
     * calculations. The executed runnables are guaranteed to be executed separately and sequential.
     * @since 12634 (as a replacement to {@code Main.worker})
     */
    public static final ExecutorService worker = new ProgressMonitorExecutor("main-worker-%d", Thread.NORM_PRIORITY);

    /**
     * Provides access to the layers displayed in the main view.
     */
    private static final MainLayerManager layerManager = new MainLayerManager();

    private static final LayerChangeListener undoRedoCleaner = new LayerChangeListener() {
        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            Layer layer = e.getRemovedLayer();
            if (layer instanceof OsmDataLayer) {
                UndoRedoHandler.getInstance().clean(((OsmDataLayer) layer).getDataSet());
            }
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // Do nothing
        }

        @Override
        public void layerAdded(LayerAddEvent e) {
            // Do nothing
        }
    };

    private static final ProjectionBoundsProvider mainBoundsProvider = new ProjectionBoundsProvider() {
        @Override
        public Bounds getRealBounds() {
            return isDisplayingMapView() ? map.mapView.getRealBounds() : null;
        }

        @Override
        public void restoreOldBounds(Bounds oldBounds) {
            if (isDisplayingMapView()) {
                map.mapView.zoomTo(oldBounds);
            }
        }
    };

    private static final List<CLIModule> cliModules = new ArrayList<>();

    /**
     * Default JOSM command line interface.
     * <p>
     * Runs JOSM and performs some action, depending on the options and positional
     * arguments.
     */
    public static final CLIModule JOSM_CLI_MODULE = new CLIModule() {
        @Override
        public String getActionKeyword() {
            return "runjosm";
        }

        @Override
        public void processArguments(String[] argArray) {
            ProgramArguments args = null;
            // construct argument table
            try {
                args = new ProgramArguments(argArray);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            mainJOSM(args);
        }
    };

    /**
     * Listener that sets the enabled state of undo/redo menu entries.
     */
    final CommandQueueListener redoUndoListener = (queueSize, redoSize) -> {
            menu.undo.setEnabled(queueSize > 0);
            menu.redo.setEnabled(redoSize > 0);
        };

    /**
     * Source of NTV2 shift files: Download from JOSM website.
     * @since 12777
     */
    public static final NTV2GridShiftFileSource JOSM_WEBSITE_NTV2_SOURCE = gridFileName -> {
        String location = Config.getUrls().getJOSMWebsite() + "/proj/" + gridFileName;
        // Try to load grid file
        @SuppressWarnings("resource")
        CachedFile cf = new CachedFile(location);
        try {
            return cf.getInputStream();
        } catch (IOException ex) {
            Logging.warn(ex);
            return null;
        }
    };

    static {
        registerCLIModule(JOSM_CLI_MODULE);
        registerCLIModule(ProjectionCLI.INSTANCE);
        registerCLIModule(RenderingCLI.INSTANCE);
    }

    /**
     * Register a command line interface module.
     * @param module the module
     * @since 12886
     */
    public static void registerCLIModule(CLIModule module) {
        cliModules.add(module);
    }

    /**
     * Constructs a new {@code MainApplication} without a window.
     */
    public MainApplication() {
        this(null);
    }

    /**
     * Constructs a main frame, ready sized and operating. Does not display the frame.
     * @param mainFrame The main JFrame of the application
     * @since 10340
     */
    @SuppressWarnings("StaticAssignmentInConstructor")
    public MainApplication(MainFrame mainFrame) {
        MainApplication.mainFrame = mainFrame;
        getLayerManager().addLayerChangeListener(undoRedoCleaner);
        ProjectionRegistry.setboundsProvider(mainBoundsProvider);
        Lifecycle.setShutdownSequence(new MainTermination());
    }

    /**
     * Asks user to update its version of Java.
     * @param updVersion target update version
     * @param url download URL
     * @param major true for a migration towards a major version of Java (8:9), false otherwise
     * @param eolDate the EOL/expiration date
     * @since 12270
     */
    public static void askUpdateJava(String updVersion, String url, String eolDate, boolean major) {
        ExtendedDialog ed = new ExtendedDialog(
                mainFrame,
                tr("Outdated Java version"),
                tr("OK"), tr("Update Java"), tr("Cancel"));
        // Check if the dialog has not already been permanently hidden by user
        if (!ed.toggleEnable("askUpdateJava"+updVersion).toggleCheckState()) {
            ed.setButtonIcons("ok", "java", "cancel").setCancelButton(3);
            ed.setMinimumSize(new Dimension(480, 300));
            ed.setIcon(JOptionPane.WARNING_MESSAGE);
            StringBuilder content = new StringBuilder(tr("You are running version {0} of Java.",
                    "<b>"+getSystemProperty("java.version")+"</b>")).append("<br><br>");
            if ("Sun Microsystems Inc.".equals(getSystemProperty("java.vendor")) && !PlatformManager.getPlatform().isOpenJDK()) {
                content.append("<b>").append(tr("This version is no longer supported by {0} since {1} and is not recommended for use.",
                        "Oracle", eolDate)).append("</b><br><br>");
            }
            content.append("<b>")
                   .append(major ?
                        tr("JOSM will soon stop working with this version; we highly recommend you to update to Java {0}.", updVersion) :
                        tr("You may face critical Java bugs; we highly recommend you to update to Java {0}.", updVersion))
                   .append("</b><br><br>")
                   .append(tr("Would you like to update now ?"));
            ed.setContent(content.toString());

            if (ed.showDialog().getValue() == 2) {
                try {
                    PlatformManager.getPlatform().openUrl(url);
                } catch (IOException e) {
                    Logging.warn(e);
                }
            }
        }
    }

    /**
     * Called once at startup to initialize the main window content.
     * Should set {@link #menu} and {@link #mainPanel}
     */
    protected void initializeMainWindow() {
        if (mainFrame != null) {
            mainPanel = mainFrame.getPanel();
            mainFrame.initialize();
            menu = mainFrame.getMenu();
        } else {
            // required for running some tests.
            mainPanel = new MainPanel(layerManager);
            menu = new MainMenu();
        }
        mainPanel.addMapFrameListener((o, n) -> redoUndoListener.commandChanged(0, 0));
        mainPanel.reAddListeners();
    }

    /**
     * Returns the JOSM main frame.
     * @return the JOSM main frame
     * @since 14140
     */
    public static MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Returns the command-line arguments used to run the application.
     * @return the command-line arguments used to run the application
     * @since 11650
     */
    public static List<String> getCommandLineArgs() {
        return commandLineArgs == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(commandLineArgs);
    }

    /**
     * Returns the main layer manager that is used by the map view.
     * @return The layer manager. The value returned will never change.
     * @since 12636 (as a replacement to {@code Main.getLayerManager()})
     */
    public static MainLayerManager getLayerManager() {
        return layerManager;
    }

    /**
     * Returns the MapFrame.
     * <p>
     * There should be no need to access this to access any map data. Use {@link #layerManager} instead.
     * @return the MapFrame
     * @see MainPanel
     * @since 12630
     */
    public static MapFrame getMap() {
        return map;
    }

    /**
     * Returns the main panel.
     * @return the main panel
     * @since 12642
     */
    public static MainPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Returns the main menu, at top of screen.
     * @return the main menu
     * @since 12643 (as a replacement to {@code MainApplication.getMenu()})
     */
    public static MainMenu getMenu() {
        return menu;
    }

    /**
     * Returns the toolbar preference control to register new actions.
     * @return the toolbar preference control
     * @since 12637
     */
    public static ToolbarPreferences getToolbar() {
        return toolbar;
    }

    /**
     * Replies true if JOSM currently displays a map view. False, if it doesn't, i.e. if
     * it only shows the MOTD panel.
     * <p>
     * You do not need this when accessing the layer manager. The layer manager will be empty if no map view is shown.
     *
     * @return <code>true</code> if JOSM currently displays a map view
     * @since 12630 (as a replacement to {@code Main.isDisplayingMapView()})
     */
    public static boolean isDisplayingMapView() {
        return map != null && map.mapView != null;
    }

    /**
     * Closes JOSM and optionally terminates the Java Virtual Machine (JVM).
     * If there are some unsaved data layers, asks first for user confirmation.
     * @param exit If {@code true}, the JVM is terminated by running {@link System#exit} with a given return code.
     * @param exitCode The return code
     * @param reason the reason for exiting
     * @return {@code true} if JOSM has been closed, {@code false} if the user has cancelled the operation.
     * @since 12636 (specialized version of {@link Lifecycle#exitJosm})
     */
    public static boolean exitJosm(boolean exit, int exitCode, SaveLayersDialog.Reason reason) {
        final boolean proceed = Boolean.TRUE.equals(GuiHelper.runInEDTAndWaitAndReturn(() ->
                SaveLayersDialog.saveUnsavedModifications(layerManager.getLayers(),
                        reason != null ? reason : SaveLayersDialog.Reason.EXIT)));
        if (proceed) {
            return Lifecycle.exitJosm(exit, exitCode);
        }
        return false;
    }

    /**
     * Redirects the key inputs from {@code source} to main content pane.
     * @param source source component from which key inputs are redirected
     */
    public static void redirectToMainContentPane(JComponent source) {
        RedirectInputMap.redirect(source, contentPanePrivate);
    }

    /**
     * Registers a new {@code MapFrameListener} that will be notified of MapFrame changes.
     * <p>
     * It will fire an initial mapFrameInitialized event when the MapFrame is present.
     * Otherwise will only fire when the MapFrame is created or destroyed.
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @see #addMapFrameListener
     * @since 12639 (as a replacement to {@code Main.addAndFireMapFrameListener})
     */
    public static boolean addAndFireMapFrameListener(MapFrameListener listener) {
        return mainPanel != null && mainPanel.addAndFireMapFrameListener(listener);
    }

    /**
     * Registers a new {@code MapFrameListener} that will be notified of MapFrame changes
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @see #addAndFireMapFrameListener
     * @since 12639 (as a replacement to {@code Main.addMapFrameListener})
     */
    public static boolean addMapFrameListener(MapFrameListener listener) {
        return mainPanel != null && mainPanel.addMapFrameListener(listener);
    }

    /**
     * Unregisters the given {@code MapFrameListener} from MapFrame changes
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @since 12639 (as a replacement to {@code Main.removeMapFrameListener})
     */
    public static boolean removeMapFrameListener(MapFrameListener listener) {
        return mainPanel != null && mainPanel.removeMapFrameListener(listener);
    }

    /**
     * Registers a {@code JosmAction} and its shortcut.
     * @param action action defining its own shortcut
     * @since 12639 (as a replacement to {@code Main.registerActionShortcut})
     */
    public static void registerActionShortcut(JosmAction action) {
        registerActionShortcut(action, action.getShortcut());
    }

    /**
     * Registers an action and its shortcut.
     * @param action action to register
     * @param shortcut shortcut to associate to {@code action}
     * @since 12639 (as a replacement to {@code Main.registerActionShortcut})
     */
    public static void registerActionShortcut(Action action, Shortcut shortcut) {
        KeyStroke keyStroke = shortcut.getKeyStroke();
        if (keyStroke == null)
            return;

        InputMap inputMap = contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        Object existing = inputMap.get(keyStroke);
        if (existing != null && !existing.equals(action)) {
            Logging.info(String.format("Keystroke %s is already assigned to %s, will be overridden by %s", keyStroke, existing, action));
        }
        inputMap.put(keyStroke, action);

        contentPanePrivate.getActionMap().put(action, action);
    }

    /**
     * Unregisters a shortcut.
     * @param shortcut shortcut to unregister
     * @since 12639 (as a replacement to {@code Main.unregisterShortcut})
     */
    public static void unregisterShortcut(Shortcut shortcut) {
        contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortcut.getKeyStroke());
    }

    /**
     * Unregisters a {@code JosmAction} and its shortcut.
     * @param action action to unregister
     * @since 12639 (as a replacement to {@code Main.unregisterActionShortcut})
     */
    public static void unregisterActionShortcut(JosmAction action) {
        unregisterActionShortcut(action, action.getShortcut());
    }

    /**
     * Unregisters an action and its shortcut.
     * @param action action to unregister
     * @param shortcut shortcut to unregister
     * @since 12639 (as a replacement to {@code Main.unregisterActionShortcut})
     */
    public static void unregisterActionShortcut(Action action, Shortcut shortcut) {
        unregisterShortcut(shortcut);
        contentPanePrivate.getActionMap().remove(action);
    }

    /**
     * Replies the registered action for the given shortcut
     * @param shortcut The shortcut to look for
     * @return the registered action for the given shortcut
     * @since 12639 (as a replacement to {@code Main.getRegisteredActionShortcut})
     */
    public static Action getRegisteredActionShortcut(Shortcut shortcut) {
        KeyStroke keyStroke = shortcut.getKeyStroke();
        if (keyStroke == null)
            return null;
        Object action = contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(keyStroke);
        if (action instanceof Action)
            return (Action) action;
        return null;
    }

    /**
     * Displays help on the console
     * @since 2748
     */
    public static void showHelp() {
        // TODO: put in a platformHook for system that have no console by default
        System.out.println(getHelp());
    }

    static String getHelp() {
        // IMPORTANT: when changing the help texts, also update:
        // - native/linux/tested/usr/share/man/man1/josm.1
        // - native/linux/latest/usr/share/man/man1/josm-latest.1
        return tr("Java OpenStreetMap Editor")+" ["
                +Version.getInstance().getAgentString()+"]\n\n"+
                tr("usage")+":\n"+
                "\tjava -jar josm.jar [<command>] <options>...\n\n"+
                tr("commands")+":\n"+
                "\trunjosm     "+tr("launch JOSM (default, performed when no command is specified)")+'\n'+
                "\trender      "+tr("render data and save the result to an image file")+'\n'+
                "\tproject     "+tr("convert coordinates from one coordinate reference system to another")+"\n\n"+
                tr("For details on the {0} and {1} commands, run them with the {2} option.", "render", "project", "--help")+'\n'+
                tr("The remainder of this help page documents the {0} command.", "runjosm")+"\n\n"+
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
                "\t--offline=" + Arrays.stream(OnlineResource.values()).map(OnlineResource::name).collect(
                        Collectors.joining("|", "<", ">")) + "\n" +
                "\t                                          "+tr("Disable access to the given resource(s), separated by comma") + "\n" +
                "\t                                          "+Arrays.stream(OnlineResource.values()).map(OnlineResource::getLocName).collect(
                        Collectors.joining("|", "<", ">")) + "\n\n" +
                tr("options provided as Java system properties")+":\n"+
                align("\t-Djosm.dir.name=JOSM") + tr("Change the JOSM directory name") + "\n\n" +
                align("\t-Djosm.pref=" + tr("/PATH/TO/JOSM/PREF    ")) + tr("Set the preferences directory") + "\n" +
                align("\t") + tr("Default: {0}", PlatformManager.getPlatform().getDefaultPrefDirectory()) + "\n\n" +
                align("\t-Djosm.userdata=" + tr("/PATH/TO/JOSM/USERDATA")) + tr("Set the user data directory") + "\n" +
                align("\t") + tr("Default: {0}", PlatformManager.getPlatform().getDefaultUserDataDirectory()) + "\n\n" +
                align("\t-Djosm.cache=" + tr("/PATH/TO/JOSM/CACHE   ")) + tr("Set the cache directory") + "\n" +
                align("\t") + tr("Default: {0}", PlatformManager.getPlatform().getDefaultCacheDirectory()) + "\n\n" +
                align("\t-Djosm.home=" + tr("/PATH/TO/JOSM/HOMEDIR ")) +
                tr("Set the preferences+data+cache directory (cache directory will be josm.home/cache)")+"\n\n"+
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
                "\tjava -Djosm.dir.name=josm_dev -jar josm.jar\n"+
                "\tjava -Djosm.home=/home/user/.josm_dev -jar josm.jar\n"+
                "\tjava -Xmx1024m -jar josm.jar\n\n"+
                tr("Parameters --download, --downloadgps, and --selection are processed in this order.")+'\n'+
                tr("Make sure you load some data if you use --selection.")+'\n';
    }

    private static String align(String str) {
        return str + Stream.generate(() -> " ").limit(Math.max(0, 43 - str.length())).collect(Collectors.joining(""));
    }

    /**
     * Main application Startup
     * @param argArray Command-line arguments
     */
    public static void main(final String[] argArray) {
        I18n.init();
        commandLineArgs = Arrays.asList(Arrays.copyOf(argArray, argArray.length));

        if (argArray.length > 0) {
            String moduleStr = argArray[0];
            for (CLIModule module : cliModules) {
                if (Objects.equals(moduleStr, module.getActionKeyword())) {
                   String[] argArrayCdr = Arrays.copyOfRange(argArray, 1, argArray.length);
                   module.processArguments(argArrayCdr);
                   return;
                }
            }
        }
        // no module specified, use default (josm)
        JOSM_CLI_MODULE.processArguments(argArray);
    }

    /**
     * Main method to run the JOSM GUI.
     * @param args program arguments
     */
    public static void mainJOSM(ProgramArguments args) {

        if (!GraphicsEnvironment.isHeadless()) {
            BugReportQueue.getInstance().setBugReportHandler(BugReportDialog::showFor);
            BugReportSender.setBugReportSendingHandler(new DefaultBugReportSendingHandler());
        }

        Level logLevel = args.getLogLevel();
        Logging.setLogLevel(logLevel);
        if (!args.showVersion() && !args.showHelp()) {
            Logging.info(tr("Log level is at {0} ({1}, {2})", logLevel.getLocalizedName(), logLevel.getName(), logLevel.intValue()));
        }

        Optional<String> language = args.getSingle(Option.LANGUAGE);
        I18n.set(language.orElse(null));

        try {
            Policy.setPolicy(new Policy() {
                // Permissions for plug-ins loaded when josm is started via webstart
                private final PermissionCollection pc;

                {
                    pc = new Permissions();
                    pc.add(new AllPermission());
                }

                @Override
                public PermissionCollection getPermissions(CodeSource codesource) {
                    return pc;
                }
            });
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set permissions", e);
        }

        try {
            Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set uncaught exception handler", e);
        }

        // initialize the platform hook, and
        PlatformManager.getPlatform().setNativeOsCallback(new DefaultNativeOsCallback());
        // call the really early hook before we do anything else
        PlatformManager.getPlatform().preStartupHook();

        Preferences prefs = Preferences.main();
        Config.setPreferencesInstance(prefs);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());

        if (args.showVersion()) {
            System.out.println(Version.getInstance().getAgentString());
            return;
        } else if (args.showHelp()) {
            showHelp();
            return;
        }

        boolean skipLoadingPlugins = args.hasOption(Option.SKIP_PLUGINS);
        if (skipLoadingPlugins) {
            Logging.info(tr("Plugin loading skipped"));
        }

        if (Logging.isLoggingEnabled(Logging.LEVEL_TRACE)) {
            // Enable debug in OAuth signpost via system preference, but only at trace level
            Utils.updateSystemProperty("debug", "true");
            Logging.info(tr("Enabled detailed debug level (trace)"));
        }

        try {
            Preferences.main().init(args.hasOption(Option.RESET_PREFERENCES));
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to initialize preferences", e);
        }

        args.getPreferencesToSet().forEach(prefs::put);

        if (!language.isPresent()) {
            I18n.set(Config.getPref().get("language", null));
        }
        updateSystemProperties();
        Preferences.main().addPreferenceChangeListener(e -> updateSystemProperties());

        checkIPv6();

        processOffline(args);

        PlatformManager.getPlatform().afterPrefStartupHook();

        applyWorkarounds();

        FontsManager.initialize();

        GuiHelper.setupLanguageFonts();

        Handler.install();

        WindowGeometry geometry = WindowGeometry.mainWindow("gui.geometry",
                args.getSingle(Option.GEOMETRY).orElse(null),
                !args.hasOption(Option.NO_MAXIMIZE) && Config.getPref().getBoolean("gui.maximized", false));
        final MainFrame mainFrame = createMainFrame(geometry);
        final Container contentPane = mainFrame.getContentPane();
        if (contentPane instanceof JComponent) {
            contentPanePrivate = (JComponent) contentPane;
        }
        mainPanel = mainFrame.getPanel();

        if (args.hasOption(Option.LOAD_PREFERENCES)) {
            XMLCommandProcessor config = new XMLCommandProcessor(prefs);
            for (String i : args.get(Option.LOAD_PREFERENCES)) {
                try {
                    URL url = i.contains(":/") ? new URL(i) : Paths.get(i).toUri().toURL();
                    Logging.info("Reading preferences from " + url);
                    try (InputStream is = Utils.openStream(url)) {
                        config.openAndReadXML(is);
                    }
                } catch (IOException | InvalidPathException ex) {
                    Logging.error(ex);
                    return;
                }
            }
        }

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            Logging.warn(ex);
            Logging.warn(Logging.getErrorMessage(Utils.getRootCause(ex)));
        }
        try {
            Authenticator.setDefault(DefaultAuthenticator.getInstance());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set default authenticator", e);
        }
        DefaultProxySelector proxySelector = null;
        try {
            proxySelector = new DefaultProxySelector(ProxySelector.getDefault());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get default proxy selector", e);
        }
        try {
            if (proxySelector != null) {
                ProxySelector.setDefault(proxySelector);
            }
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set default proxy selector", e);
        }
        OAuthAccessTokenHolder.getInstance().init(CredentialsManager.getInstance());

        setupCallbacks();

        if (!skipLoadingPlugins) {
            PluginHandler.loadVeryEarlyPlugins();
        }
        // Configure Look and feel before showing SplashScreen (#19290)
        setupUIManager();

        final SplashScreen splash = GuiHelper.runInEDTAndWaitAndReturn(SplashScreen::new);
        // splash can be null sometimes on Linux, in this case try to load JOSM silently
        final SplashProgressMonitor monitor = splash != null ? splash.getProgressMonitor() : new SplashProgressMonitor(null, e -> {
            if (e != null) {
                Logging.debug(e.toString());
            }
        });
        monitor.beginTask(tr("Initializing"));
        if (splash != null) {
            GuiHelper.runInEDT(() -> splash.setVisible(Config.getPref().getBoolean("draw.splashscreen", true)));
        }
        Lifecycle.setInitStatusListener(new InitStatusListener() {

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
        toolbar = new ToolbarPreferences();
        ProjectionPreference.setProjection();
        setupNadGridSources();
        GuiHelper.translateJavaInternalMessages();

        monitor.indeterminateSubTask(tr("Creating main GUI"));
        Lifecycle.initialize(new MainInitialization(new MainApplication(mainFrame)));

        if (!skipLoadingPlugins) {
            loadLatePlugins(splash, monitor, pluginsToLoad);
        }

        // Wait for splash disappearance (fix #9714)
        GuiHelper.runInEDTAndWait(() -> {
            if (splash != null) {
                splash.setVisible(false);
                splash.dispose();
            }
            mainFrame.setVisible(true);
        });

        boolean maximized = Config.getPref().getBoolean("gui.maximized", false);
        if ((!args.hasOption(Option.NO_MAXIMIZE) && maximized) || args.hasOption(Option.MAXIMIZE)) {
            mainFrame.setMaximized(true);
        }
        if (menu.fullscreenToggleAction != null) {
            menu.fullscreenToggleAction.initial();
        }

        SwingUtilities.invokeLater(new GuiFinalizationWorker(args, proxySelector));

        if (RemoteControl.PROP_REMOTECONTROL_ENABLED.get()) {
            RemoteControl.start();
        }

        if (MessageNotifier.PROP_NOTIFIER_ENABLED.get()) {
            MessageNotifier.start();
        }

        ChangesetUpdater.start();

        if (Config.getPref().getBoolean("debug.edt-checker.enable", Version.getInstance().isLocalBuild())) {
            // Repaint manager is registered so late for a reason - there is lots of violation during startup process
            // but they don't seem to break anything and are difficult to fix
            Logging.info("Enabled EDT checker, wrongful access to gui from non EDT thread will be printed to console");
            RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
        }
    }

    private static MainFrame createMainFrame(WindowGeometry geometry) {
        try {
            return new MainFrame(geometry);
        } catch (AWTError e) {
            // #12022 #16666 On Debian, Ubuntu and Linux Mint the first AWT toolkit access can fail because of ATK wrapper
            // Good news: the error happens after the toolkit initialization so we can just try again and it will work
            Logging.error(e);
            return new MainFrame(geometry);
        }
    }

    /**
     * Updates system properties with the current values in the preferences.
     */
    private static void updateSystemProperties() {
        if ("true".equals(Config.getPref().get("prefer.ipv6", "auto"))
                && !"true".equals(Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true"))) {
            // never set this to false, only true!
            Logging.info(tr("Try enabling IPv6 network, preferring IPv6 over IPv4 (only works on early startup)."));
        }
        Utils.updateSystemProperty("http.agent", Version.getInstance().getAgentString());
        Utils.updateSystemProperty("user.language", Config.getPref().get("language"));
        // Workaround to fix a Java bug. This ugly hack comes from Sun bug database: https://bugs.openjdk.java.net/browse/JDK-6292739
        // Force AWT toolkit to update its internal preferences (fix #6345).
        // Does not work anymore with Java 9, to remove with Java 9 migration
        if (Utils.getJavaVersion() < 9 && !GraphicsEnvironment.isHeadless()) {
            try {
                Field field = Toolkit.class.getDeclaredField("resources");
                ReflectionUtils.setObjectsAccessible(field);
                field.set(null, ResourceBundle.getBundle("sun.awt.resources.awt"));
            } catch (ReflectiveOperationException | RuntimeException e) { // NOPMD
                // Catch RuntimeException in order to catch InaccessibleObjectException, new in Java 9
                Logging.log(Logging.LEVEL_WARN, null, e);
            }
        }
        // Possibility to disable SNI (not by default) in case of misconfigured https servers
        // See #9875 + http://stackoverflow.com/a/14884941/2257172
        // then https://josm.openstreetmap.de/ticket/12152#comment:5 for details
        if (Config.getPref().getBoolean("jdk.tls.disableSNIExtension", false)) {
            Utils.updateSystemProperty("jsse.enableSNIExtension", "false");
        }
        // Disable automatic POST retry after 5 minutes, see #17882 / https://bugs.openjdk.java.net/browse/JDK-6382788
        Utils.updateSystemProperty("sun.net.http.retryPost", "false");
    }

    /**
     * Setup the sources for NTV2 grid shift files for projection support.
     * @since 12795
     */
    public static void setupNadGridSources() {
        NTV2GridShiftFileWrapper.registerNTV2GridShiftFileSource(
                NTV2GridShiftFileWrapper.NTV2_SOURCE_PRIORITY_LOCAL,
                NTV2Proj4DirGridShiftFileSource.getInstance());
        NTV2GridShiftFileWrapper.registerNTV2GridShiftFileSource(
                NTV2GridShiftFileWrapper.NTV2_SOURCE_PRIORITY_DOWNLOAD,
                JOSM_WEBSITE_NTV2_SOURCE);
    }

    static void applyWorkarounds() {
        // Workaround for JDK-8180379: crash on Windows 10 1703 with Windows L&F and java < 8u141 / 9+172
        // To remove during Java 9 migration
        if (getSystemProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows 10") &&
                PlatformManager.getPlatform().getDefaultStyle().equals(LafPreference.LAF.get())) {
            try {
                String build = PlatformHookWindows.getCurrentBuild();
                if (build != null) {
                    final int currentBuild = Integer.parseInt(build);
                    final int javaVersion = Utils.getJavaVersion();
                    final int javaUpdate = Utils.getJavaUpdate();
                    final int javaBuild = Utils.getJavaBuild();
                    // See https://technet.microsoft.com/en-us/windows/release-info.aspx
                    if (currentBuild >= 15_063 && ((javaVersion == 8 && javaUpdate < 141)
                            || (javaVersion == 9 && javaUpdate == 0 && javaBuild < 173))) {
                        // Workaround from https://bugs.openjdk.java.net/browse/JDK-8179014
                        UIManager.put("FileChooser.useSystemExtensionHiding", Boolean.FALSE);
                    }
                }
            } catch (NumberFormatException | ReflectiveOperationException | JosmRuntimeException e) {
                Logging.error(e);
            } catch (ExceptionInInitializerError e) {
                Logging.log(Logging.LEVEL_ERROR, null, e);
            }
        }
    }

    static void setupCallbacks() {
        HttpClient.setFactory(Http1Client::new);
        OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);
        AbstractCredentialsAgent.setCredentialsProvider(CredentialDialog::promptCredentials);
        MessageNotifier.setNotifierCallback(MainApplication::notifyNewMessages);
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
        SplitWayCommand.setWarningNotifier(msg -> new Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show());
        FileWatcher.registerLoader(SourceType.MAP_PAINT_STYLE, MapPaintStyleLoader::reloadStyle);
        FileWatcher.registerLoader(SourceType.TAGCHECKER_RULE, MapCSSTagChecker::reloadRule);
        OsmUrlToBounds.setMapSizeSupplier(() -> {
            if (isDisplayingMapView()) {
                MapView mapView = getMap().mapView;
                return new Dimension(mapView.getWidth(), mapView.getHeight());
            } else {
                return GuiHelper.getScreenSize();
            }
        });
    }

    static void setupUIManager() {
        String defaultlaf = PlatformManager.getPlatform().getDefaultStyle();
        String laf = LafPreference.LAF.get();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            // Try to find look and feel in plugin classloaders
            Logging.trace(e);
            Class<?> klass = null;
            for (ClassLoader cl : PluginHandler.getResourceClassLoaders()) {
                try {
                    klass = cl.loadClass(laf);
                    break;
                } catch (ClassNotFoundException ex) {
                    Logging.trace(ex);
                }
            }
            if (klass != null && LookAndFeel.class.isAssignableFrom(klass)) {
                try {
                    UIManager.setLookAndFeel((LookAndFeel) klass.getConstructor().newInstance());
                } catch (ReflectiveOperationException ex) {
                    Logging.log(Logging.LEVEL_WARN, "Cannot set Look and Feel: " + laf + ": "+ex.getMessage(), ex);
                } catch (UnsupportedLookAndFeelException ex) {
                    Logging.info("Look and Feel not supported: " + laf);
                    LafPreference.LAF.put(defaultlaf);
                    Logging.trace(ex);
                }
            } else {
                Logging.info("Look and Feel not found: " + laf);
                LafPreference.LAF.put(defaultlaf);
            }
        } catch (UnsupportedLookAndFeelException e) {
            Logging.info("Look and Feel not supported: " + laf);
            LafPreference.LAF.put(defaultlaf);
            Logging.trace(e);
        } catch (InstantiationException | IllegalAccessException e) {
            Logging.error(e);
        }

        UIManager.put("OptionPane.okIcon", ImageProvider.getIfAvailable("ok"));
        UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
        UIManager.put("OptionPane.cancelIcon", ImageProvider.getIfAvailable("cancel"));
        UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));
        // Ensures caret color is the same than text foreground color, see #12257
        // See https://docs.oracle.com/javase/8/docs/api/javax/swing/plaf/synth/doc-files/componentProperties.html
        for (String p : Arrays.asList(
                "EditorPane", "FormattedTextField", "PasswordField", "TextArea", "TextField", "TextPane")) {
            UIManager.put(p+".caretForeground", UIManager.getColor(p+".foreground"));
        }

        scaleFonts(Config.getPref().getDouble("gui.scale.menu.font", 1.0),
                "Menu.font", "MenuItem.font", "CheckBoxMenuItem.font", "RadioButtonMenuItem.font", "MenuItem.acceleratorFont");
        scaleFonts(Config.getPref().getDouble("gui.scale.list.font", 1.0),
                "List.font");
        // "Table.font" see org.openstreetmap.josm.gui.util.TableHelper.setFont
    }

    private static void scaleFonts(double factor, String... fonts) {
        if (factor == 1.0) {
            return;
        }
        for (String key : fonts) {
            Font font = UIManager.getFont(key);
            if (font != null) {
                font = font.deriveFont((float) (font.getSize2D() * factor));
                UIManager.put(key, new FontUIResource(font));
            }
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
        try {
            PluginHandler.installDownloadedPlugins(pluginsToLoad, true);
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to install plugins", e);
        }

        monitor.indeterminateSubTask(tr("Loading early plugins"));
        PluginHandler.loadEarlyPlugins(splash, pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        return pluginsToLoad;
    }

    static void loadLatePlugins(SplashScreen splash, SplashProgressMonitor monitor, Collection<PluginInformation> pluginsToLoad) {
        monitor.indeterminateSubTask(tr("Loading plugins"));
        PluginHandler.loadLatePlugins(splash, pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        GuiHelper.runInEDTAndWait(() -> toolbar.refreshToolbarControl());
    }

    private static void processOffline(ProgramArguments args) {
        for (String offlineNames : args.get(Option.OFFLINE)) {
            for (String s : offlineNames.split(",", -1)) {
                try {
                    NetworkManager.setOffline(OnlineResource.valueOf(s.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    Logging.log(Logging.LEVEL_ERROR,
                            tr("''{0}'' is not a valid value for argument ''{1}''. Possible values are {2}, possibly delimited by commas.",
                            s.toUpperCase(Locale.ENGLISH), Option.OFFLINE.getName(), Arrays.toString(OnlineResource.values())), e);
                    System.exit(1);
                    return;
                }
            }
        }
        Set<OnlineResource> offline = NetworkManager.getOfflineResources();
        if (!offline.isEmpty()) {
            Logging.warn(trn("JOSM is running in offline mode. This resource will not be available: {0}",
                    "JOSM is running in offline mode. These resources will not be available: {0}",
                    offline.size(), offline.stream().map(OnlineResource::getLocName).collect(Collectors.joining(", "))));
        }
    }

    /**
     * Check if IPv6 can be safely enabled and do so. Because this cannot be done after network activation,
     * disabling or enabling IPV6 may only be done with next start.
     */
    private static void checkIPv6() {
        if ("auto".equals(Config.getPref().get("prefer.ipv6", "auto"))) {
            new Thread((Runnable) () -> { /* this may take some time (DNS, Connect) */
                boolean hasv6 = false;
                boolean wasv6 = Config.getPref().getBoolean("validated.ipv6", false);
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
                                SSLSocketFactory.getDefault().createSocket(a, 443).close();
                                hasv6 = true;
                                /* in case of routing problems to the main openstreetmap domain don't enable IPv6 */
                                for (InetAddress b : InetAddress.getAllByName("api.openstreetmap.org")) {
                                    if (b instanceof Inet6Address) {
                                        if (b.isReachable(1000)) {
                                            SSLSocketFactory.getDefault().createSocket(b, 443).close();
                                        } else {
                                            hasv6 = false;
                                        }
                                        break; /* we're done */
                                    }
                                }
                                Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true");
                                if (!wasv6) {
                                    Logging.info(tr("Detected useable IPv6 network, preferring IPv6 over IPv4 after next restart."));
                                } else {
                                    Logging.info(tr("Detected useable IPv6 network, preferring IPv6 over IPv4."));
                                }
                            }
                            break; /* we're done */
                        }
                    }
                } catch (IOException | SecurityException e) {
                    Logging.debug("Exception while checking IPv6 connectivity: {0}", e);
                    hasv6 = false;
                    Logging.trace(e);
                }
                if (wasv6 && !hasv6) {
                    Logging.info(tr("Detected no useable IPv6 network, preferring IPv4 over IPv6 after next restart."));
                    Config.getPref().putBoolean("validated.ipv6", hasv6); // be sure it is stored before the restart!
                    try {
                        RestartAction.restartJOSM();
                    } catch (IOException e) {
                        Logging.error(e);
                    }
                }
                Config.getPref().putBoolean("validated.ipv6", hasv6);
            }, "IPv6-checker").start();
        }
    }

    /**
     * Download area specified as Bounds value.
     * @param rawGps Flag to download raw GPS tracks
     * @param b The bounds value
     * @return the complete download task (including post-download handler)
     */
    static List<Future<?>> downloadFromParamBounds(final boolean rawGps, Bounds b) {
        DownloadTask task = rawGps ? new DownloadGpsTask() : new DownloadOsmTask();
        // asynchronously launch the download task ...
        Future<?> future = task.download(new DownloadParams().withNewLayer(true), b, null);
        // ... and the continuation when the download is finished (this will wait for the download to finish)
        return Collections.singletonList(MainApplication.worker.submit(new PostDownloadHandler(task, future)));
    }

    /**
     * Handle command line instructions after GUI has been initialized.
     * @param args program arguments
     * @return the list of submitted tasks
     */
    static List<Future<?>> postConstructorProcessCmdLine(ProgramArguments args) {
        List<Future<?>> tasks = new ArrayList<>();
        List<File> fileList = new ArrayList<>();
        for (String s : args.get(Option.DOWNLOAD)) {
            tasks.addAll(DownloadParamType.paramType(s).download(s, fileList));
        }
        if (!fileList.isEmpty()) {
            tasks.add(OpenFileAction.openFiles(fileList, true));
        }
        for (String s : args.get(Option.DOWNLOADGPS)) {
            tasks.addAll(DownloadParamType.paramType(s).downloadGps(s));
        }
        final Collection<String> selectionArguments = args.get(Option.SELECTION);
        if (!selectionArguments.isEmpty()) {
            tasks.add(MainApplication.worker.submit(() -> {
                for (String s : selectionArguments) {
                    SearchAction.search(s, SearchMode.add);
                }
            }));
        }
        return tasks;
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
                            mainFrame,
                            tr("Unsaved osm data"),
                            tr("Restore"), tr("Cancel"), tr("Discard")
                            );
                    dialog.setContent(
                            trn("JOSM found {0} unsaved osm data layer. ",
                                    "JOSM found {0} unsaved osm data layers. ", unsavedLayerFiles.size(), unsavedLayerFiles.size()) +
                                    tr("It looks like JOSM crashed last time. Would you like to restore the data?"));
                    dialog.setButtonIcons("ok", "cancel", "dialogs/delete");
                    int selection = dialog.showDialog().getValue();
                    if (selection == 1) {
                        autosaveTask.recoverUnsavedLayers();
                    } else if (selection == 3) {
                        autosaveTask.discardUnsavedLayers();
                    }
                }
                try {
                    autosaveTask.schedule();
                } catch (SecurityException e) {
                    Logging.log(Logging.LEVEL_ERROR, "Unable to schedule autosave!", e);
                }
            }
        }

        private static boolean handleNetworkOrProxyErrors(boolean hasErrors, String title, String message) {
            if (hasErrors) {
                ExtendedDialog ed = new ExtendedDialog(
                        mainFrame, title,
                        tr("Change proxy settings"), tr("Cancel"));
                ed.setButtonIcons("preference", "cancel").setCancelButton(2);
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
            return proxySelector != null &&
                handleNetworkOrProxyErrors(proxySelector.hasErrors(), tr("Proxy errors occurred"),
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
            Map<String, Throwable> networkErrors = NetworkManager.getNetworkErrors();
            boolean condition = !networkErrors.isEmpty();
            if (condition) {
                Set<String> errors = networkErrors.values().stream()
                        .map(Throwable::toString)
                        .collect(Collectors.toCollection(TreeSet::new));
                return handleNetworkOrProxyErrors(condition, tr("Network errors occurred"),
                        tr("JOSM tried to access the following resources:<br>" +
                                "{0}" +
                                "but <b>failed</b> to do so, because of the following network errors:<br>" +
                                "{1}" +
                                "It may be due to a missing proxy configuration.<br>" +
                                "Would you like to change your proxy settings now?",
                                Utils.joinAsHtmlUnorderedList(networkErrors.keySet()),
                                Utils.joinAsHtmlUnorderedList(errors)
                        ));
            }
            return false;
        }
    }

    private static class DefaultNativeOsCallback implements NativeOsCallback {
        @Override
        public void openFiles(List<File> files) {
            Executors.newSingleThreadExecutor(Utils.newThreadFactory("openFiles-%d", Thread.NORM_PRIORITY)).submit(
                    new OpenFileTask(files, null) {
                @Override
                protected void realRun() throws SAXException, IOException, OsmTransferException {
                    // Wait for JOSM startup is advanced enough to load a file
                    while (mainFrame == null || !mainFrame.isVisible()) {
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            Logging.warn(e);
                            Thread.currentThread().interrupt();
                        }
                    }
                    super.realRun();
                }
            });
        }

        @Override
        public boolean handleQuitRequest() {
            return MainApplication.exitJosm(false, 0, null);
        }

        @Override
        public void handleAbout() {
            MainApplication.getMenu().about.actionPerformed(null);
        }

        @Override
        public void handlePreferences() {
            MainApplication.getMenu().preferences.actionPerformed(null);
        }
    }

    static void notifyNewMessages(UserInfo userInfo) {
        GuiHelper.runInEDT(() -> {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(new JLabel(trn("You have {0} unread message.", "You have {0} unread messages.",
                    userInfo.getUnreadMessages(), userInfo.getUnreadMessages())),
                    GBC.eol());
            panel.add(new UrlLabel(Config.getUrls().getBaseUserUrl() + '/' + userInfo.getDisplayName() + "/inbox",
                    tr("Click here to see your inbox.")), GBC.eol());
            panel.setOpaque(false);
            new Notification().setContent(panel)
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_LONG)
                .show();
        });
    }
}
