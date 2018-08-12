// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import java.awt.Component;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.IOsmDataManager;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.lifecycle.Lifecycle;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * Abstract class holding various static global variables and methods used in large parts of JOSM application.
 * @since 98
 */
public abstract class Main implements IOsmDataManager {

    /**
     * Global parent component for all dialogs and message boxes
     */
    public static Component parent;

    /**
     * Global application.
     * @deprecated Not needed anymore
     */
    @Deprecated
    public static volatile Main main;

    /**
     * Global application preferences
     * @deprecated Use {@link Config#getPref()} or {@link Preferences#main()}
     */
    @Deprecated
    public static final Preferences pref = new Preferences(JosmBaseDirectories.getInstance());

    /**
     * The commands undo/redo handler.
     * @deprecated Use {@link UndoRedoHandler#getInstance}
     */
    @Deprecated
    public final UndoRedoHandler undoRedo = UndoRedoHandler.getInstance();

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
     * @deprecated Use {@link PlatformManager#getPlatform}
     */
    @Deprecated
    public static final PlatformHook platform = PlatformManager.getPlatform();

    /**
     * Constructs new {@code Main} object.
     */
    protected Main() {
        setInstance(this);
    }

    private static void setInstance(Main instance) {
        main = instance;
    }

    ///////////////////////////////////////////////////////////////////////////
    //  Implementation part
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Closes JOSM and optionally terminates the Java Virtual Machine (JVM).
     * @param exit If {@code true}, the JVM is terminated by running {@link System#exit} with a given return code.
     * @param exitCode The return code
     * @return {@code true}
     * @since 12636
     * @deprecated Use {@link Lifecycle#exitJosm}
     */
    @Deprecated
    public static boolean exitJosm(boolean exit, int exitCode) {
        return Lifecycle.exitJosm(exit, exitCode);
    }

    /**
     * Identifies the current operating system family and initializes the platform hook accordingly.
     * @since 1849
     * @deprecated Not needed anymore
     */
    @Deprecated
    public static void determinePlatformHook() {
        // Do nothing
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
     * @deprecated Use {@link PlatformManager#isPlatformOsx}
     */
    @Deprecated
    public static boolean isPlatformOsx() {
        return PlatformManager.isPlatformOsx();
    }

    /**
     * Determines if we are currently running on Windows.
     * @return {@code true} if we are currently running on Windows
     * @since 7335
     * @deprecated Use {@link PlatformManager#isPlatformWindows}
     */
    @Deprecated
    public static boolean isPlatformWindows() {
        return PlatformManager.isPlatformWindows();
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
