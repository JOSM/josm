// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

/**
 * Listener notified of MapFrame changes.
 * @since 5957
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface MapFrameListener {

    /**
     * Called after Main.mapFrame is initialized. (After the first data is loaded).
     * You can use this callback to tweak the newFrame to your needs, as example install
     * an alternative Painter.
     * @param oldFrame The old MapFrame
     * @param newFrame The new MapFrame
     */
    void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame);
}
