// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.downloadtasks.DownloadTaskList;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action synchronizes the dataset with the current state on the server.
 *
 * It does so by re-downloading all areas and thereby merging all compatible
 * changes from the current server version.
 */
public class UpdateDataAction extends JosmAction {

    /**
     * Constructs a new {@code UpdateDataAction}.
     */
    public UpdateDataAction() {
        super(tr("Update data"),
                "updatedata",
                tr("Updates the objects in the active data layer from the server."),
                Shortcut.registerShortcut("file:updatedata",
                        tr("File: {0}", tr("Update data")),
                        KeyEvent.VK_U, Shortcut.CTRL),
                true);
        setHelpId(ht("/Action/UpdateData"));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        setEnabled(editLayer != null && editLayer.isDownloadable() && !NetworkManager.isOffline(OnlineResource.OSM_API));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (!isEnabled() || editLayer == null || !editLayer.isDownloadable())
            return;

        List<Area> areas = editLayer.data.getDataSources().stream()
                .map(ds -> new Area(ds.bounds.asRect()))
                .collect(Collectors.toList());

        // The next two blocks removes every intersection from every DataSource Area
        // This prevents downloading the same data numerous times at intersections
        // and also skips smaller bounding boxes that are contained within larger ones entirely.
        for (int i = 0; i < areas.size(); i++) {
            for (int j = i+1; j < areas.size(); j++) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        for (int i = areas.size()-1; i > 0; i--) {
            for (int j = i-1; j > 0; j--) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        List<Area> areasToDownload = areas.stream()
                .filter(a -> !a.isEmpty())
                .collect(Collectors.toList());

        if (areasToDownload.isEmpty()) {
            // no bounds defined in the dataset? we update all primitives in the data set using a series of multi fetch requests
            UpdateSelectionAction.updatePrimitives(editLayer.data.allPrimitives());
        } else {
            // bounds defined? => use the bbox downloader
            final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
            final Future<?> future = new DownloadTaskList(Config.getPref().getBoolean("update.data.zoom-after-download"))
                    .download(false /* no new layer */, areasToDownload, true, false, monitor);
            waitFuture(future, monitor);
        }
    }
}
