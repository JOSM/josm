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
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.Shortcut;

public class UpdateDataAction extends JosmAction{
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
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    public void updateLayer(OsmDataLayer layer) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        if (getEditLayer() == null)
            return;

        List<Area> areas = new ArrayList<Area>();
        for(DataSource ds : getEditLayer().data.dataSources) {
            areas.add(new Area(ds.bounds.asRect()));
        }

        // The next two blocks removes every intersection from every DataSource Area
        // This prevents downloading the same data numerous times at intersections
        // and also skips smaller bounding boxes that are contained within larger ones
        // entirely.
        for(int i = 0; i < areas.size(); i++) {
            for(int j = i+1; j < areas.size(); j++) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        for(int i = areas.size()-1; i > 0 ; i--) {
            for(int j = i-1; j > 0; j--) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        List<Area> areasToDownload = new ArrayList<Area>();
        for(Area a : areas) {
            if(a.isEmpty()) {
                continue;
            }
            areasToDownload.add(a);
        }

        if(areasToDownload.isEmpty()) {
            // no bounds defined in the dataset? we update all primitives in the data set
            // using a series of multi fetch requests
            //
            UpdateSelectionAction.updatePrimitives(getEditLayer().data.allPrimitives());
        } else {
            // bounds defined? => use the bbox downloader
            //
            final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
            final Future<?> future = new DownloadTaskList().download(false /* no new layer */, areasToDownload, true, false, monitor);
            Main.worker.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                future.get();
                            } catch(Exception e) {
                                Main.error(e);
                                return;
                            }
                            monitor.close();
                        }
                    }
            );
        }
    }
}
