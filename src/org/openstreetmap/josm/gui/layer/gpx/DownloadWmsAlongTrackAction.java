// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractMergeAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.gui.progress.ProgressTaskIds;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Class downloading WMS and TMS along the GPX track.
 * @since 5715
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

    static class PrecacheWmsTask extends PleaseWaitRunnable {

        private final AbstractTileSourceLayer<? extends AbstractTMSTileSource> layer;
        private final List<LatLon> points;
        private AbstractTileSourceLayer<? extends AbstractTMSTileSource>.PrecacheTask precacheTask;

        protected PrecacheWmsTask(AbstractTileSourceLayer<? extends AbstractTMSTileSource> layer, List<LatLon> points) {
            super(tr("Precaching WMS"));
            this.layer = layer;
            this.points = points;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            precacheTask = layer.downloadAreaToCache(progressMonitor, points, 0, 0);
            synchronized (this) {
                try {
                    while (!precacheTask.isFinished() && !progressMonitor.isCanceled()) {
                        wait(200);
                    }
                } catch (InterruptedException ex) {
                    Logging.warn("InterruptedException in "+getClass().getSimpleName()+" while precaching WMS");
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        protected void finish() {
            // Do nothing
        }

        @Override
        protected void cancel() {
            precacheTask.cancel();
        }

        @Override
        public ProgressTaskId canRunInBackground() {
            return ProgressTaskIds.PRECACHE_WMS;
        }
    }

    PrecacheWmsTask createTask() {
        List<LatLon> points = new ArrayList<>();
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
        AbstractTileSourceLayer<? extends AbstractTMSTileSource> layer = askedLayer();
        return layer != null ? new PrecacheWmsTask(layer, points) : null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrecacheWmsTask task = createTask();
        if (task != null) {
            Main.worker.execute(task);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected AbstractTileSourceLayer<? extends AbstractTMSTileSource> askedLayer() {
        List<AbstractTileSourceLayer> targetLayers = Main.getLayerManager().getLayersOfType(AbstractTileSourceLayer.class);
        if (targetLayers.isEmpty()) {
            if (!GraphicsEnvironment.isHeadless()) {
                warnNoImageryLayers();
            }
            return null;
        }
        return AbstractMergeAction.askTargetLayer(targetLayers.toArray(new AbstractTileSourceLayer[targetLayers.size()]),
                tr("Please select the imagery layer."),
                tr("Select imagery layer"),
                tr("Download"), "dialogs/down");
    }

    protected void warnNoImageryLayers() {
        JOptionPane.showMessageDialog(Main.parent, tr("There are no imagery layers."), tr("No imagery layers"), JOptionPane.WARNING_MESSAGE);
    }
}
