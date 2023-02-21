// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;
import org.openstreetmap.josm.tools.Utils;

/**
 * This transfer support allows us to transfer tags from the copied primitives on to the selected ones.
 * @author Michael Zangl
 * @since 10604
 */
public final class TagTransferPaster extends AbstractTagPaster {
    /**
     * Create a new {@link TagTransferPaster}
     */
    public TagTransferPaster() {
        super(TagTransferData.FLAVOR);
    }

    @Override
    protected Map<String, String> getTags(TransferSupport support) throws UnsupportedFlavorException, IOException {
        final Object data = support.getTransferable().getTransferData(df);
        if (data instanceof TagTransferData) {
            return ((TagTransferData) data).getTags();
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
