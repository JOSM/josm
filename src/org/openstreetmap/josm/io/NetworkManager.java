// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.tools.Logging;

/**
 * Handles global network features (errors and online/offline resources).
 * @since 14121
 */
public final class NetworkManager {

    private static final Map<String, Throwable> NETWORK_ERRORS = new HashMap<>();

    private static final Set<OnlineResource> OFFLINE_RESOURCES = EnumSet.noneOf(OnlineResource.class);

    private NetworkManager() {
        // Hide constructor
    }

    /**
     * Adds a new network error that occur to give a hint about broken Internet connection.
     * Do not use this method for errors known for sure thrown because of a bad proxy configuration.
     *
     * @param url The accessed URL that caused the error
     * @param t The network error
     * @return The previous error associated to the given resource, if any. Can be {@code null}
     */
    public static Throwable addNetworkError(String url, Throwable t) {
        if (url != null && t != null) {
            return NETWORK_ERRORS.put(url, t);
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
     * Returns the network errors that occurred until now.
     * @return the network errors that occurred until now, indexed by URL
     */
    public static Map<String, Throwable> getNetworkErrors() {
        return new HashMap<>(NETWORK_ERRORS);
    }

    /**
     * Clears the network errors cache.
     */
    public static void clearNetworkErrors() {
        NETWORK_ERRORS.clear();
    }

    /**
     * Determines if the given online resource specified as URL is currently offline.
     * @param url the online resource specified as URL
     * @return {@code true} if {@code url} is offline and should not be accessed
     * @since 16426
     */
    public static boolean isOffline(String url) {
        return OFFLINE_RESOURCES.stream().anyMatch(r -> r.matches(url));
    }

    /**
     * Determines if the given online resource is currently offline.
     * @param r the online resource
     * @return {@code true} if {@code r} is offline and should not be accessed
     */
    public static boolean isOffline(OnlineResource r) {
        return OFFLINE_RESOURCES.contains(r) || OFFLINE_RESOURCES.contains(OnlineResource.ALL);
    }

    /**
     * Sets the given online resource to offline state.
     * @param r the online resource
     * @return {@code true} if {@code r} was not already offline
     */
    public static boolean setOffline(OnlineResource r) {
        return OFFLINE_RESOURCES.add(r);
    }

    /**
     * Sets the given online resource to online state.
     * @param r the online resource
     * @return {@code true} if {@code r} was offline
     */
    public static boolean setOnline(OnlineResource r) {
        return OFFLINE_RESOURCES.remove(r);
    }

    /**
     * Replies the set of online resources currently offline.
     * @return the set of online resources currently offline
     */
    public static Set<OnlineResource> getOfflineResources() {
        return EnumSet.copyOf(OFFLINE_RESOURCES);
    }
}
