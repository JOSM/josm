// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * An abstract action for a conversion from a {@code T} {@link Layer} to a {@link OsmDataLayer}.
 * @param <T> the source layer class
 */
public abstract class ConvertToDataLayerAction<T extends Layer> extends AbstractAction {
    protected final transient T layer;

    protected ConvertToDataLayerAction(final T layer) {
        super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
        this.layer = layer;
        putValue("help", ht("/Action/ConvertToDataLayer"));
    }

    /**
     * Converts a {@link GpxLayer} to a {@link OsmDataLayer}.
     */
    public static class FromGpxLayer extends ConvertToDataLayerAction<GpxLayer> {

        /**
         * Creates a new {@code FromGpxLayer}.
         * @param layer the source layer
         */
        public FromGpxLayer(GpxLayer layer) {
            super(layer);
        }

        @Override
        public DataSet convert() {
            final DataSet ds = new DataSet();
            for (GpxTrack trk : layer.data.tracks) {
                for (GpxTrackSegment segment : trk.getSegments()) {
                    List<Node> nodes = new ArrayList<>();
                    for (WayPoint p : segment.getWayPoints()) {
                        Node n = new Node(p.getCoor());
                        String timestr = p.getString(GpxConstants.PT_TIME);
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
            return ds;
        }
    }

    /**
     * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
     */
    public static class FromMarkerLayer extends ConvertToDataLayerAction<MarkerLayer> {

        /**
         * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
         */
        public FromMarkerLayer(MarkerLayer layer) {
            super(layer);
        }

        @Override
        public DataSet convert() {
            final DataSet ds = new DataSet();
            for (Marker marker : layer.data) {
                final Node node = new Node(marker.getCoor());
                node.put("name", marker.getText());
                ds.addPrimitive(node);
            }
            return ds;
        }
    }

    /**
     * Performs the conversion to a {@link DataSet}.
     * @return the resulting dataset
     */
    public abstract DataSet convert();

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(
                // CHECKSTYLE.OFF: LineLength
                tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:</html>")),
                // CHECKSTYLE.ON: LineLength
                GBC.eol());
        msg.add(new UrlLabel(Main.getOSMWebsite() + "/traces", 2), GBC.eop());
        if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION)) {
            return;
        }
        final DataSet ds = convert();
        final OsmDataLayer layer = new OsmDataLayer(ds, tr("Converted from: {0}", this.layer.getName()), null);
        layer.setAssociatedFile(new File(this.layer.getAssociatedFile().getParentFile(), this.layer.getAssociatedFile().getName() + ".osm"));
        layer.setUploadDiscouraged(true);
        Main.main.addLayer(layer);
        Main.main.removeLayer(this.layer);
    }
}
