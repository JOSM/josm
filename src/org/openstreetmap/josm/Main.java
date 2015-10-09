// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.GettingStarted;
import org.openstreetmap.josm.gui.MainApplication.Option;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitorExecutor;
import org.openstreetmap.josm.gui.tagging.TaggingPresets;
import org.openstreetmap.josm.gui.util.RedirectInputMap;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformHookOsx;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Abstract class holding various static global variables and methods used in large parts of JOSM application.
 * @since 98
 */
public abstract class Main {

    /**
     * The JOSM website URL.
     * @since 6897 (was public from 6143 to 6896)
     */
    private static final String JOSM_WEBSITE = "https://josm.openstreetmap.de";

    /**
     * The OSM website URL.
     * @since 6897 (was public from 6453 to 6896)
     */
    private static final String OSM_WEBSITE = "https://www.openstreetmap.org";

    /**
     * Replies true if JOSM currently displays a map view. False, if it doesn't, i.e. if
     * it only shows the MOTD panel.
     *
     * @return <code>true</code> if JOSM currently displays a map view
     */
    public static boolean isDisplayingMapView() {
        if (map == null) return false;
        if (map.mapView == null) return false;
        return true;
    }

    /**
     * Global parent component for all dialogs and message boxes
     */
    public static Component parent;

    /**
     * Global application.
     */
    public static volatile Main main;

    /**
     * Command-line arguments used to run the application.
     */
    protected static final List<String> COMMAND_LINE_ARGS = new ArrayList<>();

    /**
     * The worker thread slave. This is for executing all long and intensive
     * calculations. The executed runnables are guaranteed to be executed separately
     * and sequential.
     */
    public static final ExecutorService worker = new ProgressMonitorExecutor("main-worker-%d", Thread.NORM_PRIORITY);

    /**
     * Global application preferences
     */
    public static Preferences pref;

    /**
     * The global paste buffer.
     */
    public static final PrimitiveDeepCopy pasteBuffer = new PrimitiveDeepCopy();

    /**
     * The layer source from which {@link Main#pasteBuffer} data comes from.
     */
    public static Layer pasteSource;

    /**
     * The MapFrame. Use {@link Main#setMapFrame} to set or clear it.
     */
    public static MapFrame map;

    /**
     * The toolbar preference control to register new actions.
     */
    public static volatile ToolbarPreferences toolbar;

    /**
     * The commands undo/redo handler.
     */
    public final UndoRedoHandler undoRedo = new UndoRedoHandler();

    /**
     * The progress monitor being currently displayed.
     */
    public static PleaseWaitProgressMonitor currentProgressMonitor;

    /**
     * The main menu bar at top of screen.
     */
    public MainMenu menu;

    /**
     * The data validation handler.
     */
    public OsmValidator validator;

    /**
     * The file watcher service.
     */
    public static final FileWatcher fileWatcher = new FileWatcher();

    /**
     * The MOTD Layer.
     */
    public final GettingStarted gettingStarted = new GettingStarted();

    private static final Collection<MapFrameListener> mapFrameListeners = new ArrayList<>();

    protected static final Map<String, Throwable> NETWORK_ERRORS = new HashMap<>();

    // First lines of last 5 error and warning messages, used for bug reports
    private static final List<String> ERRORS_AND_WARNINGS = Collections.<String>synchronizedList(new ArrayList<String>());

    private static final Set<OnlineResource> OFFLINE_RESOURCES = new HashSet<>();

    /**
     * Logging level (5 = trace, 4 = debug, 3 = info, 2 = warn, 1 = error, 0 = none).
     * @since 6248
     */
    public static int logLevel = 3;

    private static void rememberWarnErrorMsg(String msg) {
        // Only remember first line of message
        int idx = msg.indexOf('\n');
        if (idx > 0) {
            ERRORS_AND_WARNINGS.add(msg.substring(0, idx));
        } else {
            ERRORS_AND_WARNINGS.add(msg);
        }
        // Only keep 5 lines to avoid memory leak and incomplete stacktraces in bug reports
        while (ERRORS_AND_WARNINGS.size() > 5) {
            ERRORS_AND_WARNINGS.remove(0);
        }
    }

    /**
     * Replies the first lines of last 10 error and warning messages, used for bug reports
     * @return the first lines of last 10 error and warning messages
     * @since 7420
     */
    public static final Collection<String> getLastErrorAndWarnings() {
        return Collections.unmodifiableList(ERRORS_AND_WARNINGS);
    }

    /**
     * Prints an error message if logging is on.
     * @param msg The message to print.
     * @since 6248
     */
    public static void error(String msg) {
        if (logLevel < 1)
            return;
        if (msg != null && !msg.isEmpty()) {
            System.err.println(tr("ERROR: {0}", msg));
            rememberWarnErrorMsg("E: "+msg);
        }
    }

    /**
     * Prints a warning message if logging is on.
     * @param msg The message to print.
     */
    public static void warn(String msg) {
        if (logLevel < 2)
            return;
        if (msg != null && !msg.isEmpty()) {
            System.err.println(tr("WARNING: {0}", msg));
            rememberWarnErrorMsg("W: "+msg);
        }
    }

    /**
     * Prints an informational message if logging is on.
     * @param msg The message to print.
     */
    public static void info(String msg) {
        if (logLevel < 3)
            return;
        if (msg != null && !msg.isEmpty()) {
            System.out.println(tr("INFO: {0}", msg));
        }
    }

    /**
     * Prints a debug message if logging is on.
     * @param msg The message to print.
     */
    public static void debug(String msg) {
        if (logLevel < 4)
            return;
        if (msg != null && !msg.isEmpty()) {
            System.out.println(tr("DEBUG: {0}", msg));
        }
    }

    /**
     * Prints a trace message if logging is on.
     * @param msg The message to print.
     */
    public static void trace(String msg) {
        if (logLevel < 5)
            return;
        if (msg != null && !msg.isEmpty()) {
            System.out.print("TRACE: ");
            System.out.println(msg);
        }
    }

    /**
     * Determines if debug log level is enabled.
     * Useful to avoid costly construction of debug messages when not enabled.
     * @return {@code true} if log level is at least debug, {@code false} otherwise
     * @since 6852
     */
    public static boolean isDebugEnabled() {
        return logLevel >= 4;
    }

    /**
     * Determines if trace log level is enabled.
     * Useful to avoid costly construction of trace messages when not enabled.
     * @return {@code true} if log level is at least trace, {@code false} otherwise
     * @since 6852
     */
    public static boolean isTraceEnabled() {
        return logLevel >= 5;
    }

    /**
     * Prints a formatted error message if logging is on. Calls {@link MessageFormat#format}
     * function to format text.
     * @param msg The formatted message to print.
     * @param objects The objects to insert into format string.
     * @since 6248
     */
    public static void error(String msg, Object... objects) {
        error(MessageFormat.format(msg, objects));
    }

    /**
     * Prints a formatted warning message if logging is on. Calls {@link MessageFormat#format}
     * function to format text.
     * @param msg The formatted message to print.
     * @param objects The objects to insert into format string.
     */
    public static void warn(String msg, Object... objects) {
        warn(MessageFormat.format(msg, objects));
    }

    /**
     * Prints a formatted informational message if logging is on. Calls {@link MessageFormat#format}
     * function to format text.
     * @param msg The formatted message to print.
     * @param objects The objects to insert into format string.
     */
    public static void info(String msg, Object... objects) {
        info(MessageFormat.format(msg, objects));
    }

    /**
     * Prints a formatted debug message if logging is on. Calls {@link MessageFormat#format}
     * function to format text.
     * @param msg The formatted message to print.
     * @param objects The objects to insert into format string.
     */
    public static void debug(String msg, Object... objects) {
        debug(MessageFormat.format(msg, objects));
    }

    /**
     * Prints a formatted trace message if logging is on. Calls {@link MessageFormat#format}
     * function to format text.
     * @param msg The formatted message to print.
     * @param objects The objects to insert into format string.
     */
    public static void trace(String msg, Object... objects) {
        trace(MessageFormat.format(msg, objects));
    }

    /**
     * Prints an error message for the given Throwable.
     * @param t The throwable object causing the error
     * @since 6248
     */
    public static void error(Throwable t) {
        error(t, true);
    }

    /**
     * Prints a warning message for the given Throwable.
     * @param t The throwable object causing the error
     * @since 6248
     */
    public static void warn(Throwable t) {
        warn(t, true);
    }

    /**
     * Prints an error message for the given Throwable.
     * @param t The throwable object causing the error
     * @param stackTrace {@code true}, if the stacktrace should be displayed
     * @since 6642
     */
    public static void error(Throwable t, boolean stackTrace) {
        error(getErrorMessage(t));
        if (stackTrace) {
            t.printStackTrace();
        }
    }

    /**
     * Prints a warning message for the given Throwable.
     * @param t The throwable object causing the error
     * @param stackTrace {@code true}, if the stacktrace should be displayed
     * @since 6642
     */
    public static void warn(Throwable t, boolean stackTrace) {
        warn(getErrorMessage(t));
        if (stackTrace) {
            t.printStackTrace();
        }
    }

    /**
     * Returns a human-readable message of error, also usable for developers.
     * @param t The error
     * @return The human-readable error message
     * @since 6642
     */
    public static String getErrorMessage(Throwable t) {
        if (t == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(t.getClass().getName());
        String msg = t.getMessage();
        if (msg != null) {
            sb.append(": ").append(msg.trim());
        }
        Throwable cause = t.getCause();
        if (cause != null && !cause.equals(t)) {
            sb.append(". ").append(tr("Cause: ")).append(getErrorMessage(cause));
        }
        return sb.toString();
    }

    /**
     * Platform specific code goes in here.
     * Plugins may replace it, however, some hooks will be called before any plugins have been loeaded.
     * So if you need to hook into those early ones, split your class and send the one with the early hooks
     * to the JOSM team for inclusion.
     */
    public static volatile PlatformHook platform;

    /**
     * Whether or not the java vm is openjdk
     * We use this to work around openjdk bugs
     */
    public static boolean isOpenjdk;

    /**
     * Initializes {@code Main.pref} in normal application context.
     * @since 6471
     */
    public static void initApplicationPreferences() {
        Main.pref = new Preferences();
    }

    /**
     * Set or clear (if passed <code>null</code>) the map.
     * @param map The map to set {@link Main#map} to. Can be null.
     */
    public final void setMapFrame(final MapFrame map) {
        MapFrame old = Main.map;
        panel.setVisible(false);
        panel.removeAll();
        if (map != null) {
            map.fillPanel(panel);
        } else {
            old.destroy();
            panel.add(gettingStarted, BorderLayout.CENTER);
        }
        panel.setVisible(true);
        redoUndoListener.commandChanged(0, 0);

        Main.map = map;

        for (MapFrameListener listener : mapFrameListeners) {
            listener.mapFrameInitialized(old, map);
        }
        if (map == null && currentProgressMonitor != null) {
            currentProgressMonitor.showForegroundDialog();
        }
    }

    /**
     * Remove the specified layer from the map. If it is the last layer,
     * remove the map as well.
     * @param layer The layer to remove
     */
    public final synchronized void removeLayer(final Layer layer) {
        if (map != null) {
            map.mapView.removeLayer(layer);
            if (isDisplayingMapView() && map.mapView.getAllLayers().isEmpty()) {
                setMapFrame(null);
            }
        }
    }

    private static volatile InitStatusListener initListener;

    public interface InitStatusListener {

        Object updateStatus(String event);

        void finish(Object status);
    }

    public static void setInitStatusListener(InitStatusListener listener) {
        CheckParameterUtil.ensureParameterNotNull(listener);
        initListener = listener;
    }

    /**
     * Constructs new {@code Main} object. A lot of global variables are initialized here.
     */
    public Main() {
        main = this;
        isOpenjdk = System.getProperty("java.vm.name").toUpperCase(Locale.ENGLISH).indexOf("OPENJDK") != -1;

        new InitializationTask(tr("Executing platform startup hook")) {
            @Override
            public void initialize() {
                platform.startupHook();
            }
        }.call();

        new InitializationTask(tr("Building main menu")) {

            @Override
            public void initialize() {
                contentPanePrivate.add(panel, BorderLayout.CENTER);
                panel.add(gettingStarted, BorderLayout.CENTER);
                menu = new MainMenu();
            }
        }.call();

        undoRedo.addCommandQueueListener(redoUndoListener);

        // creating toolbar
        contentPanePrivate.add(toolbar.control, BorderLayout.NORTH);

        registerActionShortcut(menu.help, Shortcut.registerShortcut("system:help", tr("Help"),
                KeyEvent.VK_F1, Shortcut.DIRECT));

        // contains several initialization tasks to be executed (in parallel) by a ExecutorService
        List<Callable<Void>> tasks = new ArrayList<>();

        tasks.add(new InitializationTask(tr("Initializing OSM API")) {

            @Override
            public void initialize() {
                // We try to establish an API connection early, so that any API
                // capabilities are already known to the editor instance. However
                // if it goes wrong that's not critical at this stage.
                try {
                    OsmApi.getOsmApi().initialize(null, true);
                } catch (Exception e) {
                    Main.warn(getErrorMessage(Utils.getRootCause(e)));
                }
            }
        });

        tasks.add(new InitializationTask(tr("Initializing validator")) {

            @Override
            public void initialize() {
                validator = new OsmValidator();
                MapView.addLayerChangeListener(validator);
            }
        });

        tasks.add(new InitializationTask(tr("Initializing presets")) {

            @Override
            public void initialize() {
                TaggingPresets.initialize();
            }
        });

        tasks.add(new InitializationTask(tr("Initializing map styles")) {

            @Override
            public void initialize() {
                MapPaintPreference.initialize();
            }
        });

        tasks.add(new InitializationTask(tr("Loading imagery preferences")) {

            @Override
            public void initialize() {
                ImageryPreference.initialize();
            }
        });

        try {
            final ExecutorService service = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(), Utils.newThreadFactory("main-init-%d", Thread.NORM_PRIORITY));
            for (Future<Void> i : service.invokeAll(tasks)) {
                i.get();
            }
            service.shutdown();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // hooks for the jmapviewer component
        FeatureAdapter.registerBrowserAdapter(new FeatureAdapter.BrowserAdapter() {
            @Override
            public void openLink(String url) {
                OpenBrowser.displayUrl(url);
            }
        });
        FeatureAdapter.registerTranslationAdapter(I18n.getTranslationAdapter());
        FeatureAdapter.registerLoggingAdapter(new FeatureAdapter.LoggingAdapter() {
            @Override
            public Logger getLogger(String name) {
                Logger logger = Logger.getAnonymousLogger();
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.ALL);
                if (logger.getHandlers().length == 0) {
                    logger.addHandler(new Handler() {
                        @Override
                        public void publish(LogRecord record) {
                            String msg = MessageFormat.format(record.getMessage(), record.getParameters());
                            if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                                Main.error(msg);
                            } else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                                Main.warn(msg);
                            } else if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                                Main.info(msg);
                            } else if (record.getLevel().intValue() >= Level.FINE.intValue()) {
                                Main.debug(msg);
                            } else {
                                Main.trace(msg);
                            }
                        }

                        @Override
                        public void flush() {
                        }

                        @Override
                        public void close() {
                        }
                    });
                }
                return logger;
            }
        });

        new InitializationTask(tr("Updating user interface")) {

            @Override
            public void initialize() {
                toolbar.refreshToolbarControl();
                toolbar.control.updateUI();
                contentPanePrivate.updateUI();
            }
        }.call();
    }

    private abstract class InitializationTask implements Callable<Void> {

        private final String name;

        protected InitializationTask(String name) {
            this.name = name;
        }

        public abstract void initialize();

        @Override
        public Void call() {
            Object status = null;
            if (initListener != null) {
                status = initListener.updateStatus(name);
            }
            initialize();
            if (initListener != null) {
                initListener.finish(status);
            }
            return null;
        }
    }

    /**
     * Add a new layer to the map.
     *
     * If no map exists, create one.
     *
     * @param layer the layer
     *
     * @see #addLayer(Layer, ProjectionBounds)
     * @see #addLayer(Layer, ViewportData)
     */
    public final void addLayer(final Layer layer) {
        BoundingXYVisitor v = new BoundingXYVisitor();
        layer.visitBoundingBox(v);
        addLayer(layer, v.getBounds());
    }

    /**
     * Add a new layer to the map.
     *
     * If no map exists, create one.
     *
     * @param layer the layer
     * @param bounds the bounds of the layer (target zoom area); can be null, then
     * the viewport isn't changed
     */
    public final synchronized void addLayer(final Layer layer, ProjectionBounds bounds) {
        addLayer(layer, bounds == null ? null : new ViewportData(bounds));
    }

    /**
     * Add a new layer to the map.
     *
     * If no map exists, create one.
     *
     * @param layer the layer
     * @param viewport the viewport to zoom to; can be null, then the viewport
     * isn't changed
     */
    public final synchronized void addLayer(final Layer layer, ViewportData viewport) {
        boolean noMap = map == null;
        if (noMap) {
            createMapFrame(layer, viewport);
        }
        layer.hookUpMapView();
        map.mapView.addLayer(layer);
        if (noMap) {
            Main.map.setVisible(true);
        } else if (viewport != null) {
            Main.map.mapView.zoomTo(viewport);
        }
    }

    public synchronized void createMapFrame(Layer firstLayer, ViewportData viewportData) {
        MapFrame mapFrame = new MapFrame(contentPanePrivate, viewportData);
        setMapFrame(mapFrame);
        if (firstLayer != null) {
            mapFrame.selectMapMode((MapMode) mapFrame.getDefaultButtonAction(), firstLayer);
        }
        mapFrame.initializeDialogsPane();
        // bootstrapping problem: make sure the layer list dialog is going to
        // listen to change events of the very first layer
        //
        if (firstLayer != null) {
            firstLayer.addPropertyChangeListener(LayerListDialog.getInstance().getModel());
        }
    }

    /**
     * Replies <code>true</code> if there is an edit layer
     *
     * @return <code>true</code> if there is an edit layer
     */
    public boolean hasEditLayer() {
        if (getEditLayer() == null) return false;
        return true;
    }

    /**
     * Replies the current edit layer
     *
     * @return the current edit layer. <code>null</code>, if no current edit layer exists
     */
    public OsmDataLayer getEditLayer() {
        if (!isDisplayingMapView()) return null;
        return map.mapView.getEditLayer();
    }

    /**
     * Replies the current data set.
     *
     * @return the current data set. <code>null</code>, if no current data set exists
     */
    public DataSet getCurrentDataSet() {
        if (!hasEditLayer()) return null;
        return getEditLayer().data;
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
    public Collection<OsmPrimitive> getInProgressSelection() {
        if (map != null && map.mapMode instanceof DrawAction) {
            return ((DrawAction) map.mapMode).getInProgressSelection();
        } else {
            DataSet ds = getCurrentDataSet();
            if (ds == null) return null;
            return ds.getSelected();
        }
    }

    /**
     * Returns the currently active  layer
     *
     * @return the currently active layer. <code>null</code>, if currently no active layer exists
     */
    public Layer getActiveLayer() {
        if (!isDisplayingMapView()) return null;
        return map.mapView.getActiveLayer();
    }

    protected static final JPanel contentPanePrivate = new JPanel(new BorderLayout());

    public static void redirectToMainContentPane(JComponent source) {
        RedirectInputMap.redirect(source, contentPanePrivate);
    }

    public static void registerActionShortcut(JosmAction action) {
        registerActionShortcut(action, action.getShortcut());
    }

    public static void registerActionShortcut(Action action, Shortcut shortcut) {
        KeyStroke keyStroke = shortcut.getKeyStroke();
        if (keyStroke == null)
            return;

        InputMap inputMap = contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        Object existing = inputMap.get(keyStroke);
        if (existing != null && !existing.equals(action)) {
            info(String.format("Keystroke %s is already assigned to %s, will be overridden by %s", keyStroke, existing, action));
        }
        inputMap.put(keyStroke, action);

        contentPanePrivate.getActionMap().put(action, action);
    }

    public static void unregisterShortcut(Shortcut shortcut) {
        contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortcut.getKeyStroke());
    }

    public static void unregisterActionShortcut(JosmAction action) {
        unregisterActionShortcut(action, action.getShortcut());
    }

    public static void unregisterActionShortcut(Action action, Shortcut shortcut) {
        unregisterShortcut(shortcut);
        contentPanePrivate.getActionMap().remove(action);
    }

    /**
     * Replies the registered action for the given shortcut
     * @param shortcut The shortcut to look for
     * @return the registered action for the given shortcut
     * @since 5696
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

    ///////////////////////////////////////////////////////////////////////////
    //  Implementation part
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Global panel.
     */
    public static final JPanel panel = new JPanel(new BorderLayout());

    protected static volatile WindowGeometry geometry;
    protected static int windowState = JFrame.NORMAL;

    private final CommandQueueListener redoUndoListener = new CommandQueueListener() {
        @Override
        public void commandChanged(final int queueSize, final int redoSize) {
            menu.undo.setEnabled(queueSize > 0);
            menu.redo.setEnabled(redoSize > 0);
        }
    };

    /**
     * Should be called before the main constructor to setup some parameter stuff
     * @param args The parsed argument list.
     */
    public static void preConstructorInit(Map<Option, Collection<String>> args) {
        ProjectionPreference.setProjection();

        try {
            String defaultlaf = platform.getDefaultStyle();
            String laf = Main.pref.get("laf", defaultlaf);
            try {
                UIManager.setLookAndFeel(laf);
            } catch (final NoClassDefFoundError | ClassNotFoundException e) {
                // Try to find look and feel in plugin classloaders
                Class<?> klass = null;
                for (ClassLoader cl : PluginHandler.getResourceClassLoaders()) {
                    try {
                        klass = cl.loadClass(laf);
                        break;
                    } catch (ClassNotFoundException ex) {
                        // Do nothing
                        if (Main.isTraceEnabled()) {
                            Main.trace(ex.getMessage());
                        }
                    }
                }
                if (klass != null && LookAndFeel.class.isAssignableFrom(klass)) {
                    try {
                        UIManager.setLookAndFeel((LookAndFeel) klass.newInstance());
                    } catch (Exception ex) {
                        warn("Cannot set Look and Feel: " + laf + ": "+ex.getMessage());
                    }
                } else {
                    info("Look and Feel not found: " + laf);
                    Main.pref.put("laf", defaultlaf);
                }
            } catch (final UnsupportedLookAndFeelException e) {
                info("Look and Feel not supported: " + laf);
                Main.pref.put("laf", defaultlaf);
            }
            toolbar = new ToolbarPreferences();
            contentPanePrivate.updateUI();
            panel.updateUI();
        } catch (final Exception e) {
            error(e);
        }
        UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
        UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
        UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
        UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));

        I18n.translateJavaInternalMessages();

        // init default coordinate format
        //
        try {
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.valueOf(Main.pref.get("coordinates")));
        } catch (IllegalArgumentException iae) {
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.DECIMAL_DEGREES);
        }

        geometry = WindowGeometry.mainWindow("gui.geometry",
            args.containsKey(Option.GEOMETRY) ? args.get(Option.GEOMETRY).iterator().next() : null,
            !args.containsKey(Option.NO_MAXIMIZE) && Main.pref.getBoolean("gui.maximized", false));
    }

    protected static void postConstructorProcessCmdLine(Map<Option, Collection<String>> args) {
        if (args.containsKey(Option.DOWNLOAD)) {
            List<File> fileList = new ArrayList<>();
            for (String s : args.get(Option.DOWNLOAD)) {
                DownloadParamType.paramType(s).download(s, fileList);
            }
            if (!fileList.isEmpty()) {
                OpenFileAction.openFiles(fileList, true);
            }
        }
        if (args.containsKey(Option.DOWNLOADGPS)) {
            for (String s : args.get(Option.DOWNLOADGPS)) {
                DownloadParamType.paramType(s).downloadGps(s);
            }
        }
        if (args.containsKey(Option.SELECTION)) {
            for (String s : args.get(Option.SELECTION)) {
                SearchAction.search(s, SearchAction.SearchMode.add);
            }
        }
    }

    /**
     * Asks user to perform "save layer" operations (save on disk and/or upload data to server) for all
     * {@link AbstractModifiableLayer} before JOSM exits.
     * @return {@code true} if there was nothing to save, or if the user wants to proceed to save operations.
     *         {@code false} if the user cancels.
     * @since 2025
     */
    public static boolean saveUnsavedModifications() {
        if (!isDisplayingMapView()) return true;
        return saveUnsavedModifications(map.mapView.getLayersOfType(AbstractModifiableLayer.class), true);
    }

    /**
     * Asks user to perform "save layer" operations (save on disk and/or upload data to server) before data layers deletion.
     *
     * @param selectedLayers The layers to check. Only instances of {@link AbstractModifiableLayer} are considered.
     * @param exit {@code true} if JOSM is exiting, {@code false} otherwise.
     * @return {@code true} if there was nothing to save, or if the user wants to proceed to save operations.
     *         {@code false} if the user cancels.
     * @since 5519
     */
    public static boolean saveUnsavedModifications(Iterable<? extends Layer> selectedLayers, boolean exit) {
        SaveLayersDialog dialog = new SaveLayersDialog(parent);
        List<AbstractModifiableLayer> layersWithUnmodifiedChanges = new ArrayList<>();
        for (Layer l: selectedLayers) {
            if (!(l instanceof AbstractModifiableLayer)) {
                continue;
            }
            AbstractModifiableLayer odl = (AbstractModifiableLayer) l;
            if ((odl.requiresSaveToFile() || (odl.requiresUploadToServer() && !odl.isUploadDiscouraged())) && odl.isModified()) {
                layersWithUnmodifiedChanges.add(odl);
            }
        }
        if (exit) {
            dialog.prepareForSavingAndUpdatingLayersBeforeExit();
        } else {
            dialog.prepareForSavingAndUpdatingLayersBeforeDelete();
        }
        if (!layersWithUnmodifiedChanges.isEmpty()) {
            dialog.getModel().populate(layersWithUnmodifiedChanges);
            dialog.setVisible(true);
            switch(dialog.getUserAction()) {
            case PROCEED: return true;
            case CANCEL:
            default: return false;
            }
        }

        return true;
    }

    /**
     * Closes JOSM and optionally terminates the Java Virtual Machine (JVM).
     * If there are some unsaved data layers, asks first for user confirmation.
     * @param exit If {@code true}, the JVM is terminated by running {@link System#exit} with a given return code.
     * @param exitCode The return code
     * @return {@code true} if JOSM has been closed, {@code false} if the user has cancelled the operation.
     * @since 3378
     */
    public static boolean exitJosm(boolean exit, int exitCode) {
        if (Main.saveUnsavedModifications()) {
            worker.shutdown();
            ImageProvider.shutdown(false);
            JCSCacheManager.shutdown();
            geometry.remember("gui.geometry");
            if (map != null) {
                map.rememberToggleDialogWidth();
            }
            pref.put("gui.maximized", (windowState & JFrame.MAXIMIZED_BOTH) != 0);
            // Remove all layers because somebody may rely on layerRemoved events (like AutosaveTask)
            if (Main.isDisplayingMapView()) {
                Collection<Layer> layers = new ArrayList<>(Main.map.mapView.getAllLayers());
                for (Layer l: layers) {
                    Main.main.removeLayer(l);
                }
            }
            worker.shutdownNow();
            ImageProvider.shutdown(true);

            if (exit) {
                System.exit(exitCode);
            }
            return true;
        }
        return false;
    }

    /**
     * The type of a command line parameter, to be used in switch statements.
     * @see #paramType
     */
    enum DownloadParamType {
        httpUrl {
            @Override
            void download(String s, Collection<File> fileList) {
                new OpenLocationAction().openUrl(false, s);
            }

            @Override
            void downloadGps(String s) {
                final Bounds b = OsmUrlToBounds.parse(s);
                if (b == null) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Ignoring malformed URL: \"{0}\"", s),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                downloadFromParamBounds(true, b);
            }
        }, fileUrl {
            @Override
            void download(String s, Collection<File> fileList) {
                File f = null;
                try {
                    f = new File(new URI(s));
                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Ignoring malformed file URL: \"{0}\"", s),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                }
                if (f != null) {
                    fileList.add(f);
                }
            }
        }, bounds {

            /**
             * Download area specified on the command line as bounds string.
             * @param rawGps Flag to download raw GPS tracks
             * @param s The bounds parameter
             */
            private void downloadFromParamBounds(final boolean rawGps, String s) {
                final StringTokenizer st = new StringTokenizer(s, ",");
                if (st.countTokens() == 4) {
                    Bounds b = new Bounds(
                            new LatLon(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken())),
                            new LatLon(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()))
                    );
                    Main.downloadFromParamBounds(rawGps, b);
                }
            }

            @Override
            void download(String param, Collection<File> fileList) {
                downloadFromParamBounds(false, param);
            }

            @Override
            void downloadGps(String param) {
                downloadFromParamBounds(true, param);
            }
        }, fileName {
            @Override
            void download(String s, Collection<File> fileList) {
                fileList.add(new File(s));
            }
        };

        /**
         * Performs the download
         * @param param represents the object to be downloaded
         * @param fileList files which shall be opened, should be added to this collection
         */
        abstract void download(String param, Collection<File> fileList);

        /**
         * Performs the GPS download
         * @param param represents the object to be downloaded
         */
        void downloadGps(String param) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Parameter \"downloadgps\" does not accept file names or file URLs"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        }

        /**
         * Guess the type of a parameter string specified on the command line with --download= or --downloadgps.
         *
         * @param s A parameter string
         * @return The guessed parameter type
         */
        static DownloadParamType paramType(String s) {
            if (s.startsWith("http:") || s.startsWith("https:")) return DownloadParamType.httpUrl;
            if (s.startsWith("file:")) return DownloadParamType.fileUrl;
            String coorPattern = "\\s*[+-]?[0-9]+(\\.[0-9]+)?\\s*";
            if (s.matches(coorPattern + "(," + coorPattern + "){3}")) return DownloadParamType.bounds;
            // everything else must be a file name
            return DownloadParamType.fileName;
        }
    }

    /**
     * Download area specified as Bounds value.
     * @param rawGps Flag to download raw GPS tracks
     * @param b The bounds value
     */
    private static void downloadFromParamBounds(final boolean rawGps, Bounds b) {
        DownloadTask task = rawGps ? new DownloadGpsTask() : new DownloadOsmTask();
        // asynchronously launch the download task ...
        Future<?> future = task.download(true, b, null);
        // ... and the continuation when the download is finished (this will wait for the download to finish)
        Main.worker.execute(new PostDownloadHandler(task, future));
    }

    /**
     * Identifies the current operating system family and initializes the platform hook accordingly.
     * @since 1849
     */
    public static void determinePlatformHook() {
        String os = System.getProperty("os.name");
        if (os == null) {
            warn("Your operating system has no name, so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase(Locale.ENGLISH).startsWith("windows")) {
            platform = new PlatformHookWindows();
        } else if ("Linux".equals(os) || "Solaris".equals(os) ||
                "SunOS".equals(os) || "AIX".equals(os) ||
                "FreeBSD".equals(os) || "NetBSD".equals(os) || "OpenBSD".equals(os)) {
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase(Locale.ENGLISH).startsWith("mac os x")) {
            platform = new PlatformHookOsx();
        } else {
            warn("I don't know your operating system '"+os+"', so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        }
    }

    private static class WindowPositionSizeListener extends WindowAdapter implements
    ComponentListener {
        @Override
        public void windowStateChanged(WindowEvent e) {
            Main.windowState = e.getNewState();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            handleComponentEvent(e);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            handleComponentEvent(e);
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        private void handleComponentEvent(ComponentEvent e) {
            Component c = e.getComponent();
            if (c instanceof JFrame && c.isVisible()) {
                if (Main.windowState == JFrame.NORMAL) {
                    Main.geometry = new WindowGeometry((JFrame) c);
                } else {
                    Main.geometry.fixScreen((JFrame) c);
                }
            }
        }
    }

    protected static void addListener() {
        parent.addComponentListener(new WindowPositionSizeListener());
        ((JFrame) parent).addWindowStateListener(new WindowPositionSizeListener());
    }

    /**
     * Determines if JOSM currently runs with Java 8 or later.
     * @return {@code true} if the current JVM is at least Java 8, {@code false} otherwise
     * @since 7894
     */
    public static boolean isJava8orLater() {
        String version = System.getProperty("java.version");
        return version != null && !version.matches("^(1\\.)?[7].*");
    }

    /**
     * Checks that JOSM is at least running with Java 7.
     * @since 7001
     */
    public static void checkJavaVersion() {
        String version = System.getProperty("java.version");
        if (version != null) {
            if (version.matches("^(1\\.)?[789].*"))
                return;
            if (version.matches("^(1\\.)?[56].*")) {
                JMultilineLabel ho = new JMultilineLabel("<html>"+
                        tr("<h2>JOSM requires Java version {0}.</h2>"+
                                "Detected Java version: {1}.<br>"+
                                "You can <ul><li>update your Java (JRE) or</li>"+
                                "<li>use an earlier (Java {2} compatible) version of JOSM.</li></ul>"+
                                "More Info:", "7", version, "6")+"</html>");
                JTextArea link = new JTextArea(HelpUtil.getWikiBaseHelpUrl()+"/Help/SystemRequirements");
                link.setEditable(false);
                link.setBackground(panel.getBackground());
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                panel.add(ho, gbc);
                panel.add(link, gbc);
                final String EXIT = tr("Exit JOSM");
                final String CONTINUE = tr("Continue, try anyway");
                int ret = JOptionPane.showOptionDialog(null, panel, tr("Error"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE, null, new String[] {EXIT, CONTINUE}, EXIT);
                if (ret == 0) {
                    System.exit(0);
                }
                return;
            }
        }
        error("Could not recognize Java Version: "+version);
    }

    /* ----------------------------------------------------------------------------------------- */
    /* projection handling  - Main is a registry for a single, global projection instance        */
    /*                                                                                           */
    /* TODO: For historical reasons the registry is implemented by Main. An alternative approach */
    /* would be a singleton org.openstreetmap.josm.data.projection.ProjectionRegistry class.     */
    /* ----------------------------------------------------------------------------------------- */
    /**
     * The projection method used.
     * use {@link #getProjection()} and {@link #setProjection(Projection)} for access.
     * Use {@link #setProjection(Projection)} in order to trigger a projection change event.
     */
    private static volatile Projection proj;

    /**
     * Replies the current projection.
     *
     * @return the currently active projection
     */
    public static Projection getProjection() {
        return proj;
    }

    /**
     * Sets the current projection
     *
     * @param p the projection
     */
    public static void setProjection(Projection p) {
        CheckParameterUtil.ensureParameterNotNull(p);
        Projection oldValue = proj;
        Bounds b = isDisplayingMapView() ? map.mapView.getRealBounds() : null;
        proj = p;
        fireProjectionChanged(oldValue, proj, b);
    }

    /*
     * Keep WeakReferences to the listeners. This relieves clients from the burden of
     * explicitly removing the listeners and allows us to transparently register every
     * created dataset as projection change listener.
     */
    private static final List<WeakReference<ProjectionChangeListener>> listeners = new ArrayList<>();

    private static void fireProjectionChanged(Projection oldValue, Projection newValue, Bounds oldBounds) {
        if (newValue == null ^ oldValue == null
                || (newValue != null && oldValue != null && !Objects.equals(newValue.toCode(), oldValue.toCode()))) {

            synchronized (Main.class) {
                Iterator<WeakReference<ProjectionChangeListener>> it = listeners.iterator();
                while (it.hasNext()) {
                    WeakReference<ProjectionChangeListener> wr = it.next();
                    ProjectionChangeListener listener = wr.get();
                    if (listener == null) {
                        it.remove();
                        continue;
                    }
                    listener.projectionChanged(oldValue, newValue);
                }
            }
            if (newValue != null && oldBounds != null) {
                Main.map.mapView.zoomTo(oldBounds);
            }
            /* TODO - remove layers with fixed projection */
        }
    }

    /**
     * Register a projection change listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void addProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        synchronized (Main.class) {
            for (WeakReference<ProjectionChangeListener> wr : listeners) {
                // already registered ? => abort
                if (wr.get() == listener) return;
            }
            listeners.add(new WeakReference<>(listener));
        }
    }

    /**
     * Removes a projection change listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void removeProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        synchronized (Main.class) {
            Iterator<WeakReference<ProjectionChangeListener>> it = listeners.iterator();
            while (it.hasNext()) {
                WeakReference<ProjectionChangeListener> wr = it.next();
                // remove the listener - and any other listener which got garbage
                // collected in the meantime
                if (wr.get() == null || wr.get() == listener) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Listener for window switch events.
     *
     * These are events, when the user activates a window of another application
     * or comes back to JOSM. Window switches from one JOSM window to another
     * are not reported.
     */
    public interface WindowSwitchListener {
        /**
         * Called when the user activates a window of another application.
         */
        void toOtherApplication();
        /**
         * Called when the user comes from a window of another application
         * back to JOSM.
         */
        void fromOtherApplication();
    }

    private static final List<WeakReference<WindowSwitchListener>> windowSwitchListeners = new ArrayList<>();

    /**
     * Register a window switch listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void addWindowSwitchListener(WindowSwitchListener listener) {
        if (listener == null) return;
        synchronized (Main.class) {
            for (WeakReference<WindowSwitchListener> wr : windowSwitchListeners) {
                // already registered ? => abort
                if (wr.get() == listener) return;
            }
            boolean wasEmpty = windowSwitchListeners.isEmpty();
            windowSwitchListeners.add(new WeakReference<>(listener));
            if (wasEmpty) {
                // The following call will have no effect, when there is no window
                // at the time. Therefore, MasterWindowListener.setup() will also be
                // called, as soon as the main window is shown.
                MasterWindowListener.setup();
            }
        }
    }

    /**
     * Removes a window switch listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void removeWindowSwitchListener(WindowSwitchListener listener) {
        if (listener == null) return;
        synchronized (Main.class) {
            Iterator<WeakReference<WindowSwitchListener>> it = windowSwitchListeners.iterator();
            while (it.hasNext()) {
                WeakReference<WindowSwitchListener> wr = it.next();
                // remove the listener - and any other listener which got garbage
                // collected in the meantime
                if (wr.get() == null || wr.get() == listener) {
                    it.remove();
                }
            }
            if (windowSwitchListeners.isEmpty()) {
                MasterWindowListener.teardown();
            }
        }
    }

    /**
     * WindowListener, that is registered on all Windows of the application.
     *
     * Its purpose is to notify WindowSwitchListeners, that the user switches to
     * another application, e.g. a browser, or back to JOSM.
     *
     * When changing from JOSM to another application and back (e.g. two times
     * alt+tab), the active Window within JOSM may be different.
     * Therefore, we need to register listeners to <strong>all</strong> (visible)
     * Windows in JOSM, and it does not suffice to monitor the one that was
     * deactivated last.
     *
     * This class is only "active" on demand, i.e. when there is at least one
     * WindowSwitchListener registered.
     */
    protected static class MasterWindowListener extends WindowAdapter {

        private static MasterWindowListener INSTANCE;

        public static synchronized MasterWindowListener getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new MasterWindowListener();
            }
            return INSTANCE;
        }

        /**
         * Register listeners to all non-hidden windows.
         *
         * Windows that are created later, will be cared for in {@link #windowDeactivated(WindowEvent)}.
         */
        public static void setup() {
            if (!windowSwitchListeners.isEmpty()) {
                for (Window w : Window.getWindows()) {
                    if (w.isShowing()) {
                        if (!Arrays.asList(w.getWindowListeners()).contains(getInstance())) {
                            w.addWindowListener(getInstance());
                        }
                    }
                }
            }
        }

        /**
         * Unregister all listeners.
         */
        public static void teardown() {
            for (Window w : Window.getWindows()) {
                w.removeWindowListener(getInstance());
            }
        }

        @Override
        public void windowActivated(WindowEvent e) {
            if (e.getOppositeWindow() == null) { // we come from a window of a different application
                // fire WindowSwitchListeners
                synchronized (Main.class) {
                    Iterator<WeakReference<WindowSwitchListener>> it = windowSwitchListeners.iterator();
                    while (it.hasNext()) {
                        WeakReference<WindowSwitchListener> wr = it.next();
                        WindowSwitchListener listener = wr.get();
                        if (listener == null) {
                            it.remove();
                            continue;
                        }
                        listener.fromOtherApplication();
                    }
                }
            }
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            // set up windows that have been created in the meantime
            for (Window w : Window.getWindows()) {
                if (!w.isShowing()) {
                    w.removeWindowListener(getInstance());
                } else {
                    if (!Arrays.asList(w.getWindowListeners()).contains(getInstance())) {
                        w.addWindowListener(getInstance());
                    }
                }
            }
            if (e.getOppositeWindow() == null) { // we go to a window of a different application
                // fire WindowSwitchListeners
                synchronized (Main.class) {
                    Iterator<WeakReference<WindowSwitchListener>> it = windowSwitchListeners.iterator();
                    while (it.hasNext()) {
                        WeakReference<WindowSwitchListener> wr = it.next();
                        WindowSwitchListener listener = wr.get();
                        if (listener == null) {
                            it.remove();
                            continue;
                        }
                        listener.toOtherApplication();
                    }
                }
            }
        }
    }

    /**
     * Registers a new {@code MapFrameListener} that will be notified of MapFrame changes
     * @param listener The MapFrameListener
     * @param fireWhenMapViewPresent If true, will fire an initial mapFrameInitialized event
     * when the MapFrame is present. Otherwise will only fire when the MapFrame is created
     * or destroyed.
     * @return {@code true} if the listeners collection changed as a result of the call
     */
    public static boolean addMapFrameListener(MapFrameListener listener, boolean fireWhenMapViewPresent) {
        boolean changed = listener != null ? mapFrameListeners.add(listener) : false;
        if (fireWhenMapViewPresent && changed && map != null) {
            listener.mapFrameInitialized(null, map);
        }
        return changed;
    }

    /**
     * Registers a new {@code MapFrameListener} that will be notified of MapFrame changes
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @since 5957
     */
    public static boolean addMapFrameListener(MapFrameListener listener) {
        return addMapFrameListener(listener, false);
    }

    /**
     * Unregisters the given {@code MapFrameListener} from MapFrame changes
     * @param listener The MapFrameListener
     * @return {@code true} if the listeners collection changed as a result of the call
     * @since 5957
     */
    public static boolean removeMapFrameListener(MapFrameListener listener) {
        return listener != null ? mapFrameListeners.remove(listener) : false;
    }

    /**
     * Adds a new network error that occur to give a hint about broken Internet connection.
     * Do not use this method for errors known for sure thrown because of a bad proxy configuration.
     *
     * @param url The accessed URL that caused the error
     * @param t The network error
     * @return The previous error associated to the given resource, if any. Can be {@code null}
     * @since 6642
     */
    public static Throwable addNetworkError(URL url, Throwable t) {
        if (url != null && t != null) {
            Throwable old = addNetworkError(url.toExternalForm(), t);
            if (old != null) {
                Main.warn("Already here "+old);
            }
            return old;
        }
        return null;
    }

    /**
     * Adds a new network error that occur to give a hint about broken Internet connection.
     * Do not use this method for errors known for sure thrown because of a bad proxy configuration.
     *
     * @param url The accessed URL that caused the error
     * @param t The network error
     * @return The previous error associated to the given resource, if any. Can be {@code null}
     * @since 6642
     */
    public static Throwable addNetworkError(String url, Throwable t) {
        if (url != null && t != null) {
            return NETWORK_ERRORS.put(url, t);
        }
        return null;
    }

    /**
     * Returns the network errors that occured until now.
     * @return the network errors that occured until now, indexed by URL
     * @since 6639
     */
    public static Map<String, Throwable> getNetworkErrors() {
        return new HashMap<>(NETWORK_ERRORS);
    }

    /**
     * Returns the command-line arguments used to run the application.
     * @return the command-line arguments used to run the application
     * @since 8356
     */
    public static List<String> getCommandLineArgs() {
        return Collections.unmodifiableList(COMMAND_LINE_ARGS);
    }

    /**
     * Returns the JOSM website URL.
     * @return the josm website URL
     * @since 6897
     */
    public static String getJOSMWebsite() {
        if (Main.pref != null)
            return Main.pref.get("josm.url", JOSM_WEBSITE);
        return JOSM_WEBSITE;
    }

    /**
     * Returns the JOSM XML URL.
     * @return the josm XML URL
     * @since 6897
     */
    public static String getXMLBase() {
        // Always return HTTP (issues reported with HTTPS)
        return "http://josm.openstreetmap.de";
    }

    /**
     * Returns the OSM website URL.
     * @return the OSM website URL
     * @since 6897
     */
    public static String getOSMWebsite() {
        if (Main.pref != null)
            return Main.pref.get("osm.url", OSM_WEBSITE);
        return OSM_WEBSITE;
    }

    /**
     * Replies the base URL for browsing information about a primitive.
     * @return the base URL, i.e. https://www.openstreetmap.org
     * @since 7678
     */
    public static String getBaseBrowseUrl() {
        if (Main.pref != null)
            return Main.pref.get("osm-browse.url", getOSMWebsite());
        return getOSMWebsite();
    }

    /**
     * Replies the base URL for browsing information about a user.
     * @return the base URL, i.e. https://www.openstreetmap.org/user
     * @since 7678
     */
    public static String getBaseUserUrl() {
        if (Main.pref != null)
            return Main.pref.get("osm-user.url", getOSMWebsite() + "/user");
        return getOSMWebsite() + "/user";
    }

    /**
     * Determines if we are currently running on OSX.
     * @return {@code true} if we are currently running on OSX
     * @since 6957
     */
    public static boolean isPlatformOsx() {
        return Main.platform instanceof PlatformHookOsx;
    }

    /**
     * Determines if we are currently running on Windows.
     * @return {@code true} if we are currently running on Windows
     * @since 7335
     */
    public static boolean isPlatformWindows() {
        return Main.platform instanceof PlatformHookWindows;
    }

    /**
     * Determines if the given online resource is currently offline.
     * @param r the online resource
     * @return {@code true} if {@code r} is offline and should not be accessed
     * @since 7434
     */
    public static boolean isOffline(OnlineResource r) {
        return OFFLINE_RESOURCES.contains(r) || OFFLINE_RESOURCES.contains(OnlineResource.ALL);
    }

    /**
     * Sets the given online resource to offline state.
     * @param r the online resource
     * @return {@code true} if {@code r} was not already offline
     * @since 7434
     */
    public static boolean setOffline(OnlineResource r) {
        return OFFLINE_RESOURCES.add(r);
    }

    /**
     * Sets the given online resource to online state.
     * @param r the online resource
     * @return {@code true} if {@code r} was offline
     * @since 8506
     */
    public static boolean setOnline(OnlineResource r) {
        return OFFLINE_RESOURCES.remove(r);
    }

    /**
     * Replies the set of online resources currently offline.
     * @return the set of online resources currently offline
     * @since 7434
     */
    public static Set<OnlineResource> getOfflineResources() {
        return new HashSet<>(OFFLINE_RESOURCES);
    }
}
