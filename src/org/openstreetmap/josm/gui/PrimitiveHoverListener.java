// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.event.MouseEvent;

import org.openstreetmap.josm.data.osm.IPrimitive;

/**
 * Interface to notify listeners when the user moves the mouse pointer onto or off of a primitive.
 * @since 18574
 */
@FunctionalInterface
public interface PrimitiveHoverListener {
    /**
     * Method called when the primitive under the mouse pointer changes.
     * @param e Event object describing the hovered primitive and related information
     */
    void primitiveHovered(PrimitiveHoverEvent e);

    /**
     * Event that is fired when the mouse pointer is moved over a primitive.
     */
    class PrimitiveHoverEvent {
        /**
         * The primitive that is being hovered over by the mouse pointer.
         * Can be null if the mouse pointer is not over any primitive.
         */
        private final IPrimitive hoveredPrimitive;
        private final IPrimitive previousPrimitive;
        private final MouseEvent mouseEvent;

        /**
         * Construct a new {@code PrimitiveHoverEvent}
         * @param hoveredPrimitive Primitive that is hovered by the mouse pointer
         * @param previousPrimitive Previously hovered primitive
         * @param mouseEvent {@link MouseEvent} that triggered this hover event
         */
        public PrimitiveHoverEvent(IPrimitive hoveredPrimitive, IPrimitive previousPrimitive, MouseEvent mouseEvent) {
            this.hoveredPrimitive = hoveredPrimitive;
            this.previousPrimitive = previousPrimitive;
            this.mouseEvent = mouseEvent;
        }

        /**
         * Get the primitive that is being hovered over with the mouse pointer
         * @return The primitive that is being hovered over
         */
        public IPrimitive getHoveredPrimitive() {
            return hoveredPrimitive;
        }

        /**
         * Get the previously hovered primitive
         * @return The previously hovered primitive
         */
        public IPrimitive getPreviousPrimitive() {
            return previousPrimitive;
        }

        /**
         * Get the {@link MouseEvent} object that triggered this hover event
         * @return The {@link MouseEvent} that triggered this hover event
         */
        public MouseEvent getMouseEvent() {
            return mouseEvent;
        }
    }
}
