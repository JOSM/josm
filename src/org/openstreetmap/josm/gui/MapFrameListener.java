// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

/**
 * Listener notified of MapFrame changes.
 * @since 5957
 */
public interface MapFrameListener {

    /**
     * Called after Main.mapFrame is initalized. (After the first data is loaded).
     * You can use this callback to tweak the newFrame to your needs, as example install
     * an alternative Painter.
     * @param oldFrame The old MapFrame
     * @param newFrame The new MapFrame
     */
    public abstract void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame);
}