// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.gpx.DownloadAlongPanel;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Calculate area around selected ways and split it into reasonable parts so
 * that they can be downloaded.
 *
 */
public class DownloadAlongWayAction extends DownloadAlongAction {

    private static final String PREF_DOWNLOAD_ALONG_WAY_DISTANCE = "downloadAlongWay.distance";
    private static final String PREF_DOWNLOAD_ALONG_WAY_AREA = "downloadAlongWay.area";

    private static final String PREF_DOWNLOAD_ALONG_WAY_OSM = "downloadAlongWay.download.osm";
    private static final String PREF_DOWNLOAD_ALONG_WAY_GPS = "downloadAlongWay.download.gps";

    /**
     * Create new {@link DownloadAlongWayAction}.
     */
    public DownloadAlongWayAction() {
        super(tr("Download along..."), "download_along_way", tr("Download OSM data along the selected ways."),
                Shortcut.registerShortcut("file:download_along", tr("File: {0}", tr("Download Along")),
                        KeyEvent.VK_D, Shortcut.ALT_SHIFT), true);
    }

    @Override
    protected PleaseWaitRunnable createTask() {
        Collection<Way> selectedWays = getLayerManager().getEditDataSet().getSelectedWays();

        if (selectedWays.isEmpty()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select 1 or more ways to download along"));
            return null;
        }

        final DownloadAlongPanel panel = new DownloadAlongPanel(
                PREF_DOWNLOAD_ALONG_WAY_OSM, PREF_DOWNLOAD_ALONG_WAY_GPS,
                PREF_DOWNLOAD_ALONG_WAY_DISTANCE, PREF_DOWNLOAD_ALONG_WAY_AREA, null);

        int ret = panel.showInDownloadDialog(tr("Download from OSM along selected ways"), HelpUtil.ht("/Tools/DownloadAlong"));
        if (0 != ret && 1 != ret) {
            return null;
        }

        // Convert OSM ways to Path2D
        Path2D alongPath = new Path2D.Double();
        for (Way way : selectedWays) {
            boolean first = true;
            for (Node p : way.getNodes()) {
                if (first) {
                    alongPath.moveTo(p.lon(), p.lat());
                    first = false;
                } else {
                    alongPath.lineTo(p.lon(), p.lat());
                }
            }
        }
        return createCalcTask(alongPath, panel, tr("Download from OSM along selected ways"), 1 == ret);
    }

    @Override
    protected void updateEnabledState() {
        if (getLayerManager().getEditDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getLayerManager().getEditDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection.stream().anyMatch(Way.class::isInstance));
    }
}
