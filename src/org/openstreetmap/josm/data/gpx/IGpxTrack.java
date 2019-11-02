// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.Collection;

import org.openstreetmap.josm.data.Bounds;

/**
 * Gpx track. Implementations don't have to be immutable, but should always be thread safe.
 * @since 15496
 */
public interface IGpxTrack extends IWithAttributes {

    /**
     * Returns the track segments.
     * @return the track segments
     */
    Collection<IGpxTrackSegment> getSegments();

    /**
     * Returns the track bounds.
     * @return the track bounds
     */
    Bounds getBounds();

    /**
     * Returns the track length.
     * @return the track length
     */
    double length();

    /**
     * Gets the color of this track.
     * @return The color, <code>null</code> if not set or not supported by the implementation.
     * @since 15496
     */
    default Color getColor() {
        return null;
    }

    /**
     * Sets the color of this track. Not necessarily supported by all implementations.
     * @param color
     * @since 15496
     */
    default void setColor(Color color) {}

    /**
     * Add a listener that listens to changes in the GPX track.
     * @param l The listener
     */
    default void addListener(GpxTrackChangeListener l) {
        // nop
    }

    /**
     * Remove a listener that listens to changes in the GPX track.
     * @param l The listener
     */
    default void removeListener(GpxTrackChangeListener l) {
        // nop
    }

    /**
     * A listener that listens to GPX track changes.
     * @author Michael Zangl
     * @since 15496
     */
    @FunctionalInterface
    interface GpxTrackChangeListener {
        /**
         * Called when the gpx data changed.
         * @param e The event
         */
        void gpxDataChanged(GpxTrackChangeEvent e);
    }

    /**
     * A track change event for the current track.
     * @author Michael Zangl
     * @since 15496
     */
    class GpxTrackChangeEvent {
        private final IGpxTrack source;

        GpxTrackChangeEvent(IGpxTrack source) {
            super();
            this.source = source;
        }

        /**
         * Get the track that was changed.
         * @return The track.
         */
        public IGpxTrack getSource() {
            return source;
        }
    }
}
