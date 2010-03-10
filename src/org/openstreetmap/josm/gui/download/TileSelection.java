// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.bbox.BBoxChooser;
import org.openstreetmap.josm.gui.bbox.TileSelectionBBoxChooser;
/**
 * Tile selector.
 *
 * Provides a tile coordinate input field.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class TileSelection implements DownloadSelection, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(TileSelection.class.getName());

    private TileSelectionBBoxChooser chooser;
    private DownloadDialog parent;

    protected void build() {
        chooser = new TileSelectionBBoxChooser();
        chooser.addPropertyChangeListener(this);
    }

    public TileSelection() {
        build();
    }

    public void addGui(final DownloadDialog gui) {
        gui.addDownloadAreaSelector(chooser, tr("Tile Numbers"));
        parent = gui;
    }

    public void setDownloadArea(Bounds area) {
        chooser.setBoundingBox(area);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(BBoxChooser.BBOX_PROP)) {
            Bounds bbox = (Bounds)evt.getNewValue();
            parent.boundingBoxChanged(bbox, this);
        }
    }
}
