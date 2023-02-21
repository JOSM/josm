// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.Options;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

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
        final Object data = support.getTransferable().getTransferData(df);
        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) data;
            OpenFileAction.OpenFileTask task = new OpenFileAction.OpenFileTask(files, null);
            task.setOptions(Options.RECORD_HISTORY);
            MainApplication.worker.submit(task);
            return true;
        }
        // We should never hit this code -- Coverity thinks that it is possible for this to be called with a
        // StringSelection transferable, which is not currently possible with our code. It *could* be done from
        // a plugin though.
        if (data instanceof Closeable) {
            Utils.close((Closeable) data);
        }
        throw new UnsupportedFlavorException(df);
    }
}
