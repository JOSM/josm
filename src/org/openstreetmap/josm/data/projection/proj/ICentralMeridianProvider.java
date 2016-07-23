// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

/**
 * A {@link Proj} implements this interface, if it derives the central meridian
 * value from it's other input parameters.
 *
 * (Normally the central meridian is projection input parameter and the Proj
 * class does not deal with it.)
 *
 * @see Proj
 * @since  9532 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ICentralMeridianProvider {
    /**
     * Get the central meridian value as computed during initialization.
     * @return the central meridian in degrees
     */
    double getCentralMeridian();
}
