// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * ClassLoader that makes the {@link #addURL} method of {@link URLClassLoader} public.
 *
 * Like URLClassLoader, but allows to add more URLs after construction.
 * @since 14234 (extracted from PluginHandler)
 */
public class DynamicURLClassLoader extends URLClassLoader {

    /**
     * Constructs a new {@code DynamicURLClassLoader}.
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     */
    public DynamicURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
