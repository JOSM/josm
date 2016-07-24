// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.datatransfer.importers.FilePaster;

/**
 * This transfer handler allows to drop files to open them.
 *
 * @author Michael Zangl
 * @since 10620
 */
public class FileTransferHandler extends TransferHandler {

    private static final FilePaster filePaster = new FilePaster();

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return filePaster.supports(support);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            if (filePaster.supports(support)) {
                return filePaster.importData(support, null, null);
            }
        } catch (UnsupportedFlavorException | IOException e) {
            Main.warn(e, "Error while importing file.");
        }
        return super.importData(support);
    }
}
