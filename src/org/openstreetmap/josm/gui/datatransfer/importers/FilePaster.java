// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This transfer support allows us to import a file that is dropped / copied on to the map.
 * @author Michael Zangl
 * @since 10604
 */
public final class FilePaster extends AbstractOsmDataPaster {
    /**
     * Create a new {@link FilePaster}
     */
    public FilePaster() {
        super(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth pasteAt)
            throws UnsupportedFlavorException, IOException {
        @SuppressWarnings("unchecked")
        List<File> files = (List<File>) support.getTransferable().getTransferData(df);
        OpenFileAction.OpenFileTask task = new OpenFileAction.OpenFileTask(files, null);
        task.setRecordHistory(true);
        MainApplication.worker.submit(task);
        return true;
    }
}
