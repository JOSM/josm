// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.ImageProvider;

public class MarkersFromNamedPointsAction extends AbstractAction {
    private final GpxLayer layer;

    public MarkersFromNamedPointsAction(final GpxLayer layer) {
        super(tr("Markers From Named Points"), ImageProvider.get("addmarkers"));
        this.layer = layer;
        putValue("help", ht("/Action/MarkersFromNamedPoints"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GpxData namedTrackPoints = new GpxData();
        for (GpxTrack track : layer.data.tracks) {
            for (GpxTrackSegment seg : track.getSegments()) {
                for (WayPoint point : seg.getWayPoints()) {
                    if (point.attr.containsKey("name") || point.attr.containsKey("desc")) {
                        namedTrackPoints.waypoints.add(point);
                    }
                }
            }
        }
        MarkerLayer ml = new MarkerLayer(namedTrackPoints, tr("Named Trackpoints from {0}", layer.getName()), layer.getAssociatedFile(), layer);
        if (!ml.data.isEmpty()) {
            Main.main.addLayer(ml);
        }
    }

}
