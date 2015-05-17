// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;

/**
 * Fast index to look up properties of the earth surface.
 *
 * It is expected that there is a relatively slow method to look up the property
 * for a certain coordinate and that there are larger areas with a uniform
 * property.
 *
 * This index tries to find rectangles with uniform property and caches them.
 * Rectangles are subdivided, if there are different properties within.
 * (Up to a maximum level, when the slow method is used again.)
 *
 * @param <T> the property (like land/water or nation)
 */
public class GeoPropertyIndex<T> {

    /**
     * A method to look up a property of the earth surface.
     * (User input for the index.)
     * @param <T> the property
     */
    public interface GeoProperty<T> {
        /**
         * Look up the property for a point.
         * @param ll the point coordinates
         * @return property value at that point. Must not be null.
         */
        T get(LatLon ll);
        /**
         * Look up the property for a coordinate rectangle.
         * @param box the rectangle
         * @return the property, if it is the same in the entire rectangle;
         * null otherwise
         */
        T get(BBox box);
    }

    private final int maxLevel;
    private final GeoProperty<T> geoProp;
    private final GPLevel<T> root;
    private GPLevel<T> lastLevelUsed;

    private static final boolean DEBUG = false;

    /**
     * Create new GeoPropertyIndex.
     * @param geoProp the input property that should be made faster by this index
     * @param maxLevel
     */
    public GeoPropertyIndex(GeoProperty<T> geoProp, int maxLevel) {
        this.geoProp = geoProp;
        this.maxLevel = maxLevel;
        this.root = new GPLevel<T>(0, new BBox(-180, -90, 180, 90), null, this);
        this.lastLevelUsed = root;
    }

    /**
     * Look up the property for a certain point.
     * This gives the same result as {@link GeoProperty#get(LatLon)}, but
     * should be faster.
     * @param ll the point coordinates
     * @return property value at that point
     */
    public T get(LatLon ll) {
        return lastLevelUsed.get(ll);
    }

    public static int index(LatLon ll, int level) {
        long noParts = 1 << level;
        long x = ((long)((ll.lon() + 180.0) * noParts / 360.0)) & 1;
        long y = ((long)((ll.lat() + 90.0) * noParts / 180.0)) & 1;
        return (int) (2 * x + y);
    }

    protected static class GPLevel<T> {
        private final T val;
        private final int level;
        private final BBox bbox;
        private final GPLevel<T> parent;
        private final GeoPropertyIndex<T> owner;

        // child order by index is sw, nw, se, ne
        private GPLevel<T>[] children;

        public GPLevel(int level, BBox bbox, GPLevel<T> parent, GeoPropertyIndex<T> owner) {
            this.level = level;
            this.bbox = bbox;
            this.parent = parent;
            this.owner = owner;
            this.val = owner.geoProp.get(bbox);
        }

        public T get(LatLon ll) {
            if (isInside(ll))
                return getBounded(ll);
            if (DEBUG) System.err.print("up["+level+"]");
            return parent.get(ll);
        }

        private T getBounded(LatLon ll) {
            if (DEBUG) System.err.print("GPLevel["+level+"]"+bbox+" ");
            if (!isInside(ll)) {
                throw new AssertionError("Point "+ll+" should be inside "+bbox);
            }
            if (val != null) {
                if (DEBUG) System.err.println(" hit! "+val);
                owner.lastLevelUsed = this;
                return val;
            }
            if (level >= owner.maxLevel) {
                if (DEBUG) System.err.println(" max level reached !");
                return owner.geoProp.get(ll);
            }

            if (children == null) {
                @SuppressWarnings("unchecked")
                GPLevel<T>[] tmp = new GPLevel[4];
                this.children = tmp;
            }

            int idx = index(ll, level+1);
            if (children[idx] == null) {
            double lon1, lat1;
                switch (idx) {
                    case 0:
                        lon1 = bbox.getTopLeftLon();
                        lat1 = bbox.getBottomRightLat();
                        break;
                    case 1:
                        lon1 = bbox.getTopLeftLon();
                        lat1 = bbox.getTopLeftLat();
                        break;
                    case 2:
                        lon1 = bbox.getBottomRightLon();
                        lat1 = bbox.getBottomRightLat();
                        break;
                    case 3:
                        lon1 = bbox.getBottomRightLon();
                        lat1 = bbox.getTopLeftLat();
                        break;
                    default:
                        throw new AssertionError();
                }
                if (DEBUG) System.err.println(" - new with idx "+idx);
                LatLon center = bbox.getCenter();
                BBox b = new BBox(lon1,lat1, center.lon(), center.lat());
                children[idx] = new GPLevel<>(level + 1, b, this, owner);
            }
            return children[idx].getBounded(ll);
        }

        /**
         * Checks, if a point is inside this tile.
         * Makes sure, that neighboring tiles do not overlap, i.e. a point exactly
         * on the border of two tiles must be inside exactly one of the tiles.
         * @param ll the coordinates of the point
         * @return true, if it is inside of the box
         */
        boolean isInside(LatLon ll) {
            return bbox.getTopLeftLon() <= ll.lon() &&
                    (ll.lon() < bbox.getBottomRightLon() ||
                            (Utils.equalsEpsilon(ll.lon(), 180.0) && Utils.equalsEpsilon(bbox.getBottomRightLon(), 180.0))) &&
                    bbox.getBottomRightLat() <= ll.lat() &&
                    (ll.lat() < bbox.getTopLeftLat() ||
                            (Utils.equalsEpsilon(ll.lat(), 90.0) && Utils.equalsEpsilon(bbox.getTopLeftLat(), 90.0)));
        }

    }
}
