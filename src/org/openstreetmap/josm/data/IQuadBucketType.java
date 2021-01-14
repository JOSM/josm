// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.QuadBuckets;

/**
 * The minimum necessary interface to use {@link QuadBuckets}.
 * @author Taylor Smock
 * @since 17459
 */
@FunctionalInterface
public interface IQuadBucketType {
    /**
     * Fetches the bounding box of the primitive.
     * @return Bounding box of the object
     */
    BBox getBBox();
}
