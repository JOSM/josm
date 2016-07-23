// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.TextTagParser;

/**
 * This transfer support allows us to import tags from the text that was copied to the clipboard.
 * @author Michael Zangl
 * @since 10604
 */
public final class TextTagPaster extends AbstractTagPaster {

    /**
     * Create a new {@link TextTagPaster}
     */
    public TextTagPaster() {
        super(DataFlavor.stringFlavor);
    }

    @Override
    public boolean supports(TransferSupport support) {
        try {
            return super.supports(support) && getTags(support) != null;
        } catch (UnsupportedFlavorException | IOException e) {
            Main.warn(e);
            return false;
        }
    }

    @Override
    protected Map<String, String> getTags(TransferSupport support) throws UnsupportedFlavorException, IOException {
        return TextTagParser.readTagsFromText((String) support.getTransferable().getTransferData(df));
    }
}
