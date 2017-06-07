// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.tools.Logging;

/**
 * Class loader for JOSM plugins.
 * <p>
 * In addition to the classes in the plugin jar file, it loads classes of required
 * plugins. The JOSM core classes should be provided by the the parent class loader.
 * @since 12322
 */
public class PluginClassLoader extends URLClassLoader {

    Collection<PluginClassLoader> dependencies;

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
     */
    public void addDependency(PluginClassLoader dependency) {
        dependencies.add(dependency);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (PluginClassLoader dep : dependencies) {
            try {
                Class<?> result = dep.loadClass(name, resolve);
                if (result != null) {
                    return result;
                }
            } catch (ClassNotFoundException e) {
                // do nothing
                Logging.trace("Plugin class not found in {0}: {1}", dep, e.getMessage());
            }
        }
        Class<?> result = super.loadClass(name, resolve);
        if (result != null) {
            return result;
        }
        throw new ClassNotFoundException(name);
    }
}
