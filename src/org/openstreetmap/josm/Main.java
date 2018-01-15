// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
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
     */
    public static final FileWatcher fileWatcher = new FileWatcher();

    private static final Map<String, Throwable> NETWORK_ERRORS = new HashMap<>();

    private static final Set<OnlineResource> OFFLINE_RESOURCES = EnumSet.noneOf(OnlineResource.class);

    /**
     * Platform specific code goes in here.
     * Plugins may replace it, however, some hooks will be called before any plugins have been loaded.
     * So if you need to hook into those early ones, split your class and send the one with the early hooks
     * to the JOSM team for inclusion.
     */
    public static volatile PlatformHook platform;

    private static volatile InitStatusListener initListener;

    /**
     * Initialization task listener.
     */
    public interface InitStatusListener {

        /**
         * Called when an initialization task updates its status.
         * @param event task name
         * @return new status
         */
        Object updateStatus(String event);

        /**
         * Called when an initialization task completes.
         * @param status final status
         */
        void finish(Object status);
    }

    /**
     * Sets initialization task listener.
     * @param listener initialization task listener
     */
    public static void setInitStatusListener(InitStatusListener listener) {
        CheckParameterUtil.ensureParameterNotNull(listener);
        initListener = listener;
    }

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
            service.shutdown();
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

    protected static final class InitializationTask implements Callable<Void> {

        private final String name;
        private final Runnable task;

        /**
         * Constructs a new {@code InitializationTask}.
         * @param name translated name to be displayed to user
         * @param task runnable initialization task
         */
        public InitializationTask(String name, Runnable task) {
            this.name = name;
            this.task = task;
        }

        @Override
        public Void call() {
            Object status = null;
            if (initListener != null) {
                status = initListener.updateStatus(name);
            }
            task.run();
            if (initListener != null) {
                initListener.finish(status);
            }
            return null;
        }
    }

    /**
     * Replies the current selected primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link DataSet#getSelected()}.
     * @return The current selected primitives, from a end-user point of view. Can be {@code null}.
     * @since 6546
     */
    public Collection<OsmPrimitive> getInProgressSelection() {
        return Collections.emptyList();
    }

    /**
     * Gets the active edit data set.
     * @return That data set, <code>null</code>.
     * @since 12691
     */
    public abstract DataSet getEditDataSet();

    /**
     * Sets the active data set.
     * @param ds New edit data set, or <code>null</code>
     * @since 12718
     */
    public abstract void setEditDataSet(DataSet ds);

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
        } catch (IOException ex) {
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

    /* ----------------------------------------------------------------------------------------- */
    /* projection handling  - Main is a registry for a single, global projection instance        */
    /*                                                                                           */
    /* TODO: For historical reasons the registry is implemented by Main. An alternative approach */
    /* would be a singleton org.openstreetmap.josm.data.projection.ProjectionRegistry class.     */
    /* ----------------------------------------------------------------------------------------- */
    /**
     * The projection method used.
     * Use {@link #getProjection()} and {@link #setProjection(Projection)} for access.
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
        Bounds b = main != null ? main.getRealBounds() : null;
        proj = p;
        fireProjectionChanged(oldValue, proj, b);
    }

    /**
     * Returns the bounds for the current projection. Used for projection events.
     * @return the bounds for the current projection
     * @see #restoreOldBounds
     */
    protected Bounds getRealBounds() {
        // To be overriden
        return null;
    }

    /**
     * Restore clean state corresponding to old bounds after a projection change event.
     * @param oldBounds bounds previously returned by {@link #getRealBounds}, before the change of projection
     * @see #getRealBounds
     */
    protected void restoreOldBounds(Bounds oldBounds) {
        // To be overriden
    }

    /*
     * Keep WeakReferences to the listeners. This relieves clients from the burden of
     * explicitly removing the listeners and allows us to transparently register every
     * created dataset as projection change listener.
     */
    private static final List<WeakReference<ProjectionChangeListener>> listeners = new CopyOnWriteArrayList<>();

    private static void fireProjectionChanged(Projection oldValue, Projection newValue, Bounds oldBounds) {
        if ((newValue == null ^ oldValue == null)
                || (newValue != null && oldValue != null && !Objects.equals(newValue.toCode(), oldValue.toCode()))) {
            listeners.removeIf(x -> x.get() == null);
            listeners.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(x -> x.projectionChanged(oldValue, newValue));
            if (newValue != null && oldBounds != null && main != null) {
                main.restoreOldBounds(oldBounds);
            }
            /* TODO - remove layers with fixed projection */
        }
    }

    /**
     * Register a projection change listener.
     * The listener is registered to be weak, so keep a reference of it if you want it to be preserved.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void addProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        for (WeakReference<ProjectionChangeListener> wr : listeners) {
            // already registered ? => abort
            if (wr.get() == listener) return;
        }
        listeners.add(new WeakReference<>(listener));
    }

    /**
     * Removes a projection change listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void removeProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        // remove the listener - and any other listener which got garbage collected in the meantime
        listeners.removeIf(wr -> wr.get() == null || wr.get() == listener);
    }

    /**
     * Remove all projection change listeners. For testing purposes only.
     * @since 13322
     */
    public static void clearProjectionChangeListeners() {
        listeners.clear();
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
                Logging.warn("Already here "+old);
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
     * Clears the network errors cache.
     * @since 12011
     */
    public static void clearNetworkErrors() {
        NETWORK_ERRORS.clear();
    }

    /**
     * Returns the JOSM website URL.
     * @return the josm website URL
     * @since 6897
     */
    public static String getJOSMWebsite() {
        if (Config.getPref() != null)
            return Config.getPref().get("josm.url", JOSM_WEBSITE);
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
        if (Config.getPref() != null)
            return Config.getPref().get("osm.url", OSM_WEBSITE);
        return OSM_WEBSITE;
    }

    /**
     * Returns the OSM website URL depending on the selected {@link OsmApi}.
     * @return the OSM website URL depending on the selected {@link OsmApi}
     */
    private static String getOSMWebsiteDependingOnSelectedApi() {
        final String api = OsmApi.getOsmApi().getServerUrl();
        if (OsmApi.DEFAULT_API_URL.equals(api)) {
            return getOSMWebsite();
        } else {
            return api.replaceAll("/api$", "");
        }
    }

    /**
     * Replies the base URL for browsing information about a primitive.
     * @return the base URL, i.e. https://www.openstreetmap.org
     * @since 7678
     */
    public static String getBaseBrowseUrl() {
        if (Config.getPref() != null)
            return Config.getPref().get("osm-browse.url", getOSMWebsiteDependingOnSelectedApi());
        return getOSMWebsiteDependingOnSelectedApi();
    }

    /**
     * Replies the base URL for browsing information about a user.
     * @return the base URL, i.e. https://www.openstreetmap.org/user
     * @since 7678
     */
    public static String getBaseUserUrl() {
        if (Config.getPref() != null)
            return Config.getPref().get("osm-user.url", getOSMWebsiteDependingOnSelectedApi() + "/user");
        return getOSMWebsiteDependingOnSelectedApi() + "/user";
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
        return EnumSet.copyOf(OFFLINE_RESOURCES);
    }
}
