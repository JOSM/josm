// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.StyleCache;

/**
 * Object that can be rendered using a cacheable style.
 * @since 13636
 */
public interface Stylable {

    /**
     * Returns the cached style.
     * @param styles styles for which the data is retrieved
     * @return the cached style
     * @since 19528 (added param styles)
     */
    StyleCache getCachedStyle(ElemStyles styles);

    /**
     * Sets the cached style.
     * @param styles styles for which the data is stored
     * @param mappaintStyle the cached style
     * @since 19528 (added param styles)
     */
    void setCachedStyle(ElemStyles styles, StyleCache mappaintStyle);

    /**
     * Clears the cached style.
     * This should not be called from outside. Fixing the UI to add relevant
     * get/set functions calling this implicitly is preferred, so we can have
     * transparent cache handling in the future.
     */
    void clearCachedStyle();

    /**
     * Check if the cached style for this primitive is up to date.
     * @param styles styles for which the data is checked
     * @return true if the cached style for this primitive is up to date
     * @since 13420
     * @since 19528 (added param styles)
     */
    boolean isCachedStyleUpToDate(ElemStyles styles);

    /**
     * Declare that the cached style for this primitive is up to date.
     * @param styles styles for which the data is handled
     * @since 13420
     * @since 19528 (added param styles)
     */
    void declareCachedStyleUpToDate(ElemStyles styles);
}
