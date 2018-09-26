// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import org.openstreetmap.josm.data.Bounds;

/**
 * A BBoxChooser is a component which provides a UI for choosing a
 * bounding box.
 *
 */
public interface BBoxChooser {

    /**
     * A BBoxChooser emits {@link java.beans.PropertyChangeEvent}s for this property
     * if the current bounding box changes.
     */
    String BBOX_PROP = BBoxChooser.class.getName() + ".bbox";

    /**
     * Sets the current bounding box in this BboxChooser. If {@code bbox}
     * is null the current bbox in this BBoxChooser is removed.
     *
     * @param bbox the bounding box
     */
    void setBoundingBox(Bounds bbox);

    /**
     * Replies the currently selected bounding box in this BBoxChooser.
     * Replies null, if currently there isn't a bbox chosen in this
     * BBoxChooser.
     *
     * @return the currently selected bounding box
     */
    Bounds getBoundingBox();
}
