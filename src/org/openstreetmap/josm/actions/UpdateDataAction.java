// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTaskList;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OnlineResource;
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
        putValue("help", ht("/Action/UpdateData"));
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null && !Main.isOffline(OnlineResource.OSM_API));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (!isEnabled() || editLayer == null)
            return;

        List<Area> areas = new ArrayList<>();
        for (DataSource ds : editLayer.data.getDataSources()) {
            areas.add(new Area(ds.bounds.asRect()));
        }

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

        List<Area> areasToDownload = new ArrayList<>();
        for (Area a : areas) {
            if (a.isEmpty()) {
                continue;
            }
            areasToDownload.add(a);
        }

        if (areasToDownload.isEmpty()) {
            // no bounds defined in the dataset? we update all primitives in the data set using a series of multi fetch requests
            UpdateSelectionAction.updatePrimitives(editLayer.data.allPrimitives());
        } else {
            // bounds defined? => use the bbox downloader
            final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
            final Future<?> future = new DownloadTaskList().download(false /* no new layer */, areasToDownload, true, false, monitor);
            waitFuture(future, monitor);
        }
    }
}
