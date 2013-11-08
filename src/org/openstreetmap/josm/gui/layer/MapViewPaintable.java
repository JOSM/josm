// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics2D;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;

public interface MapViewPaintable {

    /**
     * Paint the dataset using the engine set.
     * @param mv The object that can translate GeoPoints to screen coordinates.
     */
    void paint(Graphics2D g, MapView mv, Bounds bbox);

}
