// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

/**
 * A {@link Proj} implements this interface, if it derives the scale factor
 * value from it's other input parameters.
 *
 * (Normally the scale factor is projection input parameter and the Proj
 * class does not deal with it.)
 *
 * @see Proj
 */
public interface IScaleFactorProvider {
    /**
     * Get the scale factor.
     * Will be multiplied by the scale factor parameter, if supplied by the user
     * explicitly.
     * @return the scale factor
     */
    double getScaleFactor();
}
