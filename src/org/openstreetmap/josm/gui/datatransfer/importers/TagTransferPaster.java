// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;

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
        TagTransferData data = (TagTransferData) support.getTransferable().getTransferData(df);
        return data.getTags();
    }
}
