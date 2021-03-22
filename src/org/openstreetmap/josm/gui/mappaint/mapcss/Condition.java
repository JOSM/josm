// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;

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
     * Checks if the condition applies in the given {@link Tagged} element.
     * @param tagged The tagged to check.
     * @return <code>true</code> if the condition applies.
     */
    default boolean applies(Tagged tagged) {
        return false;
    }

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
     * @since 10674 (ToTagConvertable), 17642 (TagCondition)
     */
    interface TagCondition extends Condition {

        @Override
        default boolean applies(Environment e) {
            CheckParameterUtil.ensureThat(!e.isLinkContext(), "Illegal state: TagCondition not supported in LINK context");
            return applies(e.osm);
        }

        @Override
        boolean applies(Tagged tagged);

        /**
         * Converts the current condition to a tag
         * @param tagged A tagged object to use as context. May be ignored.
         * @return A tag with the key/value of this condition.
         */
        Tag asTag(Tagged tagged);
    }
}
