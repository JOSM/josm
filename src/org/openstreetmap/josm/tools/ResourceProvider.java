// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Unified provider that looks up for resource in various classloaders (josm, plugins, etc.).
 * @since 15416
 */
public final class ResourceProvider {

    /** set of class loaders to take resources from */
    private static final Set<ClassLoader> classLoaders = Collections.synchronizedSet(new HashSet<>());
    static {
        try {
            classLoaders.add(ClassLoader.getSystemClassLoader());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get system classloader", e);
        }
        try {
            classLoaders.add(ResourceProvider.class.getClassLoader());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get application classloader", e);
        }
    }

    private ResourceProvider() {
        // Hide default constructor for utilities classes
    }

    /**
     * Add an additional class loader to search image for.
     * @param additionalClassLoader class loader to add to the internal set
     * @return {@code true} if the set changed as a result of the call
     */
    public static boolean addAdditionalClassLoader(ClassLoader additionalClassLoader) {
        return classLoaders.add(additionalClassLoader);
    }

    /**
     * Add a collection of additional class loaders to search image for.
     * @param additionalClassLoaders class loaders to add to the internal set
     * @return {@code true} if the set changed as a result of the call
     */
    public static boolean addAdditionalClassLoaders(Collection<ClassLoader> additionalClassLoaders) {
        return classLoaders.addAll(additionalClassLoaders);
    }

    private static <T> T getFirstNotNull(Function<ClassLoader, T> function) {
        synchronized (classLoaders) {
            for (ClassLoader source : classLoaders) {
                T res = function.apply(source);
                if (res != null)
                    return res;
            }
        }
        return null;
    }

    /**
     * Finds the resource with the given name.
     * @param name The resource name
     * @return A {@code URL} object for reading the resource, or {@code null} if the resource could not be found
     *         or the invoker doesn't have adequate  privileges to get the resource.
     * @see ClassLoader#getResource
     */
    public static URL getResource(String name) {
        return getFirstNotNull(x -> x.getResource(name));
    }

    /**
     * Finds a resource with a given name, with robustness to known JDK bugs.
     * @param name name of the desired resource
     * @return  A {@link java.io.InputStream} object or {@code null} if no resource with this name is found
     */
    public static InputStream getResourceAsStream(String name) {
        return getFirstNotNull(x -> Utils.getResourceAsStream(x, name));
    }
}
