// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.gui.mappaint.StyleCache;

/**
 * Object that can be rendered using a cacheable style.
 * @since 13636
 */
public interface Stylable {

    /**
     * Returns the cached style.
     * @return the cached style
     */
    StyleCache getCachedStyle();

    /**
     * Sets the cached style.
     * @param mappaintStyle the cached style
     */
    void setCachedStyle(StyleCache mappaintStyle);

    /**
     * Clears the cached style.
     * This should not be called from outside. Fixing the UI to add relevant
     * get/set functions calling this implicitely is preferred, so we can have
     * transparent cache handling in the future.
     */
    default void clearCachedStyle() {
        setCachedStyle(null);
    }

    /**
     * Check if the cached style for this primitive is up to date.
     * @return true if the cached style for this primitive is up to date
     * @since 13420
     */
    boolean isCachedStyleUpToDate();

    /**
     * Declare that the cached style for this primitive is up to date.
     * @since 13420
     */
    void declareCachedStyleUpToDate();
}
