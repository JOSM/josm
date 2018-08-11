// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.lifecycle.InitializationTask;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Platform;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformHookOsx;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * Abstract class holding various static global variables and methods used in large parts of JOSM application.
 * @since 98
 */
public abstract class Main {

    /**
     * Global parent component for all dialogs and message boxes
     */
    public static Component parent;

    /**
     * Global application.
     */
    public static volatile Main main;

    /**
     * Global application preferences
     */
    public static final Preferences pref = new Preferences(JosmBaseDirectories.getInstance());

    /**
     * The commands undo/redo handler.
     */
    public final UndoRedoHandler undoRedo = new UndoRedoHandler();

    /**
     * The file watcher service.
     * @deprecated Use {@link FileWatcher#getDefaultInstance}
     */
    @Deprecated
    public static final FileWatcher fileWatcher = FileWatcher.getDefaultInstance();

    /**
     * Platform specific code goes in here.
     * Plugins may replace it, however, some hooks will be called before any plugins have been loaded.
     * So if you need to hook into those early ones, split your class and send the one with the early hooks
     * to the JOSM team for inclusion.
     */
    public static volatile PlatformHook platform;

    /**
     * Constructs new {@code Main} object.
     * @see #initialize()
     */
    protected Main() {
        setInstance(this);
    }

    private static void setInstance(Main instance) {
        main = instance;
    }

    /**
     * Initializes the main object. A lot of global variables are initialized here.
     * @since 10340
     */
    public void initialize() {
        // Initializes tasks that must be run before parallel tasks
        runInitializationTasks(beforeInitializationTasks());

        // Initializes tasks to be executed (in parallel) by a ExecutorService
        try {
            ExecutorService service = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(), Utils.newThreadFactory("main-init-%d", Thread.NORM_PRIORITY));
            for (Future<Void> i : service.invokeAll(parallelInitializationTasks())) {
                i.get();
            }
            // asynchronous initializations to be completed eventually
            asynchronousRunnableTasks().forEach(service::submit);
            asynchronousCallableTasks().forEach(service::submit);
            try {
                service.shutdown();
            } catch (SecurityException e) {
                Logging.log(Logging.LEVEL_ERROR, "Unable to shutdown executor service", e);
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new JosmRuntimeException(ex);
        }

        // Initializes tasks that must be run after parallel tasks
        runInitializationTasks(afterInitializationTasks());
    }

    private static void runInitializationTasks(List<InitializationTask> tasks) {
        for (InitializationTask task : tasks) {
            try {
                task.call();
            } catch (JosmRuntimeException e) {
                // Can happen if the current projection needs NTV2 grid which is not available
                // In this case we want the user be able to change his projection
                BugReport.intercept(e).warn();
            }
        }
    }

    /**
     * Returns tasks that must be run before parallel tasks.
     * @return tasks that must be run before parallel tasks
     * @see #afterInitializationTasks
     * @see #parallelInitializationTasks
     */
    protected List<InitializationTask> beforeInitializationTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns tasks to be executed (in parallel) by a ExecutorService.
     * @return tasks to be executed (in parallel) by a ExecutorService
     */
    protected Collection<InitializationTask> parallelInitializationTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns asynchronous callable initializations to be completed eventually
     * @return asynchronous callable initializations to be completed eventually
     */
    protected List<Callable<?>> asynchronousCallableTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns asynchronous runnable initializations to be completed eventually
     * @return asynchronous runnable initializations to be completed eventually
     */
    protected List<Runnable> asynchronousRunnableTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns tasks that must be run after parallel tasks.
     * @return tasks that must be run after parallel tasks
     * @see #beforeInitializationTasks
     * @see #parallelInitializationTasks
     */
    protected List<InitializationTask> afterInitializationTasks() {
        return Collections.emptyList();
    }

    /**
     * Replies the current selected OSM primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link DataSet#getSelected()}.
     * @return The current selected OSM primitives, from a end-user point of view. Can be {@code null}.
     * @since 6546
     */
    public Collection<OsmPrimitive> getInProgressSelection() {
        return Collections.emptyList();
    }

    /**
     * Replies the current selected primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link OsmData#getSelected()}.
     * @return The current selected primitives, from a end-user point of view. Can be {@code null}.
     * @since 13926
     */
    public Collection<? extends IPrimitive> getInProgressISelection() {
        return Collections.emptyList();
    }

    /**
     * Gets the active edit data set (not read-only).
     * @return That data set, <code>null</code>.
     * @see #getActiveDataSet
     * @since 12691
     */
    public abstract DataSet getEditDataSet();

    /**
     * Gets the active data set (can be read-only).
     * @return That data set, <code>null</code>.
     * @see #getEditDataSet
     * @since 13434
     */
    public abstract DataSet getActiveDataSet();

    /**
     * Sets the active data set (and also edit data set if not read-only).
     * @param ds New data set, or <code>null</code>
     * @since 13434
     */
    public abstract void setActiveDataSet(DataSet ds);

    /**
     * Determines if the list of data sets managed by JOSM contains {@code ds}.
     * @param ds the data set to look for
     * @return {@code true} if the list of data sets managed by JOSM contains {@code ds}
     * @since 12718
     */
    public abstract boolean containsDataSet(DataSet ds);

    ///////////////////////////////////////////////////////////////////////////
    //  Implementation part
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Should be called before the main constructor to setup some parameter stuff
     */
    public static void preConstructorInit() {
        // init default coordinate format
        ICoordinateFormat fmt = CoordinateFormatManager.getCoordinateFormat(Config.getPref().get("coordinates"));
        if (fmt == null) {
            fmt = DecimalDegreesCoordinateFormat.INSTANCE;
        }
        CoordinateFormatManager.setCoordinateFormat(fmt);
    }

    /**
     * Closes JOSM and optionally terminates the Java Virtual Machine (JVM).
     * @param exit If {@code true}, the JVM is terminated by running {@link System#exit} with a given return code.
     * @param exitCode The return code
     * @return {@code true}
     * @since 12636
     */
    public static boolean exitJosm(boolean exit, int exitCode) {
        if (Main.main != null) {
            Main.main.shutdown();
        }

        if (exit) {
            System.exit(exitCode);
        }
        return true;
    }

    /**
     * Shutdown JOSM.
     */
    protected void shutdown() {
        if (!GraphicsEnvironment.isHeadless()) {
            ImageProvider.shutdown(false);
        }
        try {
            pref.saveDefaults();
        } catch (IOException | InvalidPathException ex) {
            Logging.log(Logging.LEVEL_WARN, tr("Failed to save default preferences."), ex);
        }
        if (!GraphicsEnvironment.isHeadless()) {
            ImageProvider.shutdown(true);
        }
    }

    /**
     * Identifies the current operating system family and initializes the platform hook accordingly.
     * @since 1849
     */
    public static void determinePlatformHook() {
        platform = Platform.determinePlatform().accept(PlatformHook.CONSTRUCT_FROM_PLATFORM);
    }

    /**
     * Replies the current projection.
     *
     * @return the currently active projection
     * @deprecated Use {@link ProjectionRegistry#getProjection}
     */
    @Deprecated
    public static Projection getProjection() {
        return ProjectionRegistry.getProjection();
    }

    /**
     * Sets the current projection
     *
     * @param p the projection
     * @deprecated Use {@link ProjectionRegistry#setProjection}
     */
    @Deprecated
    public static void setProjection(Projection p) {
        ProjectionRegistry.setProjection(p);
    }

    /**
     * Register a projection change listener.
     * The listener is registered to be weak, so keep a reference of it if you want it to be preserved.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     * @deprecated Use {@link ProjectionRegistry#addProjectionChangeListener}
     */
    @Deprecated
    public static void addProjectionChangeListener(ProjectionChangeListener listener) {
        ProjectionRegistry.addProjectionChangeListener(listener);
    }

    /**
     * Removes a projection change listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     * @deprecated Use {@link ProjectionRegistry#removeProjectionChangeListener}
     */
    @Deprecated
    public static void removeProjectionChangeListener(ProjectionChangeListener listener) {
        ProjectionRegistry.removeProjectionChangeListener(listener);
    }

    /**
     * Remove all projection change listeners. For testing purposes only.
     * @since 13322
     * @deprecated Use {@link ProjectionRegistry#clearProjectionChangeListeners}
     */
    @Deprecated
    public static void clearProjectionChangeListeners() {
        ProjectionRegistry.clearProjectionChangeListeners();
    }

    /**
     * Adds a new network error that occur to give a hint about broken Internet connection.
     * Do not use this method for errors known for sure thrown because of a bad proxy configuration.
     *
     * @param url The accessed URL that caused the error
     * @param t The network error
     * @return The previous error associated to the given resource, if any. Can be {@code null}
     * @deprecated Use {@link NetworkManager#addNetworkError(URL, Throwable)}
     * @since 6642
     */
    @Deprecated
    public static Throwable addNetworkError(URL url, Throwable t) {
        return NetworkManager.addNetworkError(url, t);
    }

    /**
     * Adds a new network error that occur to give a hint about broken Internet connection.
     * Do not use this method for errors known for sure thrown because of a bad proxy configuration.
     *
     * @param url The accessed URL that caused the error
     * @param t The network error
     * @return The previous error associated to the given resource, if any. Can be {@code null}
     * @deprecated Use {@link NetworkManager#addNetworkError(String, Throwable)}
     * @since 6642
     */
    @Deprecated
    public static Throwable addNetworkError(String url, Throwable t) {
        return NetworkManager.addNetworkError(url, t);
    }

    /**
     * Returns the network errors that occured until now.
     * @return the network errors that occured until now, indexed by URL
     * @deprecated Use {@link NetworkManager#getNetworkErrors}
     * @since 6639
     */
    @Deprecated
    public static Map<String, Throwable> getNetworkErrors() {
        return NetworkManager.getNetworkErrors();
    }

    /**
     * Clears the network errors cache.
     * @deprecated Use {@link NetworkManager#clearNetworkErrors}
     * @since 12011
     */
    @Deprecated
    public static void clearNetworkErrors() {
        NetworkManager.clearNetworkErrors();
    }

    /**
     * Returns the JOSM website URL.
     * @return the josm website URL
     * @deprecated Use {@link IUrls#getJOSMWebsite}
     * @since 6897
     */
    @Deprecated
    public static String getJOSMWebsite() {
        return Config.getUrls().getJOSMWebsite();
    }

    /**
     * Returns the JOSM XML URL.
     * @return the josm XML URL
     * @deprecated Use {@link IUrls#getXMLBase}
     * @since 6897
     */
    @Deprecated
    public static String getXMLBase() {
        return Config.getUrls().getXMLBase();
    }

    /**
     * Returns the OSM website URL.
     * @return the OSM website URL
     * @deprecated Use {@link IUrls#getOSMWebsite}
     * @since 6897
     */
    @Deprecated
    public static String getOSMWebsite() {
        return Config.getUrls().getOSMWebsite();
    }

    /**
     * Replies the base URL for browsing information about a primitive.
     * @return the base URL, i.e. https://www.openstreetmap.org
     * @deprecated Use {@link IUrls#getBaseBrowseUrl}
     * @since 7678
     */
    @Deprecated
    public static String getBaseBrowseUrl() {
        return Config.getUrls().getBaseBrowseUrl();
    }

    /**
     * Replies the base URL for browsing information about a user.
     * @return the base URL, i.e. https://www.openstreetmap.org/user
     * @deprecated Use {@link IUrls#getBaseUserUrl}
     * @since 7678
     */
    @Deprecated
    public static String getBaseUserUrl() {
        return Config.getUrls().getBaseUserUrl();
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
     * @deprecated Use {@link NetworkManager#isOffline}
     * @since 7434
     */
    @Deprecated
    public static boolean isOffline(OnlineResource r) {
        return NetworkManager.isOffline(r);
    }

    /**
     * Sets the given online resource to offline state.
     * @param r the online resource
     * @return {@code true} if {@code r} was not already offline
     * @deprecated Use {@link NetworkManager#setOffline}
     * @since 7434
     */
    @Deprecated
    public static boolean setOffline(OnlineResource r) {
        return NetworkManager.setOffline(r);
    }

    /**
     * Sets the given online resource to online state.
     * @param r the online resource
     * @return {@code true} if {@code r} was offline
     * @deprecated Use {@link NetworkManager#setOnline}
     * @since 8506
     */
    @Deprecated
    public static boolean setOnline(OnlineResource r) {
        return NetworkManager.setOnline(r);
    }

    /**
     * Replies the set of online resources currently offline.
     * @return the set of online resources currently offline
     * @deprecated Use {@link NetworkManager#getOfflineResources}
     * @since 7434
     */
    @Deprecated
    public static Set<OnlineResource> getOfflineResources() {
        return NetworkManager.getOfflineResources();
    }
}
