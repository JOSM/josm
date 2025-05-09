// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.openstreetmap.josm.tools.Logging;

/**
 * Class loader for JOSM plugins.
 * <p>
 * In addition to the classes in the plugin jar file, it loads classes of required
 * plugins. The JOSM core classes should be provided by the parent class loader.
 * @since 12322
 */
public class PluginClassLoader extends DynamicURLClassLoader {

    private final Collection<PluginClassLoader> dependencies;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Create a new PluginClassLoader.
     * @param urls URLs of the plugin jar file (and extra libraries)
     * @param parent the parent class loader (for JOSM core classes)
     * @param dependencies class loaders of required plugin; can be null
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent, Collection<PluginClassLoader> dependencies) {
        super(urls, parent);
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
    }

    /**
     * Add class loader of a required plugin.
     * This plugin will have access to the classes of the dependent plugin
     * @param dependency the class loader of the required plugin
     * @return {@code true} if the collection of dependencies changed as a result of the call
     * @since 12867
     */
    public boolean addDependency(PluginClassLoader dependency) {
        // Add dependency only if not already present (directly or transitively through another one)
        boolean result = !dependencies.contains(Objects.requireNonNull(dependency, "dependency"))
                && dependencies.stream().noneMatch(pcl -> pcl.dependencies.contains(dependency))
                && dependencies.add(dependency);
        if (result) {
            // Now, remove top-level single dependencies, which would be children of the added one
            dependencies.removeIf(pcl -> pcl.dependencies.isEmpty() && dependency.dependencies.contains(pcl));
        }
        return result;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            result = findClassInDependencies(name, resolve);
            try {
                // Will delegate to parent.loadClass(name, resolve) if needed
                result = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                Logging.trace("Plugin class not found in super {0}: {1}", this, e.getMessage());
                Logging.trace(e);
            }
        }
        // IcedTea-Web JNLPClassLoader overrides loadClass(String) but not loadClass(String, boolean)
        if (result == null && getParent() != null) {
            try {
                result = getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                Logging.trace("Plugin class not found in parent {0}: {1}", getParent(), e.getMessage());
                Logging.trace(e);
            }
        }
        if (result != null) {
            return result;
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Try to find the specified class in this classes dependencies
     * @param name The name of the class to find
     * @param resolve {@code true} to resolve the class
     * @return the class, if found, otherwise {@code null}
     */
    @SuppressWarnings("PMD.CloseResource") // NOSONAR We do *not* want to close class loaders in this method...
    private Class<?> findClassInDependencies(String name, boolean resolve) {
        for (PluginClassLoader dep : dependencies) {
            try {
                Class<?> result = dep.loadClass(name, resolve);
                if (result != null) {
                    return result;
                }
            } catch (ClassNotFoundException e) {
                Logging.trace("Plugin class not found in dep {0}: {1}", dep, e.getMessage());
                Logging.trace(e);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("PMD.CloseResource") // NOSONAR We do *not* want to close class loaders in this method...
    public URL findResource(String name) {
        URL resource = super.findResource(name);
        if (resource == null) {
            for (PluginClassLoader dep : dependencies) {
                resource = dep.findResource(name);
                if (resource != null) {
                    break;
                }
            }
        }
        return resource;
    }

    @Override
    public String toString() {
        return "PluginClassLoader [urls=" + Arrays.toString(getURLs()) +
                (dependencies.isEmpty() ? "" : ", dependencies=" + dependencies) + ']';
    }
}
