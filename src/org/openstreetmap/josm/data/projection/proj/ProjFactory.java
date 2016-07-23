// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

/**
 * Factory class that provides a Proj instance.
 * @since  5227 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ProjFactory {
    /**
     * Creates a Proj instance.
     * @return a Proj instance
     */
    Proj createInstance();
}
