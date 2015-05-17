// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.download.DownloadObjectDialog;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Download an OsmPrimitive by specifying type and ID
 *
 * @author Matthias Julius
 */
public class DownloadPrimitiveAction extends JosmAction {

    /**
     * Constructs a new {@code DownloadPrimitiveAction}.
     */
    public DownloadPrimitiveAction() {
        super(tr("Download object..."), "downloadprimitive", tr("Download OSM object by ID."),
                Shortcut.registerShortcut("system:download_primitive", tr("File: {0}", tr("Download object...")), KeyEvent.VK_O, Shortcut.CTRL_SHIFT), true);
        putValue("help", ht("/Action/DownloadObject"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DownloadObjectDialog dialog = new DownloadObjectDialog();
        if (dialog.showDialog().getValue() != dialog.getContinueButtonIndex()) return;

        processItems(dialog.isNewLayerRequested(), dialog.getOsmIds(), dialog.isReferrersRequested(), dialog.isFullRelationRequested());
    }

    /**
     * @param newLayer if the data should be downloaded into a new layer
     * @param ids List of primitive id to download
     * @param downloadReferrers if the referrers of the object should be downloaded as well, i.e., parent relations, and for nodes, additionally, parent ways
     * @param full if the members of a relation should be downloaded as well
     */
    public static void processItems(boolean newLayer, final List<PrimitiveId> ids, boolean downloadReferrers, boolean full) {
        final DownloadPrimitivesWithReferrersTask task =
                new DownloadPrimitivesWithReferrersTask(newLayer, ids, downloadReferrers, full, null, null);
        Main.worker.submit(task);
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                final List<PrimitiveId> downloaded = task.getDownloadedId();
                if(downloaded != null) {
                    GuiHelper.runInEDT(new Runnable() {
                        @Override
                        public void run() {
                            Main.main.getCurrentDataSet().setSelected(downloaded);
                        }
                    });
                }
            }
        });
    }
}
