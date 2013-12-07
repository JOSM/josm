// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class ConvertToDataLayerAction extends AbstractAction {
    private final GpxLayer layer;

    public ConvertToDataLayerAction(final GpxLayer layer) {
        super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
        this.layer = layer;
        putValue("help", ht("/Action/ConvertToDataLayer"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:</html>")), GBC.eol());
        msg.add(new UrlLabel(Main.OSM_WEBSITE + "/traces", 2), GBC.eop());
        if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION)) {
            return;
        }
        DataSet ds = new DataSet();
        for (GpxTrack trk : layer.data.tracks) {
            for (GpxTrackSegment segment : trk.getSegments()) {
                List<Node> nodes = new ArrayList<Node>();
                for (WayPoint p : segment.getWayPoints()) {
                    Node n = new Node(p.getCoor());
                    String timestr = p.getString("time");
                    if (timestr != null) {
                        n.setTimestamp(DateUtils.fromString(timestr));
                    }
                    ds.addPrimitive(n);
                    nodes.add(n);
                }
                Way w = new Way();
                w.setNodes(nodes);
                ds.addPrimitive(w);
            }
        }
        Main.main.addLayer(new OsmDataLayer(ds, tr("Converted from: {0}", layer.getName()), layer.getAssociatedFile()));
        Main.main.removeLayer(layer);
    }

}
