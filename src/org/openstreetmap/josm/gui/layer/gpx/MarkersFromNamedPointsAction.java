// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action that converts the named points to a new marker layer
 */
public class MarkersFromNamedPointsAction extends AbstractAction {
    private final transient GpxLayer layer;

    /**
     * Construct a new {@link MarkersFromNamedPointsAction}
     * @param layer The layer this action is for
     */
    public MarkersFromNamedPointsAction(final GpxLayer layer) {
        super(tr("Markers From Named Points"), ImageProvider.get("addmarkers"));
        this.layer = layer;
        putValue("help", ht("/Action/MarkersFromNamedPoints"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GpxData namedTrackPoints = new GpxData();
        layer.data.getTrackPoints()
            .filter(point -> point.attr.containsKey("name") || point.attr.containsKey("desc"))
            .forEach(namedTrackPoints.waypoints::add);
        MarkerLayer ml = new MarkerLayer(namedTrackPoints, tr("Named Trackpoints from {0}", layer.getName()), layer.getAssociatedFile(), layer);
        if (!ml.data.isEmpty()) {
            MainApplication.getLayerManager().addLayer(ml);
        }
    }

}
