// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.net.URL;
import java.net.URLClassLoader;
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
public class PluginClassLoader extends URLClassLoader {

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
            for (PluginClassLoader dep : dependencies) {
                try {
                    result = dep.loadClass(name, resolve);
                    if (result != null) {
                        return result;
                    }
                } catch (ClassNotFoundException e) {
                    // do nothing
                    Logging.trace("Plugin class not found in {0}: {1}", dep, e.getMessage());
                    Logging.trace(e);
                }
            }
            result = super.loadClass(name, resolve);
        }
        if (result != null) {
            return result;
        }
        throw new ClassNotFoundException(name);
    }

    @Override
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
