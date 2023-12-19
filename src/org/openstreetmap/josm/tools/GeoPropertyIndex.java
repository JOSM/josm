// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;

/**
 * Fast index to look up properties of the earth surface.
 * <p>
 * It is expected that there is a relatively slow method to look up the property
 * for a certain coordinate and that there are larger areas with a uniform
 * property.
 * <p>
 * This index tries to find rectangles with uniform property and caches them.
 * Rectangles are subdivided, if there are different properties within.
 * (Up to a maximum level, when the slow method is used again.)
 *
 * @param <T> the property (like land/water or nation)
 */
public class GeoPropertyIndex<T> {

    private final int maxLevel;
    private final GeoProperty<T> geoProp;
    private final GPLevel<T> root;
    private GPLevel<T> lastLevelUsed;

    private static final boolean DEBUG = false;

    /**
     * Create new GeoPropertyIndex.
     * @param geoProp the input property that should be made faster by this index
     * @param maxLevel max level
     */
    public GeoPropertyIndex(GeoProperty<T> geoProp, int maxLevel) {
        this.geoProp = geoProp;
        this.maxLevel = maxLevel;
        this.root = new GPLevel<>(0, new BBox(-180, -90, 180, 90), null, this);
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

    /**
     * Returns the geo property.
     * @return the geo property
     * @since 14484
     */
    public final GeoProperty<T> getGeoProperty() {
        return geoProp;
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
            if (DEBUG) Logging.trace("up[{0}]", level);
            return parent != null ? parent.get(ll) : null;
        }

        private T getBounded(LatLon ll) {
            if (DEBUG) Logging.trace("GPLevel[{0}]{1}", level, bbox);
            if (!isInside(ll)) {
                throw new AssertionError("Point "+ll+" should be inside "+bbox);
            }
            if (val != null) {
                if (DEBUG) Logging.trace("GPLevel[{0}]{1} hit! {2}", level, bbox, val);
                owner.lastLevelUsed = this;
                return val;
            }
            if (level >= owner.maxLevel) {
                if (DEBUG) Logging.trace("GPLevel[{0}]{1} max level reached !", level, bbox);
                return owner.geoProp.get(ll);
            }

            GPLevel<T>[] currentChildren = this.getChildren();

            LatLon center = bbox.getCenter();
            for (int idx = 0; idx < 4; idx++) {
                BBox testBBox = null;
                if (currentChildren[idx] != null)
                    testBBox = currentChildren[idx].bbox;

                if (testBBox == null) {
                    testBBox = generateTestBBox(idx, center.lon(), center.lat());
                }
                if (isInside(testBBox, ll)) {
                    generateChild(currentChildren, testBBox, idx);
                    return currentChildren[idx].getBounded(ll);
                }
            }
            throw new AssertionError("Point "+ll+" should be inside one of the children of "+bbox);
        }

        /**
         * Generate the bbox for the specified index in the {@link #getChildren()} array
         * @param idx The index in the {@link #getChildren()} array
         * @param lon2 The longitude of the center of the previous level
         * @param lat2 The latitude of the center of the previous level
         * @return The test bbox for the specified index in the {@link #getChildren()} array
         */
        private BBox generateTestBBox(int idx, double lon2, double lat2) {
            double lon1;
            double lat1;
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
            return new BBox(lon1, lat1, lon2, lat2);
        }

        /**
         * Safely generate the child in a multi-threaded environment
         * @param currentChildren The children array to check
         * @param testBBox The current bbox (will be used to create the new {@link GPLevel})
         * @param idx The index in the child array
         */
        private synchronized void generateChild(GPLevel<T>[] currentChildren, BBox testBBox, int idx) {
            if (currentChildren[idx] == null) {
                if (DEBUG) Logging.trace("GPLevel[{0}]{1} - new with idx {2}", level, bbox, idx);
                currentChildren[idx] = new GPLevel<>(level + 1, testBBox, this, owner);
            }
        }

        /**
         * Get the children array safely in a multi-threaded environment.
         * If this ends up being a performance issue, look up the "immutable" double-checked locking idiom.
         * Just be certain you have a good test when checking the performance differences.
         * See #23309/#23036. The issue there is that {@link Territories#getRegionalTaginfoUrls(LatLon)}
         * uses a {@link Collection#parallelStream}.
         * @return The children array (sw, nw, se, ne)
         */
        private synchronized GPLevel<T>[] getChildren() {
            GPLevel<T>[] currentChildren = this.children;
            if (currentChildren == null) {
                @SuppressWarnings("unchecked")
                GPLevel<T>[] tmp = new GPLevel[4];
                currentChildren = tmp;
                this.children = currentChildren;
            }
            return currentChildren;
        }

        /**
         * Checks, if a point is inside this tile.
         * Makes sure, that neighboring tiles do not overlap, i.e. a point exactly
         * on the border of two tiles must be inside exactly one of the tiles.
         * @param ll the coordinates of the point
         * @return true, if it is inside of the box
         */
        boolean isInside(ILatLon ll) {
            return isInside(bbox, ll);
        }

        /**
         * Checks, if a point is inside this tile.
         * Makes sure, that neighboring tiles do not overlap, i.e. a point exactly
         * on the border of two tiles must be inside exactly one of the tiles.
         * @param bbox the tile
         * @param ll the coordinates of the point
         * @return true, if it is inside of the box
         */
        boolean isInside(BBox bbox, ILatLon ll) {
            return bbox.getTopLeftLon() <= ll.lon() &&
                    (ll.lon() < bbox.getBottomRightLon() || (ll.lon() == 180.0 && bbox.getBottomRightLon() == 180.0)) &&
                    bbox.getBottomRightLat() <= ll.lat() &&
                    (ll.lat() < bbox.getTopLeftLat() || (ll.lat() == 90.0 && bbox.getTopLeftLat() == 90.0));
        }

        @Override
        public String toString() {
            return "GPLevel [val=" + val + ", level=" + level + ", bbox=" + bbox + ']';
        }
    }

    @Override
    public String toString() {
        return "GeoPropertyIndex [maxLevel=" + maxLevel + ", geoProp=" + geoProp + ", root=" + root + ", lastLevelUsed="
                + lastLevelUsed + ']';
    }
}
