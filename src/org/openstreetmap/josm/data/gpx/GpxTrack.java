// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Read-only gpx track. Implementations doesn't have to be immutable, but should always be thread safe.
 * @since 444
 */
public interface GpxTrack extends IWithAttributes {

    /**
     * Returns the track segments.
     * @return the track segments
     */
    Collection<GpxTrackSegment> getSegments();

    /**
     * Returns the track attributes.
     * @return the track attributes
     */
    Map<String, Object> getAttributes();

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
     * Returns the number of times this track has been changed.
     * @return Number of times this track has been changed. Always 0 for read-only tracks
     * @deprecated since 12156 Replaced by change listeners.
     */
    @Deprecated
    default int getUpdateCount() {
        // to allow removal
        return 0;
    }

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
     * @since 12156
     */
    @FunctionalInterface
    public interface GpxTrackChangeListener {
        /**
         * Called when the gpx data changed.
         * @param e The event
         */
        void gpxDataChanged(GpxTrackChangeEvent e);
    }

    /**
     * A track change event for the current track.
     * @author Michael Zangl
     * @since 12156
     */
    class GpxTrackChangeEvent {
        private final GpxTrack source;

        GpxTrackChangeEvent(GpxTrack source) {
            super();
            this.source = source;
        }

        /**
         * Get the track that was changed.
         * @return The track.
         */
        public GpxTrack getSource() {
            return source;
        }
    }
}
