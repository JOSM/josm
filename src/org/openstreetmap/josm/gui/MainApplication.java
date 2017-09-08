// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ProxySelector;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
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

import org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenFileAction.OpenFileTask;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileSource;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.datum.NTV2Proj4DirGridShiftFileSource;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.ProgramArguments.Option;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.gui.bugreport.BugReportDialog;
import org.openstreetmap.josm.gui.download.DownloadDialog;
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
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.display.LafPreference;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.progress.swing.ProgressMonitorExecutor;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.RedirectInputMap;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.MessageNotifier;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.io.protocols.data.Handler;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.FontsManager;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.PlatformHook.NativeOsCallback;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;
import org.xml.sax.SAXException;

/**
 * Main window class application.
 *
 * @author imi
 */
@SuppressWarnings("deprecation")
public class MainApplication extends Main {

    /**
     * Command-line arguments used to run the application.
     */
    private static final List<String> COMMAND_LINE_ARGS = new ArrayList<>();

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

    private final MainFrame mainFrame;

    /**
     * The worker thread slave. This is for executing all long and intensive
     * calculations. The executed runnables are guaranteed to be executed separately and sequential.
     * @since 12634 (as a replacement to {@code Main.worker})
     */
    public static final ExecutorService worker = new ProgressMonitorExecutor("main-worker-%d", Thread.NORM_PRIORITY);
    static {
        Main.worker = worker;
    }

    /**
     * Provides access to the layers displayed in the main view.
     */
    private static final MainLayerManager layerManager = new MainLayerManager();

    /**
     * The commands undo/redo handler.
     * @since 12641
     */
    public static UndoRedoHandler undoRedo;

    private static final LayerChangeListener undoRedoCleaner = new LayerChangeListener() {
        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            Layer layer = e.getRemovedLayer();
            if (layer instanceof OsmDataLayer) {
                undoRedo.clean(((OsmDataLayer) layer).data);
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

    /**
     * Listener that sets the enabled state of undo/redo menu entries.
     */
    private final CommandQueueListener redoUndoListener = (queueSize, redoSize) -> {
            menu.undo.setEnabled(queueSize > 0);
            menu.redo.setEnabled(redoSize > 0);
        };

    /**
     * Source of NTV2 shift files: Download from JOSM website.
     * @since 12777
     */
    public static final NTV2GridShiftFileSource JOSM_WEBSITE_NTV2_SOURCE = gridFileName -> {
        String location = Main.getJOSMWebsite() + "/proj/" + gridFileName;
        // Try to load grid file
        CachedFile cf = new CachedFile(location);
        try {
            return cf.getInputStream();
        } catch (IOException ex) {
            Logging.warn(ex);
            return null;
        }
    };

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
    public MainApplication(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        undoRedo = super.undoRedo;
        getLayerManager().addLayerChangeListener(undoRedoCleaner);
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
                Main.parent,
                tr("Outdated Java version"),
                tr("OK"), tr("Update Java"), tr("Cancel"));
        // Check if the dialog has not already been permanently hidden by user
        if (!ed.toggleEnable("askUpdateJava"+updVersion).toggleCheckState()) {
            ed.setButtonIcons("ok", "java", "cancel").setCancelButton(3);
            ed.setMinimumSize(new Dimension(480, 300));
            ed.setIcon(JOptionPane.WARNING_MESSAGE);
            StringBuilder content = new StringBuilder(tr("You are running version {0} of Java.",
                    "<b>"+System.getProperty("java.version")+"</b>")).append("<br><br>");
            if ("Sun Microsystems Inc.".equals(System.getProperty("java.vendor")) && !platform.isOpenJDK()) {
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
                    platform.openUrl(url);
                } catch (IOException e) {
                    Logging.warn(e);
                }
            }
        }
    }

    @Override
    protected List<InitializationTask> beforeInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Starting file watcher"), fileWatcher::start),
            new InitializationTask(tr("Executing platform startup hook"), () -> platform.startupHook(MainApplication::askUpdateJava)),
            new InitializationTask(tr("Building main menu"), this::initializeMainWindow),
            new InitializationTask(tr("Updating user interface"), () -> {
                undoRedo.addCommandQueueListener(redoUndoListener);
                // creating toolbar
                GuiHelper.runInEDTAndWait(() -> contentPanePrivate.add(toolbar.control, BorderLayout.NORTH));
                // help shortcut
                registerActionShortcut(menu.help, Shortcut.registerShortcut("system:help", tr("Help"),
                        KeyEvent.VK_F1, Shortcut.DIRECT));
            }),
            // This needs to be done before RightAndLefthandTraffic::initialize is called
            new InitializationTask(tr("Initializing internal boundaries data"), Territories::initialize)
        );
    }

    @Override
    protected Collection<InitializationTask> parallelInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Initializing OSM API"), () -> {
                    // We try to establish an API connection early, so that any API
                    // capabilities are already known to the editor instance. However
                    // if it goes wrong that's not critical at this stage.
                    try {
                        OsmApi.getOsmApi().initialize(null, true);
                    } catch (OsmTransferCanceledException | OsmApiInitializationException e) {
                        Logging.warn(Logging.getErrorMessage(Utils.getRootCause(e)));
                    }
                }),
            new InitializationTask(tr("Initializing internal traffic data"), RightAndLefthandTraffic::initialize),
            new InitializationTask(tr("Initializing validator"), OsmValidator::initialize),
            new InitializationTask(tr("Initializing presets"), TaggingPresets::initialize),
            new InitializationTask(tr("Initializing map styles"), MapPaintPreference::initialize),
            new InitializationTask(tr("Loading imagery preferences"), ImageryPreference::initialize)
        );
    }

    @Override
    protected List<Callable<?>> asynchronousCallableTasks() {
        return Arrays.asList(
                OverpassTurboQueryWizard::getInstance
            );
    }

    @Override
    protected List<Runnable> asynchronousRunnableTasks() {
        return Arrays.asList(
                TMSLayer::getCache,
                OsmValidator::initializeTests
            );
    }

    @Override
    protected List<InitializationTask> afterInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Updating user interface"), () -> GuiHelper.runInEDTAndWait(() -> {
                // hooks for the jmapviewer component
                FeatureAdapter.registerBrowserAdapter(OpenBrowser::displayUrl);
                FeatureAdapter.registerTranslationAdapter(I18n::tr);
                FeatureAdapter.registerLoggingAdapter(name -> Logging.getLogger());
                // UI update
                toolbar.refreshToolbarControl();
                toolbar.control.updateUI();
                contentPanePrivate.updateUI();
            }))
        );
    }

    /**
     * Called once at startup to initialize the main window content.
     * Should set {@link #menu} and {@link #mainPanel}
     */
    @SuppressWarnings("deprecation")
    protected void initializeMainWindow() {
        if (mainFrame != null) {
            mainPanel = mainFrame.getPanel();
            panel = mainPanel;
            mainFrame.initialize();
            menu = mainFrame.getMenu();
            super.menu = menu;
        } else {
            // required for running some tests.
            mainPanel = new MainPanel(layerManager);
            panel = mainPanel;
            menu = new MainMenu();
            super.menu = menu;
        }
        mainPanel.addMapFrameListener((o, n) -> redoUndoListener.commandChanged(0, 0));
        mainPanel.reAddListeners();
    }

    @Override
    protected void shutdown() {
        if (!GraphicsEnvironment.isHeadless()) {
            worker.shutdown();
            JCSCacheManager.shutdown();
        }
        if (mainFrame != null) {
            mainFrame.storeState();
        }
        if (map != null) {
            map.rememberToggleDialogWidth();
        }
        // Remove all layers because somebody may rely on layerRemoved events (like AutosaveTask)
        layerManager.resetState();
        super.shutdown();
        if (!GraphicsEnvironment.isHeadless()) {
            worker.shutdownNow();
        }
    }

    @Override
    protected Bounds getRealBounds() {
        return isDisplayingMapView() ? map.mapView.getRealBounds() : null;
    }

    @Override
    protected void restoreOldBounds(Bounds oldBounds) {
        if (isDisplayingMapView()) {
            map.mapView.zoomTo(oldBounds);
        }
    }

    /**
     * Replies the current selected primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link DataSet#getSelected()}.
     * Indeed, if the user is currently in drawing mode, only the way currently being drawn is returned,
     * see {@link DrawAction#getInProgressSelection()}.
     *
     * @return The current selected primitives, from a end-user point of view. Can be {@code null}.
     * @since 6546
     */
    @Override
    public Collection<OsmPrimitive> getInProgressSelection() {
        if (map != null && map.mapMode instanceof DrawAction) {
            return ((DrawAction) map.mapMode).getInProgressSelection();
        } else {
            DataSet ds = layerManager.getEditDataSet();
            if (ds == null) return null;
            return ds.getSelected();
        }
    }

    @Override
    public DataSet getEditDataSet() {
        return getLayerManager().getEditDataSet();
    }

    @Override
    public void setEditDataSet(DataSet ds) {
        Optional<OsmDataLayer> layer = getLayerManager().getLayersOfType(OsmDataLayer.class).stream()
                .filter(l -> l.data.equals(ds)).findFirst();
        if (layer.isPresent()) {
            getLayerManager().setActiveLayer(layer.get());
        }
    }

    @Override
    public boolean containsDataSet(DataSet ds) {
        return getLayerManager().getLayersOfType(OsmDataLayer.class).stream().anyMatch(l -> l.data.equals(ds));
    }

    /**
     * Returns the command-line arguments used to run the application.
     * @return the command-line arguments used to run the application
     * @since 11650
     */
    public static List<String> getCommandLineArgs() {
        return Collections.unmodifiableList(COMMAND_LINE_ARGS);
    }

    /**
     * Returns the main layer manager that is used by the map view.
     * @return The layer manager. The value returned will never change.
     * @since 12636 (as a replacement to {@code Main.getLayerManager()})
     */
    @SuppressWarnings("deprecation")
    public static MainLayerManager getLayerManager() {
        return layerManager;
    }

    /**
     * Returns the MapFrame.
     * <p>
     * There should be no need to access this to access any map data. Use {@link #layerManager} instead.
     * @return the MapFrame
     * @see MainPanel
     * @since 12630 (as a replacement to {@code Main.map})
     */
    public static MapFrame getMap() {
        return map;
    }

    /**
     * Returns the main panel.
     * @return the main panel
     * @since 12642 (as a replacement to {@code Main.main.panel})
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
     * @since 12637 (as a replacement to {@code Main.toolbar})
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
    @SuppressWarnings("deprecation")
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
     * @since 12636 (specialized version of {@link Main#exitJosm})
     */
    public static boolean exitJosm(boolean exit, int exitCode, SaveLayersDialog.Reason reason) {
        final boolean proceed = Boolean.TRUE.equals(GuiHelper.runInEDTAndWaitAndReturn(() ->
                SaveLayersDialog.saveUnsavedModifications(layerManager.getLayers(),
                        reason != null ? reason : SaveLayersDialog.Reason.EXIT)));
        if (proceed) {
            return Main.exitJosm(exit, exitCode);
        }
        return false;
    }

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
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    public static boolean addMapFrameListener(MapFrameListener listener) {
        return mainPanel != null && mainPanel.addMapFrameListener(listener);
    }

    /**
     * Unregisters the given {@code MapFrameListener} from MapFrame changes
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @since 12639 (as a replacement to {@code Main.removeMapFrameListener})
     */
    @SuppressWarnings("deprecation")
    public static boolean removeMapFrameListener(MapFrameListener listener) {
        return mainPanel != null && mainPanel.removeMapFrameListener(listener);
    }

    /**
     * Registers a {@code JosmAction} and its shortcut.
     * @param action action defining its own shortcut
     * @since 12639 (as a replacement to {@code Main.registerActionShortcut})
     */
    @SuppressWarnings("deprecation")
    public static void registerActionShortcut(JosmAction action) {
        registerActionShortcut(action, action.getShortcut());
    }

    /**
     * Registers an action and its shortcut.
     * @param action action to register
     * @param shortcut shortcut to associate to {@code action}
     * @since 12639 (as a replacement to {@code Main.registerActionShortcut})
     */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    public static void unregisterShortcut(Shortcut shortcut) {
        contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortcut.getKeyStroke());
    }

    /**
     * Unregisters a {@code JosmAction} and its shortcut.
     * @param action action to unregister
     * @since 12639 (as a replacement to {@code Main.unregisterActionShortcut})
     */
    @SuppressWarnings("deprecation")
    public static void unregisterActionShortcut(JosmAction action) {
        unregisterActionShortcut(action, action.getShortcut());
    }

    /**
     * Unregisters an action and its shortcut.
     * @param action action to unregister
     * @param shortcut shortcut to unregister
     * @since 12639 (as a replacement to {@code Main.unregisterActionShortcut})
     */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
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
        return tr("Java OpenStreetMap Editor")+" ["
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
                align("\t-Djosm.dir.name=JOSM") + tr("Change the JOSM directory name") + "\n\n" +
                align("\t-Djosm.pref=" + tr("/PATH/TO/JOSM/PREF    ")) + tr("Set the preferences directory") + "\n" +
                align("\t") + tr("Default: {0}", platform.getDefaultPrefDirectory()) + "\n\n" +
                align("\t-Djosm.userdata=" + tr("/PATH/TO/JOSM/USERDATA")) + tr("Set the user data directory") + "\n" +
                align("\t") + tr("Default: {0}", platform.getDefaultUserDataDirectory()) + "\n\n" +
                align("\t-Djosm.cache=" + tr("/PATH/TO/JOSM/CACHE   ")) + tr("Set the cache directory") + "\n" +
                align("\t") + tr("Default: {0}", platform.getDefaultCacheDirectory()) + "\n\n" +
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
    @SuppressWarnings("deprecation")
    public static void main(final String[] argArray) {
        I18n.init();

        ProgramArguments args = null;
        // construct argument table
        try {
            args = new ProgramArguments(argArray);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        if (!GraphicsEnvironment.isHeadless()) {
            BugReportQueue.getInstance().setBugReportHandler(BugReportDialog::showFor);
            BugReportSender.setBugReportSendingHandler(BugReportDialog.bugReportSendingHandler);
        }

        Level logLevel = args.getLogLevel();
        Logging.setLogLevel(logLevel);
        if (!args.showVersion() && !args.showHelp()) {
            Logging.info(tr("Log level is at {0} ({1}, {2})", logLevel.getLocalizedName(), logLevel.getName(), logLevel.intValue()));
        }

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
        Main.platform.setNativeOsCallback(new DefaultNativeOsCallback());
        // call the really early hook before we do anything else
        Main.platform.preStartupHook();

        if (args.showVersion()) {
            System.out.println(Version.getInstance().getAgentString());
            return;
        } else if (args.showHelp()) {
            showHelp();
            return;
        }

        COMMAND_LINE_ARGS.addAll(Arrays.asList(argArray));

        boolean skipLoadingPlugins = args.hasOption(Option.SKIP_PLUGINS);
        if (skipLoadingPlugins) {
            Logging.info(tr("Plugin loading skipped"));
        }

        if (Logging.isLoggingEnabled(Logging.LEVEL_TRACE)) {
            // Enable debug in OAuth signpost via system preference, but only at trace level
            Utils.updateSystemProperty("debug", "true");
            Logging.info(tr("Enabled detailed debug level (trace)"));
        }

        Main.pref.init(args.hasOption(Option.RESET_PREFERENCES));

        args.getPreferencesToSet().forEach(Main.pref::put);

        if (!language.isPresent()) {
            I18n.set(Main.pref.get("language", null));
        }
        Main.pref.updateSystemProperties();

        checkIPv6();

        processOffline(args);

        Main.platform.afterPrefStartupHook();

        applyWorkarounds();

        FontsManager.initialize();

        GuiHelper.setupLanguageFonts();

        Handler.install();

        WindowGeometry geometry = WindowGeometry.mainWindow("gui.geometry",
                args.getSingle(Option.GEOMETRY).orElse(null),
                !args.hasOption(Option.NO_MAXIMIZE) && Main.pref.getBoolean("gui.maximized", false));
        final MainFrame mainFrame = new MainFrame(geometry);
        if (mainFrame.getContentPane() instanceof JComponent) {
            contentPanePrivate = (JComponent) mainFrame.getContentPane();
        }
        mainPanel = mainFrame.getPanel();
        Main.parent = mainFrame;

        if (args.hasOption(Option.LOAD_PREFERENCES)) {
            XMLCommandProcessor config = new XMLCommandProcessor(Main.pref);
            for (String i : args.get(Option.LOAD_PREFERENCES)) {
                Logging.info("Reading preferences from " + i);
                try (InputStream is = openStream(new URL(i))) {
                    config.openAndReadXML(is);
                } catch (IOException ex) {
                    throw BugReport.intercept(ex).put("file", i);
                }
            }
        }

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            Logging.warn(ex);
            Logging.warn(Logging.getErrorMessage(Utils.getRootCause(ex)));
        }
        Authenticator.setDefault(DefaultAuthenticator.getInstance());
        DefaultProxySelector proxySelector = new DefaultProxySelector(ProxySelector.getDefault());
        ProxySelector.setDefault(proxySelector);
        OAuthAccessTokenHolder.getInstance().init(Main.pref, CredentialsManager.getInstance());

        setupCallbacks();

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
        setupUIManager();
        toolbar = new ToolbarPreferences();
        Main.toolbar = toolbar;
        ProjectionPreference.setProjection();
        NTV2GridShiftFileWrapper.registerNTV2GridShiftFileSource(
                NTV2GridShiftFileWrapper.NTV2_SOURCE_PRIORITY_LOCAL,
                NTV2Proj4DirGridShiftFileSource.getInstance());
        NTV2GridShiftFileWrapper.registerNTV2GridShiftFileSource(
                NTV2GridShiftFileWrapper.NTV2_SOURCE_PRIORITY_DOWNLOAD,
                JOSM_WEBSITE_NTV2_SOURCE);
        GuiHelper.translateJavaInternalMessages();
        preConstructorInit();

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
                Logging.error(e);
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
            Logging.info("Enabled EDT checker, wrongful access to gui from non EDT thread will be printed to console");
            RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
        }
    }

    static void applyWorkarounds() {
        // Workaround for JDK-8180379: crash on Windows 10 1703 with Windows L&F and java < 8u141 / 9+172
        // To remove during Java 9 migration
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows 10") &&
                platform.getDefaultStyle().equals(LafPreference.LAF.get())) {
            try {
                final int currentBuild = Integer.parseInt(PlatformHookWindows.getCurrentBuild());
                final int javaVersion = Utils.getJavaVersion();
                final int javaUpdate = Utils.getJavaUpdate();
                final int javaBuild = Utils.getJavaBuild();
                // See https://technet.microsoft.com/en-us/windows/release-info.aspx
                if (currentBuild >= 15_063 && ((javaVersion == 8 && javaUpdate < 141)
                        || (javaVersion == 9 && javaUpdate == 0 && javaBuild < 173))) {
                    // Workaround from https://bugs.openjdk.java.net/browse/JDK-8179014
                    UIManager.put("FileChooser.useSystemExtensionHiding", Boolean.FALSE);
                }
            } catch (NumberFormatException | ReflectiveOperationException e) {
                Logging.error(e);
            }
        }
    }

    static void setupCallbacks() {
        MessageNotifier.setNotifierCallback(MainApplication::notifyNewMessages);
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
    }

    static void setupUIManager() {
        String defaultlaf = platform.getDefaultStyle();
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

        UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
        UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
        UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
        UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));
        // Ensures caret color is the same than text foreground color, see #12257
        // See http://docs.oracle.com/javase/8/docs/api/javax/swing/plaf/synth/doc-files/componentProperties.html
        for (String p : Arrays.asList(
                "EditorPane", "FormattedTextField", "PasswordField", "TextArea", "TextField", "TextPane")) {
            UIManager.put(p+".caretForeground", UIManager.getColor(p+".foreground"));
        }
    }

    private static InputStream openStream(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            return url.openStream();
        } else {
            return HttpClient.create(url).connect().getContent();
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
        GuiHelper.runInEDTAndWait(() -> toolbar.refreshToolbarControl());
    }

    private static void processOffline(ProgramArguments args) {
        for (String offlineNames : args.get(Option.OFFLINE)) {
            for (String s : offlineNames.split(",")) {
                try {
                    Main.setOffline(OnlineResource.valueOf(s.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    Logging.log(Logging.LEVEL_ERROR,
                            tr("''{0}'' is not a valid value for argument ''{1}''. Possible values are {2}, possibly delimited by commas.",
                            s.toUpperCase(Locale.ENGLISH), Option.OFFLINE.getName(), Arrays.toString(OnlineResource.values())), e);
                    System.exit(1);
                    return;
                }
            }
        }
        Set<OnlineResource> offline = Main.getOfflineResources();
        if (!offline.isEmpty()) {
            Logging.warn(trn("JOSM is running in offline mode. This resource will not be available: {0}",
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
                                SSLSocketFactory.getDefault().createSocket(a, 443).close();
                                Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true");
                                if (!wasv6) {
                                    Logging.info(tr("Detected useable IPv6 network, prefering IPv6 over IPv4 after next restart."));
                                } else {
                                    Logging.info(tr("Detected useable IPv6 network, prefering IPv6 over IPv4."));
                                }
                                hasv6 = true;
                            }
                            break; /* we're done */
                        }
                    }
                } catch (IOException | SecurityException e) {
                    Logging.debug("Exception while checking IPv6 connectivity: {0}", e);
                    Logging.trace(e);
                }
                if (wasv6 && !hasv6) {
                    Logging.info(tr("Detected no useable IPv6 network, prefering IPv4 over IPv6 after next restart."));
                    Main.pref.put("validated.ipv6", hasv6); // be sure it is stored before the restart!
                    try {
                        RestartAction.restartJOSM();
                    } catch (IOException e) {
                        Logging.error(e);
                    }
                }
                Main.pref.put("validated.ipv6", hasv6);
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
        Future<?> future = task.download(true, b, null);
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
                            Main.parent,
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
                autosaveTask.schedule();
            }
        }

        private static boolean handleNetworkOrProxyErrors(boolean hasErrors, String title, String message) {
            if (hasErrors) {
                ExtendedDialog ed = new ExtendedDialog(
                        Main.parent, title,
                        tr("Change proxy settings"), tr("Cancel"));
                ed.setButtonIcons("dialogs/settings", "cancel").setCancelButton(2);
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
            Map<String, Throwable> networkErrors = Main.getNetworkErrors();
            boolean condition = !networkErrors.isEmpty();
            if (condition) {
                Set<String> errors = new TreeSet<>();
                for (Throwable t : networkErrors.values()) {
                    errors.add(t.toString());
                }
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
                    while (Main.parent == null || !Main.parent.isVisible()) {
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
            panel.add(new UrlLabel(Main.getBaseUserUrl() + '/' + userInfo.getDisplayName() + "/inbox",
                    tr("Click here to see your inbox.")), GBC.eol());
            panel.setOpaque(false);
            new Notification().setContent(panel)
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_LONG)
                .show();
        });
    }
}
