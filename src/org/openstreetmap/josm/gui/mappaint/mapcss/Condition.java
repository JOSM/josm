// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.mappaint.Environment;

/**
 * This is a condition that needs to be fulfilled in order to apply a MapCSS style.
 */
@FunctionalInterface
public interface Condition {

    /**
     * Checks if the condition applies in the given MapCSS {@link Environment}.
     * @param e The environment to check. May not be <code>null</code>.
     * @return <code>true</code> if the condition applies.
     */
    boolean applies(Environment e);

    /**
     * Context, where the condition applies.
     */
    enum Context {
        /**
         * normal primitive selector, e.g. way[highway=residential]
         */
        PRIMITIVE,

        /**
         * link between primitives, e.g. relation &gt;[role=outer] way
         */
        LINK
    }

    /**
     * This is a condition that can be converted to a tag
     * @author Michael Zangl
     * @since 10674
     */
    @FunctionalInterface
    interface ToTagConvertable {
        /**
         * Converts the current condition to a tag
         * @param primitive A primitive to use as context. May be ignored.
         * @return A tag with the key/value of this condition.
         */
        Tag asTag(OsmPrimitive primitive);
    }
}
