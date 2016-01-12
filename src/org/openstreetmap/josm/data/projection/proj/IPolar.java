// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

/**
 * If a Proj class implements this interface, it indicates that the projection
 * can be used to view one or both of the poles.
 */
public interface IPolar {
    /**
     * Return true if north / south pole can be mapped by this projection.
     * @param south if true, asks for the south pole, otherwise for the north pole
     * @return true if north / south pole can be mapped by this projection
     */
    boolean hasPole(boolean south);
}
