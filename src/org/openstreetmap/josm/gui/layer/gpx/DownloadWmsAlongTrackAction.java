// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractMergeAction.LayerListCellRenderer;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer.PrecacheTask;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.gui.progress.ProgressTaskIds;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * Class downloading WMS and TMS along the GPX track
 *
 */
public class DownloadWmsAlongTrackAction extends AbstractAction {

    private final transient GpxData data;

    /**
     * @param data that represents GPX track, along which data should be downloaded
     */
    public DownloadWmsAlongTrackAction(final GpxData data) {
        super(tr("Precache imagery tiles along this track"), ImageProvider.get("downloadalongtrack"));
        this.data = data;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final List<LatLon> points = new ArrayList<>();
        for (GpxTrack trk : data.tracks) {
            for (GpxTrackSegment segment : trk.getSegments()) {
                for (WayPoint p : segment.getWayPoints()) {
                    points.add(p.getCoor());
                }
            }
        }
        for (WayPoint p : data.waypoints) {
            points.add(p.getCoor());
        }
        final AbstractTileSourceLayer layer = askedLayer();
        if (layer != null) {
            PleaseWaitRunnable task = new PleaseWaitRunnable(tr("Precaching WMS")) {
                private PrecacheTask precacheTask;

                @Override
                protected void realRun() throws SAXException, IOException, OsmTransferException {
                    precacheTask = layer.new PrecacheTask(progressMonitor);
                    layer.downloadAreaToCache(precacheTask, points, 0, 0);
                    while (!precacheTask.isFinished() && !progressMonitor.isCanceled()) {
                        synchronized (this) {
                            try {
                                wait(200);
                            } catch (InterruptedException ex) {
                                Main.warn("InterruptedException in "+getClass().getSimpleName()+" while precaching WMS");
                            }
                        }
                    }
                }

                @Override
                protected void finish() {
                }

                @Override
                protected void cancel() {
                    precacheTask.cancel();
                }

                @Override
                public ProgressTaskId canRunInBackground() {
                    return ProgressTaskIds.PRECACHE_WMS;
                }
            };
            Main.worker.execute(task);
        }
    }

    protected AbstractTileSourceLayer askedLayer() {
        Collection<AbstractTileSourceLayer> targetLayers = Main.map.mapView.getLayersOfType(AbstractTileSourceLayer.class);
        if (targetLayers.isEmpty()) {
            warnNoImageryLayers();
            return null;
        }
        JosmComboBox<AbstractTileSourceLayer> layerList = new JosmComboBox<>(targetLayers.toArray(new AbstractTileSourceLayer[0]));
        layerList.setRenderer(new LayerListCellRenderer());
        layerList.setSelectedIndex(0);
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(new JLabel(tr("Please select the imagery layer.")), GBC.eol());
        pnl.add(layerList, GBC.eol());
        ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Select imagery layer"), new String[]{tr("Download"), tr("Cancel")});
        ed.setButtonIcons(new String[]{"dialogs/down", "cancel"});
        ed.setContent(pnl);
        ed.showDialog();
        if (ed.getValue() != 1) {
            return null;
        }
        return (AbstractTileSourceLayer) layerList.getSelectedItem();
    }

    protected void warnNoImageryLayers() {
        JOptionPane.showMessageDialog(Main.parent, tr("There are no imagery layers."), tr("No imagery layers"), JOptionPane.WARNING_MESSAGE);
    }
}
